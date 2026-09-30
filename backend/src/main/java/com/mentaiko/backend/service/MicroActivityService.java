package com.mentaiko.backend.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mentaiko.backend.dto.microactivity.CreateMicroActivityRequest;
import com.mentaiko.backend.dto.microactivity.MicroActivityResponse;
import com.mentaiko.backend.dto.microactivity.UpdateMicroActivityRequest;
import com.mentaiko.backend.entity.MicroActivity;
import com.mentaiko.backend.exception.DuplicateMicroActivityTitleException;
import com.mentaiko.backend.repository.MicroActivityRepository;

@Service
public class MicroActivityService {

    private final MicroActivityRepository microActivityRepository;

    public MicroActivityService(MicroActivityRepository microActivityRepository) {
        this.microActivityRepository = microActivityRepository;
    }

    @Transactional(readOnly = true)
    public List<MicroActivityResponse> list(boolean activeOnly) {
        List<MicroActivity> activities = activeOnly
                ? microActivityRepository.findByActiveTrueOrderByTitleAscIdAsc()
                : microActivityRepository.findAllByOrderByTitleAscIdAsc();
        return activities.stream().map(this::toResponse).toList();
    }

    @Transactional
    public MicroActivityResponse create(CreateMicroActivityRequest request) {
        String title = normalizeSpaces(request.title());
        String description = normalizeSpaces(request.description());
        validateTitle(title);
        validateDescription(description);

        String normalizedTitle = normalizeForUniqueness(title);
        if (microActivityRepository.existsByNormalizedTitle(normalizedTitle)) {
            throw duplicateTitle();
        }

        MicroActivity activity = new MicroActivity();
        activity.setTitle(title);
        activity.setNormalizedTitle(normalizedTitle);
        activity.setDescription(description);
        activity.setDurationMinutes(request.durationMinutes());
        activity.setActive(true);

        return toResponse(microActivityRepository.save(activity));
    }

    @Transactional
    public MicroActivityResponse update(Long id, UpdateMicroActivityRequest request) {
        if (request.title() == null
                && request.description() == null
                && request.durationMinutes() == null
                && request.active() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes enviar al menos un campo modificable");
        }

        MicroActivity activity = microActivityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Microactividad no encontrada"
                ));

        if (request.title() != null) {
            String title = normalizeSpaces(request.title());
            validateTitle(title);
            String normalizedTitle = normalizeForUniqueness(title);
            if (microActivityRepository.existsByNormalizedTitleAndIdNot(normalizedTitle, id)) {
                throw duplicateTitle();
            }
            activity.setTitle(title);
            activity.setNormalizedTitle(normalizedTitle);
        }

        if (request.description() != null) {
            String description = normalizeSpaces(request.description());
            validateDescription(description);
            activity.setDescription(description);
        }

        if (request.durationMinutes() != null) {
            activity.setDurationMinutes(request.durationMinutes());
        }

        if (request.active() != null) {
            activity.setActive(request.active());
        }

        return toResponse(activity);
    }

    public String normalizeForUniqueness(String value) {
        String compact = normalizeSpaces(value).toLowerCase(Locale.ROOT);
        return Normalizer.normalize(compact, Normalizer.Form.NFKC);
    }

    private String normalizeSpaces(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private void validateTitle(String title) {
        if (title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El titulo es obligatorio");
        }
        if (title.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El titulo no puede superar 120 caracteres");
        }
    }

    private void validateDescription(String description) {
        if (description.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripcion es obligatoria");
        }
        if (description.length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La descripcion no puede superar 500 caracteres"
            );
        }
    }

    private DuplicateMicroActivityTitleException duplicateTitle() {
        return new DuplicateMicroActivityTitleException("Ya existe una microactividad con ese titulo");
    }

    private MicroActivityResponse toResponse(MicroActivity activity) {
        return new MicroActivityResponse(
                activity.getId(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getDurationMinutes(),
                activity.isActive(),
                activity.getCreatedAt(),
                activity.getUpdatedAt()
        );
    }
}
