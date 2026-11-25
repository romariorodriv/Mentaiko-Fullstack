package com.mentaiko.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mentaiko.backend.dto.recommendation.CompleteRecommendationRequest;
import com.mentaiko.backend.dto.recommendation.CreateRecommendationRequest;
import com.mentaiko.backend.dto.recommendation.RecommendationFeedbackRequest;
import com.mentaiko.backend.dto.recommendation.RecommendationResponse;
import com.mentaiko.backend.service.RecommendationService;

import jakarta.validation.Valid;

@RestController
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping("/api/recommendations")
    @ResponseStatus(HttpStatus.CREATED)
    public RecommendationResponse recommend(
            Authentication authentication,
            @Valid @RequestBody CreateRecommendationRequest request
    ) {
        return recommendationService.recommend(authentication.getName(), request.checkinId(), request.maxDurationMinutes());
    }

    @GetMapping("/api/recommendations/me")
    public List<RecommendationResponse> mine(Authentication authentication) {
        return recommendationService.listMine(authentication.getName());
    }

    @PostMapping("/api/recommendations/{id}/complete")
    public RecommendationResponse complete(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) CompleteRecommendationRequest request
    ) {
        Integer postIntensity = request == null ? null : request.postIntensity();
        return recommendationService.complete(authentication.getName(), id, postIntensity);
    }

    @PostMapping("/api/recommendations/{id}/feedback")
    public RecommendationResponse feedback(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody RecommendationFeedbackRequest request
    ) {
        return recommendationService.submitFeedback(authentication.getName(), id, request);
    }
}
