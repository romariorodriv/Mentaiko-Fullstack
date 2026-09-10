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

public interface EmotionalEntryRepository extends JpaRepository<EmotionalEntry, Long> {

    @Query("""
            select entry
            from EmotionalEntry entry
            join fetch entry.emotion
            where entry.id = :id
              and entry.user = :user
            """)
    Optional<EmotionalEntry> findByIdAndUserWithEmotion(@Param("id") Long id, @Param("user") User user);

    @Query(
            value = """
                    select entry
                    from EmotionalEntry entry
                    join fetch entry.emotion
                    where entry.user = :user
                      and (:from is null or entry.createdAt >= :from)
                      and (:to is null or entry.createdAt <= :to)
                      and (:context is null or lower(entry.context) like lower(concat('%', :context, '%')))
                    """,
            countQuery = """
                    select count(entry)
                    from EmotionalEntry entry
                    where entry.user = :user
                      and (:from is null or entry.createdAt >= :from)
                      and (:to is null or entry.createdAt <= :to)
                      and (:context is null or lower(entry.context) like lower(concat('%', :context, '%')))
                    """
    )
    Page<EmotionalEntry> findHistory(
            @Param("user") User user,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("context") String context,
            Pageable pageable
    );
}
