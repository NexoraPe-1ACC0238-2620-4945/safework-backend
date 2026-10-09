package com.nexorape.safework.service.iam.application.internal.commandservices;
import com.nexorape.safework.service.iam.application.internal.security.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class SignOutService {
    private final AccessPolicy access;
    private final SessionRegistry sessions;
    public SignOutService(AccessPolicy access,SessionRegistry sessions) {this.access=access;this.sessions=sessions;}
    @Transactional public void signOut() {
        var user=access.current();
        sessions.revokeCurrent(access.principal().getSessionId(),user);
    }
}
