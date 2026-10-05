package com.mentaiko.backend.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mentaiko.backend.dto.emotion.CreateEmotionRequest;
import com.mentaiko.backend.dto.emotion.EmotionResponse;
import com.mentaiko.backend.dto.emotion.UpdateEmotionRequest;
import com.mentaiko.backend.entity.Emotion;
import com.mentaiko.backend.exception.DuplicateEmotionNameException;
import com.mentaiko.backend.repository.EmotionRepository;

@Service
public class EmotionService {

    private static final Logger log = LoggerFactory.getLogger(EmotionService.class);

    private final EmotionRepository emotionRepository;

    public EmotionService(EmotionRepository emotionRepository) {
        this.emotionRepository = emotionRepository;
    }

    @Transactional(readOnly = true)
    public List<EmotionResponse> list(boolean activeOnly) {
        List<Emotion> emotions = activeOnly
                ? emotionRepository.findByActiveTrueOrderByNameAscIdAsc()
                : emotionRepository.findAllByOrderByNameAscIdAsc();
        log.debug("Emotion catalog loaded with activeOnly={} and count={}", activeOnly, emotions.size());
        return emotions.stream().map(this::toResponse).toList();
    }

    @Transactional
    public EmotionResponse create(CreateEmotionRequest request) {
        String name = normalizeSpaces(request.name());
        validateName(name);

        String normalizedName = normalizeForUniqueness(name);
        if (emotionRepository.existsByNormalizedName(normalizedName)) {
            log.warn("Emotion creation rejected because normalized name already exists");
            throw new DuplicateEmotionNameException("Ya existe una emocion con ese nombre");
        }

        Emotion emotion = new Emotion();
        emotion.setName(name);
        emotion.setNormalizedName(normalizedName);
        emotion.setActive(true);

        Emotion savedEmotion = emotionRepository.save(emotion);
        log.info("Emotion created with id={} active={}", savedEmotion.getId(), savedEmotion.isActive());
        return toResponse(savedEmotion);
    }

    @Transactional
    public EmotionResponse update(Long id, UpdateEmotionRequest request) {
        if (request.name() == null && request.active() == null) {
            log.warn("Emotion update rejected for id={} because request had no modifiable fields", id);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes enviar nombre o estado");
        }

        Emotion emotion = emotionRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Emotion update rejected because id={} was not found", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Emocion no encontrada");
                });

        if (request.name() != null) {
            String name = normalizeSpaces(request.name());
            validateName(name);
            String normalizedName = normalizeForUniqueness(name);

            if (emotionRepository.existsByNormalizedNameAndIdNot(normalizedName, id)) {
                log.warn("Emotion update rejected for id={} because normalized name already exists", id);
                throw new DuplicateEmotionNameException("Ya existe una emocion con ese nombre");
            }

            emotion.setName(name);
            emotion.setNormalizedName(normalizedName);
        }

        if (request.active() != null) {
            emotion.setActive(request.active());
        }

        log.info("Emotion id={} updated active={}", emotion.getId(), emotion.isActive());
        return toResponse(emotion);
    }

    public String normalizeSpaces(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    public String normalizeForUniqueness(String value) {
        String compact = normalizeSpaces(value).toLowerCase(Locale.ROOT);
        return Normalizer.normalize(compact, Normalizer.Form.NFKC);
    }

    private void validateName(String name) {
        if (name.isBlank()) {
            log.warn("Emotion validation rejected blank name");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de la emocion es obligatorio");
        }
        if (name.length() < 2 || name.length() > 80) {
            log.warn("Emotion validation rejected name length={}", name.length());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre debe tener entre 2 y 80 caracteres");
        }
    }

    private EmotionResponse toResponse(Emotion emotion) {
        return new EmotionResponse(
                emotion.getId(),
                emotion.getName(),
                emotion.isActive(),
                emotion.getCreatedAt(),
                emotion.getUpdatedAt()
        );
    }
}