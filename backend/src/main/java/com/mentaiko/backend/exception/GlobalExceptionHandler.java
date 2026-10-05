package com.mentaiko.backend.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("Request validation failed path={} fields={}", pathOf(request), errors.keySet());
        return build(HttpStatus.BAD_REQUEST, "Los datos enviados no son validos", request, errors);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiError> handleDuplicateEmail(DuplicateEmailException exception, WebRequest request) {
        log.warn("Duplicate email rejected path={}", pathOf(request));
        return build(HttpStatus.CONFLICT, exception.getMessage(), request, null);
    }
    
    @ExceptionHandler(DuplicateEmotionNameException.class)
    public ResponseEntity<ApiError> handleDuplicateEmotion(DuplicateEmotionNameException exception, WebRequest request) {
        log.warn("Duplicate emotion name rejected path={}", pathOf(request));
        return build(HttpStatus.CONFLICT, exception.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateMicroActivityTitleException.class)
    public ResponseEntity<ApiError> handleDuplicateMicroActivity(
            DuplicateMicroActivityTitleException exception,
            WebRequest request
    ) {
        log.warn("Duplicate micro-activity title rejected path={}", pathOf(request));
        return build(HttpStatus.CONFLICT, exception.getMessage(), request, null);
    }

    @ExceptionHandler(UserInactiveException.class)
    public ResponseEntity<ApiError> handleInactiveUser(UserInactiveException exception, WebRequest request) {
        log.warn("Inactive user request rejected path={}", pathOf(request));
        return build(HttpStatus.FORBIDDEN, exception.getMessage(), request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException exception, WebRequest request) {
        log.warn("Resource not found path={}", pathOf(request));
        return build(HttpStatus.NOT_FOUND, "Recurso no encontrado", request, null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException exception, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        log.warn("Request rejected path={} status={} reason={}", pathOf(request), status.value(), exception.getReason());
        return build(status, exception.getReason(), request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, WebRequest request) {
        log.error("Unexpected error path={}", pathOf(request), exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error inesperado", request, null);
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status,
            String message,
            WebRequest request,
            Map<String, String> errors
    ) {
        String path = pathOf(request);
        ApiError body = new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                errors
        );
        return ResponseEntity.status(status).body(body);
    }

    private String pathOf(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}