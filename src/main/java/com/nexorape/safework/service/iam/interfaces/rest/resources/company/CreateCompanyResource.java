package com.nexorape.safework.service.iam.interfaces.rest.resources.company;

public record CreateCompanyResource(
        String name) {
    /**
     * Validates the resource.
     *
     * @throws IllegalArgumentException if the resource is invalid.
     */
    public CreateCompanyResource {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
    }
}
