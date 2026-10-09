package com.nexorape.safework.service.incidentmanagement.infrastructure.persistence.jpa.repositories;

import com.nexorape.safework.service.incidentmanagement.domain.model.entities.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    @Override @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"incident","user"})
    java.util.Optional<Assignment> findById(Long id);
    @Query("SELECT a FROM Assignment a JOIN FETCH a.incident WHERE a.user.id = :userId")
    List<Assignment> findByUserId(@Param("userId") Long userId);
}
