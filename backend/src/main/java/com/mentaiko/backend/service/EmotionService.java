package com.mentaiko.backend.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

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

    private final EmotionRepository emotionRepository;

    public EmotionService(EmotionRepository emotionRepository) {
        this.emotionRepository = emotionRepository;
    }

    @Transactional(readOnly = true)
    public List<EmotionResponse> list(boolean activeOnly) {
        List<Emotion> emotions = activeOnly
                ? emotionRepository.findByActiveTrueOrderByNameAscIdAsc()
                : emotionRepository.findAllByOrderByNameAscIdAsc();
        return emotions.stream().map(this::toResponse).toList();
    }

    @Transactional
    public EmotionResponse create(CreateEmotionRequest request) {
        String name = normalizeSpaces(request.name());
        validateName(name);

        String normalizedName = normalizeForUniqueness(name);
        if (emotionRepository.existsByNormalizedName(normalizedName)) {
            throw new DuplicateEmotionNameException("Ya existe una emocion con ese nombre");
        }

        Emotion emotion = new Emotion();
        emotion.setName(name);
        emotion.setNormalizedName(normalizedName);
        emotion.setActive(true);

        return toResponse(emotionRepository.save(emotion));
    }

    @Transactional
    public EmotionResponse update(Long id, UpdateEmotionRequest request) {
        if (request.name() == null && request.active() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes enviar nombre o estado");
        }

        Emotion emotion = emotionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Emocion no encontrada"));

        if (request.name() != null) {
            String name = normalizeSpaces(request.name());
            validateName(name);
            String normalizedName = normalizeForUniqueness(name);

            if (emotionRepository.existsByNormalizedNameAndIdNot(normalizedName, id)) {
                throw new DuplicateEmotionNameException("Ya existe una emocion con ese nombre");
            }

            emotion.setName(name);
            emotion.setNormalizedName(normalizedName);
        }

        if (request.active() != null) {
            emotion.setActive(request.active());
        }

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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de la emocion es obligatorio");
        }
        if (name.length() < 2 || name.length() > 80) {
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
