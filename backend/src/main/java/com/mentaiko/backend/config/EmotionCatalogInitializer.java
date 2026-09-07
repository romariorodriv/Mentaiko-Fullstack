package com.mentaiko.backend.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mentaiko.backend.entity.Emotion;
import com.mentaiko.backend.repository.EmotionRepository;
import com.mentaiko.backend.service.EmotionService;

@Component
public class EmotionCatalogInitializer implements ApplicationRunner {

    private static final List<String> DEFAULT_EMOTIONS = List.of(
            "Alegria",
            "Calma",
            "Tristeza",
            "Ansiedad",
            "Estres",
            "Enojo",
            "Cansancio",
            "Motivacion"
    );

    private final EmotionRepository emotionRepository;
    private final EmotionService emotionService;

    public EmotionCatalogInitializer(EmotionRepository emotionRepository, EmotionService emotionService) {
        this.emotionRepository = emotionRepository;
        this.emotionService = emotionService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initializeDefaultCatalog();
    }

    @Transactional
    public void initializeDefaultCatalog() {
        if (emotionRepository.count() > 0) {
            return;
        }

        for (String defaultName : DEFAULT_EMOTIONS) {
            Emotion emotion = new Emotion();
            emotion.setName(defaultName);
            emotion.setNormalizedName(emotionService.normalizeForUniqueness(defaultName));
            emotion.setActive(true);
            emotionRepository.save(emotion);
        }
    }
}
