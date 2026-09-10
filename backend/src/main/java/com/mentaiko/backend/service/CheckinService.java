package com.mentaiko.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

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
import com.mentaiko.backend.repository.UserRepository;

@Service
public class CheckinService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Lima");

    private final EmotionalEntryRepository emotionalEntryRepository;
    private final EmotionRepository emotionRepository;
    private final UserRepository userRepository;

    public CheckinService(
            EmotionalEntryRepository emotionalEntryRepository,
            EmotionRepository emotionRepository,
            UserRepository userRepository
    ) {
        this.emotionalEntryRepository = emotionalEntryRepository;
        this.emotionRepository = emotionRepository;
        this.userRepository = userRepository;
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

        return toResponse(emotionalEntryRepository.save(entry));
    }

    @Transactional
    public CheckinResponse update(String userEmail, Long id, UpdateCheckinRequest request) {
        User user = findActiveUser(userEmail);
        EmotionalEntry entry = emotionalEntryRepository.findByIdAndUserWithEmotion(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Check-in no encontrado"));
        Emotion emotion = findActiveEmotion(request.emotionId());

        entry.setEmotion(emotion);
        entry.setIntensity(request.intensity());
        entry.setContext(normalizeSpaces(request.context()));
        entry.setNote(normalizeOptional(request.note()));

        return toResponse(emotionalEntryRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public PageResponse<CheckinResponse> list(
            String userEmail,
            LocalDate from,
            LocalDate to,
            String context,
            int page,
            int size
    ) {
        validatePagination(page, size);

        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha inicial no puede ser posterior a la fecha final");
        }

        User user = findActiveUser(userEmail);

        LocalDateTime fromDateTime = from == null ? null : from.atStartOfDay(BUSINESS_ZONE).toLocalDateTime();
        LocalDateTime toDateTime = to == null ? null : to.atTime(LocalTime.MAX);
        String normalizedContext = normalizeFilter(context);

        Page<EmotionalEntry> result = emotionalEntryRepository.findHistory(
                user,
                fromDateTime,
                toDateTime,
                normalizedContext,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return new PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        );
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El numero de pagina no puede ser negativo");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tamano de pagina debe estar entre 1 y 50");
        }
    }

    private User findActiveUser(String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalido o usuario inactivo");
        }
        return user;
    }

    private Emotion findActiveEmotion(Long emotionId) {
        Emotion emotion = emotionRepository.findById(emotionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Emocion no encontrada"));
        if (!emotion.isActive()) {
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
                entry.getCreatedAt()
        );
    }
}
