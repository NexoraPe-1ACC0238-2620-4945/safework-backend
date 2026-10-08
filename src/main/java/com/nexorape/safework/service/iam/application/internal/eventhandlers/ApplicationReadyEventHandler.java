package com.nexorape.safework.service.iam.application.internal.eventhandlers;
import com.nexorape.safework.service.iam.domain.model.commands.role.SeedRolesCommand;
import com.nexorape.safework.service.iam.domain.services.role.RoleCommandService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
@Service
public class ApplicationReadyEventHandler {
    private final RoleCommandService roles;
    public ApplicationReadyEventHandler(RoleCommandService roles) { this.roles=roles; }
    @EventListener public void on(ApplicationReadyEvent event) { roles.handle(new SeedRolesCommand()); }
}
