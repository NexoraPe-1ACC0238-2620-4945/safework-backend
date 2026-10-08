package com.nexorape.safework.service.notificationmanagement.infrastructure.persistence.jpa.repositories;

import com.nexorape.safework.service.notificationmanagement.domain.model.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * JPA repository for managing Notification entities.
 * Provides CRUD operations and custom queries for retrieving user notifications.
 */

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    java.util.List<Notification> findByRecipientIdAndCompanyIdOrderByCreatedAtDesc(String recipientId, Long companyId);

    /**
     * Retrieves all notifications for a given user, ordered from newest to oldest.
     *
     * @param recipientId ID of the notification recipient.
     * @return List of notifications sorted by creation timestamp (descending).
     */

    java.util.List<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId);
}
