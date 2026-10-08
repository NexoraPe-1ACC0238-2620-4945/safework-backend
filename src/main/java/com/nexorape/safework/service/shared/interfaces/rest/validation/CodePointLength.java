package com.nexorape.safework.service.shared.interfaces.rest.validation;
import jakarta.validation.*;
import java.lang.annotation.*;
@Documented
@Constraint(validatedBy=CodePointLengthValidator.class)
@Target({ElementType.FIELD,ElementType.PARAMETER,ElementType.RECORD_COMPONENT,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CodePointLength {
    int max();
    String message() default "Text exceeds published limits.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
