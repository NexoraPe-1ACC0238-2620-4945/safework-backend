package com.nexorape.safework.service.iam.interfaces.rest;

import com.nexorape.safework.service.iam.domain.model.queries.user.GetAllUsersQuery;
import com.nexorape.safework.service.iam.domain.model.queries.user.GetUserByEmailQuery;
import com.nexorape.safework.service.iam.domain.model.queries.user.GetUserByIdQuery;
import com.nexorape.safework.service.iam.domain.model.valueobjects.user.EmailAddress;
import com.nexorape.safework.service.iam.domain.services.user.UserQueryService;
import com.nexorape.safework.service.iam.interfaces.rest.resources.user.UserResource;
import com.nexorape.safework.service.iam.interfaces.rest.transform.user.UserResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * This class is a REST controller that exposes the users resource.
 * It includes the following operations:
 * - GET /api/v1/users: returns all the users
 * - GET /api/v1/users/{userId}: returns the user with the given id
 **/
@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Users", description = "Available User Endpoints")
public class UsersController {
    private final UserQueryService userQueryService;
    private final com.nexorape.safework.service.iam.domain.services.user.UserCommandService userCommandService;

    public UsersController(UserQueryService userQueryService,
            com.nexorape.safework.service.iam.domain.services.user.UserCommandService userCommandService) {
        this.userQueryService = userQueryService;
        this.userCommandService = userCommandService;
    }

    /**
     * This method returns all the users.
     *
     * @return a list of user resources
     * @see UserResource
     */
    @GetMapping
    @Operation(summary = "Get all users", description = "Get all the users available in the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.") })
    public ResponseEntity<List<UserResource>> getAllUsers() {
        var getAllUsersQuery = new GetAllUsersQuery();
        var users = userQueryService.handle(getAllUsersQuery);
        var userResources = users.stream().map(UserResourceFromEntityAssembler::toResourceFromEntity).toList();
        return ResponseEntity.ok(userResources);
    }

    /**
     * This method returns the user with the given id.
     *
     * @param userId the user id
     * @return the user resource with the given id
     * @throws RuntimeException if the user is not found
     * @see UserResource
     */
    @GetMapping(value = "/{userId}")
    @Operation(summary = "Get user by id", description = "Get the user with the given id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User retrieved successfully."),
            @ApiResponse(responseCode = "404", description = "User not found."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.") })
    public ResponseEntity<UserResource> getUserById(@PathVariable Long userId) {
        var getUserByIdQuery = new GetUserByIdQuery(userId);
        var user = userQueryService.handle(getUserByIdQuery);
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var userResource = UserResourceFromEntityAssembler.toResourceFromEntity(user.get());
        return ResponseEntity.ok(userResource);
    }

    /**
     * This method returns the user with the given id.
     *
     * @param email the user email
     * @return the user resource with the given id
     * @throws RuntimeException if the user is not found
     * @see UserResource
     */
    @GetMapping(value = "/email/{email}")
    @Operation(summary = "Get user by email", description = "Get the user with the given email.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User retrieved successfully."),
            @ApiResponse(responseCode = "404", description = "User not found."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.") })
    public ResponseEntity<UserResource> getUserByEmail(@PathVariable String email) {
        var getUserByEmailQuery = new GetUserByEmailQuery(new EmailAddress(email));
        var user = userQueryService.handle(getUserByEmailQuery);
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var userResource = UserResourceFromEntityAssembler.toResourceFromEntity(user.get());
        return ResponseEntity.ok(userResource);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Get the profile of the authenticated user.")
    public ResponseEntity<UserResource> getMyProfile() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication();
        var userDetails = (com.nexorape.safework.service.iam.infrastructure.authorization.sfs.model.UserDetailsImpl) authentication
                .getPrincipal();
        var getUserByIdQuery = new GetUserByIdQuery(userDetails.getId());
        var user = userQueryService.handle(getUserByIdQuery);
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(UserResourceFromEntityAssembler.toResourceFromEntity(user.get()));
    }

    @org.springframework.web.bind.annotation.PatchMapping("/me")
    @Operation(summary = "Update current user profile", description = "Update the profile of the authenticated user.")
    public ResponseEntity<UserResource> updateMyProfile(
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.nexorape.safework.service.iam.interfaces.rest.resources.user.UpdateProfileResource resource) {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication();
        var userDetails = (com.nexorape.safework.service.iam.infrastructure.authorization.sfs.model.UserDetailsImpl) authentication
                .getPrincipal();

        var command = new com.nexorape.safework.service.iam.domain.model.commands.user.UpdateUserProfileCommand(
                userDetails.getId(),
                resource.fullName(),
                resource.phoneNumber());

        var user = userCommandService.handle(command);

        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(UserResourceFromEntityAssembler.toResourceFromEntity(user.get()));
    }
}
