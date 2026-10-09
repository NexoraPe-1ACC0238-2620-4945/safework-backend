package com.nexorape.safework.service.iam.domain.services.role;

import com.nexorape.safework.service.iam.domain.model.commands.role.SeedRolesCommand;

public interface RoleCommandService {
    /**
     * Handle seed roles command
     * @param command the {@link SeedRolesCommand} command
     *
     */
    void handle(SeedRolesCommand command);
}
