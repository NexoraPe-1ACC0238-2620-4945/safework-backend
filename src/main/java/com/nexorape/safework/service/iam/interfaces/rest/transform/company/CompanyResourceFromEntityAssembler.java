package com.nexorape.safework.service.iam.interfaces.rest.transform.company;

import com.nexorape.safework.service.iam.domain.model.aggregates.Company;
import com.nexorape.safework.service.iam.interfaces.rest.resources.company.CompanyResource;

public class CompanyResourceFromEntityAssembler {
    public static CompanyResource toResourceFromEntity(Company entity) {
        return new CompanyResource(
                entity.getId(),
                entity.getName(),
                entity.getRegistrationCode());
    }
}
