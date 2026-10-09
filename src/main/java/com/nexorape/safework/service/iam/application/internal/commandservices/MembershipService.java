package com.nexorape.safework.service.iam.application.internal.commandservices;
import com.nexorape.safework.service.iam.domain.model.aggregates.User;
import com.nexorape.safework.service.iam.domain.model.entities.CompanyInvitation;
import com.nexorape.safework.service.iam.domain.model.commands.user.SignUpCommand;
import com.nexorape.safework.service.iam.domain.model.valueobjects.user.*;
import com.nexorape.safework.service.iam.domain.model.valueobjects.Roles;
import com.nexorape.safework.service.iam.application.internal.security.AccessPolicy;
import com.nexorape.safework.service.iam.application.internal.outboundservices.hashing.HashingService;
import com.nexorape.safework.service.iam.infrastructure.persistence.jpa.repositories.*;
import com.nexorape.safework.service.shared.application.errors.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
@Service
public class MembershipService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final CompanyRepository companies;
    private final CompanyInvitationRepository invitations;
    private final HashingService hashing;
    private final AccessPolicy access;
    public record InvitationProof(String invitationToken,Instant expiresAt) { @Override public String toString(){ return "InvitationProof[redacted]"; } }
    public MembershipService(UserRepository users,RoleRepository roles,CompanyRepository companies,CompanyInvitationRepository invitations,HashingService hashing,AccessPolicy access) {
        this.users=users;this.roles=roles;this.companies=companies;this.invitations=invitations;this.hashing=hashing;this.access=access;
    }
    public static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable."); }
    }
    @Transactional
    public InvitationProof invite(Long companyId,String email) {
        var actor=access.current();
        if(!access.has(actor,"ADMIN")) {
            if(!access.has(actor,"EMPLOYER")) throw ApiException.forbidden();
            if(!actor.getCompanyId().equals(companyId)) throw ApiException.notFound();
        }
        var company=companies.findById(companyId).filter(c -> c.isActive()).orElseThrow(ApiException::notFound);
        byte[] bytes=new byte[32]; new SecureRandom().nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var expiry=Instant.now().plus(Duration.ofHours(24));
        invitations.save(new CompanyInvitation(company,new EmailAddress(email).address(),digest(token),expiry));
        return new InvitationProof(token,expiry);
    }
    @Transactional
    public User register(SignUpCommand command) {
        PasswordRules.validate(command.passwordHash());
        String email=command.emailAddress().address();
        if(command.invitationToken()==null || command.invitationToken().isBlank()) throw new ApiException(422,"INVITATION_INVALID","Invitation is invalid.");
        var invitation=invitations.lockByDigest(digest(command.invitationToken()))
            .orElseThrow(() -> new ApiException(422,"INVITATION_INVALID","Invitation is invalid."));
        invitation.consume(email,Instant.now());
        if(users.existsByEmailAddress(command.emailAddress())) throw new ApiException(409,"EMAIL_UNAVAILABLE","Email address is unavailable.");
        var worker=roles.findByName(Roles.WORKER).orElseThrow(() -> new IllegalStateException("Roles not initialized."));
        return users.saveAndFlush(new User(invitation.getCompany(),command.fullName(),command.emailAddress(),hashing.encode(command.passwordHash()),List.of(worker)));
    }
}
