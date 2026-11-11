package com.mentaiko.backend.controller;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.mentaiko.backend.service.ActivityPreferenceService;
import com.mentaiko.backend.service.ActivityPreferenceService.Preferences;

@RestController
@RequestMapping("/api/me/activity-preferences")
public class ActivityPreferenceController {
    private final ActivityPreferenceService service;
    public record Update(List<String> selected) {}
    public ActivityPreferenceController(ActivityPreferenceService service){this.service=service;}
    @GetMapping
    public Preferences get(Authentication authentication){
        return service.get(authentication.getName());
    }
    @PutMapping
    public Preferences save(Authentication authentication,@RequestBody Update request){
        return service.update(authentication.getName(),request.selected());
    }
}
