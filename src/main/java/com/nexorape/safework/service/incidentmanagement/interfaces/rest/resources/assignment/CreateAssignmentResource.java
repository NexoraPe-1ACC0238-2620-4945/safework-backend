package com.nexorape.safework.service.incidentmanagement.interfaces.rest.resources.assignment;
import jakarta.validation.constraints.*;
public record CreateAssignmentResource(@NotNull @Positive Long incidentId) {}
