package com.nexorape.safework.service.iam.infrastructure.persistence.jpa.repositories;
import com.nexorape.safework.service.iam.domain.model.entities.CompanyInvitation;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface CompanyInvitationRepository extends JpaRepository<CompanyInvitation,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from CompanyInvitation i join fetch i.company where i.tokenDigest=:digest")
    Optional<CompanyInvitation> lockByDigest(@Param("digest") String digest);
}
