package com.nexorape.safework.service.iam.domain.model.aggregates;

import com.nexorape.safework.service.iam.domain.model.entities.Role;
import com.nexorape.safework.service.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import com.nexorape.safework.service.iam.domain.model.valueobjects.user.EmailAddress;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
public class User extends AuditableAbstractAggregateRoot<User> {
    // Attributes
    /**/
    // @Embedded
    // @AttributeOverrides({
    // @AttributeOverride(name = "companyId", column = @Column(name =
    // "fk_company_id"))})
    // private CompanyId companyId;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fk_company_id", nullable = false)
    private Company company;

    /**/
    @Getter
    @NotBlank
    @com.nexorape.safework.service.shared.interfaces.rest.validation.CodePointLength(max=120)
    @Column(nullable=false,length=120)
    private String fullName;

    /**/
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address", column = @Column(name = "email", nullable = false, unique = true, length = 254)) })
    private EmailAddress emailAddress;

    @Getter
    private String passwordHash;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles;

    @Getter
    @Size(min = 7, max = 32)
    @jakarta.validation.constraints.Pattern(regexp = "(?=.*[0-9])\\+?[0-9() .-]+")
    @Column(length=32)
    private String phoneNumber;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private long securityVersion;

    @Getter
    private String profilePictureUrl;

    // Constructors
    /**
     * Default constructor
     */
    public User() {
        this.roles = new HashSet<>();
    }

    public User(Company company, String fullName, EmailAddress emailAddress, String passwordHash, List<Role> roles) {
        this();
        this.company = company;
        this.fullName = com.nexorape.safework.service.shared.domain.model.valueobjects.TextRules.normalized(fullName,120);
        this.emailAddress = emailAddress;
        this.passwordHash = passwordHash;
        addRoles(roles);
    }

    // Methods
    /**
     * Add a role to the user
     *
     * @param role the role to add
     * @return the user with the added role
     */
    public User addRole(Role role) {
        if (role == null || role.getId() == null) throw new IllegalArgumentException("A persisted role is required.");
        if(this.roles.add(role)) securityVersion++;
        return this;
    }

    /**
     * Add a list of roles to the user
     *
     * @param roles the list of roles to add
     * @return the user with the added roles
     */
    public User addRoles(List<Role> roles) {
        if (roles == null || roles.isEmpty() || roles.stream().anyMatch(r -> r.getId() == null)) {
            throw new IllegalArgumentException("A user must reference existing persisted roles.");
        }
        var validatedRoleSet = roles;
        if(this.roles.addAll(validatedRoleSet)) securityVersion++;
        return this;
    }

    public void setFullName(String fullName) { this.fullName = com.nexorape.safework.service.shared.domain.model.valueobjects.TextRules.normalized(fullName,120); }
    public void setRoles(Set<Role> roles) {
        if(roles == null || roles.isEmpty() || roles.stream().anyMatch(r -> r.getId() == null))
            throw new IllegalArgumentException("Persisted roles are required.");
        if(!this.roles.equals(roles)) { this.roles = new HashSet<>(roles); securityVersion++; }
    }
    public void setCompany(Company company) {
        if(company == null) throw new IllegalArgumentException("Company is required.");
        if(this.company == null || !this.company.getId().equals(company.getId())) {
            this.company = company; securityVersion++;
        }
    }
    public void setEnabled(boolean enabled) {
        if(this.enabled != enabled) { this.enabled = enabled; securityVersion++; }
    }
    public Long getCompanyId() {
        return this.company.getId();
    }

    public String getEmail() {
        return this.emailAddress.address();
    }
}
