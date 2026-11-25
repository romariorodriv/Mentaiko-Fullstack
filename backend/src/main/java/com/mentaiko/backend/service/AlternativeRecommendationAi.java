package com.mentaiko.backend.service;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mentaiko.backend.enums.RecommendationDiscardReason;

@Service
class AlternativeRecommendationAi {
    private static final Logger log = LoggerFactory.getLogger(AlternativeRecommendationAi.class);
    private static final Set<String> EMOTIONS = Set.of(
            "alegria", "calma", "tristeza", "ansiedad", "estres", "enojo", "cansancio", "motivacion");
    private static final SystemMessage INSTRUCTIONS = new SystemMessage("""
            Elige una microactividad cotidiana de bienestar del catálogo suministrado. Considera la emoción,
            intensidad de 1 a 5, el entorno y el tiempo. En transporte o biblioteca prioriza actividades
            discretas y silenciosas. Evita diagnósticos, indicaciones clínicas y promesas de resultados.
            Responde solo con el número de la opción elegida, o 0 si ninguna se adapta.
            """);

    private final ChatModel model;

    @Autowired
    AlternativeRecommendationAi(@Value("${app.recommendations.ai.api-key:}") String apiKey,
            @Value("${app.recommendations.ai.model:gpt-4o-mini}") String modelName) {
        this(apiKey.isBlank() ? null : OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .apiKey(apiKey)
                        .model(modelName)
                        .timeout(Duration.ofSeconds(3))
                        .maxRetries(0)
                        .maxCompletionTokens(16)
                        .build())
                .build());
    }

    AlternativeRecommendationAi(ChatModel model) {
        this.model = model;
    }

    Optional<Long> choose(String normalizedEmotion, int intensity, RecommendationDiscardReason reason,
            Integer availableMinutes, List<Candidate> candidates) {
        if (model == null || reason == null) {
            return Optional.empty();
        }

        var options = candidates.stream().limit(12).toList();
        String emotion = normalizedEmotion.toLowerCase(Locale.ROOT);
        String catalog = IntStream.range(0, options.size()).mapToObj(index -> {
            Candidate option = options.get(index);
            String type = ActivityPreferenceService.TYPES.contains(option.type()) ? option.type() : "GENERAL";
            return "%d. %s (%d min)".formatted(index + 1, type, option.minutes());
        }).collect(Collectors.joining("\n"));
        var request = new UserMessage("""
                Emoción: %s
                Intensidad: %d
                Entorno: %s
                Minutos disponibles: %s
                Opciones:
                %s
                """.formatted(EMOTIONS.contains(emotion) ? emotion : "otra", intensity, reason,
                availableMinutes == null ? "sin límite indicado" : availableMinutes, catalog));

        try {
            var result = model.call(new Prompt(List.of(INSTRUCTIONS, request))).getResult();
            String answer = result == null ? null : result.getOutput().getText();
            if (answer == null || !answer.strip().matches("[0-9]{1,2}")) {
                return Optional.empty();
            }
            int choice = Integer.parseInt(answer.strip());
            return choice > 0 && choice <= options.size() ? Optional.of(options.get(choice - 1).id()) : Optional.empty();
        } catch (RuntimeException failure) {
            log.warn("Alternative recommendation AI unavailable ({})", failure.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    record Candidate(Long id, String type, int minutes) {
    }
}
