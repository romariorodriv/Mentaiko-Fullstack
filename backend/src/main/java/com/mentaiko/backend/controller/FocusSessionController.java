package com.mentaiko.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mentaiko.backend.dto.focus.ConfirmFocusSessionRequest;
import com.mentaiko.backend.dto.focus.FocusSessionResponse;
import com.mentaiko.backend.service.FocusSessionService;
import jakarta.validation.Valid;

@RestController
public class FocusSessionController {
    private final FocusSessionService sessions;

    public FocusSessionController(FocusSessionService sessions) {
        this.sessions = sessions;
    }

    @PostMapping("/api/focus-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public FocusSessionResponse confirm(Authentication authentication, @Valid @RequestBody ConfirmFocusSessionRequest request) {
        return sessions.confirm(authentication.getName(), request);
    }
}
