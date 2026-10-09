package com.nexorape.safework.service;
import com.nexorape.safework.service.iam.infrastructure.tokens.jwt.services.TokenServiceImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class TokenSecurityTests {
    private final String syntheticKey=UUID.randomUUID().toString()+UUID.randomUUID();
    private final TokenServiceImpl service=new TokenServiceImpl(syntheticKey,7,null);
    private String token(String issuer,String audience,String algorithm,boolean expiry,Instant issuedAt) {
        var builder=Jwts.builder().id(UUID.randomUUID().toString()).issuer(issuer).audience().add(audience).and().subject("synthetic@example.test")
                .claim("companyId",1L).claim("userId",1L).issuedAt(Date.from(issuedAt));
        if(expiry) builder.expiration(Date.from(issuedAt.plusSeconds(300)));
        var key=Keys.hmacShaKeyFor(syntheticKey.getBytes(StandardCharsets.UTF_8));
        return ("HS512".equals(algorithm)?builder.signWith(key,Jwts.SIG.HS512):builder.signWith(key,Jwts.SIG.HS256)).compact();
    }
    @Test void requiresConfiguredStrongKeyAndBoundedLifetime() {
        assertThrows(IllegalStateException.class,()->new TokenServiceImpl(null,7,null));
        assertThrows(IllegalStateException.class,()->new TokenServiceImpl("synthetic short value",7,null));
        assertThrows(IllegalStateException.class,()->new TokenServiceImpl(syntheticKey,0,null));
        assertThrows(IllegalStateException.class,()->new TokenServiceImpl(syntheticKey,366,null));
        assertDoesNotThrow(()->new TokenServiceImpl(syntheticKey,8,null));
    }
    @Test void validatesSignedProfileWithoutLoggingTokens() {
        assertTrue(service.validateToken(token("safework-backend","safework-mobile","HS512",true,Instant.now().minusSeconds(2))));
        assertFalse(service.validateToken("invalid"));
        assertFalse(service.validateToken(token("other","safework-mobile","HS512",true,Instant.now())));
        assertFalse(service.validateToken(token("safework-backend","other","HS512",true,Instant.now())));
        assertFalse(service.validateToken(token("safework-backend","safework-mobile","HS256",true,Instant.now())));
        assertFalse(service.validateToken(token("safework-backend","safework-mobile","HS512",false,Instant.now())));
        assertFalse(service.validateToken(token("safework-backend","safework-mobile","HS512",true,Instant.now().minusSeconds(600))));
        assertFalse(service.validateToken(token("safework-backend","safework-mobile","HS512",true,Instant.now().plusSeconds(60))));
    }
}
