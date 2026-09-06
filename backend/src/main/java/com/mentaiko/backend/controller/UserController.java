package com.mentaiko.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mentaiko.backend.dto.auth.LoginRequest;
import com.mentaiko.backend.dto.auth.LoginResponse;
import com.mentaiko.backend.dto.auth.RegisterRequest;
import com.mentaiko.backend.dto.auth.UserResponse;
import com.mentaiko.backend.dto.user.UpdateUserProfileRequest;
import com.mentaiko.backend.service.UserService;

import jakarta.validation.Valid;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/api/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @GetMapping("/api/users/me")
    public UserResponse me(Authentication authentication) {
        return userService.getProfile(authentication.getName());
    }

    @PutMapping("/api/users/me")
    public UserResponse updateMe(
            Authentication authentication,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        return userService.updateProfile(authentication.getName(), request);
    }
}
