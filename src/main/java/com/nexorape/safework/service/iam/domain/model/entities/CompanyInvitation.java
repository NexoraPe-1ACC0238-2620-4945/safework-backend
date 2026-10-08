package com.nexorape.safework.service.iam.domain.model.entities;
import com.nexorape.safework.service.iam.domain.model.aggregates.Company;
import com.nexorape.safework.service.shared.application.errors.ApiException;
import jakarta.persistence.*;
import lombok.Getter;
import java.time.Instant;
@Entity
@Getter
public class CompanyInvitation {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(nullable=false) private Company company;
    @Column(nullable=false,length=254) private String recipientEmail;
    @Column(nullable=false,unique=true,length=64) private String tokenDigest;
    @Column(nullable=false) private Instant expiresAt;
    private Instant consumedAt;
    @Version private long version;
    protected CompanyInvitation() {}
    public CompanyInvitation(Company company,String email,String digest,Instant expiresAt) {
        this.company=company; this.recipientEmail=email; this.tokenDigest=digest; this.expiresAt=expiresAt;
    }
    public void consume(String email,Instant now) {
        if(consumedAt!=null || !expiresAt.isAfter(now) || !recipientEmail.equals(email) || !company.isActive())
            throw new ApiException(422,"INVITATION_INVALID","Invitation is invalid.");
        consumedAt=now;
    }
}
