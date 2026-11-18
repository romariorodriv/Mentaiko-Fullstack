package com.mentaiko.backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mentaiko.backend.dto.microactivity.FavoriteMicroActivityResponse;
import com.mentaiko.backend.entity.FavoriteMicroActivity;
import com.mentaiko.backend.entity.MicroActivity;
import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.exception.UserInactiveException;
import com.mentaiko.backend.repository.FavoriteMicroActivityRepository;
import com.mentaiko.backend.repository.MicroActivityRepository;
import com.mentaiko.backend.repository.UserRepository;

@Service
public class FavoriteMicroActivityService {

    private final FavoriteMicroActivityRepository favorites;
    private final MicroActivityRepository activities;
    private final UserRepository users;

    public FavoriteMicroActivityService(
            FavoriteMicroActivityRepository favorites,
            MicroActivityRepository activities,
            UserRepository users) {
        this.favorites = favorites;
        this.activities = activities;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<FavoriteMicroActivityResponse> listMine(String email) {
        User user = authenticatedUser(email);
        return favorites.findByUser_IdOrderByCreatedAtDescIdDesc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public FavoriteMicroActivityResponse save(String email, Long activityId) {
        User user = authenticatedUser(email);
        MicroActivity activity = activities.findById(activityId)
                .filter(MicroActivity::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Microactividad activa no encontrada"));

        // A lock prevents parallel requests for the same user creating duplicates.
        users.findLockedById(user.getId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        return favorites.findByUser_IdAndActivity_Id(user.getId(), activityId)
                .map(this::toResponse)
                .orElseGet(() -> toResponse(favorites.saveAndFlush(
                        new FavoriteMicroActivity(user, activity))));
    }

    @Transactional
    public void remove(String email, Long activityId) {
        User user = authenticatedUser(email);
        favorites.deleteByUser_IdAndActivity_Id(user.getId(), activityId);
    }

    private User authenticatedUser(String email) {
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (!user.isActive()) {
            throw new UserInactiveException("El usuario esta desactivado");
        }
        return user;
    }

    private FavoriteMicroActivityResponse toResponse(FavoriteMicroActivity favorite) {
        MicroActivity activity = favorite.getActivity();
        return new FavoriteMicroActivityResponse(
                activity.getId(), activity.getTitle(), activity.getDescription(),
                activity.getDurationMinutes(), activity.isActive(),
                activity.getActivityType(), favorite.getCreatedAt());
    }
}
