package com.nexorape.safework.service.shared.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

/**
 * Value object representing the student record id.
 * @summary
 * This value object is used to represent the student record id. It is an embeddable object that is used to represent the student record id in the student record entity.
 * It throws an IllegalArgumentException if the student record id is null or empty.
 * @param userId The student record id. It cannot be null or empty.
 * @see IllegalArgumentException
 * @since 1.0
 */
@Embeddable
public record UserId(Long userId) {
    /**
     * Default constructor.
     * @summary
     * This constructor is used to create a new instance of the AcmeStudentRecordId value object. It generates a new UUID and sets it as the student record id.
     * @since 1.0
     */
    public UserId {
        if (userId == null || userId < 0) {
            throw new IllegalArgumentException("User id cannot be null or less than or equal to 0");
        }
    }
}
