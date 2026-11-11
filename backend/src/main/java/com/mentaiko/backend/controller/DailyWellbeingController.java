package com.mentaiko.backend.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.mentaiko.backend.service.DailyWellbeingService;
import com.mentaiko.backend.service.DailyWellbeingService.Sleep;
import com.mentaiko.backend.service.DailyWellbeingService.Energy;

@RestController
@RequestMapping("/api/me")
public class DailyWellbeingController {
    private final DailyWellbeingService service;
    public DailyWellbeingController(DailyWellbeingService service) { this.service = service; }

    public record SleepRequest(BigDecimal hours, int quality) {}
    public record EnergyRequest(int level) {}

    @GetMapping("/sleep")
    public List<Sleep> sleepHistory(Authentication authentication,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        LocalDate end = to == null ? service.today() : to;
        return service.sleepHistory(authentication.getName(), from == null ? end.minusDays(29) : from, end);
    }

    @PutMapping("/sleep/{date}")
    public Sleep saveSleep(Authentication authentication, @PathVariable LocalDate date,
            @RequestBody SleepRequest request) {
        return service.saveSleep(authentication.getName(), date, request.hours(), request.quality());
    }

    @GetMapping("/energy")
    public List<Energy> energyHistory(Authentication authentication,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        LocalDate end = to == null ? service.today() : to;
        return service.energyHistory(authentication.getName(), from == null ? end.minusDays(29) : from, end);
    }

    @PutMapping("/energy/{date}")
    public Energy saveEnergy(Authentication authentication, @PathVariable LocalDate date,
            @RequestBody EnergyRequest request) {
        return service.saveEnergy(authentication.getName(), date, request.level());
    }
}
