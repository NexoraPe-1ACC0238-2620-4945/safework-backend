package com.nexorape.safework.service.iam.application.internal.queryservices;
import com.nexorape.safework.service.iam.domain.model.aggregates.Company;
import com.nexorape.safework.service.iam.domain.model.queries.company.*;
import com.nexorape.safework.service.iam.domain.services.company.CompanyQueryService;
import com.nexorape.safework.service.iam.application.internal.security.AccessPolicy;
import com.nexorape.safework.service.shared.application.errors.ApiException;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class CompanyQueryServiceImpl implements CompanyQueryService {
    private final AccessPolicy access;
    public CompanyQueryServiceImpl(AccessPolicy access) { this.access=access; }
    public Optional<Company> handle(GetCompanyByIdQuery query) {
        var company=access.current().getCompany(); if(!company.getId().equals(query.companyId())) throw ApiException.notFound(); return Optional.of(company);
    }
    public List<Company> handle(GetAllCompaniesQuery query) { return List.of(access.current().getCompany()); }
    public Optional<Company> handle(GetCompanyByRegistrationCodeQuery query) {
        var company=access.current().getCompany(); if(!company.getRegistrationCode().equals(query.registrationCode().code())) throw ApiException.notFound(); return Optional.of(company);
    }
    public Optional<Company> handle(GetCompanyByNameQuery query) {
        var company=access.current().getCompany(); if(!company.getName().equals(query.name())) throw ApiException.notFound(); return Optional.of(company);
    }
}
