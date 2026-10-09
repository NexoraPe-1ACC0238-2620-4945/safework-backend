package com.nexorape.safework.service.iam.application.internal.commandservices;
import com.nexorape.safework.service.iam.application.internal.security.*;
import com.nexorape.safework.service.iam.domain.model.aggregates.User;
import com.nexorape.safework.service.iam.domain.model.entities.AdministrationAudit;
import com.nexorape.safework.service.iam.domain.model.valueobjects.Roles;
import com.nexorape.safework.service.iam.infrastructure.persistence.jpa.repositories.*;
import com.nexorape.safework.service.shared.application.errors.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
/** Backend-only administration. Public registration and Android cannot grant privileges. */
@Service @Transactional
public class AccountAdministrationService {
    private final UserRepository users;
    private final CompanyRepository companies;
    private final RoleRepository roles;
    private final AdministrationAuditRepository audits;
    private final SessionRegistry sessions;
    private final AccessPolicy access;
    public AccountAdministrationService(UserRepository users,CompanyRepository companies,RoleRepository roles,
            AdministrationAuditRepository audits,SessionRegistry sessions,AccessPolicy access) {
        this.users=users;this.companies=companies;this.roles=roles;this.audits=audits;this.sessions=sessions;this.access=access;
    }
    private User target(Long id,User actor) {
        if(id==null || id<=0) throw ApiException.validation();
        if(id.equals(actor.getId())) throw ApiException.forbidden();
        var user=users.lockById(id).orElseThrow(ApiException::notFound);
        if(access.has(user,"ADMIN")) throw ApiException.forbidden();
        return user;
    }
    private User finish(User user,long previous,User actor,String action) {
        if(previous!=user.getSecurityVersion()) {
            users.saveAndFlush(user);
            sessions.revokeAll(user);
            audits.saveAndFlush(new AdministrationAudit(actor.getId(),user.getId(),action));
        }
        return user;
    }
    public User replaceRoles(Long id,Set<Roles> requested) {
        var actor=access.administrator(); var user=target(id,actor);
        if(requested==null || requested.isEmpty() || requested.contains(Roles.ADMIN)) throw ApiException.validation();
        var existing=new HashSet<com.nexorape.safework.service.iam.domain.model.entities.Role>();
        for(var role:requested) existing.add(roles.findByName(role).orElseThrow(IllegalStateException::new));
        long previous=user.getSecurityVersion();user.setRoles(existing);
        return finish(user,previous,actor,"ROLES_CHANGED");
    }
    public User moveCompany(Long id,Long companyId) {
        var actor=access.administrator();var user=target(id,actor);
        if(companyId==null || companyId<=0) throw ApiException.validation();
        var company=companies.findById(companyId).filter(c -> c.isActive()).orElseThrow(ApiException::notFound);
        long previous=user.getSecurityVersion();user.setCompany(company);
        return finish(user,previous,actor,"COMPANY_CHANGED");
    }
    public User setEnabled(Long id,boolean enabled) {
        var actor=access.administrator();var user=target(id,actor);
        long previous=user.getSecurityVersion();user.setEnabled(enabled);
        return finish(user,previous,actor,enabled?"USER_ENABLED":"USER_DISABLED");
    }
}
