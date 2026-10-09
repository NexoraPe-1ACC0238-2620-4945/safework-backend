package com.nexorape.safework.service.shared.interfaces.rest.configuration;
import com.nexorape.safework.service.shared.application.errors.ApiException;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.*;
@Configuration
public class RequestContractConfiguration implements WebMvcConfigurer {
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler) throws java.io.IOException {
                if(!request.getParameterMap().isEmpty()) throw ApiException.validation();
                Object variables=request.getAttribute(org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
                if(variables instanceof java.util.Map<?,?> map) {
                    for(var entry:map.entrySet()) if(entry.getKey().toString().endsWith("Id")) {
                        try { if(Long.parseLong(entry.getValue().toString())<=0) throw ApiException.validation(); }
                        catch(NumberFormatException ex) { throw ApiException.validation(); }
                    }
                }
                var path=request.getRequestURI();
                if("POST".equals(request.getMethod()) && path.startsWith("/api/v1/incidents/")
                        && (path.endsWith("/start") || path.endsWith("/close"))
                        && request.getInputStream().read()!=-1) throw ApiException.validation();
                if("POST".equals(request.getMethod()) && path.equals("/api/v1/authentication/sign-out")
                        && request.getInputStream().read()!=-1) throw ApiException.validation();
                response.setHeader("Cache-Control","no-store");
                return true;
            }
        }).addPathPatterns("/api/v1/**");
    }
}
