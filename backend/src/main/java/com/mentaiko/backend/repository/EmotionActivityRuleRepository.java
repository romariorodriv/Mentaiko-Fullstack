package com.mentaiko.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mentaiko.backend.entity.Emotion;
import com.mentaiko.backend.entity.EmotionActivityRule;

public interface EmotionActivityRuleRepository extends JpaRepository<EmotionActivityRule, Long> {

    java.util.Optional<EmotionActivityRule> findFirstByEmotionAndActiveTrueAndActivityActiveTrueOrderByPriorityAscActivityTitleAscActivityIdAsc(
            Emotion emotion
    );
}
