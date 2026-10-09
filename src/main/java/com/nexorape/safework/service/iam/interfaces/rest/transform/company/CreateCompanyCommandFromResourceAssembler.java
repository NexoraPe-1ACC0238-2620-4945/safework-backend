package com.nexorape.safework.service.iam.interfaces.rest.transform.company;

import com.nexorape.safework.service.iam.domain.model.commands.company.CreateCompanyCommand;
import com.nexorape.safework.service.iam.interfaces.rest.resources.company.CreateCompanyResource;

public class CreateCompanyCommandFromResourceAssembler {
    public static CreateCompanyCommand toCommandFromResource(CreateCompanyResource resource){
        return new CreateCompanyCommand(
                resource.name());
    }
}
