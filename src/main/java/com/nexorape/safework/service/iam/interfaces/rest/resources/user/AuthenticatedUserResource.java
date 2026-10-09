package com.nexorape.safework.service.iam.interfaces.rest.resources.user;
public record AuthenticatedUserResource(Long id,String username,String token) {
    @Override public String toString() { return "AuthenticatedUserResource[token redacted]"; }
}
