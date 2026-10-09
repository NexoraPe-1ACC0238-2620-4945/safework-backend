package com.nexorape.safework.service.iam.infrastructure.persistence.jpa.repositories;

import com.nexorape.safework.service.iam.domain.model.aggregates.User;
import com.nexorape.safework.service.iam.domain.model.valueobjects.user.EmailAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * This method is responsible for finding the user by username.
     * @param emailAddress The email.
     * @return The user object.
     */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.id=:id")
    Optional<User> lockById(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.emailAddress.address=:email")
    Optional<User> lockByEmail(@org.springframework.data.repository.query.Param("email") String email);

    Optional<User> findByEmailAddress(EmailAddress emailAddress);

    /**
     * This method is responsible for checking if the user exists by username.
     * @param emailAddress The email.
     * @return True if the user exists, false otherwise.
     */
    boolean existsByEmailAddress(EmailAddress emailAddress);
}
