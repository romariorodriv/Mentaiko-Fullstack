package com.mentaiko.backend.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mentaiko.backend.entity.EmotionalEntry;
import com.mentaiko.backend.entity.User;

public interface EmotionalEntryRepository
                extends JpaRepository<EmotionalEntry, Long> {

        // Conserva este método
        @Query("""
                        select entry
                        from EmotionalEntry entry
                        join fetch entry.emotion
                        where entry.id = :id
                          and entry.user = :user
                        """)
        Optional<EmotionalEntry> findByIdAndUserWithEmotion(
                        @Param("id") Long id,
                        @Param("user") User user);

        // Pega aquí los dos nuevos métodos:
        // findHistory(...)
        // findHistoryByContext(...)

        @Query(value = """
                        select entry
                        from EmotionalEntry entry
                        join fetch entry.emotion
                        where entry.user = :user
                          and entry.createdAt >= coalesce(:from, entry.createdAt)
                          and entry.createdAt <= coalesce(:to, entry.createdAt)
                        """, countQuery = """
                        select count(entry)
                        from EmotionalEntry entry
                        where entry.user = :user
                          and entry.createdAt >= coalesce(:from, entry.createdAt)
                          and entry.createdAt <= coalesce(:to, entry.createdAt)
                        """)
        Page<EmotionalEntry> findHistory(
                        @Param("user") User user,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to,
                        Pageable pageable);

        @Query(value = """
                        select entry
                        from EmotionalEntry entry
                        join fetch entry.emotion
                        where entry.user = :user
                          and entry.createdAt >= coalesce(:from, entry.createdAt)
                          and entry.createdAt <= coalesce(:to, entry.createdAt)
                          and lower(entry.context) like
                              lower(concat('%', :context, '%'))
                        """, countQuery = """
                        select count(entry)
                        from EmotionalEntry entry
                        where entry.user = :user
                          and entry.createdAt >= coalesce(:from, entry.createdAt)
                          and entry.createdAt <= coalesce(:to, entry.createdAt)
                          and lower(entry.context) like
                              lower(concat('%', :context, '%'))
                        """)
        Page<EmotionalEntry> findHistoryByContext(
                        @Param("user") User user,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to,
                        @Param("context") String context,
                        Pageable pageable);
}