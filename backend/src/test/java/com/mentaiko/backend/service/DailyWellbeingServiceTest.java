package com.mentaiko.backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import com.mentaiko.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DailyWellbeingServiceTest {
    @Mock JdbcTemplate jdbc;
    @Mock UserRepository users;
    private DailyWellbeingService service;

    @BeforeEach
    void prepare() {
        Clock clock=Clock.fixed(Instant.parse("2026-11-11T12:00:00Z"),ZoneOffset.UTC);
        service=new DailyWellbeingService(jdbc,users,clock);
    }

    @Test
    void rejectsOutOfRangeSleepDurationAndInvalidQuality() {
        for (BigDecimal hours : new BigDecimal[]{
                new BigDecimal("-1"),new BigDecimal("24.25"),new BigDecimal("7.3")}) {
            assertInvalid(()->service.saveSleep("student@example.com",
                    java.time.LocalDate.of(2026,11,11),hours,3));
        }
        assertInvalid(()->service.saveSleep("student@example.com",
                java.time.LocalDate.of(2026,11,11),new BigDecimal("8"),6));
        verifyNoInteractions(jdbc,users);
    }

    @Test
    void rejectsEnergyOutsideFivePointScale() {
        assertInvalid(()->service.saveEnergy("student@example.com",
                java.time.LocalDate.of(2026,11,11),0));
        assertInvalid(()->service.saveEnergy("student@example.com",
                java.time.LocalDate.of(2026,11,11),6));
        verifyNoInteractions(jdbc,users);
    }

    @Test
    void validatesLimaTodayAndRejectsFutureDates() {
        assertThat(service.today()).isEqualTo(java.time.LocalDate.of(2026,11,11));
        assertInvalid(()->service.saveEnergy("student@example.com",
                java.time.LocalDate.of(2026,11,12),3));
        verifyNoInteractions(jdbc,users);
    }

    private void assertInvalid(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(ResponseStatusException.class)
            .satisfies(error->assertThat(((ResponseStatusException)error).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
