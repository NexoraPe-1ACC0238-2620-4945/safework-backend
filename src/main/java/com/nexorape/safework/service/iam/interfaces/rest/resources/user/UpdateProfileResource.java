package com.nexorape.safework.service.iam.interfaces.rest.resources.user;
import jakarta.validation.constraints.*;
import com.nexorape.safework.service.shared.interfaces.rest.validation.CodePointLength;
import io.swagger.v3.oas.annotations.media.Schema;
public record UpdateProfileResource(
    @CodePointLength(max=120) @Schema(minLength=1,maxLength=120,description="Unicode code points, stripped; null keeps current value") String fullName,
    @Size(min=7,max=32) @Pattern(regexp="(?=.*[0-9])\\+?[0-9() .-]+") String phoneNumber
) {
    public UpdateProfileResource {if(fullName!=null)fullName=fullName.strip();}
}
