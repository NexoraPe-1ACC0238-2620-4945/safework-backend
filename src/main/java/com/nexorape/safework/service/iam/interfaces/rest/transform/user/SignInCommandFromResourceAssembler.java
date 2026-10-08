package com.nexorape.safework.service.iam.interfaces.rest.transform.user;

import com.nexorape.safework.service.iam.domain.model.commands.user.SignInCommand;
import com.nexorape.safework.service.iam.domain.model.valueobjects.user.EmailAddress;
import com.nexorape.safework.service.iam.interfaces.rest.resources.user.SignInResource;

public class SignInCommandFromResourceAssembler {
    public static SignInCommand toCommandFromResource(SignInResource signInResource) {
        return new SignInCommand(new EmailAddress(signInResource.email()), signInResource.password());
    }
}
