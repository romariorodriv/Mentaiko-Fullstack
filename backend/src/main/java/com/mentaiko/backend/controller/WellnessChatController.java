package com.mentaiko.backend.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.mentaiko.backend.dto.recommendation.WellnessChatRequest;
import com.mentaiko.backend.dto.recommendation.WellnessChatResponse;
import com.mentaiko.backend.service.WellnessChatService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/wellness/chat")
public class WellnessChatController {
    private final WellnessChatService service;

    public WellnessChatController(WellnessChatService service) {
        this.service = service;
    }

    @PostMapping
    public WellnessChatResponse reply(@Valid @RequestBody WellnessChatRequest request) {
        return service.reply(request);
    }
}
