package com.mentaiko.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mentaiko.backend.dto.admin.AdminCheckinEmotionResponse;
import com.mentaiko.backend.dto.admin.AdminCheckinResponse;
import com.mentaiko.backend.dto.admin.AdminCheckinUserResponse;
import com.mentaiko.backend.dto.admin.AdminDashboardResponse;
import com.mentaiko.backend.dto.admin.AdminUserResponse;
import com.mentaiko.backend.dto.common.PageResponse;
import com.mentaiko.backend.entity.Emotion;
import com.mentaiko.backend.entity.EmotionalEntry;
import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.enums.Role;
import com.mentaiko.backend.repository.ActivityRecommendationRepository;
import com.mentaiko.backend.repository.EmotionRepository;
import com.mentaiko.backend.repository.EmotionalEntryRepository;
import com.mentaiko.backend.repository.MicroActivityRepository;
import com.mentaiko.backend.repository.UserRepository;

@Service
public class AdminDashboardService {

    private static final int MAX_PAGE_SIZE = 50;

    private final UserRepository userRepository;
    private final EmotionalEntryRepository emotionalEntryRepository;
    private final EmotionRepository emotionRepository;
    private final MicroActivityRepository microActivityRepository;
    private final ActivityRecommendationRepository recommendationRepository;

    public AdminDashboardService(
            UserRepository userRepository,
            EmotionalEntryRepository emotionalEntryRepository,
            EmotionRepository emotionRepository,
            MicroActivityRepository microActivityRepository,
            ActivityRecommendationRepository recommendationRepository
    ) {
        this.userRepository = userRepository;
        this.emotionalEntryRepository = emotionalEntryRepository;
        this.emotionRepository = emotionRepository;
        this.microActivityRepository = microActivityRepository;
        this.recommendationRepository = recommendationRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard() {
        return new AdminDashboardResponse(
                userRepository.count(),
                userRepository.countByActiveTrue(),
                userRepository.countByRole(Role.ADMIN),
                emotionalEntryRepository.count(),
                emotionRepository.count(),
                microActivityRepository.count(),
                recommendationRepository.count(),
                recommendationRepository.countByCompletedTrue()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> users(int page, int size) {
        validatePagination(page, size);
        Page<User> result = userRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(page, size));
        return new PageResponse<>(
                result.getContent().stream().map(this::toUserResponse).toList(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminCheckinResponse> checkins(int page, int size) {
        validatePagination(page, size);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<EmotionalEntry> result = emotionalEntryRepository.findAllForAdmin(pageable);
        return new PageResponse<>(
                result.getContent().stream().map(this::toCheckinResponse).toList(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        );
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El numero de pagina no puede ser negativo");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tamano de pagina debe estar entre 1 y 50");
        }
    }

    private AdminUserResponse toUserResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getBirthDate(),
                user.getUniversity(),
                user.getCareer(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }

    private AdminCheckinResponse toCheckinResponse(EmotionalEntry entry) {
        User user = entry.getUser();
        Emotion emotion = entry.getEmotion();
        return new AdminCheckinResponse(
                entry.getId(),
                new AdminCheckinUserResponse(user.getId(), user.getName(), user.getEmail()),
                new AdminCheckinEmotionResponse(emotion.getId(), emotion.getName()),
                entry.getIntensity(),
                entry.getContext(),
                entry.getCreatedAt()
        );
    }
}
