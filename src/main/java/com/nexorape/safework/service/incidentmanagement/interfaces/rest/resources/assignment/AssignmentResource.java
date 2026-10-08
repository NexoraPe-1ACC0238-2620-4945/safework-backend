package com.nexorape.safework.service.incidentmanagement.interfaces.rest.resources.assignment;

import java.util.Date;

public record AssignmentResource(
                Long id,
                Long incidentId,
                Long userId,
                String incidentTitle,
                String status,
                Date assignedAt,
                String priority,
                Date completionDate) {
}
