package com.mentaiko.backend.controller;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.mentaiko.backend.service.AdminAuditService;

/** SecurityConfig restricts every /api/admin/** endpoint to ROLE_ADMIN. */
@RestController
@RequestMapping("/api/admin/audit")
public class AdminAuditController {
    private final AdminAuditService service;
    public AdminAuditController(AdminAuditService service) { this.service = service; }

    @GetMapping
    public List<Map<String, Object>> list(
            @RequestParam(defaultValue = "25") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String resourceType) {
        return service.list(limit, offset, resourceType);
    }
}
