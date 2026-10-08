package com.nexorape.safework.service.iam.infrastructure.persistence.jpa.repositories;

// CUSTOM IMPORTS
import com.nexorape.safework.service.iam.domain.model.aggregates.Company;
import com.nexorape.safework.service.iam.domain.model.valueobjects.company.RegistrationCode;

// DEFAULT
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByRegistrationCode(RegistrationCode registrationCode);

    Optional<Company> findByName(String name);

    boolean existsByName(String name);
}
