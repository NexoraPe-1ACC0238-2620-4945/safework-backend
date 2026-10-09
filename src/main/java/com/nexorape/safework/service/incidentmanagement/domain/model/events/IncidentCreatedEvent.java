package com.nexorape.safework.service.incidentmanagement.domain.model.events;

public record IncidentCreatedEvent(
        Long incidentId,
        String title,
        String description,
        Long companyId,
        Long userId) {
}
