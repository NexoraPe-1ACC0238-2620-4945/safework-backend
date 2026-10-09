package com.nexorape.safework.service.iam.interfaces.rest.resources.user;
import jakarta.validation.constraints.*;
import com.nexorape.safework.service.shared.interfaces.rest.validation.CodePointLength;
import io.swagger.v3.oas.annotations.media.Schema;
public record SignUpResource(
    @NotBlank @CodePointLength(max=120) @Schema(minLength=1,maxLength=120,description="Unicode code points, stripped") String fullName,
    @NotBlank @Email @Size(max=254) String emailAddress,
    @NotNull String password,
    @Size(max=2048) String invitationToken
) {
    public SignUpResource { if(fullName!=null) fullName=fullName.strip(); }
    @Override public String toString() { return "SignUpResource[credentials redacted]"; }
}
