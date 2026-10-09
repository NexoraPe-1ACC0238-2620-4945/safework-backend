package com.nexorape.safework.service.iam.interfaces.rest.resources.user;
import jakarta.validation.constraints.*;
public record SignInResource(@NotBlank @Email @Size(max=254) String email,@NotBlank String password) {
    @Override public String toString() { return "SignInResource[credentials redacted]"; }
}
