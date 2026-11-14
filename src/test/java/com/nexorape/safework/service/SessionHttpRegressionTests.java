package com.nexorape.safework.service;
import com.nexorape.safework.service.iam.application.internal.commandservices.PasswordRecoveryService;
import com.nexorape.safework.service.iam.application.internal.outboundservices.hashing.HashingService;
import com.nexorape.safework.service.iam.domain.model.aggregates.*;
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
import java.net.URI;import java.net.http.*;import java.nio.file.*;
import java.nio.charset.StandardCharsets;import java.time.*;import java.util.*;import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.context.ContextConfiguration(initializers=LocalTestDatabaseGuard.class)
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SessionHttpRegressionTests {
    @LocalServerPort int port;
    @Autowired UserRepository users;@Autowired CompanyRepository companies;@Autowired RoleRepository roles;
    @Autowired UserSessionRepository sessions;@Autowired MembershipAuditRepository audits;
    @Autowired OperatorRecoveryRecordRepository recoveries;@Autowired PasswordRecoveryService recovery;
    @Autowired HashingService hashing;@Autowired BearerTokenService tokens;@Autowired PlatformTransactionManager transactions;
    @Value("${authorization.jwt.secret}") String signingKey;
    final JsonMapper json=JsonMapper.builder().build();final HttpClient client=HttpClient.newHttpClient();
    final List<String> evidence=Collections.synchronizedList(new ArrayList<>());
    final String password=UUID.randomUUID().toString();
    Company alpha;User employer,unrelated;
    record Result(int status,JsonNode body) { @Override public String toString(){return "HTTP result[redacted]";} }
    @BeforeAll void fixtures() throws Exception {
        alpha=companies.saveAndFlush(new Company("Synthetic Session "+UUID.randomUUID()));
        employer=user(Roles.EMPLOYER);unrelated=user(Roles.WORKER);
    }
    User user(Roles role) {return users.saveAndFlush(new User(alpha,"Synthetic Session User",new EmailAddress(UUID.randomUUID()+"@example.test"),hashing.encode(password),List.of(roles.findByName(role).orElseThrow())));}
    Result call(String label,String method,String path,String token,Object body) throws Exception {
        var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1"+path)).timeout(Duration.ofSeconds(20)).header("Content-Type","application/json");
        if(token!=null)req.header("Authorization","Bearer "+token);
        req.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        var response=client.send(req.build(),HttpResponse.BodyHandlers.ofString());evidence.add(label+"\t"+method+"\t"+path+"\t"+response.statusCode());
        return new Result(response.statusCode(),response.body().isBlank()?json.nullNode():json.readTree(response.body()));
    }
    void status(Result r,int expected) {assertEquals(expected,r.status,"HTTP body suppressed.");if(expected>=400){assertTrue(r.body.has("code"));assertTrue(r.body.has("requestId"));}}
    String login(User user) throws Exception {var r=call("login","POST","/authentication/sign-in",null,Map.of("email",user.getEmail(),"password",password));status(r,200);return r.body.get("token").asString();}
    void profile(String token,int code)throws Exception{status(call("profile","GET","/users/me",token,null),code);}
    void revoke(User user,String manager)throws Exception{status(call("revoke membership","DELETE","/companies/"+alpha.getId()+"/members/"+user.getId(),manager,null),200);}
    @Test void twoSessionsLogoutOnlyOneAndOtherUserIsUnaffected()throws Exception {
        var target=user(Roles.WORKER);var first=login(target);var second=login(target);var other=login(unrelated);
        assertNotEquals(first,second);status(call("logout","POST","/authentication/sign-out",first,null),204);
        profile(first,401);profile(second,200);profile(other,200);
        assertNotNull(sessions.findById(tokens.getSessionIdFromToken(first)).orElseThrow().getRevokedAt());
        assertNull(sessions.findById(tokens.getSessionIdFromToken(second)).orElseThrow().getRevokedAt());
    }
    @Test void membershipRevocationInvalidatesBothSessionsAndPreservesOtherUsers()throws Exception{
        var target=user(Roles.WORKER);var first=login(target);var second=login(target);var other=login(unrelated);var manager=login(employer);
        long before=audits.count();revoke(target,manager);profile(first,401);profile(second,401);profile(other,200);
        var fresh=login(target);profile(fresh,200);
        status(call("revoked member cannot read incidents","GET","/incidents",fresh,null),403);
        status(call("revoked member cannot read notifications","GET","/notifications/my-notifications",fresh,null),403);
        revoke(target,manager);profile(fresh,200);assertEquals(before+1,audits.count());
    }
    @Test void passwordRecoveryPreservesMembershipRevokesSessionsAndRecordsOperator()throws Exception{
        var target=user(Roles.WORKER);var first=login(target);var second=login(target);var other=login(unrelated);
        long before=recoveries.count();String replacement=UUID.randomUUID().toString();
        recovery.reset(target.getEmail(),replacement,"synthetic-test-operator");
        profile(first,401);profile(second,401);profile(other,200);
        status(call("old password rejected","POST","/authentication/sign-in",null,Map.of("email",target.getEmail(),"password",password)),401);
        var fresh=call("recovered login","POST","/authentication/sign-in",null,Map.of("email",target.getEmail(),"password",replacement));
        status(fresh,200);profile(fresh.body.get("token").asString(),200);
        assertEquals(alpha.getId(),users.findById(target.getId()).orElseThrow().getCompanyId());assertEquals(before+1,recoveries.count());
    }
    @Test void workerCannotManageMembersAndRemovedAdminRouteDoesNotGrantPrivileges()throws Exception{
        var target=user(Roles.WORKER);var token=login(target);
        status(call("worker cannot manage requests","GET","/companies/"+alpha.getId()+"/membership-requests",token,null),403);
        status(call("removed global role endpoint","PATCH","/administration/users/"+target.getId()+"/roles",token,Map.of("roles",List.of("ADMIN"))),404);
        profile(token,200);
    }
    @Test void concurrentLoginAndRevocationCannotLeaveStaleSessions()throws Exception{
        var target=user(Roles.WORKER);var old=login(target);var manager=login(employer);String concurrent;
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()){
            var login=executor.submit(()->login(target));var change=executor.submit(()->call("concurrent revoke","DELETE","/companies/"+alpha.getId()+"/members/"+target.getId(),manager,null));
            concurrent=login.get();status(change.get(),200);
        }
        profile(old,401);var result=call("concurrent session","GET","/users/me",concurrent,null);
        assertTrue(result.status==200||result.status==401);
        long version=users.findById(target.getId()).orElseThrow().getSecurityVersion();
        for(var session:sessions.findAll())if(session.getUserId().equals(target.getId())&&session.getRevokedAt()==null)assertEquals(version,session.getSecurityVersion());
    }
    @Test void signedAlteredUnknownExpiredAndStoredExpiredSessionsAreRejected()throws Exception{
        var target=user(Roles.WORKER);var valid=login(target);
        int index=valid.lastIndexOf('.')+1;String altered=valid.substring(0,index)+(valid.charAt(index)=='A'?'B':'A')+valid.substring(index+1);
        profile(altered,401);profile(signed(target,UUID.randomUUID(),Instant.now().minusSeconds(2),Instant.now().plusSeconds(300)),401);
        profile(signed(target,tokens.getSessionIdFromToken(valid),Instant.now().minusSeconds(600),Instant.now().minusSeconds(300)),401);
        new TransactionTemplate(transactions).executeWithoutResult(s->{
            var manager=org.springframework.orm.jpa.EntityManagerFactoryUtils.getTransactionalEntityManager(((org.springframework.orm.jpa.JpaTransactionManager)transactions).getEntityManagerFactory());
            manager.createQuery("update UserSession s set s.expiresAt=:expiry where s.id=:id").setParameter("expiry",Instant.now().minusSeconds(1)).setParameter("id",tokens.getSessionIdFromToken(valid)).executeUpdate();
        });profile(valid,401);
    }
    String signed(User user,UUID id,Instant issued,Instant expiry) {
        return io.jsonwebtoken.Jwts.builder().id(id.toString()).issuer("safework-backend").audience().add("safework-mobile").and().subject(user.getEmail()).claim("companyId",user.getCompanyId()).claim("userId",user.getId()).issuedAt(Date.from(issued)).expiration(Date.from(expiry)).signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(signingKey.getBytes(StandardCharsets.UTF_8)),io.jsonwebtoken.Jwts.SIG.HS512).compact();
    }
    @Test void openApiPublishesCurrentMembershipContract()throws Exception{
        var response=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/v3/api-docs")).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode());var api=json.readTree(response.body());
        assertNotNull(api.get("paths").get("/api/v1/companies/{companyId}/membership-requests"));
        assertNull(api.get("paths").get("/api/v1/administration/users/{userId}/roles"));
        assertTrue(api.get("paths").get("/api/v1/companies").get("post").get("responses").has("201"));
        assertTrue(api.get("paths").get("/api/v1/authentication/sign-up").get("post").get("responses").has("201"));
        assertTrue(api.get("paths").get("/api/v1/companies/{companyId}/membership-requests").get("post").get("responses").has("201"));
        assertTrue(api.get("paths").get("/api/v1/authentication/sign-out").get("post").get("responses").has("204"));
        var token=login(user(Roles.WORKER));var payload=json.readTree(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]),StandardCharsets.UTF_8));
        assertEquals(7*86400L,payload.get("exp").asLong()-payload.get("iat").asLong());
    }
    @AfterAll void report()throws Exception{
        var path=Path.of(".local","reports","session-http-results.tsv");Files.createDirectories(path.getParent());
        var lines=new ArrayList<String>();lines.add("scenario\tmethod\tpath\tstatus");lines.addAll(evidence);Files.write(path,lines);
    }
}
