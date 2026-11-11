package com.mentaiko.backend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import static org.mockito.Mockito.mock;

/** No SMTP transport exists in the test application context. */
@TestConfiguration(proxyBeanMethods = false)
public class TestMailConfiguration {
    @Bean
    public JavaMailSender mailSender() { return mock(JavaMailSender.class); }
}
