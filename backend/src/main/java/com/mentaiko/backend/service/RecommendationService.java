package com.mentaiko.backend.service;

import java.util.List;

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

    public RecommendationService(
            ActivityRecommendationRepository recommendationRepository,
            EmotionActivityRuleRepository ruleRepository,
            EmotionalEntryRepository emotionalEntryRepository,
            MicroActivityRepository microActivityRepository,
            UserRepository userRepository
    ) {
        this.recommendationRepository = recommendationRepository;
        this.ruleRepository = ruleRepository;
        this.emotionalEntryRepository = emotionalEntryRepository;
        this.microActivityRepository = microActivityRepository;
        this.userRepository = userRepository;
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
        ActivityChoice choice = chooseActivity(emotion);

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

    private ActivityChoice chooseActivity(Emotion emotion) {
        return ruleRepository.findFirstByEmotionAndActiveTrueAndActivityActiveTrueOrderByPriorityAscActivityTitleAscActivityIdAsc(emotion)
                .map(rule -> new ActivityChoice(rule.getActivity(), false))
                .orElseGet(() -> microActivityRepository.findFirstByActiveTrueOrderByTitleAscIdAsc()
                        .map(activity -> new ActivityChoice(activity, true))
                        .orElseThrow(() -> {
                            log.warn("Recommendation creation rejected because there are no active micro-activities");
                            return new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "No hay microactividades activas disponibles"
                            );
                        }));
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