package com.nexorape.safework.service.iam.infrastructure.authorization.sfs.model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nexorape.safework.service.iam.domain.model.aggregates.User;
import lombok.Getter;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.*;
/** Internal authenticated identity; a bearer JWT is never retained here. */
@Getter
public class UserDetailsImpl implements UserDetails {
    private final String username;
    @JsonIgnore private final String password;
    private final boolean accountNonExpired=true, accountNonLocked=true, credentialsNonExpired=true;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Long companyId, id;
    private final UUID sessionId;
    private UserDetailsImpl(User user, UUID sessionId) {
        username=user.getEmail(); password=user.getPasswordHash(); enabled=user.isEnabled();
        companyId=user.getCompanyId(); id=user.getId(); this.sessionId=sessionId;
        authorities=user.getRoles().stream().map(r -> new SimpleGrantedAuthority(r.getStringName())).toList();
    }
    public static UserDetailsImpl build(User user) { return new UserDetailsImpl(user,null); }
    public static UserDetailsImpl build(User user,UUID sessionId) { return new UserDetailsImpl(user,sessionId); }
}
