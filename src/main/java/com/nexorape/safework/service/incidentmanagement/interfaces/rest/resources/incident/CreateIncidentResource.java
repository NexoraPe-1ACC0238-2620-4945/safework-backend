package com.nexorape.safework.service.incidentmanagement.interfaces.rest.resources.incident;
import jakarta.validation.constraints.NotBlank;
import com.nexorape.safework.service.shared.interfaces.rest.validation.CodePointLength;
import io.swagger.v3.oas.annotations.media.Schema;
public record CreateIncidentResource(
    @NotBlank @CodePointLength(max=120) @Schema(minLength=1,maxLength=120,description="Unicode code points, stripped") String title,
    @NotBlank @CodePointLength(max=4000) @Schema(minLength=1,maxLength=4000,description="Unicode code points, stripped") String description,
    @NotBlank @CodePointLength(max=500) @Schema(minLength=1,maxLength=500,description="Unicode code points, stripped") String location
) {
    public CreateIncidentResource {
        if(title!=null)title=title.strip();
        if(description!=null)description=description.strip();
        if(location!=null)location=location.strip();
    }
}
