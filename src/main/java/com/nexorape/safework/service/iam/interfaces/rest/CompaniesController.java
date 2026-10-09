package com.nexorape.safework.service.iam.interfaces.rest;

import com.nexorape.safework.service.iam.domain.model.queries.company.GetAllCompaniesQuery;
import com.nexorape.safework.service.iam.domain.model.queries.company.GetCompanyByIdQuery;
import com.nexorape.safework.service.iam.domain.model.queries.company.GetCompanyByRegistrationCodeQuery;
import com.nexorape.safework.service.iam.domain.model.valueobjects.company.RegistrationCode;
import com.nexorape.safework.service.iam.domain.services.company.CompanyCommandService;
import com.nexorape.safework.service.iam.domain.services.company.CompanyQueryService;
import com.nexorape.safework.service.iam.interfaces.rest.resources.company.CompanyResource;
import com.nexorape.safework.service.iam.interfaces.rest.resources.company.CreateCompanyResource;
import com.nexorape.safework.service.iam.interfaces.rest.transform.company.CompanyResourceFromEntityAssembler;
import com.nexorape.safework.service.iam.interfaces.rest.transform.company.CreateCompanyCommandFromResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping(value="/api/v1/companies", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name="Companies", description = "Company Management Endpoints")
public class CompaniesController {
    private final CompanyCommandService companyCommandService;
    private final CompanyQueryService companyQueryService;

    public CompaniesController(CompanyCommandService companyCommandService, CompanyQueryService companyQueryService) {
        this.companyCommandService = companyCommandService;
        this.companyQueryService = companyQueryService;
    }

    @PostMapping
    @Operation(summary = "Create a new company")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Company created"),
            @ApiResponse(responseCode = "400", description = "Bad request")})
    public ResponseEntity<CompanyResource> createCompany(@RequestBody CreateCompanyResource resource){
        var createCompanyCommand = CreateCompanyCommandFromResourceAssembler.toCommandFromResource(resource);
        var company = companyCommandService.handle(createCompanyCommand);
        if(company.isEmpty()) return ResponseEntity.badRequest().build();
        var createdCompany = company.get();
        var companyResource = CompanyResourceFromEntityAssembler.toResourceFromEntity(createdCompany);
        return new ResponseEntity<>(companyResource, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get All Companies")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Companies fetched!"),
            @ApiResponse(responseCode = "400", description = "Bad request")})
    public ResponseEntity getAllCompanies(){
        var getAllCompaniesQuery = new GetAllCompaniesQuery();
        var companies = companyQueryService.handle(getAllCompaniesQuery);
        var companiesResources = companies.stream().map(CompanyResourceFromEntityAssembler::toResourceFromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(companiesResources);
    }

    @GetMapping("/{companyId}")
    @Operation(summary = "Get Company By Id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "A company has been fetched!"),
            @ApiResponse(responseCode = "400", description = "Bad request")})
    public ResponseEntity<CompanyResource> getCompanyById(@PathVariable Long companyId){
        var getCompanyByIdQuery = new GetCompanyByIdQuery(companyId);
        var company = companyQueryService.handle(getCompanyByIdQuery);
        if(company.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        var companyResource = CompanyResourceFromEntityAssembler.toResourceFromEntity(company.get());
        return ResponseEntity.ok(companyResource);
    }

    @GetMapping("/registration_code/{registrationCode}")
    @Operation(summary = "Get Company By its registration code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "The company has been found!"),
            @ApiResponse(responseCode = "400", description = "Bad request")})
    public ResponseEntity<CompanyResource> getCompanyByRegistrationCode(@PathVariable String registrationCode){
        var getCompanyByRegistrationCodeQuery = new GetCompanyByRegistrationCodeQuery(new RegistrationCode(registrationCode));
        var company = companyQueryService.handle(getCompanyByRegistrationCodeQuery);
        if(company.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        var companyResource = CompanyResourceFromEntityAssembler.toResourceFromEntity(company.get());
        return ResponseEntity.ok(companyResource);
    }


}
