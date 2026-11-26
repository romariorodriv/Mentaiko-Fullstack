
package com.mentaiko.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mentaiko.backend.dto.auth.ForgotPasswordRequest;
import com.mentaiko.backend.dto.auth.ResetPasswordRequest;
import com.mentaiko.backend.dto.auth.PasswordResetMessage;
import com.mentaiko.backend.dto.auth.LoginRequest;
import com.mentaiko.backend.dto.auth.LoginResponse;
import com.mentaiko.backend.dto.auth.RegisterRequest;
import com.mentaiko.backend.dto.auth.UserResponse;
import com.mentaiko.backend.dto.user.UpdateUserProfileRequest;
import com.mentaiko.backend.dto.user.DeleteAccountRequest;
import com.mentaiko.backend.service.AccountDeletionService;
import com.mentaiko.backend.service.PersonalDataExportService;

import com.mentaiko.backend.service.PasswordResetService;
import com.mentaiko.backend.service.UserService;

import jakarta.validation.Valid;

@RestController
public class UserController {

    private final UserService userService;
    private final AccountDeletionService accountDeletionService;
    private final PersonalDataExportService personalDataExportService;
    private final PasswordResetService passwordResetService;

    public UserController(
            UserService userService,
            PasswordResetService passwordResetService,
            AccountDeletionService accountDeletionService,
            PersonalDataExportService personalDataExportService) {

        this.userService = userService;
        this.passwordResetService = passwordResetService;
        this.accountDeletionService = accountDeletionService;
        this.personalDataExportService = personalDataExportService;
    }

    // REGISTRO DE USUARIOS
    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    // INICIO DE SESION
    @PostMapping("/api/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    // SOLICITAR RECUPERACION DE CONTRASEÑA
    @PostMapping("/api/auth/forgot-password")
    public PasswordResetMessage forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        return new PasswordResetMessage(
                passwordResetService.requestReset(request.email())
        );
    }

    // RESTABLECER CONTRASEÑA
    @PostMapping("/api/auth/reset-password")
    public PasswordResetMessage resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        passwordResetService.reset(
                request.token(),
                request.newPassword()
        );

        return new PasswordResetMessage(
                "Contrasena actualizada. Ya puedes iniciar sesion."
        );
    }

    // OBTENER PERFIL DEL USUARIO
    @GetMapping("/api/users/me")
    public UserResponse me(Authentication authentication) {
        return userService.getProfile(authentication.getName());
    }

    // ACTUALIZAR PERFIL DEL USUARIO
    @PutMapping("/api/users/me")
    public UserResponse updateMe(
            Authentication authentication,
            @Valid @RequestBody UpdateUserProfileRequest request) {

        return userService.updateProfile(
                authentication.getName(),
                request
        );
    }
    @GetMapping(value = "/api/users/me/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> exportMe(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "false") boolean includeNotes) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mentaiko-datos-personales.json\"")
                .body(personalDataExportService.export(authentication.getName(), from, to, includeNotes));
    }

    @DeleteMapping("/api/users/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe(Authentication authentication, @Valid @RequestBody DeleteAccountRequest request) {
        accountDeletionService.deleteOwnAccount(authentication.getName(), request.password());
    }
}
