package com.nexorape.safework.service.incidentmanagement.domain.model.events;

public record IncidentStatusChangedEvent(
        Long incidentId,
        String newStatus,
        Long userId) {
}
