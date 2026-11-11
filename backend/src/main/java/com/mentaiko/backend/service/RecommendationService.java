package com.mentaiko.backend.service;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import org.springframework.jdbc.core.JdbcTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mentaiko.backend.dto.recommendation.RecommendationActivityResponse;
import com.mentaiko.backend.dto.recommendation.RecommendationResponse;
import com.mentaiko.backend.entity.ActivityRecommendation;
import com.mentaiko.backend.entity.Emotion;
import com.mentaiko.backend.entity.EmotionActivityRule;
import com.mentaiko.backend.entity.EmotionalEntry;
import com.mentaiko.backend.entity.MicroActivity;
import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.exception.UserInactiveException;
import com.mentaiko.backend.repository.ActivityRecommendationRepository;
import com.mentaiko.backend.repository.EmotionActivityRuleRepository;
import com.mentaiko.backend.repository.EmotionalEntryRepository;
import com.mentaiko.backend.repository.MicroActivityRepository;
import com.mentaiko.backend.repository.UserRepository;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final ActivityRecommendationRepository recommendationRepository;
    private final EmotionActivityRuleRepository ruleRepository;
    private final EmotionalEntryRepository emotionalEntryRepository;
    private final MicroActivityRepository microActivityRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbc;

    public RecommendationService(
            ActivityRecommendationRepository recommendationRepository,
            EmotionActivityRuleRepository ruleRepository,
            EmotionalEntryRepository emotionalEntryRepository,
            MicroActivityRepository microActivityRepository,
            UserRepository userRepository,
            JdbcTemplate jdbc
    ) {
        this.recommendationRepository = recommendationRepository;
        this.ruleRepository = ruleRepository;
        this.emotionalEntryRepository = emotionalEntryRepository;
        this.microActivityRepository = microActivityRepository;
        this.userRepository = userRepository;
        this.jdbc = jdbc;
    }

    @Transactional
    public RecommendationResponse recommend(String userEmail, Long checkinId) {
        User user = findActiveUser(userEmail);
        EmotionalEntry checkin = emotionalEntryRepository.findByIdAndUserWithEmotion(checkinId, user)
                .orElseThrow(() -> {
                    log.warn("Recommendation rejected for check-in id={} and user id={} because check-in was not found or is not owned by user",
                            checkinId, user.getId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Check-in no encontrado");
                });

        return recommendationRepository.findByCheckin(checkin)
                .map(recommendation -> {
                    log.debug("Existing recommendation id={} returned for check-in id={}",
                            recommendation.getId(), checkinId);
                    return toResponse(recommendation);
                })
                .orElseGet(() -> createRecommendation(user, checkin));
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> listMine(String userEmail) {
        User user = findActiveUser(userEmail);
        List<ActivityRecommendation> recommendations = recommendationRepository.findByUserWithDetails(user);
        log.debug("Recommendations loaded for user id={} count={}", user.getId(), recommendations.size());
        return recommendations.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RecommendationResponse complete(String userEmail, Long id) {
        User user = findActiveUser(userEmail);
        ActivityRecommendation recommendation = recommendationRepository.findByIdAndUserWithDetails(id, user)
                .orElseThrow(() -> {
                    log.warn("Recommendation completion rejected for id={} and user id={} because recommendation was not found or is not owned by user",
                            id, user.getId());
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Recomendacion no encontrada"
                    );
                });

        recommendation.complete();
        log.info("Recommendation id={} completed by user id={}", recommendation.getId(), user.getId());
        return toResponse(recommendation);
    }

    private RecommendationResponse createRecommendation(User user, EmotionalEntry checkin) {
        Emotion emotion = checkin.getEmotion();
        ActivityChoice choice = chooseActivity(emotion, user);

        ActivityRecommendation recommendation = new ActivityRecommendation();
        recommendation.setUser(user);
        recommendation.setCheckin(checkin);
        recommendation.setActivity(choice.activity());
        recommendation.setFallbackUsed(choice.fallbackUsed());
        recommendation.setReason(choice.fallbackUsed()
                ? "No hay una regla activa para " + emotion.getName() + "; sugerimos una actividad activa del catalogo."
                : "Recomendacion asociada a la emocion " + emotion.getName() + ".");

        ActivityRecommendation savedRecommendation = recommendationRepository.save(recommendation);
        log.info("Recommendation created with id={} for user id={} check-in id={} activity id={} fallbackUsed={}",
                savedRecommendation.getId(),
                user.getId(),
                checkin.getId(),
                choice.activity().getId(),
                choice.fallbackUsed());
        return toResponse(savedRecommendation);
    }

    private ActivityChoice chooseActivity(Emotion emotion, User user) {
        List<String> preferredTypes = jdbc.query(
                "SELECT activity_type FROM activity_preferences WHERE user_id=?",
                (rs, row) -> rs.getString(1), user.getId());
        Set<String> preferred = new HashSet<>(preferredTypes);

        var rules = ruleRepository
                .findByEmotionAndActiveTrueAndActivityActiveTrueOrderByPriorityAscActivityTitleAscActivityIdAsc(emotion);
        // If no preferences exist, preserve the original first-by-priority rule.
        if (!rules.isEmpty()) {
            var selected = preferred.isEmpty() ? rules.get(0)
                    : rules.stream()
                        .filter(rule -> preferred.contains(rule.getActivity().getActivityType()))
                        .findFirst().orElse(rules.get(0));
            return new ActivityChoice(selected.getActivity(), false);
        }

        var active = microActivityRepository.findByActiveTrueOrderByTitleAscIdAsc();
        if (active.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No hay microactividades activas disponibles");
        }
        var selected = preferred.isEmpty() ? active.get(0)
                : active.stream().filter(activity -> preferred.contains(activity.getActivityType()))
                    .findFirst().orElse(active.get(0));
        return new ActivityChoice(selected, true);
    }

    private User findActiveUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.warn("Recommendation operation rejected because authenticated user was not found");
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
                });
        if (!user.isActive()) {
            log.warn("Recommendation operation rejected because user id={} is inactive", user.getId());
            throw new UserInactiveException("El usuario esta desactivado");
        }
        return user;
    }

    private RecommendationResponse toResponse(ActivityRecommendation recommendation) {
        MicroActivity activity = recommendation.getActivity();
        return new RecommendationResponse(
                recommendation.getId(),
                recommendation.getCheckin().getId(),
                new RecommendationActivityResponse(
                        activity.getId(),
                        activity.getTitle(),
                        activity.getDescription(),
                        activity.getDurationMinutes(),
                        activity.isActive(),
                        String.valueOf(activity.getCreatedAt()),
                        String.valueOf(activity.getUpdatedAt())
                ),
                recommendation.getReason(),
                recommendation.isFallbackUsed(),
                recommendation.getCreatedAt(),
                recommendation.getCompletedAt()
        );
    }

    private record ActivityChoice(MicroActivity activity, boolean fallbackUsed) {
    }
}