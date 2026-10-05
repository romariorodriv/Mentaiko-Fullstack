package com.mentaiko.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mentaiko.backend.dto.admin.AdminCheckinResponse;
import com.mentaiko.backend.dto.admin.AdminDashboardResponse;
import com.mentaiko.backend.dto.admin.AdminUserResponse;
import com.mentaiko.backend.dto.common.PageResponse;
import com.mentaiko.backend.service.AdminDashboardService;

@RestController
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/api/admin/dashboard")
    public AdminDashboardResponse dashboard() {
        return adminDashboardService.dashboard();
    }

    @GetMapping("/api/admin/users")
    public PageResponse<AdminUserResponse> users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return adminDashboardService.users(page, size);
    }

    @GetMapping("/api/admin/checkins")
    public PageResponse<AdminCheckinResponse> checkins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return adminDashboardService.checkins(page, size);
    }
}
