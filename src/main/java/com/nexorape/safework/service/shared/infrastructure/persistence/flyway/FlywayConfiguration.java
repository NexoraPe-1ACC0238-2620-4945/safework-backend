package com.nexorape.safework.service.shared.infrastructure.persistence.flyway;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="spring.flyway.enabled", havingValue="true")
public class FlywayConfiguration {

    // Initialize Flyway with the application's DataSource and execute migrations.
    // baselineOnMigrate(true) allows applying migrations on an existing non-empty database by creating a baseline.
    @Autowired
    public FlywayConfiguration(DataSource dataSource) {
        Flyway.configure().baselineOnMigrate(true).dataSource(dataSource).load().migrate();
    }
}
