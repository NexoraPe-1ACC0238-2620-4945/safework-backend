package com.nexorape.safework.service.incidentmanagement.interfaces.rest.transform.assignment;

import com.nexorape.safework.service.incidentmanagement.domain.model.commands.assignment.CreateAssignmentCommand;
import com.nexorape.safework.service.incidentmanagement.interfaces.rest.resources.assignment.CreateAssignmentResource;

public class CreateAssignmentCommandFromResourceAssembler {
    public static CreateAssignmentCommand toCommandFromResource(CreateAssignmentResource resource, Long userId) {
        return new CreateAssignmentCommand(
                resource.incidentId(),
                userId);
    }
}
