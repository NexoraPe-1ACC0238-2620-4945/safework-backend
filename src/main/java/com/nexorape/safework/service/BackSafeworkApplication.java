package com.nexorape.safework.service;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
@SpringBootApplication
@EnableJpaAuditing
public class BackSafeworkApplication {
    public static void main(String[] args) {
        if("true".equalsIgnoreCase(System.getenv("SAFEWORK_BOOTSTRAP_ENABLED"))) {
            var application=new SpringApplication(BackSafeworkApplication.class);
            // A one-shot non-web process exposes no bootstrap listener.
            application.setDefaultProperties(java.util.Map.of("safework.bootstrap.enabled","true"));
            application.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
            try(var context=application.run(args)) { /* ApplicationRunner performs one transactional bootstrap. */ }
        } else SpringApplication.run(BackSafeworkApplication.class,args);
    }
}
