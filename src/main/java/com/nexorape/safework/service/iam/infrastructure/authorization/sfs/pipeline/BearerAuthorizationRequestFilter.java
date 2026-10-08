package com.nexorape.safework.service.iam.infrastructure.authorization.sfs.pipeline;
import com.nexorape.safework.service.iam.infrastructure.tokens.jwt.BearerTokenService;
import com.nexorape.safework.service.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.nexorape.safework.service.iam.infrastructure.authorization.sfs.model.UsernamePasswordAuthenticationTokenBuilder;
import com.nexorape.safework.service.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.nexorape.safework.service.shared.interfaces.rest.errors.ApiError;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
public class BearerAuthorizationRequestFilter extends OncePerRequestFilter {
    private final BearerTokenService tokens; private final UserRepository users;
    private final com.nexorape.safework.service.iam.application.internal.security.SessionRegistry sessions;
    public BearerAuthorizationRequestFilter(BearerTokenService tokens,UserRepository users,com.nexorape.safework.service.iam.application.internal.security.SessionRegistry sessions) { this.tokens=tokens;this.users=users;this.sessions=sessions; }
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        String token=tokens.getBearerTokenFrom(request);
        if(token!=null) {
            try {
                if(!tokens.validateToken(token)) { ApiError.authentication(response); return; }
                var user=users.findById(tokens.getUserIdFromToken(token)).orElseThrow();
                if(!user.isEnabled() || !user.getCompany().isActive() || !user.getCompanyId().equals(tokens.getCompanyIdFromToken(token)) || !user.getEmail().equals(tokens.getUsernameFromToken(token))) {
                    ApiError.authentication(response); return;
                }
                var sessionId=tokens.getSessionIdFromToken(token);
                var session=sessions.requireActive(sessionId,user);
                SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationTokenBuilder.build(UserDetailsImpl.build(user,sessionId),request));
            } catch(com.nexorape.safework.service.shared.application.errors.ApiException | io.jsonwebtoken.JwtException | IllegalArgumentException | java.util.NoSuchElementException ignored) {
                SecurityContextHolder.clearContext(); ApiError.authentication(response); return;
            } catch(RuntimeException unexpected) {
                SecurityContextHolder.clearContext(); ApiError.internal(response); return;
            }
        }
        chain.doFilter(request,response);
    }
}
