package com.nexorape.safework.service.iam.application.internal.commandservices;
import com.nexorape.safework.service.iam.domain.model.aggregates.Company;
import com.nexorape.safework.service.iam.domain.model.commands.company.*;
import com.nexorape.safework.service.iam.domain.services.company.CompanyCommandService;
import com.nexorape.safework.service.iam.infrastructure.persistence.jpa.repositories.CompanyRepository;
import com.nexorape.safework.service.iam.application.internal.security.AccessPolicy;
import com.nexorape.safework.service.shared.application.errors.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
@Service
public class CompanyCommandServiceImpl implements CompanyCommandService {
    private final CompanyRepository companies; private final AccessPolicy access;
    public CompanyCommandServiceImpl(CompanyRepository companies,AccessPolicy access) { this.companies=companies;this.access=access; }
    @Transactional public Optional<Company> handle(CreateCompanyCommand command) {
        access.administrator();
        if(command.name()==null || command.name().isBlank() || command.name().length()>120) throw ApiException.validation();
        if(companies.existsByName(command.name())) throw new ApiException(409,"STATE_CONFLICT","Company already exists.");
        return Optional.of(companies.saveAndFlush(new Company(command)));
    }
    public void handle(SeedCompaniesCommand command) { throw new IllegalStateException("Automatic test company provisioning is disabled."); }
}
