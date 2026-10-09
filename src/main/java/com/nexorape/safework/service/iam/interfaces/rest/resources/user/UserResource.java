package com.nexorape.safework.service.iam.interfaces.rest.resources.user;

import java.util.Date;
import java.util.List;

public record UserResource(Long id, Long companyId, String fullName, String email, String phoneNumber, Date createdAt,
        Date updatedAt, List<String> roles) {
}
