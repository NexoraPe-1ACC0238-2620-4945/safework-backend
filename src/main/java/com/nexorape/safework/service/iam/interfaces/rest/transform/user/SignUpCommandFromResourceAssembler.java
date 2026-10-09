package com.nexorape.safework.service.iam.interfaces.rest.transform.user;
import com.nexorape.safework.service.iam.domain.model.commands.user.SignUpCommand;
import com.nexorape.safework.service.iam.domain.model.valueobjects.user.EmailAddress;
import com.nexorape.safework.service.iam.interfaces.rest.resources.user.SignUpResource;
public final class SignUpCommandFromResourceAssembler {
    public static SignUpCommand toCommandFromResource(SignUpResource resource) {
        return new SignUpCommand(resource.fullName(),new EmailAddress(resource.emailAddress()),resource.password(),resource.invitationToken());
    }
}
