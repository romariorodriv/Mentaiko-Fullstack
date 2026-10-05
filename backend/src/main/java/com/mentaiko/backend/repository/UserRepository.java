package com.mentaiko.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.enums.Role;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    long countByActiveTrue();

    long countByRole(Role role);

    Page<User> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
