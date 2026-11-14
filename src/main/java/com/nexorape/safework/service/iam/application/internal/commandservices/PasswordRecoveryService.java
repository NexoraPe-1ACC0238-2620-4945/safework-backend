package com.nexorape.safework.service.iam.application.internal.commandservices;
import com.nexorape.safework.service.iam.infrastructure.persistence.jpa.repositories.*;
import com.nexorape.safework.service.iam.application.internal.outboundservices.hashing.HashingService;
import com.nexorape.safework.service.iam.application.internal.security.SessionRegistry;
import com.nexorape.safework.service.iam.domain.model.entities.OperatorRecoveryRecord;
import com.nexorape.safework.service.iam.domain.model.valueobjects.user.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class PasswordRecoveryService {
    private final UserRepository users;private final HashingService hashing;
    private final SessionRegistry sessions;private final OperatorRecoveryRecordRepository records;
    public PasswordRecoveryService(UserRepository users,HashingService hashing,SessionRegistry sessions,OperatorRecoveryRecordRepository records) {
        this.users=users;this.hashing=hashing;this.sessions=sessions;this.records=records;
    }
    @Transactional
    public void reset(String email,String newPassword,String operatorName) {
        reset(email,newPassword,operatorName,null);
    }
    @Transactional
    public void reset(String email,String newPassword,String operatorName,Long expectedUserId) {
        PasswordRules.validate(newPassword);
        if(operatorName==null || operatorName.isBlank() || operatorName.length()>120) throw new IllegalArgumentException("Operator name is required.");
        var user=users.lockByEmail(new EmailAddress(email).address()).orElseThrow(()->new IllegalStateException("Recovery target was not found."));
        if(expectedUserId!=null && !expectedUserId.equals(user.getId())) throw new IllegalStateException("Recovery target ID does not match.");
        user.setPasswordHash(hashing.encode(newPassword));user.invalidateSessions();
        users.saveAndFlush(user);sessions.revokeAll(user);
        records.saveAndFlush(new OperatorRecoveryRecord(user.getId(),operatorName));
    }
}
