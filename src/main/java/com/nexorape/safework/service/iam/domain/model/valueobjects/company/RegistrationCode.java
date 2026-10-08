package com.nexorape.safework.service.iam.domain.model.valueobjects.company;

import jakarta.persistence.Embeddable;

import org.apache.commons.lang3.RandomStringUtils;

@Embeddable
public record RegistrationCode(String code) {
    private static final int CODE_LENGTH = 10;
    /**
     * Default constructor
     */
    public RegistrationCode() {
        this(RandomStringUtils.randomAlphanumeric(CODE_LENGTH).toUpperCase());
    }

    /**
     * Constructor with validation
     * @param code Registration code
     */
    public RegistrationCode {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("The company registration code cannot be null or empty");
        }
    }
}
