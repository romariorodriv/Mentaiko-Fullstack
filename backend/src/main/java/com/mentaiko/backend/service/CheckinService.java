package com.mentaiko.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mentaiko.backend.dto.checkin.CheckinEmotionResponse;
import com.mentaiko.backend.dto.checkin.CheckinResponse;
import com.mentaiko.backend.dto.checkin.CreateCheckinRequest;
import com.mentaiko.backend.dto.checkin.UpdateCheckinRequest;
import com.mentaiko.backend.dto.common.PageResponse;
import com.mentaiko.backend.entity.Emotion;
import com.mentaiko.backend.entity.EmotionalEntry;
import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.repository.EmotionRepository;
import com.mentaiko.backend.repository.EmotionalEntryRepository;
import com.mentaiko.backend.repository.ActivityRecommendationRepository;
import com.mentaiko.backend.repository.UserRepository;

@Service
public class CheckinService {

    private static final Logger log = LoggerFactory.getLogger(CheckinService.class);

    private static final int MAX_PAGE_SIZE = 50;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Lima");

    private final EmotionalEntryRepository emotionalEntryRepository;
    private final EmotionRepository emotionRepository;
    private final UserRepository userRepository;
    private final ActivityRecommendationRepository recommendationRepository;

    public CheckinService(
            EmotionalEntryRepository emotionalEntryRepository,
            EmotionRepository emotionRepository,
            UserRepository userRepository,
            ActivityRecommendationRepository recommendationRepository) {
        this.emotionalEntryRepository = emotionalEntryRepository;
        this.emotionRepository = emotionRepository;
        this.userRepository = userRepository;
        this.recommendationRepository = recommendationRepository;
    }

    @Transactional
    public CheckinResponse create(String userEmail, CreateCheckinRequest request) {
        User user = findActiveUser(userEmail);
        Emotion emotion = findActiveEmotion(request.emotionId());

        EmotionalEntry entry = new EmotionalEntry();
        entry.setUser(user);
        entry.setEmotion(emotion);
        entry.setIntensity(request.intensity());
        entry.setContext(normalizeSpaces(request.context()));
        entry.setNote(normalizeOptional(request.note()));

        EmotionalEntry savedEntry = emotionalEntryRepository.save(entry);
        log.info("Check-in created with id={} for user id={} and emotion id={}",
                savedEntry.getId(), user.getId(), emotion.getId());
        return toResponse(savedEntry);
    }

    @Transactional
    public CheckinResponse update(String userEmail, Long id, UpdateCheckinRequest request) {
        User user = findActiveUser(userEmail);
        EmotionalEntry entry = emotionalEntryRepository.findByIdAndUserWithEmotion(id, user)
                .orElseThrow(() -> {
                    log.warn("Check-in update rejected for id={} and user id={} because record was not found or is not owned by user",
                            id, user.getId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Check-in no encontrado");
                });
        Emotion emotion = findActiveEmotion(request.emotionId());

        entry.setEmotion(emotion);
        entry.setIntensity(request.intensity());
        entry.setContext(normalizeSpaces(request.context()));
        entry.setNote(normalizeOptional(request.note()));

        EmotionalEntry savedEntry = emotionalEntryRepository.save(entry);
        log.info("Check-in id={} updated by user id={}", savedEntry.getId(), user.getId());
        return toResponse(savedEntry);
    }

    @Transactional
    public void delete(String userEmail, Long id) {
        User user = findActiveUser(userEmail);
        EmotionalEntry entry = emotionalEntryRepository.findByIdAndUserWithEmotion(id, user)
                .orElseThrow(() -> {
                    log.warn("Check-in delete rejected for id={} and user id={} because record was not found or is not owned by user",
                            id, user.getId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Check-in no encontrado");
                });

        recommendationRepository.deleteByCheckin(entry);
        emotionalEntryRepository.delete(entry);
        log.info("Check-in id={} deleted by user id={}", id, user.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<CheckinResponse> list(
            String userEmail,
            LocalDate from,
            LocalDate to,
            String context,
            int page,
            int size) {
        validatePagination(page, size);

        if (from != null && to != null && from.isAfter(to)) {
            log.warn("Check-in list rejected because date range is invalid");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha inicial no puede ser posterior a la fecha final");
        }

        User user = findActiveUser(userEmail);

        LocalDateTime fromDateTime = from == null ? null : from.atStartOfDay(BUSINESS_ZONE).toLocalDateTime();
        LocalDateTime toDateTime = to == null ? null : to.atTime(LocalTime.MAX);
        String normalizedContext = normalizeFilter(context);

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<EmotionalEntry> result = normalizedContext == null
                ? emotionalEntryRepository.findHistory(
                        user,
                        fromDateTime,
                        toDateTime,
                        pageable)
                : emotionalEntryRepository.findHistoryByContext(
                        user,
                        fromDateTime,
                        toDateTime,
                        normalizedContext,
                        pageable);

        log.debug("Check-in list loaded for user id={} page={} size={} totalElements={} totalPages={}",
                user.getId(), page, size, result.getTotalElements(), result.getTotalPages());
        return new PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize());
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            log.warn("Check-in list rejected because page={} is negative", page);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El numero de pagina no puede ser negativo");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            log.warn("Check-in list rejected because size={} is outside allowed range", size);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tamano de pagina debe estar entre 1 y 50");
        }
    }

    private User findActiveUser(String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> {
                    log.warn("Check-in operation rejected because authenticated user was not found");
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
                });
        if (!user.isActive()) {
            log.warn("Check-in operation rejected because user id={} is inactive", user.getId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalido o usuario inactivo");
        }
        return user;
    }

    private Emotion findActiveEmotion(Long emotionId) {
        Emotion emotion = emotionRepository.findById(emotionId)
                .orElseThrow(() -> {
                    log.warn("Check-in operation rejected because emotion id={} was not found", emotionId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Emocion no encontrada");
                });
        if (!emotion.isActive()) {
            log.warn("Check-in operation rejected because emotion id={} is inactive", emotionId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La emocion seleccionada no esta activa");
        }
        return emotion;
    }

    private String normalizeSpaces(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return normalizeSpaces(value);
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return normalizeSpaces(value);
    }

    private CheckinResponse toResponse(EmotionalEntry entry) {
        Emotion emotion = entry.getEmotion();
        return new CheckinResponse(
                entry.getId(),
                new CheckinEmotionResponse(emotion.getId(), emotion.getName()),
                entry.getIntensity(),
                entry.getContext(),
                entry.getNote(),
                entry.getCreatedAt());
    }
}