package com.mentaiko.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import com.mentaiko.backend.entity.Emotion;
import com.mentaiko.backend.entity.EmotionalEntry;
import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.repository.EmotionalEntryRepository;
import com.mentaiko.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class EmotionalCalendarControllerTest {
    @Mock UserRepository users;
    @Mock EmotionalEntryRepository entries;
    @Mock Authentication authentication;
    EmotionalCalendarController controller;

    @BeforeEach
    void setup() {controller=new EmotionalCalendarController(users,entries);}

    @Test
    void aggregatesDailyEmotionsWithoutReturningPersonalNotes() {
        User user=new User();user.setId(7L);user.setActive(true);
        Emotion calm=new Emotion();calm.setId(3L);calm.setName("Calma");
        EmotionalEntry first=new EmotionalEntry();first.setEmotion(calm);
        first.setCreatedAt(LocalDateTime.of(2026,11,10,9,0));
        EmotionalEntry second=new EmotionalEntry();second.setEmotion(calm);
        second.setCreatedAt(LocalDateTime.of(2026,11,10,18,0));
        when(authentication.getName()).thenReturn("student@example.com");
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user));
        when(entries.findReportEntries(eq(user),any(),any())).thenReturn(List.of(first,second));

        var result=controller.month(authentication,2026,11);
        assertThat(result.days()).hasSize(1);
        assertThat(result.days().get(0).date()).isEqualTo(LocalDate.of(2026,11,10));
        assertThat(result.days().get(0).total()).isEqualTo(2);
        assertThat(result.days().get(0).emotions().get(0).count()).isEqualTo(2);
        assertThat(result.days().get(0).emotions().get(0).emotion()).isEqualTo("Calma");
    }

    @Test
    void rejectsInvalidMonthBeforeLookingUpUser() {
        assertThatThrownBy(()->controller.month(authentication,2026,13))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e->assertThat(((ResponseStatusException)e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
