package com.nexorape.safework.service.shared.infrastructure.documentation.openapi.configuration;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.*;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.*;
@Configuration
public class OpenApiConfiguration {
    @Bean public OpenAPI safeWorkOpenApi() {
        return new OpenAPI().info(new Info().title("SafeWork backend").version("1.0.0-SNAPSHOT")
            .description("Current-course backend. Durable revocable sessions; UTC timestamps. No refresh endpoint. Local verification and source provenance are documented in the repository."))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components().addSecuritySchemes("bearerAuth",new SecurityScheme()
                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                .description("HS512; issuer safework-backend; audience safework-mobile; sub/userId/companyId/jti/iat/exp required. jti identifies a persisted active, unexpired session. Lifetime defaults to seven days, configurable by JWT_EXPIRATION (1-365 days). Current account, company, security version and roles are checked on each authenticated request.")));
    }
    @Bean public OpenApiCustomizer sessionAndErrorContract() {
        return api -> {
            var fields=new MapSchema().additionalProperties(new ArraySchema().items(new StringSchema()));
            var error=new ObjectSchema().addProperty("code",new StringSchema())
                .addProperty("message",new StringSchema()).addProperty("fieldErrors",fields)
                .addProperty("requestId",new StringSchema());
            error.setRequired(java.util.List.of("code","message","fieldErrors","requestId"));
            api.getComponents().addSchemas("ApiError",error);
            api.getPaths().forEach((path,item) -> item.readOperations().forEach(op -> {
                if(!path.startsWith("/api/v1/")) return;
                if(op.getResponses()==null) op.setResponses(new ApiResponses());
                addError(op,"400","VALIDATION_ERROR: invalid JSON/types/limits or forbidden overrides.");
                addError(op,"401",path.endsWith("/sign-in")
                    ? "INVALID_CREDENTIALS: wrong credentials. An invalid supplied bearer also returns SESSION_INVALID."
                    : "SESSION_INVALID: missing, altered, expired, unknown or revoked session, disabled account or changed security context.");
                if(!path.endsWith("/sign-in") && !path.endsWith("/sign-up") && !path.endsWith("/sign-out") && !path.startsWith("/api/v1/users"))
                    addError(op,"403","ROLE_FORBIDDEN / NOT_RESPONSIBLE: valid session without required authority.");
                addError(op,"500","INTERNAL_ERROR: sanitized unexpected failure.");
                if(path.endsWith("/sign-in") || path.endsWith("/sign-up")) addError(op,"429","RATE_LIMITED: retry after 60 seconds.");
                if(path.endsWith("/sign-up")) {
                    addError(op,"422","INVITATION_INVALID: missing, expired, used or mismatched proof.");
                    addError(op,"409","EMAIL_UNAVAILABLE: registration conflict.");
                }
                if(path.startsWith("/api/v1/incidents") || path.startsWith("/api/v1/assignments")) {
                    addError(op,"404","RESOURCE_NOT_FOUND: absent or invisible company-scoped resource.");
                    addError(op,"409","STATE_CONFLICT: invalid transition or concurrent assignment.");
                }
            }));
        };
    }
    private void addError(Operation op,String status,String description) {
        var response=new ApiResponse().description(description).content(new Content().addMediaType("application/json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiError"))));
        op.getResponses().addApiResponse(status,response);
    }
}
