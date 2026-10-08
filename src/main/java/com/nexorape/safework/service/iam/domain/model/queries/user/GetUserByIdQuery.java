package com.nexorape.safework.service.iam.domain.model.queries.user;

/**
 * Get user by id query
 * <p>
 *     This class represents the query to get a user by its id.
 * </p>
 * @param userId the id of the user
 */
public record GetUserByIdQuery(Long userId) {
}
