package com.nexorape.safework.service.incidentmanagement.domain.model.commands.incident;

public record StartIncidentProgressCommand(
        Long incidentId,
        Long userId) {
}
