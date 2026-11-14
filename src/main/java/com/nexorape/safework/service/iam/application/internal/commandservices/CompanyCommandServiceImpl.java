package com.nexorape.safework.service.iam.application.internal.commandservices;
import com.nexorape.safework.service.iam.domain.model.aggregates.Company;
import com.nexorape.safework.service.iam.domain.model.commands.company.*;
import com.nexorape.safework.service.iam.domain.services.company.CompanyCommandService;
import org.springframework.stereotype.Service;
import java.util.Optional;
@Service
public class CompanyCommandServiceImpl implements CompanyCommandService {
    private final MembershipService memberships;
    public CompanyCommandServiceImpl(MembershipService memberships) {this.memberships=memberships;}
    public Optional<Company> handle(CreateCompanyCommand command) {return Optional.of(memberships.createCompany(command.name()));}
    public void handle(SeedCompaniesCommand command) {throw new IllegalStateException("Automatic company provisioning is disabled.");}
}
