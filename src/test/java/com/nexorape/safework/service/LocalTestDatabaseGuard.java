package com.nexorape.safework.service;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
/** Refuse database-backed tests before context creation unless the explicit isolated test database is selected. */
public class LocalTestDatabaseGuard implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    public void initialize(ConfigurableApplicationContext context) {
        var env=context.getEnvironment();
        if(!"127.0.0.1".equals(env.getProperty("DB_HOST")) || !"33317".equals(env.getProperty("DB_PORT"))
                || !"safework_course_test".equals(env.getProperty("DB_NAME"))
                || !env.getProperty("spring.datasource.url","").startsWith("jdbc:mysql://127.0.0.1:33317/safework_course_test?"))
            throw new IllegalStateException("Tests require the isolated safework_course_test database at 127.0.0.1:33317.");
    }
}
