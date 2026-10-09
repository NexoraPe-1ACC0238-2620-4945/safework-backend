package com.nexorape.safework.service.iam.domain.services.user;

import com.nexorape.safework.service.iam.domain.model.aggregates.User;
import com.nexorape.safework.service.iam.domain.model.queries.user.GetAllUsersQuery;
import com.nexorape.safework.service.iam.domain.model.queries.user.GetUserByEmailQuery;
import com.nexorape.safework.service.iam.domain.model.queries.user.GetUserByIdQuery;

import java.util.List;
import java.util.Optional;

public interface UserQueryService {
    /**
     * Handle get all users query
     * @param query the {@link GetAllUsersQuery} query
     * @return a list of {@link User} entities
     */
    List<User> handle(GetAllUsersQuery query);

    /**
     * Handle get user by id query
     * @param query the {@link GetUserByIdQuery} query
     * @return an {@link Optional} of {@link User} entity
     */
    Optional<User> handle(GetUserByIdQuery query);

    /**
     * Handle get user by username query
     * @param query the {@link GetUserByEmailQuery} query
     * @return an {@link Optional} of {@link User} entity
     */
    Optional<User> handle(GetUserByEmailQuery query);
}
