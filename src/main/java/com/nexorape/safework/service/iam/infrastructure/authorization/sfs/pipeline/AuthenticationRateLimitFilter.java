package com.nexorape.safework.service.iam.infrastructure.authorization.sfs.pipeline;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
/** Single-process development limit; deployment needs a shared gateway limit. Never trust client forwarding headers. */
public class AuthenticationRateLimitFilter extends OncePerRequestFilter {
    private final ConcurrentHashMap<String,Window> windows=new ConcurrentHashMap<>();
    private static final int LIMIT=60;
    private static class Window { long started; int count; Window(long started){this.started=started;} }
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        var path=request.getRequestURI();
        if(!path.equals("/api/v1/authentication/sign-in") && !path.equals("/api/v1/authentication/sign-up")) { chain.doFilter(request,response); return; }
        var now=System.currentTimeMillis();
        if(windows.size()>=10000) windows.entrySet().removeIf(e -> now-e.getValue().started>=60000);
        var window=windows.get(request.getRemoteAddr());
        if(window==null && windows.size()>=10000) { limited(response); return; }
        window=windows.computeIfAbsent(request.getRemoteAddr(),key -> new Window(now));
        synchronized(window) {
            if(now-window.started>=60000) { window.started=now;window.count=0; }
            if(++window.count>LIMIT) { limited(response);return; }
        }
        chain.doFilter(request,response);
    }
    private void limited(HttpServletResponse response) throws IOException {
        response.setStatus(429);response.setContentType("application/json");response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control","no-store"); response.setHeader("Retry-After","60");
        response.getWriter().write("{\"code\":\"RATE_LIMITED\",\"message\":\"Try again later.\",\"fieldErrors\":{},\"requestId\":\""+UUID.randomUUID()+"\"}");
    }
}
