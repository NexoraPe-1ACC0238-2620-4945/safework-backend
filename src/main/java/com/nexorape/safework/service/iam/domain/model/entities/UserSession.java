package com.nexorape.safework.service.iam.domain.model.entities;

import com.nexorape.safework.service.iam.domain.model.aggregates.User;
import jakarta.persistence.*;
import lombok.Getter;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

/** Durable session metadata; never contains a JWT or password. */
@Entity
@Getter
@Table(indexes = @Index(name = "idx_session_user", columnList = "user_id"))
public class UserSession {
    @Id private UUID id;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false) private Long companyId;
    @Column(nullable = false) private long securityVersion;
    @Column(nullable = false, length = 64) private String rolesSnapshot;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant expiresAt;
    private Instant revokedAt;

    protected UserSession() {}
    public UserSession(User user, Instant createdAt, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = user.getId();
        this.companyId = user.getCompanyId();
        this.securityVersion = user.getSecurityVersion();
        this.rolesSnapshot = rolesOf(user);
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
    public static String rolesOf(User user) {
        return user.getRoles().stream().map(r -> r.getStringName()).sorted().collect(Collectors.joining(","));
    }
    public boolean isActiveFor(User user, Instant now) {
        return revokedAt == null && expiresAt.isAfter(now) && user.isEnabled()
                && user.getCompany().isActive() && userId.equals(user.getId())
                && companyId.equals(user.getCompanyId()) && securityVersion == user.getSecurityVersion()
                && rolesSnapshot.equals(rolesOf(user));
    }
}
