package com.nexorape.safework.service.iam.domain.model.commands.user;
import com.nexorape.safework.service.iam.domain.model.valueobjects.user.EmailAddress;
public record SignUpCommand(String fullName,EmailAddress emailAddress,String passwordHash,String invitationToken) {
    @Override public String toString() { return "SignUpCommand[credentials redacted]"; }
}
