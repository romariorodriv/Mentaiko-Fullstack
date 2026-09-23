package com.mentaiko.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mentaiko.backend.dto.microactivity.CreateMicroActivityRequest;
import com.mentaiko.backend.dto.microactivity.MicroActivityResponse;
import com.mentaiko.backend.dto.microactivity.UpdateMicroActivityRequest;
import com.mentaiko.backend.service.MicroActivityService;

import jakarta.validation.Valid;

@RestController
public class MicroActivityController {

    private final MicroActivityService microActivityService;

    public MicroActivityController(MicroActivityService microActivityService) {
        this.microActivityService = microActivityService;
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
