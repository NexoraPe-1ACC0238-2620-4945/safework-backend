package com.nexorape.safework.service.iam.infrastructure.persistence.jpa.repositories;
import com.nexorape.safework.service.iam.domain.model.entities.UserSession;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.UUID;
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    @Modifying
    @Query("update UserSession s set s.revokedAt=:now where s.id=:id and s.userId=:userId and s.revokedAt is null")
    int revokeOne(@Param("id") UUID id, @Param("userId") Long userId, @Param("now") Instant now);
    @Modifying
    @Query("update UserSession s set s.revokedAt=:now where s.userId=:userId and s.revokedAt is null")
    int revokeAll(@Param("userId") Long userId, @Param("now") Instant now);
}
