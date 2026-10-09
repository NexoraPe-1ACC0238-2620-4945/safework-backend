package com.nexorape.safework.service.incidentmanagement.domain.model.queries.incident;

import com.nexorape.safework.service.iam.domain.model.aggregates.Company;

public record GetAllIncidentsByCompanyQuery(Company company) {

}
