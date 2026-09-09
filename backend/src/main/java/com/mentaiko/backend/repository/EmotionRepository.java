package com.mentaiko.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mentaiko.backend.entity.Emotion;

public interface EmotionRepository extends JpaRepository<Emotion, Long> {

    Optional<Emotion> findByNameIgnoreCase(String name);

    boolean existsByNormalizedName(String normalizedName);

    boolean existsByNormalizedNameAndIdNot(String normalizedName, Long id);

    List<Emotion> findByActiveTrueOrderByNameAscIdAsc();

    List<Emotion> findAllByOrderByNameAscIdAsc();
}
