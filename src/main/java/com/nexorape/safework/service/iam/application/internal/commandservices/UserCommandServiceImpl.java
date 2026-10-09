package com.nexorape.safework.service.iam.application.internal.commandservices;
import com.nexorape.safework.service.iam.application.internal.outboundservices.hashing.HashingService;
import com.nexorape.safework.service.iam.application.internal.outboundservices.tokens.TokenService;
import com.nexorape.safework.service.iam.domain.model.aggregates.User;
import com.nexorape.safework.service.iam.domain.model.commands.user.*;
import com.nexorape.safework.service.iam.domain.model.valueobjects.user.PasswordRules;
import com.nexorape.safework.service.iam.domain.services.user.UserCommandService;
import com.nexorape.safework.service.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.nexorape.safework.service.iam.application.internal.security.AccessPolicy;
import com.nexorape.safework.service.shared.application.errors.ApiException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
public class UserCommandServiceImpl implements UserCommandService {
    private final UserRepository users; private final HashingService hashing; private final TokenService tokens;
    private final MembershipService memberships; private final AccessPolicy access; private final String dummyHash;
    public UserCommandServiceImpl(UserRepository users,HashingService hashing,TokenService tokens,MembershipService memberships,AccessPolicy access) {
        this.users=users;this.hashing=hashing;this.tokens=tokens;this.memberships=memberships;this.access=access;
        dummyHash=hashing.encode(UUID.randomUUID().toString());
    }
    @Override @Transactional
    public Optional<ImmutablePair<User,String>> handle(SignInCommand command) {
        PasswordRules.validate(command.passwordHash());
        var user=users.lockByEmail(command.emailAddress().address());
        boolean matches=hashing.matches(command.passwordHash(),user.map(User::getPasswordHash).orElse(dummyHash));
        if(user.isEmpty() || !matches || !user.get().isEnabled() || !user.get().getCompany().isActive())
            throw new ApiException(401,"INVALID_CREDENTIALS","Credentials are invalid.");
        return Optional.of(ImmutablePair.of(user.get(),tokens.generateToken(user.get())));
    }
    public Optional<User> handle(SignUpCommand command) { return Optional.of(memberships.register(command)); }
    @Transactional
    public Optional<User> handle(UpdateUserProfileCommand command) {
        var user=access.current(); if(!user.getId().equals(command.userId())) throw ApiException.notFound();
        if(command.fullName()!=null) user.setFullName(command.fullName());
        if(command.phoneNumber()!=null) user.setPhoneNumber(command.phoneNumber());
        return Optional.of(users.saveAndFlush(user));
    }
}
