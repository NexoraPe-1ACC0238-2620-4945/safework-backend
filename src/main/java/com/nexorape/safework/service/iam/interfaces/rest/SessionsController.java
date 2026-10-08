package com.nexorape.safework.service.iam.interfaces.rest;
import com.nexorape.safework.service.iam.application.internal.commandservices.SignOutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/authentication")
public class SessionsController {
    private final SignOutService service;
    public SessionsController(SignOutService service) {this.service=service;}
    @PostMapping("/sign-out")
    @Operation(summary="Revoke only the authenticated session", description="No request body. Other sessions remain active. No refresh endpoint.")
    @ApiResponse(responseCode="204",description="Current session revoked")
    @ApiResponse(responseCode="401",description="Missing, expired, invalid or revoked session")
    public ResponseEntity<Void> signOut() {service.signOut();return ResponseEntity.noContent().build();}
}
