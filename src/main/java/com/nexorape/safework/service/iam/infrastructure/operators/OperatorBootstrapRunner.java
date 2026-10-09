package com.nexorape.safework.service.iam.infrastructure.operators;
import com.nexorape.safework.service.iam.application.internal.commandservices.OperatorBootstrapService;
import org.springframework.boot.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
/** One-shot opt-in process. Values come from private environment configuration and are never echoed. */
@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="safework.bootstrap.enabled",havingValue="true")
public class OperatorBootstrapRunner {
    @Bean public ApplicationRunner bootstrap(OperatorBootstrapService service,Environment env) {
        return args -> service.initialize(env.getRequiredProperty("BOOTSTRAP_COMPANY_NAME"),
                env.getRequiredProperty("BOOTSTRAP_FULL_NAME"),env.getRequiredProperty("BOOTSTRAP_EMAIL"),
                env.getRequiredProperty("BOOTSTRAP_PASSWORD"));
    }
}
