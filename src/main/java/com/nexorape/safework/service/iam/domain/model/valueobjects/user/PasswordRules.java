package com.nexorape.safework.service.iam.domain.model.valueobjects.user;
import java.nio.charset.StandardCharsets;
public final class PasswordRules {
    private PasswordRules() {}
    public static void validate(String value) {
        if(value==null || value.codePointCount(0,value.length())<12 || value.codePointCount(0,value.length())>64 || value.getBytes(StandardCharsets.UTF_8).length>64)
            throw new IllegalArgumentException("Password does not satisfy the published limits.");
    }
}
