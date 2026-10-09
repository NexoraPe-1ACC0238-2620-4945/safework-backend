package com.nexorape.safework.service.incidentmanagement.interfaces.rest.resources.incident;

public record IncidentResource(
                Long id,
                Long userId,
                Long companyId,
                String title,
                String description,
                String location,
                String status,
                String documentUrl,
                String reporterName,
                String assigneeName) {
}
