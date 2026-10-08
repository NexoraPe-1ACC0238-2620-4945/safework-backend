package com.nexorape.safework.service.shared.application.errors;
public class ApiException extends RuntimeException {
    private final int status;
    private final String code;
    public ApiException(int status, String code, String message) { super(message); this.status=status; this.code=code; }
    public int status() { return status; }
    public String code() { return code; }
    public static ApiException notFound() { return new ApiException(404,"RESOURCE_NOT_FOUND","Resource not found."); }
    public static ApiException session() { return new ApiException(401,"SESSION_INVALID","Session invalid."); }
    public static ApiException forbidden() { return new ApiException(403,"ROLE_FORBIDDEN","Role is not permitted."); }
    public static ApiException state() { return new ApiException(409,"STATE_CONFLICT","Incident state does not permit this operation."); }
    public static ApiException validation() { return new ApiException(400,"VALIDATION_ERROR","Request validation failed."); }
}
