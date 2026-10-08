package com.nexorape.safework.service.iam.infrastructure.authorization.sfs.pipeline;
import com.nexorape.safework.service.shared.interfaces.rest.errors.ApiError;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
@Component
public class UnauthorizedRequestHandlerEntryPoint implements AuthenticationEntryPoint {
    public void commence(HttpServletRequest request,HttpServletResponse response,AuthenticationException error) throws IOException { ApiError.authentication(response); }
}
