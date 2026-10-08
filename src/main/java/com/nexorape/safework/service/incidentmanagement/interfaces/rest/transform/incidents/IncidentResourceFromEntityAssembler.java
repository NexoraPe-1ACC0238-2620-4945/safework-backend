package com.nexorape.safework.service.incidentmanagement.interfaces.rest.transform.incidents;

import com.nexorape.safework.service.incidentmanagement.domain.model.aggregates.Incident;
import com.nexorape.safework.service.incidentmanagement.interfaces.rest.resources.incident.IncidentResource;

public class IncidentResourceFromEntityAssembler {
    public static IncidentResource toResourceFromEntity(Incident entity) {
        return new IncidentResource(
                entity.getId(),
                entity.getUserId(),
                entity.getCompanyId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getLocation(),
                entity.getStatus(),
                entity.getDocumentUrl(),
                entity.getUser().getFullName(),
                entity.getAssignment() != null ? entity.getAssignment().getUser().getFullName() : null);
    }
}
