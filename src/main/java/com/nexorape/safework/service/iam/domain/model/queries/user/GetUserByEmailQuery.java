package com.nexorape.safework.service.iam.domain.model.queries.user;

import com.nexorape.safework.service.iam.domain.model.valueobjects.user.EmailAddress;

public record GetUserByEmailQuery(EmailAddress email) {
}
