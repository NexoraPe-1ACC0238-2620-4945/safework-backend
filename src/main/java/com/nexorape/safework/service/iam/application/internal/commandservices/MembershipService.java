package com.nexorape.safework.service.iam.application.internal.commandservices;
import com.nexorape.safework.service.iam.domain.model.aggregates.*;
import com.nexorape.safework.service.iam.domain.model.entities.*;
import com.nexorape.safework.service.iam.domain.model.entities.MembershipRequest.Status;
import com.nexorape.safework.service.iam.domain.model.commands.user.SignUpCommand;
import com.nexorape.safework.service.iam.domain.model.valueobjects.Roles;
import com.nexorape.safework.service.iam.domain.model.valueobjects.user.PasswordRules;
import com.nexorape.safework.service.iam.application.internal.security.*;
import com.nexorape.safework.service.iam.application.internal.outboundservices.hashing.HashingService;
import com.nexorape.safework.service.iam.infrastructure.persistence.jpa.repositories.*;
import com.nexorape.safework.service.shared.application.errors.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.util.*;
@Service @Transactional
public class MembershipService {
    private final UserRepository users; private final CompanyRepository companies;
    private final RoleRepository roles; private final MembershipRequestRepository requests;
    private final AccessPolicy access; private final SessionRegistry sessions; private final HashingService hashing;
    private final MembershipAuditRepository audits;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    public MembershipService(UserRepository users,CompanyRepository companies,RoleRepository roles,MembershipRequestRepository requests,
            AccessPolicy access,SessionRegistry sessions,HashingService hashing,MembershipAuditRepository audits) {
        this.users=users;this.companies=companies;this.roles=roles;this.requests=requests;
        this.access=access;this.sessions=sessions;this.hashing=hashing;this.audits=audits;
    }
    private ApiException conflict(String message) {return new ApiException(409,"STATE_CONFLICT",message);}
    private Role role(Roles name) {return roles.findByName(name).orElseThrow(IllegalStateException::new);}
    public User register(SignUpCommand command) {
        PasswordRules.validate(command.passwordHash());
        if(users.existsByEmailAddress(command.emailAddress())) throw new ApiException(409,"EMAIL_UNAVAILABLE","Email address is unavailable.");
        return users.saveAndFlush(new User(null,command.fullName(),command.emailAddress(),hashing.encode(command.passwordHash()),List.of(role(Roles.WORKER))));
    }
    public Company createCompany(String name) {
        var actor=access.current();
        if(actor.getCompanyId()!=null) throw conflict("This account already belongs to a company.");
        if(name==null || name.isBlank() || name.strip().length()>120) throw ApiException.validation();
        name=name.strip();
        if(companies.existsByName(name)) throw conflict("Company already exists.");
        var company=companies.saveAndFlush(new Company(name));
        actor.setCompany(company);actor.setMembershipActive(true);actor.setRoles(Set.of(role(Roles.EMPLOYER)));
        cancelPending(actor);users.saveAndFlush(actor);sessions.revokeAll(actor);
        audits.save(new MembershipAudit(actor.getId(),actor.getId(),company.getId(),"COMPANY_CREATED"));
        return company;
    }
    public MembershipRequest request(Long companyId) {
        var applicant=access.current();
        if(applicant.getCompanyId()!=null && (applicant.isMembershipActive() || !applicant.getCompanyId().equals(companyId)))
            throw conflict("This account cannot join another company.");
        var company=companies.findById(companyId).filter(Company::isActive).orElseThrow(ApiException::notFound);
        if(requests.existsByApplicantIdAndStatus(applicant.getId(),Status.PENDING)) throw conflict("A membership request is already pending.");
        return requests.saveAndFlush(new MembershipRequest(applicant,company));
    }
    @Transactional(readOnly=true)
    public List<MembershipRequest> ownRequests(int page,int size) {
        return requests.findByApplicantIdOrderByIdDesc(access.current().getId(),PageRequest.of(page,size));
    }
    @Transactional(readOnly=true)
    public List<MembershipRequest> companyRequests(Long companyId,int page,int size) {
        manager(companyId);return requests.findByCompanyIdOrderByIdDesc(companyId,PageRequest.of(page,size));
    }
    @Transactional(readOnly=true)
    public List<User> companyMembers(Long companyId,int page,int size) {
        manager(companyId);
        return users.findByCompany_IdOrderByIdAsc(companyId,PageRequest.of(page,size));
    }
    private User manager(Long companyId) {
        var actor=access.employer();
        if(!actor.getCompanyId().equals(companyId)) throw ApiException.notFound();
        return actor;
    }
    public MembershipRequest decide(Long companyId,Long requestId,boolean accept) {
        var actor=manager(companyId);
        var observed=requests.findById(requestId).filter(r->r.getCompany().getId().equals(companyId)).orElseThrow(ApiException::notFound);
        var applicant=users.lockById(observed.getApplicant().getId()).orElseThrow(ApiException::notFound);
        var request=requests.lockById(requestId).orElseThrow(ApiException::notFound);
        // The initial lookup eagerly loaded both entities before the lock. Refresh
        // under that lock so a concurrent commit cannot leave stale managed state.
        entityManager.refresh(applicant,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(request,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if(request.getStatus()!=Status.PENDING) throw conflict("Request is not pending.");
        if(accept) {
            if(!applicant.isEnabled()) throw conflict("The applicant is disabled.");
            if(applicant.getCompanyId()!=null && (applicant.isMembershipActive() || !applicant.getCompanyId().equals(companyId)))
                throw conflict("The applicant already belongs to a company.");
            applicant.setCompany(request.getCompany());applicant.setMembershipActive(true);
            applicant.setRoles(Set.of(role(Roles.WORKER)));
            users.saveAndFlush(applicant);sessions.revokeAll(applicant);
        }
        request.decide(accept?Status.ACCEPTED:Status.REJECTED,actor.getId());requests.saveAndFlush(request);
        if(accept) cancelPending(applicant);
        audits.save(new MembershipAudit(actor.getId(),applicant.getId(),companyId,accept?"ACCEPTED":"REJECTED"));
        return request;
    }
    public User revoke(Long companyId,Long userId) {
        var actor=manager(companyId);
        if(actor.getId().equals(userId)) throw ApiException.forbidden();
        var target=users.lockById(userId).orElseThrow(ApiException::notFound);
        if(!companyId.equals(target.getCompanyId())) throw ApiException.notFound();
        if(access.has(target,"EMPLOYER")) throw ApiException.forbidden();
        if(target.isMembershipActive()) {
            target.setMembershipActive(false);users.saveAndFlush(target);sessions.revokeAll(target);
            audits.save(new MembershipAudit(actor.getId(),target.getId(),companyId,"REVOKED"));
        }
        return target;
    }
    private void cancelPending(User applicant) {
        for(var request:requests.findByApplicantIdAndStatus(applicant.getId(),Status.PENDING))
            request.decide(Status.CANCELLED,applicant.getId());
        requests.flush();
    }
}
