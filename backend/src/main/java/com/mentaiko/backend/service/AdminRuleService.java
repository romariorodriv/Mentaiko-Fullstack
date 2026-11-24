package com.mentaiko.backend.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mentaiko.backend.dto.admin.AdminRuleResponse;
import com.mentaiko.backend.dto.admin.CreateAdminRuleRequest;
import com.mentaiko.backend.dto.admin.UpdateAdminRuleRequest;
import com.mentaiko.backend.entity.Emotion;
import com.mentaiko.backend.entity.EmotionActivityRule;
import com.mentaiko.backend.entity.MicroActivity;
import com.mentaiko.backend.exception.DuplicateRuleException;
import com.mentaiko.backend.repository.EmotionActivityRuleRepository;
import com.mentaiko.backend.repository.EmotionRepository;
import com.mentaiko.backend.repository.MicroActivityRepository;

/**
 * HU-33: administracion de las reglas emocion -> microactividad.
 * Solo gestiona las reglas; RecommendationService ya consume las activas ordenadas por prioridad.
 * Las recomendaciones guardan la actividad elegida (no la regla), por eso activar, desactivar o
 * repriorizar una regla nunca altera las recomendaciones ya emitidas.
 */
@Service
public class AdminRuleService {

    private static final Logger log = LoggerFactory.getLogger(AdminRuleService.class);
    private static final String DUPLICATE_MESSAGE = "Ya existe una regla para esa emocion y microactividad";

    private final EmotionActivityRuleRepository ruleRepository;
    private final EmotionRepository emotionRepository;
    private final MicroActivityRepository microActivityRepository;

    public AdminRuleService(
            EmotionActivityRuleRepository ruleRepository,
            EmotionRepository emotionRepository,
            MicroActivityRepository microActivityRepository
    ) {
        this.ruleRepository = ruleRepository;
        this.emotionRepository = emotionRepository;
        this.microActivityRepository = microActivityRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminRuleResponse> list() {
        List<EmotionActivityRule> rules = ruleRepository.findAllByOrderByEmotionNameAscPriorityAscActivityTitleAscIdAsc();
        log.debug("Emotion-activity rules loaded count={}", rules.size());
        return rules.stream().map(this::toResponse).toList();
    }

    @Transactional
    public AdminRuleResponse create(CreateAdminRuleRequest request) {
        Emotion emotion = emotionRepository.findById(request.emotionId())
                .orElseThrow(() -> {
                    log.warn("Rule creation rejected because emotion id={} was not found", request.emotionId());
                    return new ResponseStatusException(HttpStatus.BAD_REQUEST, "La emocion seleccionada no existe");
                });
        if (!emotion.isActive()) {
            log.warn("Rule creation rejected because emotion id={} is inactive", emotion.getId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La emocion seleccionada esta inactiva");
        }

        MicroActivity activity = microActivityRepository.findById(request.activityId())
                .orElseThrow(() -> {
                    log.warn("Rule creation rejected because activity id={} was not found", request.activityId());
                    return new ResponseStatusException(HttpStatus.BAD_REQUEST, "La microactividad seleccionada no existe");
                });
        if (!activity.isActive()) {
            log.warn("Rule creation rejected because activity id={} is inactive", activity.getId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La microactividad seleccionada esta inactiva");
        }

        if (ruleRepository.existsByEmotionIdAndActivityId(emotion.getId(), activity.getId())) {
            log.warn("Rule creation rejected because emotion id={} and activity id={} are already linked",
                    emotion.getId(), activity.getId());
            throw new DuplicateRuleException(DUPLICATE_MESSAGE);
        }

        EmotionActivityRule rule = new EmotionActivityRule();
        rule.setEmotion(emotion);
        rule.setActivity(activity);
        rule.setPriority(request.priority());
        rule.setActive(request.active() == null || request.active());

        EmotionActivityRule savedRule;
        try {
            savedRule = ruleRepository.saveAndFlush(rule);
        } catch (DataIntegrityViolationException exception) {
            // Otra peticion concurrente creo la misma pareja: la restriccion unica la rechaza.
            log.warn("Rule creation rejected by unique constraint for emotion id={} and activity id={}",
                    emotion.getId(), activity.getId());
            throw new DuplicateRuleException(DUPLICATE_MESSAGE);
        }
        log.info("Rule created with id={} emotion id={} activity id={} priority={} active={}",
                savedRule.getId(), emotion.getId(), activity.getId(), savedRule.getPriority(), savedRule.isActive());
        return toResponse(savedRule);
    }

    @Transactional
    public AdminRuleResponse update(Long id, UpdateAdminRuleRequest request) {
        if (request.priority() == null && request.active() == null) {
            log.warn("Rule update rejected for id={} because request had no modifiable fields", id);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes enviar prioridad o estado");
        }

        EmotionActivityRule rule = ruleRepository.findWithDetailsById(id)
                .orElseThrow(() -> {
                    log.warn("Rule update rejected because id={} was not found", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Regla no encontrada");
                });

        if (request.priority() != null) {
            rule.setPriority(request.priority());
        }
        if (request.active() != null) {
            rule.setActive(request.active());
        }

        log.info("Rule id={} updated priority={} active={}", rule.getId(), rule.getPriority(), rule.isActive());
        return toResponse(rule);
    }

    private AdminRuleResponse toResponse(EmotionActivityRule rule) {
        Emotion emotion = rule.getEmotion();
        MicroActivity activity = rule.getActivity();
        return new AdminRuleResponse(
                rule.getId(),
                emotion.getId(),
                emotion.getName(),
                emotion.isActive(),
                activity.getId(),
                activity.getTitle(),
                activity.isActive(),
                rule.getPriority(),
                rule.isActive(),
                rule.getCreatedAt()
        );
    }
}
