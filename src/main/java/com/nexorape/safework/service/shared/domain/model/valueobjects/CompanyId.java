package com.nexorape.safework.service.shared.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

/**
 * Value object representing the company id.
 * @summary
 * This value object is used to represent the company id. It is an embeddable object that is used to represent the company id in the company entity.
 * It throws an IllegalArgumentException if the company id is null or empty.
 * @param companyId The company id. It cannot be null or empty.
 * @see IllegalArgumentException
 * @since 1.0
 */
@Embeddable
public record CompanyId(Long companyId) {
    /**
     * Default constructor.
     * @summary
     * This constructor is used to create a new instance of the CompanyId value object. It generates a new UUID and sets it as the company id.
     * @since 1.0
     */
    public CompanyId {
        if (companyId == null || companyId < 0) {
            throw new IllegalArgumentException("CompanyId cannot be null or less than or equal to 0");
        }
    }
}
