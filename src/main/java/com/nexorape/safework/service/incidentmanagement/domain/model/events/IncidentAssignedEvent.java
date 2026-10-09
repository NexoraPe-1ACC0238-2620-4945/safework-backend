package com.nexorape.safework.service.incidentmanagement.domain.model.events;

public record IncidentAssignedEvent(
        Long incidentId,
        Long assignedUserId) {
}
