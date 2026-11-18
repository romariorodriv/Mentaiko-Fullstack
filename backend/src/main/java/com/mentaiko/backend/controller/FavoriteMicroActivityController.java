package com.mentaiko.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mentaiko.backend.dto.microactivity.FavoriteMicroActivityResponse;
import com.mentaiko.backend.service.FavoriteMicroActivityService;

@RestController
@RequestMapping("/api/me/favorite-micro-activities")
public class FavoriteMicroActivityController {

    private final FavoriteMicroActivityService service;

    public FavoriteMicroActivityController(FavoriteMicroActivityService service) {
        this.service = service;
    }

    @GetMapping
    public List<FavoriteMicroActivityResponse> list(Authentication authentication) {
        return service.listMine(authentication.getName());
    }

    @PutMapping("/{activityId}")
    public FavoriteMicroActivityResponse save(
            Authentication authentication, @PathVariable Long activityId) {
        return service.save(authentication.getName(), activityId);
    }

    @DeleteMapping("/{activityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(Authentication authentication, @PathVariable Long activityId) {
        service.remove(authentication.getName(), activityId);
    }
}
