package com.nexorape.safework.service.incidentmanagement.domain.services;

import com.nexorape.safework.service.incidentmanagement.domain.model.aggregates.Incident;
import com.nexorape.safework.service.incidentmanagement.domain.model.commands.assignment.CreateAssignmentCommand;
import com.nexorape.safework.service.incidentmanagement.domain.model.commands.incident.CloseIncidentCommand;
import com.nexorape.safework.service.incidentmanagement.domain.model.commands.incident.CreateIncidentCommand;
import com.nexorape.safework.service.incidentmanagement.domain.model.commands.incident.StartIncidentProgressCommand;
import com.nexorape.safework.service.incidentmanagement.domain.model.entities.Assignment;

import java.util.Optional;

public interface IncidentCommandService {
    Optional<Incident> handle(CreateIncidentCommand createIncidentCommand);

    // ASSIGNMENTS
    /*
     * La razon de la porque no tiene su propio command service, es porque la
     * creacion de assigments
     * depende totalmente de que haya un incidente
     */
    Optional<Assignment> handle(CreateAssignmentCommand createAssignmentCommand);

    Optional<Incident> handle(StartIncidentProgressCommand command);

    Optional<Incident> handle(CloseIncidentCommand command);

    Optional<Assignment> handle(
            com.nexorape.safework.service.incidentmanagement.domain.model.commands.assignment.UpdateAssignmentPriorityCommand command);

    Optional<Incident> handle(
            com.nexorape.safework.service.incidentmanagement.domain.model.commands.incident.UpdateIncidentDocumentCommand command);
}
