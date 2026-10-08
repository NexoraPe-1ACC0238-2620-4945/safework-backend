package com.nexorape.safework.service.iam.application.internal.queryservices;
import com.nexorape.safework.service.iam.domain.model.aggregates.User;
import com.nexorape.safework.service.iam.domain.model.queries.user.*;
import com.nexorape.safework.service.iam.domain.services.user.UserQueryService;
import com.nexorape.safework.service.iam.application.internal.security.AccessPolicy;
import com.nexorape.safework.service.shared.application.errors.ApiException;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class UserQueryServiceImpl implements UserQueryService {
    private final AccessPolicy access;
    public UserQueryServiceImpl(AccessPolicy access) { this.access=access; }
    public List<User> handle(GetAllUsersQuery query) { return List.of(access.current()); }
    public Optional<User> handle(GetUserByIdQuery query) {
        var user=access.current(); if(!user.getId().equals(query.userId())) throw ApiException.notFound(); return Optional.of(user);
    }
    public Optional<User> handle(GetUserByEmailQuery query) {
        var user=access.current(); if(!user.getEmail().equals(query.email().address())) throw ApiException.notFound(); return Optional.of(user);
    }
}
