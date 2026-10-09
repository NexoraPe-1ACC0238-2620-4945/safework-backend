package com.nexorape.safework.service.iam.interfaces.rest;
import com.nexorape.safework.service.iam.application.internal.commandservices.AccountAdministrationService;
import com.nexorape.safework.service.iam.domain.model.valueobjects.Roles;
import com.nexorape.safework.service.iam.interfaces.rest.resources.user.UserResource;
import com.nexorape.safework.service.iam.interfaces.rest.transform.user.UserResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.Set;
@RestController @RequestMapping("/api/v1/administration/users")
@io.swagger.v3.oas.annotations.tags.Tag(name="Backend administration",description="ADMIN-only operator workflow; no Android ADMIN functionality")
@ApiResponse(responseCode="401",description="Invalid session")
@ApiResponse(responseCode="403",description="Valid session without ADMIN authority or prohibited ADMIN/self target")
public class AccountAdministrationController {
    private final AccountAdministrationService service;
    public AccountAdministrationController(AccountAdministrationService service){this.service=service;}
    public record RolesRequest(@NotEmpty Set<Roles> roles) {}
    public record MembershipRequest(@NotNull @Positive Long companyId) {}
    public record EnabledRequest(@NotNull Boolean enabled) {}
    @PatchMapping("/{userId}/roles")
    @Operation(summary="Replace WORKER/EMPLOYER roles",description="ADMIN only. ADMIN cannot be assigned here. A change revokes all target sessions atomically.")
    public UserResource roles(@PathVariable Long userId,@Valid @RequestBody RolesRequest body) {
        return UserResourceFromEntityAssembler.toResourceFromEntity(service.replaceRoles(userId,body.roles()));
    }
    @PatchMapping("/{userId}/membership")
    @Operation(summary="Change company membership",description="ADMIN only. Server checks active company and revokes all target sessions atomically. Does not transfer incidents or assignments.")
    public UserResource membership(@PathVariable Long userId,@Valid @RequestBody MembershipRequest body) {
        return UserResourceFromEntityAssembler.toResourceFromEntity(service.moveCompany(userId,body.companyId()));
    }
    @PatchMapping("/{userId}/enabled")
    @Operation(summary="Enable or disable a user",description="ADMIN only. A change revokes all target sessions atomically.")
    public UserResource enabled(@PathVariable Long userId,@Valid @RequestBody EnabledRequest body) {
        return UserResourceFromEntityAssembler.toResourceFromEntity(service.setEnabled(userId,body.enabled()));
    }
}
