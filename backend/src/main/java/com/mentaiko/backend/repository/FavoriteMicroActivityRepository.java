package com.mentaiko.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mentaiko.backend.entity.FavoriteMicroActivity;

public interface FavoriteMicroActivityRepository extends JpaRepository<FavoriteMicroActivity, Long> {

    @EntityGraph(attributePaths = "activity")
    List<FavoriteMicroActivity> findByUser_IdOrderByCreatedAtDescIdDesc(Long userId);

    @EntityGraph(attributePaths = "activity")
    Optional<FavoriteMicroActivity> findByUser_IdAndActivity_Id(Long userId, Long activityId);

    boolean existsByUser_IdAndActivity_Id(Long userId, Long activityId);

    long deleteByUser_IdAndActivity_Id(Long userId, Long activityId);
}
