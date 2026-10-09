package com.nexorape.safework.service.incidentmanagement.domain.model.commands.assignment;

public record CreateAssignmentCommand(
        Long incidentId,
        Long userId) {
}
