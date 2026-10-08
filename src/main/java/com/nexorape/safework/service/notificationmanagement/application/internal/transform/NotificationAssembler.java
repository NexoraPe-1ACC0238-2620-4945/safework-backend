package com.nexorape.safework.service.notificationmanagement.application.internal.transform;

import com.nexorape.safework.service.incidentmanagement.domain.model.events.IncidentAssignedEvent;
import com.nexorape.safework.service.incidentmanagement.domain.model.events.IncidentCreatedEvent;
import com.nexorape.safework.service.notificationmanagement.domain.model.entities.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationAssembler {
    private final com.nexorape.safework.service.incidentmanagement.infrastructure.persistence.jpa.repositories.IncidentRepository incidents;
    public NotificationAssembler(com.nexorape.safework.service.incidentmanagement.infrastructure.persistence.jpa.repositories.IncidentRepository incidents) { this.incidents=incidents; }
    private Long company(Long incidentId) { return incidents.findById(incidentId).orElseThrow().getCompanyId(); }

    public Notification toEntityFrom(IncidentCreatedEvent event) {
        String subject = "New Incident Created: " + event.title();
        String body = "A new incident has been created with ID " + event.incidentId() + ". Description: "
                + event.description();
        return new Notification(String.valueOf(event.userId()), subject, body, company(event.incidentId()));
    }

    public Notification toEntityFrom(IncidentAssignedEvent event) {
        String subject = "Incident Assigned";
        String body = "You have been assigned to incident " + event.incidentId();
        return new Notification(String.valueOf(event.assignedUserId()), subject, body, company(event.incidentId()));
    }

    public Notification toEntityFrom(
            com.nexorape.safework.service.incidentmanagement.domain.model.events.IncidentStatusChangedEvent event) {
        String subject = "Incident Status Changed";
        String body = switch (event.newStatus()) {
            case "IN_PROGRESS" -> "Your incident " + event.incidentId() + " is now IN PROGRESS.";
            case "CLOSED" -> "Your incident " + event.incidentId() + " has been CLOSED.";
            default -> "Your incident " + event.incidentId() + " status changed to " + event.newStatus();
        };
        return new Notification(String.valueOf(event.userId()), subject, body, company(event.incidentId()));
    }

    public com.nexorape.safework.service.notificationmanagement.application.internal.queryservices.NotificationResponse toResponseFrom(
            Notification entity) {
        return new com.nexorape.safework.service.notificationmanagement.application.internal.queryservices.NotificationResponse(
                entity.getId(),
                entity.getSubject(),
                entity.getBody(),
                entity.getCreatedAt(),
                false // Defaulting to false as isRead is not yet implemented in Entity
        );
    }
}
