package com.nexorape.safework.service.iam.domain.services.role;

import com.nexorape.safework.service.iam.domain.model.entities.Role;
import com.nexorape.safework.service.iam.domain.model.queries.role.GetAllRolesQuery;
import com.nexorape.safework.service.iam.domain.model.queries.role.GetRoleByNameQuery;

import java.util.List;
import java.util.Optional;

public interface RoleQueryService {
    /**
     * Handle get all roles query
     * @param query the {@link GetAllRolesQuery} query
     * @return a list of {@link Role} entities
     */
    List<Role> handle(GetAllRolesQuery query);

    /**
     * Handle get role by name query
     * @param query the {@link GetRoleByNameQuery} query
     * @return an {@link Optional} of {@link Role} entity
     */
    Optional<Role> handle(GetRoleByNameQuery query);
}
