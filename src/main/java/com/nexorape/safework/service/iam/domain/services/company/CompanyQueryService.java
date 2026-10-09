package com.nexorape.safework.service.iam.domain.services.company;

import com.nexorape.safework.service.iam.domain.model.aggregates.Company;
import com.nexorape.safework.service.iam.domain.model.queries.company.GetAllCompaniesQuery;
import com.nexorape.safework.service.iam.domain.model.queries.company.GetCompanyByIdQuery;
import com.nexorape.safework.service.iam.domain.model.queries.company.GetCompanyByRegistrationCodeQuery;
import com.nexorape.safework.service.iam.domain.model.queries.company.GetCompanyByNameQuery;

import java.util.List;
import java.util.Optional;

public interface CompanyQueryService {
    Optional<Company> handle(GetCompanyByIdQuery query);

    List<Company> handle(GetAllCompaniesQuery query);

    Optional<Company> handle(GetCompanyByRegistrationCodeQuery query);

    Optional<Company> handle(GetCompanyByNameQuery query);
}
