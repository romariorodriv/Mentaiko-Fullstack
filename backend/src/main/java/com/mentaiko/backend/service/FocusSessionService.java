package com.mentaiko.backend.service;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mentaiko.backend.dto.focus.ConfirmFocusSessionRequest;
import com.mentaiko.backend.dto.focus.FocusSessionResponse;
import com.mentaiko.backend.entity.ActivityRecommendation;
import com.mentaiko.backend.entity.FocusSession;
import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.exception.UserInactiveException;
import com.mentaiko.backend.repository.ActivityRecommendationRepository;
import com.mentaiko.backend.repository.FocusSessionRepository;
import com.mentaiko.backend.repository.UserRepository;

@Service
public class FocusSessionService {
    private final FocusSessionRepository sessions;
    private final UserRepository users;
    private final MicroActivityService activities;
    private final ActivityRecommendationRepository recommendations;
    private final RecommendationService recommendationService;

    public FocusSessionService(FocusSessionRepository sessions, UserRepository users, MicroActivityService activities,
            ActivityRecommendationRepository recommendations, RecommendationService recommendationService) {
        this.sessions = sessions;
        this.users = users;
        this.activities = activities;
        this.recommendations = recommendations;
        this.recommendationService = recommendationService;
    }

    @Transactional
    public FocusSessionResponse confirm(String email, ConfirmFocusSessionRequest request) {
        User user = users.findLockedByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (!user.isActive()) {
            throw new UserInactiveException("El usuario está desactivado");
        }

        var recorded = sessions.findByUserAndRequestId(user, request.requestId());
        if (recorded.isPresent()) {
            FocusSession session = recorded.get();
            if (!session.getActivityId().equals(request.activityId())
                    || !Objects.equals(session.getRecommendationId(), request.recommendationId())
                    || session.getDurationSeconds() != request.durationSeconds()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El identificador ya corresponde a otra sesión");
            }
            return response(session);
        }

        var activity = activities.findActive(request.activityId());
        if (activity.getDurationMinutes() * 60 != request.durationSeconds()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La duración cambió. Abre el modo guiado de nuevo.");
        }

        ActivityRecommendation recommendation = null;
        if (request.recommendationId() != null) {
            recommendation = recommendations.findByIdAndUserWithDetails(request.recommendationId(), user)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recomendación no encontrada"));
            if (!recommendation.getActivity().getId().equals(activity.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La recomendación corresponde a otra microactividad");
            }
            recommendationService.complete(email, recommendation.getId());
        }

        return response(sessions.save(new FocusSession(user, activity, recommendation,
                request.requestId(), request.durationSeconds())));
    }

    private FocusSessionResponse response(FocusSession session) {
        return new FocusSessionResponse(session.getId(), session.getActivityId(), session.getRecommendationId(),
                session.getDurationSeconds(), session.getCompletedAt());
    }
}
