package com.mentaiko.backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mentaiko.backend.dto.report.DistributionResponse;
import com.mentaiko.backend.dto.report.WeeklyPointResponse;
import com.mentaiko.backend.service.ReportService;

@RestController
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/api/reports/weekly")
    public List<WeeklyPointResponse> weekly(Authentication authentication, @RequestParam String week) {
        return reportService.weekly(authentication.getName(), week);
    }

    @GetMapping("/api/reports/distribution")
    public DistributionResponse distribution(
            Authentication authentication,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        return reportService.distribution(authentication.getName(), from, to);
    }
}
