package com.nexorape.safework.service.iam.infrastructure.tokens.jwt;
import com.nexorape.safework.service.iam.application.internal.outboundservices.tokens.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
public interface BearerTokenService extends TokenService {
    String getBearerTokenFrom(HttpServletRequest request);
    Long getCompanyIdFromToken(String token);
    Long getUserIdFromToken(String token);
    UUID getSessionIdFromToken(String token);
}
