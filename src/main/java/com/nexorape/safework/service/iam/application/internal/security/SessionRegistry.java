package com.nexorape.safework.service.iam.application.internal.security;
import com.nexorape.safework.service.iam.domain.model.aggregates.User;
import com.nexorape.safework.service.iam.domain.model.entities.UserSession;
import com.nexorape.safework.service.iam.infrastructure.persistence.jpa.repositories.UserSessionRepository;
import com.nexorape.safework.service.shared.application.errors.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
import java.util.UUID;
@Service
public class SessionRegistry {
    private final UserSessionRepository sessions;
    public SessionRegistry(UserSessionRepository sessions) { this.sessions = sessions; }

    @Transactional(propagation = Propagation.MANDATORY)
    public UserSession open(User user, Instant createdAt, Instant expiresAt) {
        if (!user.isEnabled() || !user.getCompany().isActive()) throw ApiException.session();
        return sessions.saveAndFlush(new UserSession(user, createdAt, expiresAt));
    }
    @Transactional(readOnly = true)
    public UserSession requireActive(UUID id, User user) {
        if (id == null) throw ApiException.session();
        var session = sessions.findById(id).orElseThrow(ApiException::session);
        if (!session.isActiveFor(user, Instant.now())) throw ApiException.session();
        return session;
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public void revokeCurrent(UUID id, User user) {
        if (sessions.revokeOne(id, user.getId(), Instant.now()) != 1) throw ApiException.session();
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public void revokeAll(User user) { sessions.revokeAll(user.getId(), Instant.now()); }
}
