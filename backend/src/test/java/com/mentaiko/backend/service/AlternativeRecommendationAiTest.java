package com.mentaiko.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.SocketTimeoutException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.client.ResourceAccessException;

import com.mentaiko.backend.enums.RecommendationDiscardReason;

class AlternativeRecommendationAiTest {
    private final ChatModel model = mock(ChatModel.class);
    private final AlternativeRecommendationAi ai = new AlternativeRecommendationAi(model);
    private final List<AlternativeRecommendationAi.Candidate> candidates = List.of(
            new AlternativeRecommendationAi.Candidate(32413L, "RESPIRACION", 4),
            new AlternativeRecommendationAi.Candidate(93672L, "private@example.invalid", 2));

    @Test
    void sendsOnlyApprovedCategoriesAndMapsTheSelectedOrdinalLocally() {
        when(model.call(any(Prompt.class))).thenReturn(response("2"));

        assertThat(ai.choose("personal-emotion@example.invalid", 4, RecommendationDiscardReason.BIBLIOTECA, 5, candidates))
                .contains(93672L);

        var prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(model).call(prompt.capture());
        assertThat(prompt.getValue().getInstructions().get(0).getText()).contains("Evita diagnósticos");
        assertThat(prompt.getValue().getInstructions().get(1).getText())
                .contains("Emoción: otra", "Intensidad: 4", "BIBLIOTECA", "Minutos disponibles: 5",
                        "1. RESPIRACION (4 min)", "2. GENERAL (2 min)")
                .doesNotContain("@", "32413", "93672");
    }

    @Test
    void timeoutOrUnusableOutputLeavesTheChoiceToLocalRules() {
        when(model.call(any(Prompt.class)))
                .thenThrow(new ResourceAccessException("Timeout", new SocketTimeoutException()))
                .thenReturn(response("99"), response("1. Texto no permitido"));

        for (int attempt = 0; attempt < 3; attempt++) {
            assertThat(ai.choose("calma", 3, RecommendationDiscardReason.TRANSPORTE, null, candidates)).isEmpty();
        }
    }

    private ChatResponse response(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
