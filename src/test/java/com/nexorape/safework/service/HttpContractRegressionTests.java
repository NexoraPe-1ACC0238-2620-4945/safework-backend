package com.nexorape.safework.service;

import com.nexorape.safework.service.iam.domain.model.aggregates.*;
import com.nexorape.safework.service.iam.domain.model.entities.CompanyInvitation;
import com.nexorape.safework.service.iam.domain.model.valueobjects.*;
import com.nexorape.safework.service.iam.domain.model.valueobjects.user.EmailAddress;
import com.nexorape.safework.service.iam.infrastructure.persistence.jpa.repositories.*;
import com.nexorape.safework.service.iam.application.internal.outboundservices.hashing.HashingService;
import com.nexorape.safework.service.iam.application.internal.commandservices.MembershipService;
import com.nexorape.safework.service.incidentmanagement.infrastructure.persistence.jpa.repositories.*;
import com.nexorape.safework.service.notificationmanagement.infrastructure.persistence.jpa.repositories.NotificationRepository;
import com.nexorape.safework.service.notificationmanagement.domain.model.entities.Notification;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real loopback HTTP and real configured MySQL; no mocked security or persistence. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.context.ContextConfiguration(initializers=LocalTestDatabaseGuard.class)
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HttpContractRegressionTests {
    @LocalServerPort int port;
    @Autowired CompanyRepository companies;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired CompanyInvitationRepository invitations;
    @Autowired IncidentRepository incidents;
    @Autowired AssignmentRepository assignments;
    @Autowired NotificationRepository notifications;
    @Autowired HashingService hashing;
    @Autowired PlatformTransactionManager transactions;
    @org.springframework.beans.factory.annotation.Value("\u0024{authorization.jwt.secret}") String signingKey;
    final JsonMapper json=JsonMapper.builder().build();
    final HttpClient client=HttpClient.newHttpClient();
    final List<String> evidence=Collections.synchronizedList(new ArrayList<>());
    final String password=UUID.randomUUID().toString();
    Company alpha,beta;
    User worker,employer,otherEmployer,foreignWorker,foreignEmployer,admin;
    String workerToken,employerToken,otherToken,foreignToken,foreignEmployerToken,adminToken;
    record Result(int status,JsonNode body) { @Override public String toString(){return "HTTP result[redacted]";} }
    @BeforeAll void fixture() throws Exception {
        alpha=companies.saveAndFlush(new Company("Synthetic Alpha "+UUID.randomUUID()));
        beta=companies.saveAndFlush(new Company("Synthetic Beta "+UUID.randomUUID()));
        worker=user(alpha,Roles.WORKER); employer=user(alpha,Roles.EMPLOYER);
        otherEmployer=user(alpha,Roles.EMPLOYER); foreignWorker=user(beta,Roles.WORKER);
        foreignEmployer=user(beta,Roles.EMPLOYER); admin=user(alpha,Roles.ADMIN);
        workerToken=login(worker); employerToken=login(employer); otherToken=login(otherEmployer);
        foreignToken=login(foreignWorker); foreignEmployerToken=login(foreignEmployer); adminToken=login(admin);
    }
    User user(Company company,Roles role) {
        return users.saveAndFlush(new User(company,"Synthetic User",new EmailAddress(UUID.randomUUID()+"@example.test"),
                hashing.encode(password),List.of(roles.findByName(role).orElseThrow())));
    }
    String login(User user) throws Exception {
        var result=call("fixture login","POST","/authentication/sign-in",null,Map.of("email",user.getEmail(),"password",password));
        status(result,200); return result.body.get("token").asString();
    }
    Result call(String label,String method,String route,String token,Object payload) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1"+route))
                .timeout(Duration.ofSeconds(20)).header("Content-Type","application/json");
        if(token!=null) builder.header("Authorization","Bearer "+token);
        builder.method(method,payload==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload)));
        var response=client.send(builder.build(),HttpResponse.BodyHandlers.ofString());
        evidence.add(label+"\t"+method+"\t"+route+"\t"+response.statusCode());
        return new Result(response.statusCode(),response.body().isBlank()?json.nullNode():json.readTree(response.body()));
    }
    void status(Result result,int expected) {
        assertEquals(expected,result.status,"Unexpected HTTP status; response body suppressed.");
        if(expected>=400) {
            assertTrue(result.body.has("code")); assertTrue(result.body.has("message"));
            assertTrue(result.body.has("fieldErrors")); assertTrue(result.body.has("requestId"));
        }
    }
    long create(String token,String title) throws Exception {
        var result=call("create incident","POST","/incidents",token,Map.of("title",title,"description","Synthetic description","location","Synthetic gate"));
        status(result,201); assertEquals(10,result.body.size()); return result.body.get("id").asLong();
    }
    String state(long id) { return incidents.findById(id).orElseThrow().getStatus(); }
    String snapshot(long id) {
        return new TransactionTemplate(transactions).execute(s -> {
            var incident=incidents.findById(id).orElseThrow();
            var a=incident.getAssignment();
            return incident.getStatus()+":"+(a==null?"none":a.getId()+":"+a.getUser().getId()+":"+a.getPriority()+":"+a.getCompletionDate())+":"+notifications.count();
        });
    }
    Map<String,Object> signup(String email,String proof) {
        return Map.of("fullName","Synthetic New Worker","emailAddress",email,"password",password,"invitationToken",proof);
    }
    String invite(String email) throws Exception {
        var result=call("issue company invitation","POST","/companies/"+alpha.getId()+"/invitations",employerToken,Map.of("emailAddress",email));
        status(result,201); return result.body.get("invitationToken").asString();
    }
    @Test void authenticationAndOwnProfile() throws Exception {
        status(call("missing token","GET","/users/me",null,null),401);
        var expired=io.jsonwebtoken.Jwts.builder().issuer("safework-backend").audience().add("safework-mobile").and()
            .subject(worker.getEmail()).claim("companyId",alpha.getId()).claim("userId",worker.getId())
            .issuedAt(Date.from(Instant.now().minusSeconds(600))).expiration(Date.from(Instant.now().minusSeconds(300)))
            .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(signingKey.getBytes(java.nio.charset.StandardCharsets.UTF_8)),io.jsonwebtoken.Jwts.SIG.HS512).compact();
        status(call("expired token","GET","/users/me",expired,null),401);
        status(call("invalid token","GET","/users/me","invalid",null),401);
        var wrong=call("incorrect login","POST","/authentication/sign-in",null,Map.of("email",worker.getEmail(),"password",UUID.randomUUID().toString()));
        status(wrong,401); assertEquals("INVALID_CREDENTIALS",wrong.body.get("code").asString());
        status(call("unknown account login","POST","/authentication/sign-in",null,Map.of("email",UUID.randomUUID()+"@example.test","password",password)),401);
        var profile=call("current profile","GET","/users/me",workerToken,null);
        status(profile,200); assertEquals(worker.getId().longValue(),profile.body.get("id").asLong());
        assertEquals(alpha.getId().longValue(),profile.body.get("companyId").asLong());
        assertNotNull(OffsetDateTime.parse(profile.body.get("createdAt").asString()));
        status(call("foreign user detail","GET","/users/"+foreignWorker.getId(),workerToken,null),404);
        var list=call("legacy user list","GET","/users",workerToken,null); status(list,200); assertEquals(1,list.body.size());
        status(call("foreign company detail","GET","/companies/"+beta.getId(),workerToken,null),404);
        status(call("profile update","PATCH","/users/me",workerToken,Map.of("fullName","Synthetic Updated","phoneNumber","+51 900 000 000")),200);
        status(call("profile role injection","PATCH","/users/me",workerToken,Map.of("roles",List.of("ADMIN"))),400);
        status(call("profile invalid phone","PATCH","/users/me",workerToken,Map.of("phoneNumber","abcd12345")),400);
    }
    @Test void invitationRegistrationAndSharedRoles() throws Exception {
        long roleCount=roles.count();
        var invalidEmail=UUID.randomUUID()+"@example.test"; var invalidProof=invite(invalidEmail);
        var shortPassword=new HashMap<String,Object>(signup(invalidEmail,invalidProof));shortPassword.put("password","x".repeat(11));
        status(call("short signup password","POST","/authentication/sign-up",null,shortPassword),400);
        var email=UUID.randomUUID()+"@example.test"; var proof=invite(email);
        var injection=new HashMap<String,Object>(signup(email,proof)); injection.put("roles",List.of("ADMIN","EMPLOYER")); injection.put("companyId",beta.getId());
        status(call("public privilege and company injection","POST","/authentication/sign-up",null,injection),400);
        status(call("invite email mismatch","POST","/authentication/sign-up",null,signup(UUID.randomUUID()+"@example.test",proof)),422);
        var created=call("invitation registration","POST","/authentication/sign-up",null,signup(email,proof));
        status(created,201); assertEquals(alpha.getId().longValue(),created.body.get("companyId").asLong());
        assertEquals(1,created.body.get("roles").size()); assertEquals("WORKER",created.body.get("roles").get(0).asString());
        status(call("invite replay","POST","/authentication/sign-up",null,signup(email,proof)),422);
        status(call("missing invite","POST","/authentication/sign-up",null,Map.of("fullName","Synthetic","emailAddress",UUID.randomUUID()+"@example.test","password",password)),422);
        var expired=UUID.randomUUID().toString();
        invitations.saveAndFlush(new CompanyInvitation(alpha,email,MembershipService.digest(expired),Instant.now().minusSeconds(5)));
        status(call("expired invite","POST","/authentication/sign-up",null,signup(email,expired)),422);
        var email2=UUID.randomUUID()+"@example.test";
        status(call("second worker registration","POST","/authentication/sign-up",null,signup(email2,invite(email2))),201);
        assertEquals(roleCount,roles.count());
        status(call("worker cannot issue invite","POST","/companies/"+alpha.getId()+"/invitations",workerToken,Map.of("emailAddress",email)),403);
        status(call("foreign employer cannot invite","POST","/companies/"+alpha.getId()+"/invitations",foreignEmployerToken,Map.of("emailAddress",email)),404);
    }
    @Test void selfAssignmentAndResponsibleLifecycle() throws Exception {
        long id=create(workerToken,"Lifecycle synthetic hazard");
        var visible=call("own-company incident detail","GET","/incidents/"+id,workerToken,null);
        status(visible,200);assertEquals(10,visible.body.size());
        String base=snapshot(id);
        status(call("close OPEN","POST","/incidents/"+id+"/close",employerToken,null),409); assertEquals(base,snapshot(id));
        status(call("start OPEN","POST","/incidents/"+id+"/start",employerToken,null),409); assertEquals(base,snapshot(id));
        status(call("worker cannot take","POST","/assignments",workerToken,Map.of("incidentId",id)),403); assertEquals(base,snapshot(id));
        status(call("foreign employer cannot take","POST","/assignments",foreignEmployerToken,Map.of("incidentId",id)),404); assertEquals(base,snapshot(id));
        status(call("cannot select assignee","POST","/assignments",employerToken,Map.of("incidentId",id,"userId",otherEmployer.getId())),400); assertEquals(base,snapshot(id));
        status(call("reject fractional incident ID","POST","/assignments",employerToken,Map.of("incidentId",id+0.5)),400); assertEquals(base,snapshot(id));
        status(call("reject string incident ID","POST","/assignments",employerToken,Map.of("incidentId",Long.toString(id))),400); assertEquals(base,snapshot(id));
        var taken=call("employer self assignment","POST","/assignments",employerToken,Map.of("incidentId",id));
        status(taken,201); long assignment=taken.body.get("id").asLong();
        assertEquals(employer.getId().longValue(),taken.body.get("userId").asLong());
        assertEquals("ASSIGNED",state(id)); base=snapshot(id);
        status(call("other responsible start","POST","/incidents/"+id+"/start",otherToken,null),403); assertEquals(base,snapshot(id));
        status(call("worker start","POST","/incidents/"+id+"/start",workerToken,null),403); assertEquals(base,snapshot(id));
        status(call("foreign start","POST","/incidents/"+id+"/start",foreignToken,null),404); assertEquals(base,snapshot(id));
        status(call("foreign close","POST","/incidents/"+id+"/close",foreignEmployerToken,null),404); assertEquals(base,snapshot(id));
        status(call("close ASSIGNED","POST","/incidents/"+id+"/close",employerToken,null),409); assertEquals(base,snapshot(id));
        status(call("foreign assignment detail","GET","/assignments/"+assignment,foreignToken,null),404);
        status(call("other assignment detail","GET","/assignments/"+assignment,otherToken,null),404);
        status(call("foreign assignment priority","PATCH","/assignments/"+assignment+"/priority",foreignEmployerToken,Map.of("priority","HIGH")),404); assertEquals(base,snapshot(id));
        status(call("other assignment priority","PATCH","/assignments/"+assignment+"/priority",otherToken,Map.of("priority","HIGH")),403); assertEquals(base,snapshot(id));
        status(call("reject start actor override","POST","/incidents/"+id+"/start",employerToken,Map.of("userId",otherEmployer.getId())),400); assertEquals(base,snapshot(id));
        status(call("responsible start","POST","/incidents/"+id+"/start",employerToken,null),200);
        assertEquals("IN_PROGRESS",state(id)); base=snapshot(id);
        status(call("take IN_PROGRESS","POST","/assignments",employerToken,Map.of("incidentId",id)),409); assertEquals(base,snapshot(id));
        status(call("other responsible close","POST","/incidents/"+id+"/close",otherToken,null),403); assertEquals(base,snapshot(id));
        status(call("responsible close","POST","/incidents/"+id+"/close",employerToken,null),200);
        assertEquals("CLOSED",state(id));
        var detail=call("completed assignment","GET","/assignments/"+assignment,employerToken,null);
        status(detail,200); assertNotNull(OffsetDateTime.parse(detail.body.get("completionDate").asString()));
        base=snapshot(id);
        status(call("take CLOSED","POST","/assignments",employerToken,Map.of("incidentId",id)),409); assertEquals(base,snapshot(id));
        status(call("repeat close","POST","/incidents/"+id+"/close",employerToken,null),409); assertEquals(base,snapshot(id));
    }
    @Test void companyScopeNotificationsAndStorageLimits() throws Exception {
        long id=create(foreignToken,"Foreign synthetic incident");
        status(call("foreign incident detail","GET","/incidents/"+id,workerToken,null),404);
        status(call("foreign document update","PATCH","/incidents/"+id+"/document",employerToken,Map.of("documentUrl","https://example.test/document")),404);
        var list=call("same company list","GET","/incidents",workerToken,null); status(list,200);
        for(var item:list.body) assertEquals(alpha.getId().longValue(),item.get("companyId").asLong());
        status(call("ADMIN no global business access","GET","/incidents",adminToken,null),403);
        status(call("reject company query selector","GET","/incidents?companyId="+beta.getId(),workerToken,null),400);
        status(call("maximum storage lengths","POST","/incidents",workerToken,Map.of("title","t".repeat(120),"description","d".repeat(4000),"location","l".repeat(500))),201);
        String supplementary=new String(Character.toChars(0x1F6A7));
        status(call("Unicode code point boundaries","POST","/incidents",workerToken,Map.of("title",supplementary.repeat(120),"description",supplementary.repeat(4000),"location",supplementary.repeat(500))),201);
        status(call("Unicode point overflow","POST","/incidents",workerToken,Map.of("title",supplementary.repeat(121),"description","Synthetic","location","Gate")),400);
        status(call("Unicode profile boundary","PATCH","/users/me",workerToken,Map.of("fullName",supplementary.repeat(120))),200);
        status(call("misplaced phone plus","PATCH","/users/me",workerToken,Map.of("phoneNumber","900+000000")),400);
        status(call("blank phone","PATCH","/users/me",workerToken,Map.of("phoneNumber","        ")),400);
        long count=incidents.count();
        status(call("blank title","POST","/incidents",workerToken,Map.of("title"," ","description","desc","location","gate")),400);
        status(call("title 121","POST","/incidents",workerToken,Map.of("title","t".repeat(121),"description","desc","location","gate")),400);
        assertEquals(count,incidents.count());
        var own=notifications.saveAndFlush(new Notification(worker.getId().toString(),"Own synthetic notice","Synthetic body",alpha.getId()));
        var foreign=notifications.saveAndFlush(new Notification(foreignWorker.getId().toString(),"Foreign synthetic notice","Synthetic body",beta.getId()));
        var stale=notifications.saveAndFlush(new Notification(worker.getId().toString(),"Prior company synthetic notice","Synthetic body",beta.getId()));
        var messages=call("recipient notifications","GET","/notifications/my-notifications",workerToken,null); status(messages,200);
        boolean found=false;
        for(var item:messages.body) {
            var notification=notifications.findById(UUID.fromString(item.get("id").asString())).orElseThrow();
            assertEquals(worker.getId().toString(),notification.getRecipientId()); assertEquals(alpha.getId(),notification.getCompanyId());
            assertNotNull(OffsetDateTime.parse(item.get("createdAt").asString())); assertFalse(item.get("isRead").asBoolean());
            if(item.get("id").asString().equals(own.getId().toString())) found=true;
            assertNotEquals(foreign.getId().toString(),item.get("id").asString()); assertNotEquals(stale.getId().toString(),item.get("id").asString());
        }
        assertTrue(found);
        status(call("reject recipient selector","GET","/notifications/my-notifications?recipientId="+foreignWorker.getId(),workerToken,null),400);
    }
    @Test void concurrentSelfAssignmentHasSingleWinner() throws Exception {
        long id=create(workerToken,"Concurrent synthetic hazard");
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            var first=executor.submit(()->call("concurrent take A","POST","/assignments",employerToken,Map.of("incidentId",id)));
            var second=executor.submit(()->call("concurrent take B","POST","/assignments",otherToken,Map.of("incidentId",id)));
            var codes=new ArrayList<>(List.of(first.get().status,second.get().status)); Collections.sort(codes);
            assertEquals(List.of(201,409),codes);
            assertEquals("ASSIGNED",state(id));
        }
    }
    @Test void currentAccountAndMembershipAreAuthoritative() throws Exception {
        var account=user(alpha,Roles.WORKER); var bearer=login(account);
        account.setEnabled(false); users.saveAndFlush(account);
        status(call("disabled account token","GET","/users/me",bearer,null),401);
        account.setEnabled(true); account.setCompany(beta); users.saveAndFlush(account);
        status(call("changed company token","GET","/users/me",bearer,null),401);
        var manager=user(alpha,Roles.EMPLOYER); var managerToken=login(manager);
        manager.setRoles(new HashSet<>(List.of(roles.findByName(Roles.WORKER).orElseThrow()))); users.saveAndFlush(manager);
        long id=create(workerToken,"Role removal synthetic hazard");
        var base=snapshot(id);
        status(call("removed employer cannot take","POST","/assignments",managerToken,Map.of("incidentId",id)),401);
        assertEquals(base,snapshot(id));
    }
    @Test void duplicateEmailRollsBackInvitationConsumption() throws Exception {
        String proof=invite(worker.getEmail());
        status(call("duplicate registered email","POST","/authentication/sign-up",null,signup(worker.getEmail(),proof)),409);
        var record=new TransactionTemplate(transactions).execute(s -> invitations.lockByDigest(MembershipService.digest(proof)).orElseThrow());
        assertNull(record.getConsumedAt());
    }
    @Test void concurrentInvitationHasSingleConsumer() throws Exception {
        String email=UUID.randomUUID()+"@example.test"; String proof=invite(email);
        long before=users.count();long roleCount=roles.count();
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            var a=executor.submit(()->call("concurrent registration A","POST","/authentication/sign-up",null,signup(email,proof)));
            var b=executor.submit(()->call("concurrent registration B","POST","/authentication/sign-up",null,signup(email,proof)));
            var codes=new ArrayList<>(List.of(a.get().status,b.get().status));Collections.sort(codes);
            assertEquals(List.of(201,422),codes);
        }
        assertEquals(before+1,users.count());assertEquals(roleCount,roles.count());
    }
    @Test @Order(Integer.MAX_VALUE) void authRateLimitReturnsSanitizedError() throws Exception {
        boolean limited=false;
        for(int i=0;i<61;i++) {
            var response=call("authentication rate limit probe","GET","/authentication/sign-in",null,null);
            if(response.status==429) {status(response,429); assertEquals("RATE_LIMITED",response.body.get("code").asString());limited=true;break;}
            status(response,405);
        }
        assertTrue(limited);
    }
    @AfterAll void writeSafeEvidence() throws Exception {
        var report=Path.of(".local","reports","http-regression-results.tsv");
        Files.createDirectories(report.getParent());
        var lines=new ArrayList<String>(); lines.add("scenario\tmethod\troute\tstatus"); lines.addAll(evidence);
        Files.write(report,lines);
    }
}
