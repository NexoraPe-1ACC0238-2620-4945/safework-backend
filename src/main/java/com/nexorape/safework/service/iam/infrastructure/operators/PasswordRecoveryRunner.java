package com.nexorape.safework.service.iam.infrastructure.operators;
import com.nexorape.safework.service.iam.application.internal.commandservices.PasswordRecoveryService;
import org.springframework.context.annotation.*;
import org.springframework.boot.*;
import org.springframework.core.env.Environment;
@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="safework.password-reset.enabled",havingValue="true")
public class PasswordRecoveryRunner {
    @Bean ApplicationRunner recover(PasswordRecoveryService service,Environment env,javax.sql.DataSource dataSource) {
        return args->{
            try(var connection=dataSource.getConnection();var statement=connection.createStatement();var result=statement.executeQuery("SELECT DATABASE(), @@server_uuid")) {
                if(!result.next() || !env.getRequiredProperty("RESET_EXPECTED_DATABASE").equals(result.getString(1))
                    || !env.getRequiredProperty("RESET_EXPECTED_SERVER_UUID").equals(result.getString(2)))
                    throw new IllegalStateException("The recovery database identity does not match.");
            }
            service.reset(env.getRequiredProperty("RESET_EMAIL"),env.getRequiredProperty("RESET_PASSWORD"),env.getRequiredProperty("RESET_OPERATOR"),env.getRequiredProperty("RESET_EXPECTED_USER_ID",Long.class));
        };
    }
}
