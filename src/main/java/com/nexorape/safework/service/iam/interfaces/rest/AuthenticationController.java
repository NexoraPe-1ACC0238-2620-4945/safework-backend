package com.nexorape.safework.service.iam.interfaces.rest;
import com.nexorape.safework.service.iam.domain.services.user.UserCommandService;
import com.nexorape.safework.service.iam.interfaces.rest.resources.user.*;
import com.nexorape.safework.service.iam.interfaces.rest.transform.user.*;
import com.nexorape.safework.service.shared.application.errors.ApiException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
@RestController @RequestMapping(value="/api/v1/authentication",produces=MediaType.APPLICATION_JSON_VALUE)
public class AuthenticationController {
    private final UserCommandService users;
    public AuthenticationController(UserCommandService users) { this.users=users; }
    @PostMapping("/sign-in") @SecurityRequirements
    public AuthenticatedUserResource signIn(@Valid @RequestBody SignInResource resource) {
        var result=users.handle(SignInCommandFromResourceAssembler.toCommandFromResource(resource)).orElseThrow(() -> new ApiException(401,"INVALID_CREDENTIALS","Credentials are invalid."));
        return AuthenticatedUserResourceFromEntityAssembler.toResourceFromEntity(result.getLeft(),result.getRight());
    }
    @PostMapping("/sign-up") @SecurityRequirements
    public ResponseEntity<UserResource> signUp(@Valid @RequestBody SignUpResource resource) {
        var user=users.handle(SignUpCommandFromResourceAssembler.toCommandFromResource(resource)).orElseThrow(ApiException::validation);
        return ResponseEntity.status(201).body(UserResourceFromEntityAssembler.toResourceFromEntity(user));
    }
}
