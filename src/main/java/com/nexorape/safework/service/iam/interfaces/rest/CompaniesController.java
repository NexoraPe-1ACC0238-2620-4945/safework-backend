package com.nexorape.safework.service.iam.interfaces.rest;
import com.nexorape.safework.service.iam.application.internal.commandservices.MembershipService;
import com.nexorape.safework.service.iam.application.internal.security.AccessPolicy;
import com.nexorape.safework.service.iam.infrastructure.persistence.jpa.repositories.CompanyRepository;
import com.nexorape.safework.service.iam.interfaces.rest.resources.company.*;
import com.nexorape.safework.service.shared.application.errors.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.data.domain.*;
import java.util.List;
@RestController @RequestMapping("/api/v1/companies") @Validated
public class CompaniesController {
    private final MembershipService memberships;private final CompanyRepository companies;private final AccessPolicy access;
    public CompaniesController(MembershipService memberships,CompanyRepository companies,AccessPolicy access) {this.memberships=memberships;this.companies=companies;this.access=access;}
    @PostMapping
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="201",description="Company created; sign in again after the membership change")
    public ResponseEntity<CompanyResource> create(@Valid @RequestBody CreateCompanyResource body) {
        var company=memberships.createCompany(body.name());
        return ResponseEntity.status(201).body(new CompanyResource(company.getId(),company.getName()));
    }
    @GetMapping
    public List<CompanyResource> search(@RequestParam(defaultValue="") @Size(max=120) String search,
            @RequestParam(defaultValue="0") @Min(0) @Max(10000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(50) int size) {
        access.current();
        return companies.findByActiveTrueAndNameContainingIgnoreCase(search.strip(),PageRequest.of(page,size,Sort.by("name").and(Sort.by("id"))))
            .stream().map(c->new CompanyResource(c.getId(),c.getName())).toList();
    }
    @GetMapping("/{companyId}")
    public CompanyResource detail(@PathVariable @Positive Long companyId) {
        access.current();
        var company=companies.findById(companyId).filter(c->c.isActive()).orElseThrow(ApiException::notFound);
        return new CompanyResource(company.getId(),company.getName());
    }
}
