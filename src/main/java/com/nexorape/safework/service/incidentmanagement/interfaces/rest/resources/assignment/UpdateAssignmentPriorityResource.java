package com.nexorape.safework.service.incidentmanagement.interfaces.rest.resources.assignment;
import jakarta.validation.constraints.*;
public record UpdateAssignmentPriorityResource(@NotBlank @Pattern(regexp="LOW|MEDIUM|HIGH") String priority) {}
