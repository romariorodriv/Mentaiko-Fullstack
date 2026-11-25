package com.mentaiko.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mentaiko.backend.dto.admin.AdminRuleResponse;
import com.mentaiko.backend.dto.admin.CreateAdminRuleRequest;
import com.mentaiko.backend.dto.admin.UpdateAdminRuleRequest;
import com.mentaiko.backend.service.AdminRuleService;

import jakarta.validation.Valid;

/** HU-33: reglas emocion -> microactividad. SecurityConfig restringe /api/admin/** al rol ADMIN. */
@RestController
@RequestMapping("/api/admin/rules")
public class AdminRuleController {

    private final AdminRuleService adminRuleService;

    public AdminRuleController(AdminRuleService adminRuleService) {
        this.adminRuleService = adminRuleService;
    }

    @GetMapping
    public List<AdminRuleResponse> list() {
        return adminRuleService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminRuleResponse create(@Valid @RequestBody CreateAdminRuleRequest request) {
        return adminRuleService.create(request);
    }

    @PutMapping("/{id}")
    public AdminRuleResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAdminRuleRequest request
    ) {
        return adminRuleService.update(id, request);
    }
}
