package com.nexorape.safework.service.iam.domain.model.queries.company;

import com.nexorape.safework.service.iam.domain.model.valueobjects.company.RegistrationCode;

public record GetCompanyByRegistrationCodeQuery(RegistrationCode registrationCode) {
}
