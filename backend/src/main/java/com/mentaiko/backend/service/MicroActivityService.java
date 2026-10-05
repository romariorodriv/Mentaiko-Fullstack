package com.mentaiko.backend.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(MicroActivityService.class);

    private final MicroActivityRepository microActivityRepository;

    public MicroActivityService(MicroActivityRepository microActivityRepository) {
        this.microActivityRepository = microActivityRepository;
    }

    @Transactional(readOnly = true)
    public List<MicroActivityResponse> list(boolean activeOnly) {
        List<MicroActivity> activities = activeOnly
                ? microActivityRepository.findByActiveTrueOrderByTitleAscIdAsc()
                : microActivityRepository.findAllByOrderByTitleAscIdAsc();
        log.debug("Micro-activity catalog loaded with activeOnly={} and count={}", activeOnly, activities.size());
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
            log.warn("Micro-activity creation rejected because normalized title already exists");
            throw duplicateTitle();
        }

        MicroActivity activity = new MicroActivity();
        activity.setTitle(title);
        activity.setNormalizedTitle(normalizedTitle);
        activity.setDescription(description);
        activity.setDurationMinutes(request.durationMinutes());
        activity.setActive(true);

        MicroActivity savedActivity = microActivityRepository.save(activity);
        log.info("Micro-activity created with id={} durationMinutes={} active={}",
                savedActivity.getId(), savedActivity.getDurationMinutes(), savedActivity.isActive());
        return toResponse(savedActivity);
    }

    @Transactional
    public MicroActivityResponse update(Long id, UpdateMicroActivityRequest request) {
        if (request.title() == null
                && request.description() == null
                && request.durationMinutes() == null
                && request.active() == null) {
            log.warn("Micro-activity update rejected for id={} because request had no modifiable fields", id);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes enviar al menos un campo modificable");
        }

        MicroActivity activity = microActivityRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Micro-activity update rejected because id={} was not found", id);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Microactividad no encontrada"
                    );
                });

        if (request.title() != null) {
            String title = normalizeSpaces(request.title());
            validateTitle(title);
            String normalizedTitle = normalizeForUniqueness(title);
            if (microActivityRepository.existsByNormalizedTitleAndIdNot(normalizedTitle, id)) {
                log.warn("Micro-activity update rejected for id={} because normalized title already exists", id);
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

        log.info("Micro-activity id={} updated durationMinutes={} active={}",
                activity.getId(), activity.getDurationMinutes(), activity.isActive());
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
            log.warn("Micro-activity validation rejected blank title");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El titulo es obligatorio");
        }
        if (title.length() > 120) {
            log.warn("Micro-activity validation rejected title length={}", title.length());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El titulo no puede superar 120 caracteres");
        }
    }

    private void validateDescription(String description) {
        if (description.isBlank()) {
            log.warn("Micro-activity validation rejected blank description");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripcion es obligatoria");
        }
        if (description.length() > 500) {
            log.warn("Micro-activity validation rejected description length={}", description.length());
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