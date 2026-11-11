package com.mentaiko.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@org.springframework.context.annotation.Import(com.mentaiko.backend.config.TestMailConfiguration.class)
@SpringBootTest
class MentaikoBackendApplicationTests {

    @Test
    void contextLoads() {
    }
}