package com.nexorape.safework.service.shared.interfaces.rest.errors;
import com.nexorape.safework.service.shared.application.errors.ApiException;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.security.access.AccessDeniedException;
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> business(ApiException e) { return ResponseEntity.status(e.status()).body(new ApiError(e.code(),e.getMessage())); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
        Map<String,List<String>> errors=new TreeMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> errors.computeIfAbsent(f.getField(), k -> new ArrayList<>()).add("Invalid value."));
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR","Request validation failed.",errors,UUID.randomUUID().toString()));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class,ConstraintViolationException.class,IllegalArgumentException.class})
    public ResponseEntity<ApiError> malformed(Exception e) { return business(ApiException.validation()); }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> state(IllegalStateException e) { return business(ApiException.state()); }
    @ExceptionHandler({DataIntegrityViolationException.class,PessimisticLockingFailureException.class})
    public ResponseEntity<ApiError> conflict(Exception e) { return ResponseEntity.status(409).body(new ApiError("STATE_CONFLICT","Concurrent or conflicting operation.")); }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> forbidden(Exception e) { return business(ApiException.forbidden()); }
    @ExceptionHandler({org.springframework.web.servlet.resource.NoResourceFoundException.class,org.springframework.web.servlet.NoHandlerFoundException.class})
    public ResponseEntity<ApiError> missing(Exception e) { return business(ApiException.notFound()); }
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> method(Exception e) { return ResponseEntity.status(405).body(new ApiError("METHOD_NOT_ALLOWED","Method not allowed.")); }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception e) {
        var error=new ApiError("INTERNAL_ERROR","An internal error occurred.");
        org.slf4j.LoggerFactory.getLogger(ApiExceptionHandler.class).error("Request {} failed with exception type {}",error.requestId(),e.getClass().getName());
        return ResponseEntity.internalServerError().body(error);
    }
}
