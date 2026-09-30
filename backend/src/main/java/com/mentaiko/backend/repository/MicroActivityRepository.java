package com.mentaiko.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mentaiko.backend.entity.MicroActivity;

public interface MicroActivityRepository extends JpaRepository<MicroActivity, Long> {

    boolean existsByNormalizedTitle(String normalizedTitle);

    boolean existsByNormalizedTitleAndIdNot(String normalizedTitle, Long id);

    List<MicroActivity> findByActiveTrueOrderByTitleAscIdAsc();

    List<MicroActivity> findAllByOrderByTitleAscIdAsc();
}
