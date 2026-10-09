package com.nexorape.safework.service.incidentmanagement.domain.model.commands.incident;

public record UpdateIncidentDocumentCommand(Long incidentId, String documentUrl) {
}
