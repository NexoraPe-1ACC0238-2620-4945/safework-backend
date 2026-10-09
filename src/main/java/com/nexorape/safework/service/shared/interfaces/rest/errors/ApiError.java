package com.nexorape.safework.service.shared.interfaces.rest.errors;
import java.util.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
public record ApiError(String code, String message, Map<String,List<String>> fieldErrors, String requestId) {
    public ApiError(String code, String message) { this(code,message,Map.of(),UUID.randomUUID().toString()); }
    public static void internal(HttpServletResponse response) throws IOException {
        response.setStatus(500); response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control","no-store");
        response.getWriter().write("{\"code\":\"INTERNAL_ERROR\",\"message\":\"An internal error occurred.\",\"fieldErrors\":{},\"requestId\":\""+UUID.randomUUID()+"\"}");
    }
    public static void authentication(HttpServletResponse response) throws IOException {
        response.setStatus(401); response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control","no-store");
        response.getWriter().write("{\"code\":\"SESSION_INVALID\",\"message\":\"Session invalid.\",\"fieldErrors\":{},\"requestId\":\""+UUID.randomUUID()+"\"}");
    }
}
