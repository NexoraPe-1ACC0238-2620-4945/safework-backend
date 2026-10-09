package com.nexorape.safework.service.iam.infrastructure.tokens.jwt.services;

import com.nexorape.safework.service.iam.domain.model.aggregates.User;
import com.nexorape.safework.service.iam.infrastructure.tokens.jwt.BearerTokenService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/** Strict signed tokens. Account authorization is reloaded separately on every request. */
@Service
public class TokenServiceImpl implements BearerTokenService {
    private static final String ISSUER = "safework-backend";
    private static final String AUDIENCE = "safework-mobile";
    private final SecretKey key;
    private final int expirationDays;
    private final com.nexorape.safework.service.iam.application.internal.security.SessionRegistry sessions;

    public TokenServiceImpl(@Value("\u0024{authorization.jwt.secret}") String secret,
                            @Value("\u0024{authorization.jwt.expiration.days}") int expirationDays, com.nexorape.safework.service.iam.application.internal.security.SessionRegistry sessions) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 64)
            throw new IllegalStateException("JWT_SECRET must contain at least 64 UTF-8 bytes.");
        if (expirationDays < 1 || expirationDays > 365)
            throw new IllegalStateException("JWT expiration must be between one and 365 days.");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationDays = expirationDays;
        this.sessions = sessions;
    }

    @Override public String generateToken(User user) {
        return build(user);
    }
    private String build(User user) {
        String email=user.getEmail(); Long companyId=user.getCompanyId(); Long userId=user.getId();
        if (email == null || companyId == null || userId == null)
            throw new IllegalArgumentException("A persisted account is required.");
        var now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        var session = sessions.open(user, now, now.plus(expirationDays, ChronoUnit.DAYS));
        return Jwts.builder().issuer(ISSUER).audience().add(AUDIENCE).and()
                .id(session.getId().toString()).subject(email).claim("companyId", companyId).claim("userId", userId)
                .issuedAt(Date.from(now)).expiration(Date.from(session.getExpiresAt()))
                .signWith(key, Jwts.SIG.HS512).compact();
    }
    private Claims claims(String token) {
        var parsed = Jwts.parser().verifyWith(key).requireIssuer(ISSUER)
                .requireAudience(AUDIENCE).build().parseSignedClaims(token);
        var claims = parsed.getPayload();
        if(claims.getId()==null) throw new MalformedJwtException("Session identifier required.");
        try { java.util.UUID.fromString(claims.getId()); } catch(IllegalArgumentException ex) { throw new MalformedJwtException("Invalid session identifier."); }
        if (!"HS512".equals(parsed.getHeader().getAlgorithm()) || claims.getSubject() == null
                || claims.getIssuedAt() == null || claims.getExpiration() == null
                || claims.get("companyId", Long.class) == null || claims.get("userId", Long.class) == null
                || claims.get("companyId", Long.class) <= 0 || claims.get("userId", Long.class) <= 0
                || claims.getIssuedAt().after(new Date())
                || !claims.getExpiration().after(claims.getIssuedAt()))
            throw new MalformedJwtException("Invalid session claims.");
        return claims;
    }
    @Override public java.util.UUID getSessionIdFromToken(String token) { return java.util.UUID.fromString(claims(token).getId()); }
    @Override public boolean validateToken(String token) {
        try { claims(token); return true; }
        catch (JwtException | IllegalArgumentException ex) { return false; }
    }
    @Override public String getUsernameFromToken(String token) { return claims(token).getSubject(); }
    @Override public Long getCompanyIdFromToken(String token) { return claims(token).get("companyId", Long.class); }
    @Override public Long getUserIdFromToken(String token) { return claims(token).get("userId", Long.class); }
    @Override public String getBearerTokenFrom(HttpServletRequest request) {
        var header = request.getHeader("Authorization");
        return header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)
                ? header.substring(7).trim() : null;
    }
}
