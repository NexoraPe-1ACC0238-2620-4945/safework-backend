package com.nexorape.safework.service.iam.interfaces.rest.transform.user;

import com.nexorape.safework.service.iam.domain.model.aggregates.User;
import com.nexorape.safework.service.iam.domain.model.entities.Role;
import com.nexorape.safework.service.iam.interfaces.rest.resources.user.UserResource;

public class UserResourceFromEntityAssembler {
    public static UserResource toResourceFromEntity(User user) {
        var roles = user.getRoles().stream().map(Role::getStringName).toList();
        return new UserResource(user.getId(), user.getCompanyId(), user.getFullName(), user.getEmail(),
                user.getPhoneNumber(), user.getCreatedAt(), user.getUpdatedAt(), roles);
    }
}
