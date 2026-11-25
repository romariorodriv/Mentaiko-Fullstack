package com.mentaiko.backend.service;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashMap;
import org.springframework.jdbc.core.JdbcTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.mentaiko.backend.dto.recommendation.AlternativeRecommendationRequest;
import com.mentaiko.backend.dto.recommendation.RecommendationActivityResponse;
import com.mentaiko.backend.dto.recommendation.RecommendationFeedbackRequest;
import com.mentaiko.backend.dto.recommendation.RecommendationResponse;
import com.mentaiko.backend.entity.ActivityRecommendation;
import com.mentaiko.backend.entity.Emotion;
import com.mentaiko.backend.entity.EmotionActivityRule;
import com.mentaiko.backend.entity.EmotionalEntry;
import com.mentaiko.backend.entity.MicroActivity;
import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.enums.RecommendationDiscardReason;
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
    private final AlternativeRecommendationAi alternativeAi;
    private final TransactionTemplate reads;
    private final TransactionTemplate writes;

    public RecommendationService(
            ActivityRecommendationRepository recommendationRepository,
            EmotionActivityRuleRepository ruleRepository,
            EmotionalEntryRepository emotionalEntryRepository,
            MicroActivityRepository microActivityRepository,
            UserRepository userRepository,
            JdbcTemplate jdbc,
            AlternativeRecommendationAi alternativeAi,
            PlatformTransactionManager transactionManager
    ) {
        this.recommendationRepository = recommendationRepository;
        this.ruleRepository = ruleRepository;
        this.emotionalEntryRepository = emotionalEntryRepository;
        this.microActivityRepository = microActivityRepository;
        this.userRepository = userRepository;
        this.jdbc = jdbc;
        this.alternativeAi = alternativeAi;
        this.reads = new TransactionTemplate(transactionManager);
        this.reads.setReadOnly(true);
        this.writes = new TransactionTemplate(transactionManager);
    }

    @Transactional
    public RecommendationResponse recommend(String userEmail, Long checkinId) {
        return recommend(userEmail, checkinId, null);
    }

    @Transactional
    public RecommendationResponse recommend(String userEmail, Long checkinId, Integer maxDurationMinutes) {
        if (maxDurationMinutes != null && (maxDurationMinutes < 1 || maxDurationMinutes > 120)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tiempo debe estar entre 1 y 120 minutos");
        }
        User user = findActiveUser(userEmail);
        EmotionalEntry checkin = emotionalEntryRepository.findLockedByIdAndUser(checkinId, user)
                .orElseThrow(() -> {
                    log.warn("Recommendation rejected for check-in id={} and user id={} because check-in was not found or is not owned by user",
                            checkinId, user.getId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Check-in no encontrado");
                });

        return recommendationRepository.findByCheckinAndDiscardedAtIsNull(checkin)
                .map(recommendation -> {
                    log.debug("Existing recommendation id={} returned for check-in id={}",
                            recommendation.getId(), checkinId);
                    if (maxDurationMinutes != null && !recommendation.isFallbackUsed()
                            && recommendation.getActivity().getDurationMinutes() > maxDurationMinutes) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Este check-in ya tiene una recomendacion de mayor duracion; conserva la recomendacion original");
                    }
                    return toResponse(recommendation);
                })
                .orElseGet(() -> createRecommendation(user, checkin, maxDurationMinutes));
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
        return complete(userEmail, id, null);
    }

    /**
     * Completa la recomendacion. Si postIntensity no es nulo se guarda junto a la actividad
     * completada; omitirlo no impide completarla. La operacion sigue siendo idempotente:
     * completedAt no cambia en llamadas posteriores.
     */
    @Transactional
    public RecommendationResponse complete(String userEmail, Long id, Integer postIntensity) {
        User user = findActiveUser(userEmail);
        ActivityRecommendation recommendation = recommendationRepository.findLockedByIdAndUser(id, user)
                .orElseThrow(() -> {
                    log.warn("Recommendation completion rejected for id={} and user id={} because recommendation was not found or is not owned by user",
                            id, user.getId());
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Recomendacion no encontrada"
                    );
                });

        if (recommendation.getDiscardedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La recomendación ya fue descartada");
        }
        recommendation.complete();
        if (postIntensity != null) {
            recommendation.recordPostIntensity(postIntensity);
        }
        log.info("Recommendation id={} completed by user id={} withPostIntensity={}",
                recommendation.getId(), user.getId(), postIntensity != null);
        return toResponse(recommendation);
    }

    public RecommendationResponse alternative(String userEmail, Long id, AlternativeRecommendationRequest request) {
        var context = reads.execute(status -> {
            User user = findActiveUser(userEmail);
            var current = recommendationRepository.findByIdAndUserWithDetails(id, user)
                    .orElseThrow(this::recommendationNotFound);
            requirePending(current);
            var checkin = current.getCheckin();
            return new AlternativeContext(checkin.getId(), checkin.getEmotion().getNormalizedName(),
                    checkin.getIntensity(), alternativeCandidates(checkin, request.availableMinutes()).stream()
                    .map(activity -> new AlternativeRecommendationAi.Candidate(
                            activity.getId(), activity.getActivityType(), activity.getDurationMinutes()))
                    .toList());
        });

        // The provider may take time. The write transaction starts after it returns.
        Long suggestedId = alternativeAi.choose(context.emotion(), context.intensity(), request.reason(),
                request.availableMinutes(), context.candidates()).orElse(null);

        return writes.execute(status -> {
            User user = findActiveUser(userEmail);
            var checkin = emotionalEntryRepository.findLockedByIdAndUser(context.checkinId(), user)
                    .orElseThrow(this::recommendationNotFound);
            var current = recommendationRepository.findLockedByIdAndUser(id, user)
                    .orElseThrow(this::recommendationNotFound);
            requirePending(current);
            var candidates = alternativeCandidates(checkin, request.availableMinutes());
            var suggested = candidates.stream().filter(activity -> activity.getId().equals(suggestedId)).findFirst();
            var activity = suggested.orElse(candidates.getFirst());

            current.discard(request.reason());
            recommendationRepository.flush();

            var replacement = new ActivityRecommendation();
            replacement.setUser(user);
            replacement.setCheckin(checkin);
            replacement.setActivity(activity);
            replacement.setFallbackUsed(suggested.isEmpty());
            replacement.setReason(alternativeReason(request.reason()));
            return toResponse(recommendationRepository.save(replacement));
        });
    }

    private List<MicroActivity> alternativeCandidates(EmotionalEntry checkin, Integer availableMinutes) {
        Set<Long> previouslySuggested = new HashSet<>(recommendationRepository.findActivityIdsByCheckin(checkin));
        var candidates = new LinkedHashMap<Long, MicroActivity>();
        var rules = ruleRepository
                .findByEmotionAndActiveTrueAndActivityActiveTrueOrderByPriorityAscActivityTitleAscActivityIdAsc(checkin.getEmotion());
        rules.forEach(rule -> candidates.putIfAbsent(rule.getActivity().getId(), rule.getActivity()));
        microActivityRepository.findByActiveTrueOrderByTitleAscIdAsc()
                .forEach(activity -> candidates.putIfAbsent(activity.getId(), activity));

        var available = candidates.values().stream()
                .filter(activity -> !previouslySuggested.contains(activity.getId()))
                .filter(activity -> availableMinutes == null || activity.getDurationMinutes() <= availableMinutes)
                .toList();
        if (available.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    availableMinutes == null ? "No hay otra microactividad activa disponible"
                            : "No hay otra microactividad que encaje en el tiempo indicado");
        }
        return available;
    }

    private void requirePending(ActivityRecommendation recommendation) {
        if (recommendation.isCompleted() || recommendation.getDiscardedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo puedes cambiar una recomendación pendiente");
        }
    }

    private ResponseStatusException recommendationNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Recomendación no encontrada");
    }

    private String alternativeReason(RecommendationDiscardReason reason) {
        if (reason == null) {
            return "Aquí tienes otra microactividad que puedes probar a tu ritmo.";
        }
        return switch (reason) {
            case TRANSPORTE -> "Si estás en camino, prueba esta opción cuando puedas hacerla con comodidad.";
            case BIBLIOTECA -> "Si necesitas mantener el silencio, prueba esta opción cuando te resulte cómoda.";
            case POCO_ESPACIO -> "Si tienes poco espacio, prueba esta opción si puedes realizarla con comodidad.";
            case POCO_TIEMPO -> "Si tienes poco tiempo, puedes probar esta opción a tu ritmo.";
            case OTRO -> "Está bien cambiar de actividad. Prueba esta opción si te resulta cómoda.";
        };
    }

    @Transactional
    public RecommendationResponse submitFeedback(
            String userEmail,
            Long id,
            RecommendationFeedbackRequest request
    ) {
        User user = findActiveUser(userEmail);
        ActivityRecommendation recommendation = recommendationRepository.findByIdAndUserWithDetails(id, user)
                .orElseThrow(() -> {
                    log.warn("Recommendation feedback rejected for id={} and user id={} because recommendation was not found or is not owned by user",
                            id, user.getId());
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Recomendacion no encontrada"
                    );
                });

        if (!recommendation.isCompleted()) {
            log.warn("Recommendation feedback rejected for id={} and user id={} because the activity is not completed",
                    id, user.getId());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Primero debes realizar la actividad para poder valorarla"
            );
        }

        String comment = request.feedbackComment() == null ? null : request.feedbackComment().strip();
        recommendation.rate(request.helpfulnessRating(), comment == null || comment.isEmpty() ? null : comment);
        log.info("Recommendation id={} rated by user id={}", recommendation.getId(), user.getId());
        return toResponse(recommendation);
    }

    private RecommendationResponse createRecommendation(User user, EmotionalEntry checkin, Integer maxDurationMinutes) {
        Emotion emotion = checkin.getEmotion();
        ActivityChoice choice = chooseActivity(emotion, user, maxDurationMinutes);

        ActivityRecommendation recommendation = new ActivityRecommendation();
        recommendation.setUser(user);
        recommendation.setCheckin(checkin);
        recommendation.setActivity(choice.activity());
        recommendation.setFallbackUsed(choice.fallbackUsed());
        recommendation.setReason(choice.fallbackUsed()
                ? (maxDurationMinutes == null
                    ? "No hay una regla activa para " + emotion.getName() + "; sugerimos una actividad activa del catalogo."
                    : "No existe una regla compatible con " + maxDurationMinutes
                        + " minutos; sugerimos la microactividad activa mas corta de "
                        + choice.activity().getDurationMinutes() + " minutos.")
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

    private ActivityChoice chooseActivity(Emotion emotion, User user, Integer maxDurationMinutes) {
        List<String> preferredTypes = jdbc.query(
                "SELECT activity_type FROM activity_preferences WHERE user_id=?",
                (rs, row) -> rs.getString(1), user.getId());
        Set<String> preferred = new HashSet<>(preferredTypes);

        var rules = ruleRepository
                .findByEmotionAndActiveTrueAndActivityActiveTrueOrderByPriorityAscActivityTitleAscActivityIdAsc(emotion);
        // If no preferences exist, preserve the original first-by-priority rule.
        var compatibleRules = maxDurationMinutes == null ? rules : rules.stream()
                .filter(rule -> rule.getActivity().getDurationMinutes() <= maxDurationMinutes)
                .toList();
        if (!compatibleRules.isEmpty()) {
            var selected = preferred.isEmpty() ? compatibleRules.get(0)
                    : compatibleRules.stream()
                        .filter(rule -> preferred.contains(rule.getActivity().getActivityType()))
                        .findFirst().orElse(compatibleRules.get(0));
            return new ActivityChoice(selected.getActivity(), false);
        }

        var active = microActivityRepository.findByActiveTrueOrderByTitleAscIdAsc();
        if (active.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No hay microactividades activas disponibles");
        }
        var selected = maxDurationMinutes == null
                ? (preferred.isEmpty() ? active.get(0)
                    : active.stream().filter(activity -> preferred.contains(activity.getActivityType()))
                        .findFirst().orElse(active.get(0)))
                : active.stream().min(java.util.Comparator
                    .comparing(MicroActivity::getDurationMinutes)
                    .thenComparing(MicroActivity::getTitle)
                    .thenComparing(MicroActivity::getId)).orElseThrow();
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
                recommendation.getCompletedAt(),
                recommendation.getHelpfulnessRating(),
                recommendation.getFeedbackComment(),
                recommendation.getCheckin().getIntensity(),
                recommendation.getPostIntensity(),
                recommendation.getDiscardedAt(),
                recommendation.getDiscardReason()
        );
    }

    private record ActivityChoice(MicroActivity activity, boolean fallbackUsed) {
    }

    private record AlternativeContext(Long checkinId, String emotion, int intensity,
            List<AlternativeRecommendationAi.Candidate> candidates) {
    }
}
