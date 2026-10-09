package com.nexorape.safework.service.iam.application.internal.security;
import com.nexorape.safework.service.iam.domain.model.aggregates.User;
import com.nexorape.safework.service.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.nexorape.safework.service.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.nexorape.safework.service.shared.application.errors.ApiException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
@Component
public class AccessPolicy {
    private final UserRepository users;
    private final SessionRegistry sessions;
    public AccessPolicy(UserRepository users,SessionRegistry sessions) { this.users=users;this.sessions=sessions; }
    public UserDetailsImpl principal() {
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        if(authentication==null || !(authentication.getPrincipal() instanceof UserDetailsImpl principal)) throw ApiException.session();
        return principal;
    }
    public User current() {
        var principal=principal();
        boolean writing=TransactionSynchronizationManager.isActualTransactionActive()
            && !TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        var user=(writing?users.lockById(principal.getId()):users.findById(principal.getId())).orElseThrow(ApiException::session);
        if(!user.isEnabled() || !user.getCompany().isActive() || !user.getCompanyId().equals(principal.getCompanyId())) throw ApiException.session();
        sessions.requireActive(principal.getSessionId(),user);
        return user;
    }
    public boolean has(User user,String role) { return user.getRoles().stream().anyMatch(r -> r.getStringName().equals(role)); }
    public User mobile() { var user=current(); if(!has(user,"WORKER") && !has(user,"EMPLOYER")) throw ApiException.forbidden(); return user; }
    public User employer() { var user=mobile(); if(!has(user,"EMPLOYER")) throw ApiException.forbidden(); return user; }
    public User administrator() { var user=current(); if(!has(user,"ADMIN")) throw ApiException.forbidden(); return user; }
    public void ownCompany(Long companyId) { if(!mobile().getCompanyId().equals(companyId)) throw ApiException.notFound(); }
}
