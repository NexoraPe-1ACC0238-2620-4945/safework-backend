package com.nexorape.safework.service.notificationmanagement.domain.model.entities;

import com.nexorape.safework.service.notificationmanagement.domain.model.valueobjects.NotificationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;


/**
 * Notification entity representing a system-generated message sent to a user.
 * Stores basic metadata such as subject, body, status, and timestamps.
 */

@Entity
@Getter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String recipientId;

    @Column(length = 180) private String subject;

    @Column(length = 5000) private String body;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    private Instant createdAt;

    @Column(nullable = false)
    private Long companyId;

    public Notification(String recipientId, String subject, String body, Long companyId) {
        this.recipientId = recipientId;
        this.subject = subject;
        this.body = body;
        this.status = NotificationStatus.PENDING;
        this.createdAt = Instant.now();
        this.companyId = companyId;
    }

    public void markAsSent() {
        this.status = NotificationStatus.SENT;
    }
}
