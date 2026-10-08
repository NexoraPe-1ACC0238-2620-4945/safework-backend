package com.nexorape.safework.service.iam.interfaces.rest;
import com.nexorape.safework.service.iam.application.internal.commandservices.MembershipService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/companies/{companyId}/invitations")
public class InvitationsController {
    public record CreateInvitation(@NotBlank @Email @Size(max=254) String emailAddress) {}
    private final MembershipService memberships;
    public InvitationsController(MembershipService memberships) { this.memberships=memberships; }
    @PostMapping public ResponseEntity<MembershipService.InvitationProof> create(@PathVariable @Positive Long companyId,@Valid @RequestBody CreateInvitation resource) {
        return ResponseEntity.status(201).header("Cache-Control","no-store").body(memberships.invite(companyId,resource.emailAddress()));
    }
}
