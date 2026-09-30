package com.mentaiko.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mentaiko.backend.entity.ActivityRecommendation;
import com.mentaiko.backend.entity.EmotionalEntry;
import com.mentaiko.backend.entity.User;

public interface ActivityRecommendationRepository extends JpaRepository<ActivityRecommendation, Long> {

    Optional<ActivityRecommendation> findByCheckin(EmotionalEntry checkin);

    @Query("""
            select recommendation
            from ActivityRecommendation recommendation
            join fetch recommendation.activity
            join fetch recommendation.checkin
            where recommendation.id = :id
              and recommendation.user = :user
            """)
    Optional<ActivityRecommendation> findByIdAndUserWithDetails(
            @Param("id") Long id,
            @Param("user") User user
    );

    @Query("""
            select recommendation
            from ActivityRecommendation recommendation
            join fetch recommendation.activity
            join fetch recommendation.checkin
            where recommendation.user = :user
            order by recommendation.createdAt desc, recommendation.id desc
            """)
    List<ActivityRecommendation> findByUserWithDetails(@Param("user") User user);
}
