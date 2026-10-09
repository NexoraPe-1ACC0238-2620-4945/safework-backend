package com.nexorape.safework.service.notificationmanagement.application.internal.queryservices;

import java.time.Instant;
import java.util.UUID;


/**
 * DTO returned to clients when fetching notifications.
 * Provides basic notification details without exposing the full entity.
 *
 * @param id        Unique identifier of the notification.
 * @param subject   Short title or summary of the notification.
 * @param body      Main message content.
 * @param createdAt Timestamp when the notification was generated.
 * @param isRead    Indicates whether the user has opened/viewed the notification.
 */

public record NotificationResponse(
        UUID id,
        String subject,
        String body,
        Instant createdAt,
        boolean isRead) {
}
