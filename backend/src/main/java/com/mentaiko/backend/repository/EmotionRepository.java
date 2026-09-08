package com.mentaiko.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mentaiko.backend.entity.EmotionalEntry;

public interface EmotionalEntryRepository extends JpaRepository<EmotionalEntry, Long> {
}
