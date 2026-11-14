package com.nexorape.safework.service.iam.domain.model.entities;
import com.nexorape.safework.service.iam.domain.model.aggregates.*;
import jakarta.persistence.*;
import lombok.Getter;
import java.time.Instant;
@Entity @Getter
@Table(indexes=@Index(name="idx_membership_company",columnList="company_id,status"))
public class MembershipRequest {
    public enum Status { PENDING, ACCEPTED, REJECTED, CANCELLED }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="applicant_id",nullable=false) private User applicant;
    @ManyToOne(optional=false) @JoinColumn(name="company_id",nullable=false) private Company company;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status=Status.PENDING;
    @Column(nullable=false) private Instant createdAt=Instant.now();
    private Instant decidedAt; private Long decidedBy;
    @Version private long version;
    protected MembershipRequest() {}
    public MembershipRequest(User applicant,Company company) {this.applicant=applicant;this.company=company;}
    public void decide(Status decision,Long actorId) {
        if(status!=Status.PENDING || decision==Status.PENDING) throw new IllegalStateException("Request is not pending.");
        status=decision;decidedAt=Instant.now();decidedBy=actorId;
    }
}
