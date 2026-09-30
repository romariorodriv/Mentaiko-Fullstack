package com.mentaiko.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mentaiko.backend.dto.admin.AdminIndicatorsResponse;
import com.mentaiko.backend.service.AdminIndicatorsService;

@RestController
public class AdminIndicatorsController {

    private final AdminIndicatorsService adminIndicatorsService;

    public AdminIndicatorsController(AdminIndicatorsService adminIndicatorsService) {
        this.adminIndicatorsService = adminIndicatorsService;
    }

    @GetMapping("/api/admin/indicators")
    public AdminIndicatorsResponse indicators() {
        return adminIndicatorsService.indicators();
    }
}
