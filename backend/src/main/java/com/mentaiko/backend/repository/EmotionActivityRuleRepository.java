package com.mentaiko.backend.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mentaiko.backend.entity.Emotion;
import com.mentaiko.backend.entity.EmotionActivityRule;

public interface EmotionActivityRuleRepository extends JpaRepository<EmotionActivityRule, Long> {

    java.util.List<EmotionActivityRule> findByEmotionAndActiveTrueAndActivityActiveTrueOrderByPriorityAscActivityTitleAscActivityIdAsc(
            Emotion emotion
    );

    java.util.Optional<EmotionActivityRule> findFirstByEmotionAndActiveTrueAndActivityActiveTrueOrderByPriorityAscActivityTitleAscActivityIdAsc(
            Emotion emotion
    );

    // HU-33: administracion de reglas.
    @EntityGraph(attributePaths = {"emotion", "activity"})
    java.util.List<EmotionActivityRule> findAllByOrderByEmotionNameAscPriorityAscActivityTitleAscIdAsc();

    @EntityGraph(attributePaths = {"emotion", "activity"})
    java.util.Optional<EmotionActivityRule> findWithDetailsById(Long id);

    boolean existsByEmotionIdAndActivityId(Long emotionId, Long activityId);
}
