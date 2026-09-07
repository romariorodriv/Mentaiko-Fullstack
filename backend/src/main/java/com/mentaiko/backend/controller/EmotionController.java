package com.mentaiko.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mentaiko.backend.dto.emotion.CreateEmotionRequest;
import com.mentaiko.backend.dto.emotion.EmotionResponse;
import com.mentaiko.backend.dto.emotion.UpdateEmotionRequest;
import com.mentaiko.backend.service.EmotionService;

import jakarta.validation.Valid;

@RestController
public class EmotionController {

    private final EmotionService emotionService;

    public EmotionController(EmotionService emotionService) {
        this.emotionService = emotionService;
    }

    @GetMapping("/api/emotions")
    public List<EmotionResponse> list(@RequestParam(defaultValue = "true") boolean activeOnly) {
        return emotionService.list(activeOnly);
    }

    @PostMapping("/api/admin/emotions")
    @ResponseStatus(HttpStatus.CREATED)
    public EmotionResponse create(@Valid @RequestBody CreateEmotionRequest request) {
        return emotionService.create(request);
    }

    @PutMapping("/api/admin/emotions/{id}")
    public EmotionResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmotionRequest request
    ) {
        return emotionService.update(id, request);
    }
}
