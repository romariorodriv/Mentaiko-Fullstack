package com.mentaiko.backend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.mentaiko.backend.service.ReminderService;
import com.mentaiko.backend.service.ReminderService.Preference;
import com.mentaiko.backend.service.ReminderService.Update;

@RestController
@RequestMapping("/api/me/reminders")
public class ReminderController {
    private final ReminderService service;
    public ReminderController(ReminderService service) {this.service=service;}
    @GetMapping
    public Preference current(Authentication authentication) {
        return service.get(authentication.getName());
    }
    @PutMapping
    public Preference update(Authentication authentication, @RequestBody Update update) {
        return service.update(authentication.getName(),update);
    }
    @PostMapping("/acknowledge")
    public Preference acknowledge(Authentication authentication) {
        return service.acknowledge(authentication.getName());
    }
}
