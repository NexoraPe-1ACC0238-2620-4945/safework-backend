package com.nexorape.safework.service.iam.interfaces.rest;
import com.nexorape.safework.service.iam.application.internal.commandservices.MembershipService;
import com.nexorape.safework.service.iam.domain.model.entities.MembershipRequest;
import com.nexorape.safework.service.iam.interfaces.rest.transform.user.UserResourceFromEntityAssembler;
import com.nexorape.safework.service.iam.interfaces.rest.resources.user.UserResource;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import java.time.Instant;
import java.util.List;
@RestController @RequestMapping("/api/v1") @Validated
public class MembershipsController {
    private final MembershipService service;
    public MembershipsController(MembershipService service) {this.service=service;}
    public record RequestResource(Long id,Long companyId,Long userId,String fullName,String email,String status,Instant createdAt,Instant decidedAt,Long decidedBy) {
        static RequestResource from(MembershipRequest r) {
            return new RequestResource(r.getId(),r.getCompany().getId(),r.getApplicant().getId(),r.getApplicant().getFullName(),r.getApplicant().getEmail(),r.getStatus().name(),r.getCreatedAt(),r.getDecidedAt(),r.getDecidedBy());
        }
    }
    @PostMapping("/companies/{companyId}/membership-requests")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="201",description="Pending membership request created; no company access granted")
    public ResponseEntity<RequestResource> request(@PathVariable @Positive Long companyId) {
        return ResponseEntity.status(201).body(RequestResource.from(service.request(companyId)));
    }
    @GetMapping("/users/me/membership-requests")
    public List<RequestResource> own(@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(50) int size) {
        return service.ownRequests(page,size).stream().map(RequestResource::from).toList();
    }
    @GetMapping("/companies/{companyId}/membership-requests")
    public List<RequestResource> list(@PathVariable @Positive Long companyId,@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(50) int size) {
        return service.companyRequests(companyId,page,size).stream().map(RequestResource::from).toList();
    }
    @PostMapping("/companies/{companyId}/membership-requests/{requestId}/accept")
    public RequestResource accept(@PathVariable @Positive Long companyId,@PathVariable @Positive Long requestId) {return RequestResource.from(service.decide(companyId,requestId,true));}
    @PostMapping("/companies/{companyId}/membership-requests/{requestId}/reject")
    public RequestResource reject(@PathVariable @Positive Long companyId,@PathVariable @Positive Long requestId) {return RequestResource.from(service.decide(companyId,requestId,false));}
    @DeleteMapping("/companies/{companyId}/members/{userId}")
    public UserResource revoke(@PathVariable @Positive Long companyId,@PathVariable @Positive Long userId) {return UserResourceFromEntityAssembler.toResourceFromEntity(service.revoke(companyId,userId));}
}
