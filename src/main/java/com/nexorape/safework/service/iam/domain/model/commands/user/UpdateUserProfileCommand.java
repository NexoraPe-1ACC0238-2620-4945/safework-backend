package com.nexorape.safework.service.iam.domain.model.commands.user;

public record UpdateUserProfileCommand(Long userId, String fullName, String phoneNumber) {
}
