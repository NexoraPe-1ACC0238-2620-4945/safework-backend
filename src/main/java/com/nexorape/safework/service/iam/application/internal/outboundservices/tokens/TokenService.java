package com.nexorape.safework.service.iam.application.internal.outboundservices.tokens;
import com.nexorape.safework.service.iam.domain.model.aggregates.User;
/** Tokens always represent a persisted identity and membership; authorities are reloaded independently. */
public interface TokenService {
    String generateToken(User user);
    String getUsernameFromToken(String token);
    boolean validateToken(String token);
}
