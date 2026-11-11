package com.mentaiko.backend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.mentaiko.backend.service.WeeklyGoalService;
import com.mentaiko.backend.service.WeeklyGoalService.Goal;

@RestController
@RequestMapping("/api/me/weekly-goal")
public class WeeklyGoalController {
    private final WeeklyGoalService service;
    public record Request(Integer targetDays) {}
    public WeeklyGoalController(WeeklyGoalService service) {this.service = service;}

    @GetMapping
    public Goal get(Authentication authentication) {
        return service.get(authentication.getName());
    }

    @PutMapping
    public Goal save(Authentication authentication, @RequestBody Request request) {
        return service.save(authentication.getName(), request.targetDays());
    }
}
