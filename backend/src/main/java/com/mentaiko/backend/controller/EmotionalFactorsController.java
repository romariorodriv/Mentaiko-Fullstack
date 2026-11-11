package com.mentaiko.backend.controller;

import java.time.LocalDate;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.mentaiko.backend.service.EmotionalFactorsService;
import com.mentaiko.backend.service.EmotionalFactorsService.Factor;
import com.mentaiko.backend.service.EmotionalFactorsService.Analysis;

@RestController
public class EmotionalFactorsController {
    private final EmotionalFactorsService service;
    public EmotionalFactorsController(EmotionalFactorsService service) {this.service=service;}
    public record Selection(List<Integer> factorIds) {}
    @GetMapping("/api/me/emotional-factors")
    public List<Factor> catalog(Authentication authentication) {
        return service.catalog(authentication.getName());
    }
    @GetMapping("/api/checkins/{checkinId}/factors")
    public List<Integer> selected(Authentication authentication, @PathVariable Long checkinId) {
        return service.selection(authentication.getName(),checkinId);
    }
    @PutMapping("/api/checkins/{checkinId}/factors")
    public List<Integer> save(Authentication authentication, @PathVariable Long checkinId,
                              @RequestBody Selection request) {
        return service.replace(authentication.getName(),checkinId,request.factorIds());
    }
    @GetMapping("/api/me/emotional-factors/analysis")
    public Analysis analysis(Authentication authentication,@RequestParam LocalDate from,
                             @RequestParam LocalDate to) {
        return service.analysis(authentication.getName(),from,to);
    }
}
