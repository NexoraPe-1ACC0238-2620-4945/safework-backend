package com.nexorape.safework.service.iam.infrastructure.authorization.sfs.configuration;
import com.nexorape.safework.service.iam.infrastructure.tokens.jwt.BearerTokenService;
import com.nexorape.safework.service.iam.infrastructure.authorization.sfs.pipeline.*;
import com.nexorape.safework.service.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.DispatcherType;
@Configuration @EnableMethodSecurity
@org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication(type=org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET)
public class WebSecurityConfiguration {
    @Bean public SecurityFilterChain security(HttpSecurity http,BearerTokenService tokens,UserRepository users,UnauthorizedRequestHandlerEntryPoint unauthorized,com.nexorape.safework.service.iam.application.internal.security.SessionRegistry sessions) throws Exception {
        return http.csrf(c -> c.disable()).cors(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e.authenticationEntryPoint(unauthorized))
            .authorizeHttpRequests(a -> a.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/api/v1/authentication/sign-in","/api/v1/authentication/sign-up","/v3/api-docs/**","/swagger-ui/**","/swagger-ui.html").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(new BearerAuthorizationRequestFilter(tokens,users,sessions),UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new AuthenticationRateLimitFilter(),BearerAuthorizationRequestFilter.class).build();
    }
}
