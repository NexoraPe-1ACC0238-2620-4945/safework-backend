package com.nexorape.safework.service.iam.domain.model.entities;
import jakarta.persistence.*;
import lombok.Getter;
import java.time.Instant;
import java.util.UUID;
@Entity @Getter
public class OperatorRecoveryRecord {
    @Id private UUID id;
    @Column(nullable=false) private Long targetUserId;
    @Column(nullable=false,length=120) private String operatorName;
    @Column(nullable=false,length=40) private String action;
    @Column(nullable=false) private Instant occurredAt;
    protected OperatorRecoveryRecord() {}
    public OperatorRecoveryRecord(Long userId,String operatorName) {
        id=UUID.randomUUID();targetUserId=userId;this.operatorName=operatorName;action="PASSWORD_RESET";occurredAt=Instant.now();
    }
}
