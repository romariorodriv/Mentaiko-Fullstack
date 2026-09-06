package com.mentaiko.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mentaiko.backend.dto.auth.LoginRequest;
import com.mentaiko.backend.dto.auth.LoginResponse;
import com.mentaiko.backend.dto.auth.RegisterRequest;
import com.mentaiko.backend.dto.auth.UserResponse;
import com.mentaiko.backend.dto.user.UpdateUserProfileRequest;
import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.enums.Role;
import com.mentaiko.backend.exception.DuplicateEmailException;
import com.mentaiko.backend.exception.UserInactiveException;
import com.mentaiko.backend.repository.UserRepository;
import com.mentaiko.backend.security.JwtService;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateEmailException("El correo ya esta registrado");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setBirthDate(request.birthDate());
        user.setUniversity(request.university().trim());
        user.setCareer(request.career().trim());
        user.setRole(Role.USER);
        user.setActive(true);

        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        User user = userRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Correo o contrasena incorrectos"
                ));

        if (!user.isActive()) {
            throw new UserInactiveException("El usuario esta desactivado");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Correo o contrasena incorrectos"
            );
        }

        return new LoginResponse(jwtService.generateToken(user), toResponse(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(String email) {
        return toResponse(findActiveUser(email));
    }

    @Transactional
    public UserResponse updateProfile(String email, UpdateUserProfileRequest request) {
        User user = findActiveUser(email);
        user.setName(request.name().trim());
        user.setUniversity(request.university().trim());
        user.setCareer(request.career().trim());
        return toResponse(user);
    }

    private User findActiveUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (!user.isActive()) {
            throw new UserInactiveException("El usuario esta desactivado");
        }
        return user;
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getBirthDate(),
                user.getUniversity(),
                user.getCareer(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
