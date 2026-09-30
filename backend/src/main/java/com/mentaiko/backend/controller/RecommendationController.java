package com.mentaiko.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mentaiko.backend.dto.recommendation.CreateRecommendationRequest;
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
        return recommendationService.recommend(authentication.getName(), request.checkinId());
    }

    @GetMapping("/api/recommendations/me")
    public List<RecommendationResponse> mine(Authentication authentication) {
        return recommendationService.listMine(authentication.getName());
    }
}
