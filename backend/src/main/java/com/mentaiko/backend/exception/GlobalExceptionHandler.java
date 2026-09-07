package com.mentaiko.backend.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Los datos enviados no son validos", request, errors);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiError> handleDuplicateEmail(DuplicateEmailException exception, WebRequest request) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), request, null);
    }
    
    @ExceptionHandler(DuplicateEmotionNameException.class)
    public ResponseEntity<ApiError> handleDuplicateEmotion(DuplicateEmotionNameException exception, WebRequest request) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), request, null);
    }

    @ExceptionHandler(UserInactiveException.class)
    public ResponseEntity<ApiError> handleInactiveUser(UserInactiveException exception, WebRequest request) {
        return build(HttpStatus.FORBIDDEN, exception.getMessage(), request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException exception, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, "Recurso no encontrado", request, null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException exception, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return build(status, exception.getReason(), request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, WebRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error inesperado", request, null);
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status,
            String message,
            WebRequest request,
            Map<String, String> errors
    ) {
        String path = request.getDescription(false).replace("uri=", "");
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
}
