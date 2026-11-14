package com.nexorape.safework.service.iam.domain.model.entities;
import jakarta.persistence.*;
import lombok.Getter;
import java.time.Instant;
import java.util.UUID;
@Entity @Getter
public class MembershipAudit {
    @Id private UUID id;
    @Column(nullable=false) private Long actorId;
    @Column(nullable=false) private Long targetUserId;
    @Column(nullable=false) private Long companyId;
    @Column(nullable=false,length=40) private String action;
    @Column(nullable=false) private Instant occurredAt;
    protected MembershipAudit() {}
    public MembershipAudit(Long actor,Long target,Long company,String action) {
        id=UUID.randomUUID();actorId=actor;targetUserId=target;companyId=company;this.action=action;occurredAt=Instant.now();
    }
}
