package com.nexorape.safework.service.iam.domain.model.aggregates;

import com.nexorape.safework.service.iam.domain.model.commands.company.CreateCompanyCommand;
import com.nexorape.safework.service.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;

//Value Objects
import com.nexorape.safework.service.iam.domain.model.valueobjects.company.RegistrationCode;

@Entity
public class Company extends AuditableAbstractAggregateRoot<Company> {

    // Attributes
    /**/
    @Getter
    @Column(nullable = false, length = 120)
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(max = 120)
    private String name;

    @Getter @lombok.Setter @Column(nullable = false)
    private boolean active = true;

    /**/
    @Embedded /*For the Value Objects, need to have the tag @Embeddable in the  value Object*/
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "registration_code"))})
    private RegistrationCode registrationCode;

    // Constructors
    /*Por defecto*/
    public Company() {
    }

    /*Sobrecargados*/
    public Company(String name) {
        this.name = name;
        this.registrationCode = new RegistrationCode();
    }

    public Company(CreateCompanyCommand command) {
        this.name = command.name();
        this.registrationCode = new RegistrationCode();
    }

    // Methods
    /*GETTERS*/
    /**
     * Company registrationCode getter
     * @return Company registrationCode
     */
    public String getRegistrationCode() {
        return registrationCode.code();
    }
}
