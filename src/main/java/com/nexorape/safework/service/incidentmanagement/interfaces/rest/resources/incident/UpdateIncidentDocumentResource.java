package com.nexorape.safework.service.incidentmanagement.interfaces.rest.resources.incident;
import jakarta.validation.constraints.*;
public record UpdateIncidentDocumentResource(@NotBlank @Size(max=2048) @Pattern(regexp="https://[^\\s]+") String documentUrl) {}
