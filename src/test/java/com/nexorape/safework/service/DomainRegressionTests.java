package com.nexorape.safework.service;
import com.nexorape.safework.service.iam.domain.model.aggregates.Company;
import com.nexorape.safework.service.iam.domain.model.aggregates.User;
import com.nexorape.safework.service.iam.domain.model.entities.MembershipRequest;
import com.nexorape.safework.service.iam.domain.model.valueobjects.user.PasswordRules;
import com.nexorape.safework.service.incidentmanagement.domain.model.aggregates.Incident;
import com.nexorape.safework.service.incidentmanagement.domain.model.commands.incident.CreateIncidentCommand;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
class DomainRegressionTests {
    private Incident incident() { return new Incident(new CreateIncidentCommand(1L,1L,"Synthetic hazard","Synthetic description","Gate A")); }
    @Test void rejectedTransitionsDoNotMutateAggregate() {
        var item=incident();
        assertThrows(IllegalStateException.class,item::startProgress);
        assertThrows(IllegalStateException.class,item::close);
        assertEquals("OPEN",item.getStatus()); assertNull(item.getAssignment());
        item.assignTo(new User());
        assertThrows(IllegalStateException.class,()->item.assignTo(new User()));
        assertThrows(IllegalStateException.class,item::close);
        assertEquals("ASSIGNED",item.getStatus());
        item.startProgress(); item.close();
        assertEquals("CLOSED",item.getStatus()); assertNotNull(item.getAssignment().getCompletionDate());
        assertThrows(IllegalStateException.class,item::startProgress);
        assertThrows(IllegalStateException.class,item::close);
    }
    @Test void rejectsValuesOutsideStorageContract() {
        assertThrows(IllegalArgumentException.class,()->new Incident(new CreateIncidentCommand(1L,1L," ","a","b")));
        assertThrows(IllegalArgumentException.class,()->new Incident(new CreateIncidentCommand(1L,1L,"x".repeat(121),"a","b")));
        assertDoesNotThrow(()->new Incident(new CreateIncidentCommand(1L,1L,"x".repeat(120),"d".repeat(4000),"l".repeat(500))));
    }
    @Test void passwordLimitsIncludeUtf8Bytes() {
        assertThrows(IllegalArgumentException.class,()->PasswordRules.validate("x".repeat(11)));
        assertThrows(IllegalArgumentException.class,()->PasswordRules.validate("é".repeat(33)));
        assertDoesNotThrow(()->PasswordRules.validate("x".repeat(64)));
    }
    @Test void membershipDecisionIsFinal() {
        var request=new MembershipRequest(new User(),new Company("Synthetic"));
        request.decide(MembershipRequest.Status.REJECTED,1L);
        assertEquals(MembershipRequest.Status.REJECTED,request.getStatus());assertNotNull(request.getDecidedAt());
        assertThrows(IllegalStateException.class,()->request.decide(MembershipRequest.Status.ACCEPTED,1L));
    }

}
