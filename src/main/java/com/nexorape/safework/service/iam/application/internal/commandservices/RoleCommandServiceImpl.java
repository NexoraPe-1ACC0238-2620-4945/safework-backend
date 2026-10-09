package com.nexorape.safework.service.iam.application.internal.commandservices;

import com.nexorape.safework.service.iam.domain.model.commands.role.SeedRolesCommand;
import com.nexorape.safework.service.iam.domain.model.entities.Role;
import com.nexorape.safework.service.iam.domain.model.valueobjects.Roles;
import com.nexorape.safework.service.iam.domain.services.role.RoleCommandService;
import com.nexorape.safework.service.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class RoleCommandServiceImpl implements RoleCommandService {
    private final RoleRepository roleRepository;

    public RoleCommandServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * This method will handle the {@link SeedRolesCommand} and will create the roles if not exists
     * @param command {@link SeedRolesCommand}
     * @see SeedRolesCommand
     */
    @Override
    public void handle(SeedRolesCommand command) {
        Arrays.stream(Roles.values()).forEach(role -> {
            if(!roleRepository.existsByName(role)) {
                roleRepository.save(new Role(Roles.valueOf(role.name())));
            }
        } );
    }
}
