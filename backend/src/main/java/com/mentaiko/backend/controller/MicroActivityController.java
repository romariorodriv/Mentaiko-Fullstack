package com.mentaiko.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mentaiko.backend.dto.microactivity.CreateMicroActivityRequest;
import com.mentaiko.backend.dto.microactivity.MicroActivityResponse;
import com.mentaiko.backend.dto.microactivity.FavoriteMicroActivityResponse;
import com.mentaiko.backend.dto.microactivity.UpdateMicroActivityRequest;
import com.mentaiko.backend.service.MicroActivityService;
import com.mentaiko.backend.service.FavoriteMicroActivityService;

import jakarta.validation.Valid;

@RestController
public class MicroActivityController {

    private final MicroActivityService microActivityService;
    private final FavoriteMicroActivityService favoriteService;

    public MicroActivityController(MicroActivityService microActivityService, FavoriteMicroActivityService favoriteService) {
        this.microActivityService = microActivityService;
        this.favoriteService = favoriteService;
    }

    @GetMapping("/api/micro-activities")
    public List<MicroActivityResponse> list(
            @RequestParam(defaultValue = "true") boolean activeOnly,
            Authentication authentication
    ) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return microActivityService.list(activeOnly || !admin);
    }

    @GetMapping("/api/micro-activities/favorites")
    public List<FavoriteMicroActivityResponse> myFavorites(Authentication authentication) {
        return favoriteService.listMine(authentication.getName());
    }

    @PostMapping("/api/micro-activities/{id}/favorite")
    public FavoriteMicroActivityResponse favorite(@PathVariable Long id, Authentication authentication) {
        return favoriteService.save(authentication.getName(), id);
    }

    @DeleteMapping("/api/micro-activities/{id}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfavorite(@PathVariable Long id, Authentication authentication) {
        favoriteService.remove(authentication.getName(), id);
    }

    @PostMapping("/api/admin/micro-activities")
    @ResponseStatus(HttpStatus.CREATED)
    public MicroActivityResponse create(@Valid @RequestBody CreateMicroActivityRequest request) {
        return microActivityService.create(request);
    }

    @PutMapping("/api/admin/micro-activities/{id}")
    public MicroActivityResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMicroActivityRequest request
    ) {
        return microActivityService.update(id, request);
    }
}
