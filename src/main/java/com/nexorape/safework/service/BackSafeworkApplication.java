package com.nexorape.safework.service;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import java.util.*;
@SpringBootApplication @EnableJpaAuditing
public class BackSafeworkApplication {
    public static void main(String[] args) {
        if("true".equalsIgnoreCase(System.getenv("SAFEWORK_BOOTSTRAP_ENABLED")))
            throw new IllegalStateException("The global ADMIN bootstrap is not part of this alternative.");
        boolean reset="true".equalsIgnoreCase(System.getenv("SAFEWORK_PASSWORD_RESET_ENABLED"));
        boolean migrate="true".equalsIgnoreCase(System.getenv("SAFEWORK_MIGRATE_ENABLED"));
        if(reset && migrate) throw new IllegalStateException("Run migration and recovery separately.");
        var app=new SpringApplication(BackSafeworkApplication.class);
        if(reset || migrate) {
            app.setWebApplicationType(WebApplicationType.NONE);
            var flags=new ArrayList<>(Arrays.asList(args));
            flags.add(reset?"--safework.password-reset.enabled=true":"--spring.flyway.enabled=true");
            if(migrate && "true".equalsIgnoreCase(System.getenv("SAFEWORK_BASELINE_LEGACY_SCHEMA"))) {
                flags.add("--spring.flyway.baseline-on-migrate=true");
                flags.add("--spring.flyway.baseline-version=1");
            }
            try(var context=app.run(flags.toArray(String[]::new))) { }
        } else app.run(args);
    }
}
