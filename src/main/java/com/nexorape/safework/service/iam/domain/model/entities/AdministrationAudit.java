package com.nexorape.safework.service.iam.domain.model.entities;
import jakarta.persistence.*;
import lombok.Getter;
import java.time.Instant;
import java.util.UUID;
/** Safe administrative evidence; contains IDs/actions only, never credentials or invitation proofs. */
@Entity @Getter
public class AdministrationAudit {
    @Id private UUID id;
    @Column(nullable=false) private Long actorId;
    @Column(nullable=false) private Long targetUserId;
    @Column(nullable=false,length=40) private String action;
    @Column(nullable=false) private Instant occurredAt;
    protected AdministrationAudit() {}
    public AdministrationAudit(Long actorId,Long targetUserId,String action) {
        this.id=UUID.randomUUID(); this.actorId=actorId; this.targetUserId=targetUserId;
        this.action=action; this.occurredAt=Instant.now();
    }
}
