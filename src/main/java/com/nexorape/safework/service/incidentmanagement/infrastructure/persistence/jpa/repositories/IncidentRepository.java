package com.nexorape.safework.service.incidentmanagement.infrastructure.persistence.jpa.repositories;

import java.util.List;

import com.nexorape.safework.service.incidentmanagement.domain.model.aggregates.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select i from Incident i where i.id=:id and i.company.id=:companyId")
    java.util.Optional<Incident> lockByIdAndCompany(@org.springframework.data.repository.query.Param("id") Long id, @org.springframework.data.repository.query.Param("companyId") Long companyId);

    @org.springframework.data.jpa.repository.Query("select i from Incident i join fetch i.company join fetch i.user left join fetch i.assignment a left join fetch a.user where i.id=:id and i.company.id=:companyId")
    java.util.Optional<Incident> findVisible(@org.springframework.data.repository.query.Param("id") Long id, @org.springframework.data.repository.query.Param("companyId") Long companyId);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Incident i JOIN FETCH i.user LEFT JOIN FETCH i.assignment a LEFT JOIN FETCH a.user")
    List<Incident> findAll();

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Incident i JOIN FETCH i.user LEFT JOIN FETCH i.assignment a LEFT JOIN FETCH a.user WHERE i.company.id = :companyId")
    List<Incident> findByCompanyId(Long companyId);

    @org.springframework.data.jpa.repository.Query("SELECT i.status, COUNT(i) FROM Incident i WHERE i.company.id = :companyId GROUP BY i.status")
    List<Object[]> countTotalIncidentsByStatusAndCompanyId(Long companyId);
}
