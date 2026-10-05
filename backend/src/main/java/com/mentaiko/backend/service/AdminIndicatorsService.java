package com.mentaiko.backend.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mentaiko.backend.dto.admin.AdminIndicatorsResponse;
import com.mentaiko.backend.dto.report.DistributionItemResponse;
import com.mentaiko.backend.entity.EmotionalEntry;
import com.mentaiko.backend.repository.ActivityRecommendationRepository;
import com.mentaiko.backend.repository.EmotionalEntryRepository;
import com.mentaiko.backend.repository.UserRepository;

@Service
public class AdminIndicatorsService {

    private static final Logger log = LoggerFactory.getLogger(AdminIndicatorsService.class);

    private final UserRepository userRepository;
    private final EmotionalEntryRepository emotionalEntryRepository;
    private final ActivityRecommendationRepository recommendationRepository;

    public AdminIndicatorsService(
            UserRepository userRepository,
            EmotionalEntryRepository emotionalEntryRepository,
            ActivityRecommendationRepository recommendationRepository
    ) {
        this.userRepository = userRepository;
        this.emotionalEntryRepository = emotionalEntryRepository;
        this.recommendationRepository = recommendationRepository;
    }

    @Transactional(readOnly = true)
    public AdminIndicatorsResponse indicators() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByActiveTrue();
        List<EmotionalEntry> checkins = emotionalEntryRepository.findAllWithEmotion();
        long recommendationsGenerated = recommendationRepository.count();
        long recommendationsCompleted = recommendationRepository.countByCompletedTrue();
        List<DistributionItemResponse> distribution = emotionDistribution(checkins);
        log.info("Admin indicators generated totalUsers={} activeUsers={} totalCheckins={} recommendationsGenerated={} recommendationsCompleted={}",
                totalUsers,
                activeUsers,
                checkins.size(),
                recommendationsGenerated,
                recommendationsCompleted);
        log.debug("Admin indicators emotion distribution count={}", distribution.size());

        return new AdminIndicatorsResponse(
                totalUsers,
                activeUsers,
                checkins.size(),
                distribution,
                recommendationsGenerated,
                recommendationsCompleted,
                recommendationsGenerated == 0 ? 0 : roundOne((recommendationsCompleted * 100.0) / recommendationsGenerated),
                distribution.isEmpty() ? "Sin datos" : distribution.get(0).label()
        );
    }

    private List<DistributionItemResponse> emotionDistribution(List<EmotionalEntry> checkins) {
        if (checkins.isEmpty()) {
            return List.of();
        }

        Map<String, Long> counts = new LinkedHashMap<>();
        for (EmotionalEntry checkin : checkins) {
            String label = checkin.getEmotion().getName();
            counts.put(label, counts.getOrDefault(label, 0L) + 1);
        }

        long total = checkins.size();
        return counts.entrySet().stream()
                .map(entry -> new DistributionItemResponse(
                        entry.getKey(),
                        entry.getValue(),
                        roundOne((entry.getValue() * 100.0) / total)
                ))
                .sorted(Comparator
                        .comparingLong(DistributionItemResponse::count).reversed()
                        .thenComparing(DistributionItemResponse::label))
                .toList();
    }

    private double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}