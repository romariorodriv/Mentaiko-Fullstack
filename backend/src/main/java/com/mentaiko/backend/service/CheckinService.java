package com.mentaiko.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mentaiko.backend.dto.checkin.CheckinEmotionResponse;
import com.mentaiko.backend.dto.checkin.CheckinResponse;
import com.mentaiko.backend.dto.checkin.CreateCheckinRequest;
import com.mentaiko.backend.entity.Emotion;
import com.mentaiko.backend.entity.EmotionalEntry;
import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.repository.EmotionRepository;
import com.mentaiko.backend.repository.EmotionalEntryRepository;
import com.mentaiko.backend.repository.UserRepository;

@Service
public class CheckinService {

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
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalido o usuario inactivo");
        }

        Emotion emotion = emotionRepository.findById(request.emotionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Emocion no encontrada"));

        if (!emotion.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La emocion seleccionada no esta activa");
        }

        EmotionalEntry entry = new EmotionalEntry();
        entry.setUser(user);
        entry.setEmotion(emotion);
        entry.setIntensity(request.intensity());
        entry.setContext(normalizeSpaces(request.context()));
        entry.setNote(normalizeOptional(request.note()));

        return toResponse(emotionalEntryRepository.save(entry));
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
