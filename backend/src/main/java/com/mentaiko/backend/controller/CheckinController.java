package com.mentaiko.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mentaiko.backend.dto.checkin.CheckinResponse;
import com.mentaiko.backend.dto.checkin.CreateCheckinRequest;
import com.mentaiko.backend.service.CheckinService;

import jakarta.validation.Valid;

@RestController
public class CheckinController {

    private final CheckinService checkinService;

    public CheckinController(CheckinService checkinService) {
        this.checkinService = checkinService;
    }

    @PostMapping("/api/checkins")
    @ResponseStatus(HttpStatus.CREATED)
    public CheckinResponse create(Authentication authentication, @Valid @RequestBody CreateCheckinRequest request) {
        return checkinService.create(authentication.getName(), request);
    }
}
