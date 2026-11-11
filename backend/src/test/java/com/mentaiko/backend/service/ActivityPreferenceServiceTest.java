package com.mentaiko.backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import com.mentaiko.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ActivityPreferenceServiceTest {
    @Mock JdbcTemplate jdbc;
    @Mock UserRepository users;

    @Test
    void rejectsInvalidAndRepeatedActivityTypesBeforeDatabaseAccess() {
        var service=new ActivityPreferenceService(jdbc,users);
        for(List<String> invalid:List.of(
              List.of("NO_EXISTE"),List.of("MOVIMIENTO","MOVIMIENTO"),
              List.of("GENERAL","RESPIRACION","MOVIMIENTO","RELAJACION",
                      "ESCRITURA","ATENCION_PLENA","GENERAL"))) {
            assertThatThrownBy(()->service.update("student@example.com",invalid))
              .isInstanceOf(ResponseStatusException.class)
              .satisfies(error->assertThat(((ResponseStatusException)error).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST));
        }
        verifyNoInteractions(jdbc,users);
    }

    @Test
    void exposesExpectedCategoriesInStableOrder() {
        assertThat(ActivityPreferenceService.TYPES).containsExactly(
          "RESPIRACION","MOVIMIENTO","RELAJACION","ESCRITURA","ATENCION_PLENA","GENERAL");
    }
}
