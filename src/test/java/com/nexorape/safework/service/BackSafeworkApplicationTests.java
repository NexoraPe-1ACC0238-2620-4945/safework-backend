package com.nexorape.safework.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@org.springframework.test.context.ContextConfiguration(initializers=LocalTestDatabaseGuard.class)
@SpringBootTest
class BackSafeworkApplicationTests {

    @Test
    void contextLoads() {
    }

}
