package com.nexorape.safework.service;

import com.nexorape.safework.service.iam.domain.model.aggregates.*;

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
class OnboardingHttpTests {
    @LocalServerPort int port;
    @Autowired CompanyRepository companies;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;

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
        foreignEmployer=user(beta,Roles.EMPLOYER); admin=user(null,Roles.WORKER);
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
        return Map.of("fullName","Synthetic New Worker","emailAddress",email,"password",password);
    }
    @Test void unassignedAccountCanLogInEditProfileAndSearchButCannotReadCompanyData() throws Exception {
        var account=user(null,Roles.WORKER);var token=login(account);
        var me=call("unassigned profile","GET","/users/me",token,null);status(me,200);assertTrue(me.body.get("companyId").isNull());
        status(call("unassigned edit","PATCH","/users/me",token,Map.of("fullName","Synthetic Unassigned")),200);
        status(call("unassigned incident access","GET","/incidents",token,null),403);
        status(call("unassigned notifications","GET","/notifications/my-notifications",token,null),403);
        var search=call("minimal company search","GET","/companies?search=Synthetic&page=0&size=5",token,null);
        status(search,200);assertTrue(search.body.size()<=5);
        for(var company:search.body){assertEquals(2,company.size());assertTrue(company.has("id"));assertTrue(company.has("name"));}
        status(call("invalid page size","GET","/companies?size=0",token,null),400);
        status(call("unknown discovery parameter","GET","/companies?members=true",token,null),400);
    }
    @Test void creatingCompanyNeedsNoAdminAndBindsAccountPermanently()throws Exception {
        var account=user(null,Roles.WORKER);var token=login(account);
        var created=call("create company without ADMIN","POST","/companies",token,Map.of("name","Synthetic Self Service "+UUID.randomUUID()));
        status(created,201);assertEquals(2,created.body.size());
        status(call("old pre-company session revoked","GET","/users/me",token,null),401);
        var fresh=login(account);var me=call("creator profile","GET","/users/me",fresh,null);status(me,200);
        assertEquals("EMPLOYER",me.body.get("roles").get(0).asString());assertEquals("ACTIVE",me.body.get("membershipStatus").asString());
        status(call("second company denied","POST","/companies",fresh,Map.of("name","Synthetic forbidden")),409);
        status(call("joining second company denied","POST","/companies/"+alpha.getId()+"/membership-requests",fresh,null),409);
    }
    @Test void pendingRequestDoesNotGrantAccessAndApprovalIsCompanyScoped()throws Exception{
        var account=user(null,Roles.WORKER);var token=login(account);
        var request=call("request membership","POST","/companies/"+alpha.getId()+"/membership-requests",token,null);status(request,201);
        long id=request.body.get("id").asLong();
        status(call("duplicate pending request","POST","/companies/"+alpha.getId()+"/membership-requests",token,null),409);
        status(call("other pending company denied","POST","/companies/"+beta.getId()+"/membership-requests",token,null),409);
        status(call("pending cannot read incidents","GET","/incidents",token,null),403);
        status(call("pending cannot read notifications","GET","/notifications/my-notifications",token,null),403);
        var own=call("own request history","GET","/users/me/membership-requests",token,null);status(own,200);assertEquals(id,own.body.get(0).get("id").asLong());
        status(call("foreign employer cannot approve","POST","/companies/"+alpha.getId()+"/membership-requests/"+id+"/accept",foreignEmployerToken,null),404);
        status(call("worker cannot approve","POST","/companies/"+alpha.getId()+"/membership-requests/"+id+"/accept",workerToken,null),403);
        status(call("correct employer accepts","POST","/companies/"+alpha.getId()+"/membership-requests/"+id+"/accept",employerToken,null),200);
        status(call("pre-approval session invalidated","GET","/users/me",token,null),401);
        var fresh=login(account);status(call("approved worker reads incidents","GET","/incidents",fresh,null),200);
        status(call("approved worker notifications","GET","/notifications/my-notifications",fresh,null),200);
        status(call("repeat decision rejected","POST","/companies/"+alpha.getId()+"/membership-requests/"+id+"/accept",employerToken,null),409);
        status(call("approved cannot join second company","POST","/companies/"+beta.getId()+"/membership-requests",fresh,null),409);
        var list=call("employer request list","GET","/companies/"+alpha.getId()+"/membership-requests",employerToken,null);status(list,200);
        for(var item:list.body)assertEquals(alpha.getId().longValue(),item.get("companyId").asLong());
    }
    @Test void rejectionAllowsAnotherRequestWithoutGrantingAccess()throws Exception{
        var account=user(null,Roles.WORKER);var token=login(account);
        var request=call("request for rejection","POST","/companies/"+alpha.getId()+"/membership-requests",token,null);status(request,201);
        long id=request.body.get("id").asLong();
        status(call("reject request","POST","/companies/"+alpha.getId()+"/membership-requests/"+id+"/reject",employerToken,null),200);
        status(call("rejected profile still usable","GET","/users/me",token,null),200);
        status(call("rejected cannot read incidents","GET","/incidents",token,null),403);
        status(call("new request after rejection","POST","/companies/"+beta.getId()+"/membership-requests",token,null),201);
    }
    @Test void revokedWorkerMayOnlyRequestTheSameCompany()throws Exception{
        var account=user(alpha,Roles.WORKER);var token=login(account);
        status(call("revoke worker","DELETE","/companies/"+alpha.getId()+"/members/"+account.getId(),employerToken,null),200);
        var fresh=login(account);
        status(call("revoked cannot switch companies","POST","/companies/"+beta.getId()+"/membership-requests",fresh,null),409);
        status(call("revoked cannot create company","POST","/companies",fresh,Map.of("name","Synthetic forbidden")),409);
        status(call("revoked can request same company","POST","/companies/"+alpha.getId()+"/membership-requests",fresh,null),201);
        status(call("foreign member revocation denied","DELETE","/companies/"+beta.getId()+"/members/"+account.getId(),foreignEmployerToken,null),404);
        status(call("employer cannot revoke itself","DELETE","/companies/"+alpha.getId()+"/members/"+employer.getId(),employerToken,null),403);
    }
    @Test void concurrentPendingRequestsHaveOneWinner()throws Exception{
        var account=user(null,Roles.WORKER);var token=login(account);
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()){
            var a=executor.submit(()->call("concurrent request A","POST","/companies/"+alpha.getId()+"/membership-requests",token,null));
            var b=executor.submit(()->call("concurrent request B","POST","/companies/"+beta.getId()+"/membership-requests",token,null));
            var codes=new ArrayList<>(List.of(a.get().status,b.get().status));Collections.sort(codes);assertEquals(List.of(201,409),codes);
        }
    }
    @Test void concurrentApprovalsHaveOneWinner()throws Exception{
        var account=user(null,Roles.WORKER);var token=login(account);
        var request=call("concurrent approval request","POST","/companies/"+alpha.getId()+"/membership-requests",token,null);status(request,201);
        String path="/companies/"+alpha.getId()+"/membership-requests/"+request.body.get("id").asLong()+"/accept";
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()){
            var a=executor.submit(()->call("concurrent accept A","POST",path,employerToken,null));
            var b=executor.submit(()->call("concurrent accept B","POST",path,otherToken,null));
            var codes=new ArrayList<>(List.of(a.get().status,b.get().status));Collections.sort(codes);assertEquals(List.of(200,409),codes);
        }
        var saved=users.findById(account.getId()).orElseThrow();assertTrue(saved.isMembershipActive());assertEquals(alpha.getId(),saved.getCompanyId());
    }
    @Test void companyCreationRacingApprovalCannotProduceTwoMemberships()throws Exception{
        var account=user(null,Roles.WORKER);var token=login(account);
        var request=call("race request","POST","/companies/"+alpha.getId()+"/membership-requests",token,null);status(request,201);
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()){
            var create=executor.submit(()->call("race company creation","POST","/companies",token,Map.of("name","Synthetic race "+UUID.randomUUID())));
            var accept=executor.submit(()->call("race membership approval","POST","/companies/"+alpha.getId()+"/membership-requests/"+request.body.get("id").asLong()+"/accept",employerToken,null));
            var a=create.get();var b=accept.get();
            assertTrue(a.status==201||b.status==200);
            assertFalse(a.status==201&&b.status==200);
            assertTrue(Set.of(201,401,409).contains(a.status));assertTrue(Set.of(200,409).contains(b.status));
        }
        assertTrue(users.findById(account.getId()).orElseThrow().isMembershipActive());
    }
    @Test void legacyShortPasswordAuthenticatesButNewShortPasswordCannotBeRegistered()throws Exception{
        String legacy="old-demo";
        var account=users.saveAndFlush(new User(null,"Synthetic Legacy",new EmailAddress(UUID.randomUUID()+"@example.test"),hashing.encode(legacy),List.of(roles.findByName(Roles.WORKER).orElseThrow())));
        status(call("legacy short password login","POST","/authentication/sign-in",null,Map.of("email",account.getEmail(),"password",legacy)),200);
        var payload=new HashMap<String,Object>(signup(UUID.randomUUID()+"@example.test",""));payload.put("password",legacy);
        status(call("new short password rejected","POST","/authentication/sign-up",null,payload),400);
    }
    @AfterAll void report()throws Exception {
        var path=Path.of(".local","reports","onboarding-http-results.tsv");Files.createDirectories(path.getParent());
        var lines=new ArrayList<String>();lines.add("scenario\tmethod\troute\tstatus");lines.addAll(evidence);Files.write(path,lines);
    }
}
