package com.nexorape.safework.service.incidentmanagement.domain.model.commands.incident;

public record CloseIncidentCommand(
        Long incidentId,
        Long userId) {
}
