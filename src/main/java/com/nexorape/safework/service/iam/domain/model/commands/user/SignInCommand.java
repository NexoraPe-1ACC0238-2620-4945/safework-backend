package com.nexorape.safework.service.iam.domain.model.commands.user;
import com.nexorape.safework.service.iam.domain.model.valueobjects.user.EmailAddress;
public record SignInCommand(EmailAddress emailAddress,String passwordHash) {
    @Override public String toString() { return "SignInCommand[credentials redacted]"; }
}
