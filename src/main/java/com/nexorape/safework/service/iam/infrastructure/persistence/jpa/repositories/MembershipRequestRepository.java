package com.nexorape.safework.service.iam.infrastructure.persistence.jpa.repositories;
import com.nexorape.safework.service.iam.domain.model.entities.MembershipRequest;
import com.nexorape.safework.service.iam.domain.model.entities.MembershipRequest.Status;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface MembershipRequestRepository extends JpaRepository<MembershipRequest,Long> {
    boolean existsByApplicantIdAndStatus(Long applicantId,Status status);
    List<MembershipRequest> findByApplicantIdAndStatus(Long applicantId,Status status);
    List<MembershipRequest> findByApplicantIdOrderByIdDesc(Long applicantId,org.springframework.data.domain.Pageable page);
    List<MembershipRequest> findByCompanyIdOrderByIdDesc(Long companyId,org.springframework.data.domain.Pageable page);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from MembershipRequest r where r.id=:id")
    Optional<MembershipRequest> lockById(@Param("id") Long id);
}
