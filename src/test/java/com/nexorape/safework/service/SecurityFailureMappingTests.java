package com.nexorape.safework.service;
import com.nexorape.safework.service.iam.infrastructure.authorization.sfs.pipeline.BearerAuthorizationRequestFilter;
import com.nexorape.safework.service.iam.infrastructure.tokens.jwt.BearerTokenService;
import com.nexorape.safework.service.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.nexorape.safework.service.iam.domain.model.aggregates.User;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
class SecurityFailureMappingTests {
    static class Tokens implements BearerTokenService {
        private final boolean valid;
        Tokens(boolean valid) {this.valid=valid;}
        public String getBearerTokenFrom(HttpServletRequest r){return "synthetic-token";}
        public boolean validateToken(String t){return valid;}
        public java.util.UUID getSessionIdFromToken(String t){return java.util.UUID.randomUUID();}
        public Long getUserIdFromToken(String t){return 1L;}
        public Long getCompanyIdFromToken(String t){return 1L;}
        public String getUsernameFromToken(String t){return "synthetic@example.test";}
        public String generateToken(User user){throw new UnsupportedOperationException();}
    }
    @AfterEach void clearContext(){SecurityContextHolder.clearContext();}
    @Test void invalidTokenReturns401WithoutContinuing() throws Exception {
        var response=new MockHttpServletResponse();var continued=new AtomicBoolean();
        new BearerAuthorizationRequestFilter(new Tokens(false),null,null).doFilter(new MockHttpServletRequest(),response,(req,res)->continued.set(true));
        assertEquals(401,response.getStatus());assertTrue(response.getContentAsString().contains("SESSION_INVALID"));
        assertFalse(continued.get());
    }
    @Test void databaseFailureIsNotMisreportedAsBadCredentials() throws Exception {
        var unavailable=(UserRepository)Proxy.newProxyInstance(UserRepository.class.getClassLoader(),new Class<?>[]{UserRepository.class},
            (proxy,method,args)->{throw new org.springframework.dao.DataAccessResourceFailureException("Synthetic database outage.");});
        var response=new MockHttpServletResponse();var continued=new AtomicBoolean();
        new BearerAuthorizationRequestFilter(new Tokens(true),unavailable,null).doFilter(new MockHttpServletRequest(),response,(req,res)->continued.set(true));
        assertEquals(500,response.getStatus());assertTrue(response.getContentAsString().contains("INTERNAL_ERROR"));
        assertFalse(response.getContentAsString().contains("outage"));assertFalse(continued.get());
    }
}
