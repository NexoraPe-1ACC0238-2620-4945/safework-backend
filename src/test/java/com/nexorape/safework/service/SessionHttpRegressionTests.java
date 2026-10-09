package com.nexorape.safework.service;

import com.nexorape.safework.service.iam.application.internal.outboundservices.hashing.HashingService;
import com.nexorape.safework.service.iam.domain.model.aggregates.*;
import com.nexorape.safework.service.iam.domain.model.entities.UserSession;
import com.nexorape.safework.service.iam.domain.model.valueobjects.*;
import com.nexorape.safework.service.iam.domain.model.valueobjects.user.EmailAddress;
import com.nexorape.safework.service.iam.infrastructure.persistence.jpa.repositories.*;
import com.nexorape.safework.service.iam.infrastructure.tokens.jwt.BearerTokenService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.context.ContextConfiguration(initializers=LocalTestDatabaseGuard.class)
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SessionHttpRegressionTests {
    @LocalServerPort int port;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired CompanyRepository companies;
    @Autowired UserSessionRepository sessions;
    @Autowired AdministrationAuditRepository audits;
    @Autowired HashingService hashing;
    @Autowired BearerTokenService tokens;
    @Autowired PlatformTransactionManager transactions;
    @Value("\u0024{authorization.jwt.secret}") String signingKey;
    final JsonMapper json=JsonMapper.builder().build();
    final HttpClient client=HttpClient.newHttpClient();
    final List<String> evidence=Collections.synchronizedList(new ArrayList<>());
    final String password=UUID.randomUUID().toString();
    Company alpha,beta;
    User admin,restartUser,unrelatedUser;
    String adminToken;
    record Result(int status,JsonNode body) { @Override public String toString(){return "Result[redacted]";} }
    @BeforeAll void fixtures() throws Exception {
        alpha=companies.saveAndFlush(new Company("Synthetic Session Alpha "+UUID.randomUUID()));
        beta=companies.saveAndFlush(new Company("Synthetic Session Beta "+UUID.randomUUID()));
        admin=user(Roles.ADMIN);restartUser=user(Roles.WORKER);unrelatedUser=user(Roles.WORKER);
        adminToken=login(admin);
        // Client fixture credentials only; never a JWT. Private test file, not application persistence.
        var fixture=Path.of(".local","runtime","live-fixtures.json");
        Files.writeString(fixture,json.writeValueAsString(Map.of("adminEmail",admin.getEmail(),
                "userEmail",restartUser.getEmail(),"otherEmail",unrelatedUser.getEmail(),
                "password",password,"companyId",alpha.getId(),"otherCompanyId",beta.getId())));
    }
    User user(Roles role) {
        return users.saveAndFlush(new User(alpha,"Synthetic Session User",new EmailAddress(UUID.randomUUID()+"@example.test"),
                hashing.encode(password),List.of(roles.findByName(role).orElseThrow())));
    }
    Result call(String label,String method,String path,String bearer,Object payload) throws Exception {
        var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1"+path))
                .timeout(Duration.ofSeconds(20)).header("Content-Type","application/json");
        if(bearer!=null)req.header("Authorization","Bearer "+bearer);
        req.method(method,payload==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload)));
        var response=client.send(req.build(),HttpResponse.BodyHandlers.ofString());
        evidence.add(label+"\t"+method+"\t"+path+"\t"+response.statusCode());
        return new Result(response.statusCode(),response.body().isBlank()?json.nullNode():json.readTree(response.body()));
    }
    void status(Result result,int code) {
        assertEquals(code,result.status,"Unexpected HTTP status; sensitive body suppressed.");
        if(code>=400) {
            assertTrue(result.body.has("requestId"));assertTrue(result.body.has("fieldErrors"));
            if(code==401)assertEquals("SESSION_INVALID",result.body.get("code").asString());
            if(code==403)assertEquals("ROLE_FORBIDDEN",result.body.get("code").asString());
        }
    }
    String login(User user) throws Exception {
        var result=call("session login","POST","/authentication/sign-in",null,Map.of("email",user.getEmail(),"password",password));
        assertEquals(200,result.status,"Login response body suppressed.");
        return result.body.get("token").asString();
    }
    void profile(String label,String token,int expected) throws Exception {status(call(label,"GET","/users/me",token,null),expected);}
    void assertRevoked(String token) {
        assertNotNull(sessions.findById(tokens.getSessionIdFromToken(token)).orElseThrow().getRevokedAt());
    }
    @Test void twoSessionsLogoutOnlyOneAndOtherUserIsUnaffected() throws Exception {
        var target=user(Roles.WORKER);
        String first=login(target),second=login(target),other=login(unrelatedUser);
        assertFalse(first.equals(second),"Independent session tokens must differ.");
        assertNotEquals(tokens.getSessionIdFromToken(first),tokens.getSessionIdFromToken(second));
        profile("first active",first,200);profile("second active",second,200);
        status(call("logout one session","POST","/authentication/sign-out",first,null),204);
        profile("logged out session",first,401);profile("other session retained",second,200);
        profile("other user retained",other,200);
        assertRevoked(first);assertNull(sessions.findById(tokens.getSessionIdFromToken(second)).orElseThrow().getRevokedAt());
        status(call("logout already revoked","POST","/authentication/sign-out",first,null),401);
    }
    @Test void administrativeRoleChangeRevokesBothSessionsAndPreservesOthers() throws Exception {
        assertChangeRevokesBoth("roles",Map.of("roles",List.of("WORKER","EMPLOYER")));
    }
    @Test void administrativeCompanyChangeRevokesBothSessions() throws Exception {
        assertChangeRevokesBoth("membership",Map.of("companyId",beta.getId()));
    }
    @Test void administrativeDisableRevokesBothSessions() throws Exception {
        assertChangeRevokesBoth("enabled",Map.of("enabled",false));
    }
    void assertChangeRevokesBoth(String operation,Object body) throws Exception {
        var target=user(Roles.WORKER);
        String a=login(target),b=login(target),other=login(unrelatedUser);
        long auditBefore=audits.count();
        status(call("administrative "+operation,"PATCH","/administration/users/"+target.getId()+"/"+operation,adminToken,body),200);
        profile(operation+" invalidates first",a,401);profile(operation+" invalidates second",b,401);
        profile(operation+" preserves unrelated user",other,200);
        assertRevoked(a);assertRevoked(b);assertEquals(auditBefore+1,audits.count());
    }
    @Test void validSessionWithoutAdminPermissionReturns403WithoutRevocation() throws Exception {
        var target=user(Roles.WORKER);String token=login(target);
        status(call("negative legacy user ID","GET","/users/-1",token,null),400);
        status(call("negative assignment ID","GET","/assignments/-1",token,null),400);
        long auditBefore=audits.count();
        status(call("worker cannot grant roles","PATCH","/administration/users/"+restartUser.getId()+"/roles",token,Map.of("roles",List.of("EMPLOYER"))),403);
        profile("denied worker session remains valid",token,200);
        assertEquals(auditBefore,audits.count());
        status(call("ADMIN grant prohibited","PATCH","/administration/users/"+target.getId()+"/roles",adminToken,Map.of("roles",List.of("ADMIN"))),400);
        profile("rejected privilege change preserves session",token,200);
    }
    @Test void identicalAdministrativeUpdateDoesNotInvalidateSession() throws Exception {
        var target=user(Roles.WORKER);var token=login(target);
        long auditBefore=audits.count();
        status(call("unchanged roles","PATCH","/administration/users/"+target.getId()+"/roles",adminToken,Map.of("roles",List.of("WORKER"))),200);
        profile("unchanged roles preserve session",token,200);assertEquals(auditBefore,audits.count());
    }
    @Test void expiredAlteredMissingSessionAndExpiredDatabaseSessionAreRejected() throws Exception {
        var target=user(Roles.WORKER);var valid=login(target);
        int index=valid.lastIndexOf('.')+1;
        String altered=valid.substring(0,index)+(valid.charAt(index)=='A'?'B':'A')+valid.substring(index+1);
        profile("altered signature",altered,401);
        profile("signed unknown session",signed(target,UUID.randomUUID(),Instant.now().minusSeconds(2),Instant.now().plusSeconds(300)),401);
        profile("expired JWT",signed(target,tokens.getSessionIdFromToken(valid),Instant.now().minusSeconds(600),Instant.now().minusSeconds(300)),401);
        new TransactionTemplate(transactions).executeWithoutResult(s -> {
            var record=sessions.findById(tokens.getSessionIdFromToken(valid)).orElseThrow();
            var manager=org.springframework.orm.jpa.EntityManagerFactoryUtils.getTransactionalEntityManager(
                    ((org.springframework.orm.jpa.JpaTransactionManager)transactions).getEntityManagerFactory());
            manager.createQuery("update UserSession s set s.expiresAt=:expiry where s.id=:id")
                    .setParameter("expiry",Instant.now().minusSeconds(1)).setParameter("id",record.getId()).executeUpdate();
        });
        profile("expired stored session with signed live JWT",valid,401);
        assertFalse(Arrays.stream(UserSession.class.getDeclaredFields()).anyMatch(f -> f.getName().equals("token") || f.getName().equals("password")));
    }
    String signed(User user,UUID id,Instant issued,Instant expiry) {
        return io.jsonwebtoken.Jwts.builder().id(id.toString()).issuer("safework-backend").audience().add("safework-mobile").and()
            .subject(user.getEmail()).claim("companyId",user.getCompanyId()).claim("userId",user.getId())
            .issuedAt(Date.from(issued)).expiration(Date.from(expiry))
            .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(signingKey.getBytes(StandardCharsets.UTF_8)),io.jsonwebtoken.Jwts.SIG.HS512).compact();
    }
    @Test void concurrentLoginAndRoleChangeCannotLeaveStaleActiveSessions() throws Exception {
        var target=user(Roles.WORKER);var old=login(target);
        String concurrent;
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            var login=executor.submit(()->login(target));
            var change=executor.submit(()->call("concurrent role change","PATCH","/administration/users/"+target.getId()+"/roles",adminToken,Map.of("roles",List.of("EMPLOYER"))));
            concurrent=login.get();status(change.get(),200);
        }
        profile("old session after concurrent role change",old,401);
        var result=call("concurrent login session outcome","GET","/users/me",concurrent,null);
        assertTrue(result.status==200 || result.status==401,"Concurrent session must be current or invalid.");
        if(result.status==200)assertEquals("EMPLOYER",result.body.get("roles").get(0).asString());
        long currentVersion=users.findById(target.getId()).orElseThrow().getSecurityVersion();
        for(var session:sessions.findAll())
            if(session.getUserId().equals(target.getId()) && session.getRevokedAt()==null)assertEquals(currentVersion,session.getSecurityVersion());
    }
    @Test void openApiPublishesSessionAndErrorContract() throws Exception {
        var response=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/v3/api-docs")).GET().build(),HttpResponse.BodyHandlers.ofString());
        evidence.add("session OpenAPI\tGET\t/v3/api-docs\t"+response.statusCode());
        assertEquals(200,response.statusCode());
        var api=json.readTree(response.body());
        var logout=api.get("paths").get("/api/v1/authentication/sign-out").get("post");
        assertTrue(logout.get("responses").has("204"));assertTrue(logout.get("responses").has("401"));
        assertEquals("#/components/schemas/ApiError",logout.get("responses").get("401").get("content").get("application/json").get("schema").get("$ref").asString());
        assertTrue(api.get("components").get("securitySchemes").get("bearerAuth").get("description").asString().contains("jti"));
        assertTrue(api.get("paths").get("/api/v1/administration/users/{userId}/roles").get("patch").get("responses").has("403"));
        var target=user(Roles.WORKER);var token=login(target);
        var payload=json.readTree(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]),StandardCharsets.UTF_8));
        assertEquals(7*86400L,payload.get("exp").asLong()-payload.get("iat").asLong());
        assertEquals(tokens.getSessionIdFromToken(token).toString(),payload.get("jti").asString());
    }
    @AfterAll void evidence() throws Exception {
        var path=Path.of(".local","reports","session-http-results.tsv");
        Files.createDirectories(path.getParent());var lines=new ArrayList<String>();
        lines.add("scenario\tmethod\tpath\tstatus");lines.addAll(evidence);Files.write(path,lines);
    }
}
