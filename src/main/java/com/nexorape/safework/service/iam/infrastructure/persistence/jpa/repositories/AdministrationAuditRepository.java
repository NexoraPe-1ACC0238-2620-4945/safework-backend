package com.nexorape.safework.service.iam.infrastructure.persistence.jpa.repositories;
import com.nexorape.safework.service.iam.domain.model.entities.AdministrationAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface AdministrationAuditRepository extends JpaRepository<AdministrationAudit,UUID> {}
