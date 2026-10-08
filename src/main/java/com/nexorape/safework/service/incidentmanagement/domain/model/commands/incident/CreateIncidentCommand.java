package com.nexorape.safework.service.incidentmanagement.domain.model.commands.incident;

public record CreateIncidentCommand(
        Long userId,
        Long companyId,
        String title,
        String description,
        String location) {
}
