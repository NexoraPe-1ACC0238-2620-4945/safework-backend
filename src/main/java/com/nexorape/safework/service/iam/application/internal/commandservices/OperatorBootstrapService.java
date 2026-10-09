package com.nexorape.safework.service.iam.application.internal.commandservices;
import com.nexorape.safework.service.iam.application.internal.outboundservices.hashing.HashingService;
import com.nexorape.safework.service.iam.domain.model.aggregates.*;
import com.nexorape.safework.service.iam.domain.model.entities.Role;
import com.nexorape.safework.service.iam.domain.model.valueobjects.Roles;
import com.nexorape.safework.service.iam.domain.model.valueobjects.user.*;
import com.nexorape.safework.service.iam.infrastructure.persistence.jpa.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
/** Explicit local operator command, never a public REST route. Refuses any pre-existing user database. */
@Service
public class OperatorBootstrapService {
    private final UserRepository users;private final CompanyRepository companies;
    private final RoleRepository roles;private final HashingService hashing;
    public OperatorBootstrapService(UserRepository users,CompanyRepository companies,RoleRepository roles,HashingService hashing){
        this.users=users;this.companies=companies;this.roles=roles;this.hashing=hashing;
    }
    @Transactional
    public void initialize(String companyName,String fullName,String email,String password) {
        if(users.count()!=0) throw new IllegalStateException("Operator bootstrap requires an empty user database.");
        if(companyName==null || companyName.isBlank() || companyName.length()>120 || fullName==null || fullName.isBlank() || fullName.length()>120)
            throw new IllegalArgumentException("Bootstrap names do not satisfy limits.");
        if(email==null || email.length()>254 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new IllegalArgumentException("Bootstrap email does not satisfy limits.");
        PasswordRules.validate(password);
        var admin=roles.findByName(Roles.ADMIN).orElseGet(()->roles.saveAndFlush(new Role(Roles.ADMIN)));
        var company=companies.findByName(companyName).orElseGet(()->companies.saveAndFlush(new Company(companyName)));
        if(!company.isActive()) throw new IllegalStateException("Bootstrap company must be active.");
        users.saveAndFlush(new User(company,fullName,new EmailAddress(email),hashing.encode(password),List.of(admin)));
    }
}
