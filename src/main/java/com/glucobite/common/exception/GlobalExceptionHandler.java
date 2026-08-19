package com.glucobite.common.exception;

import com.glucobite.auth.exception.DuplicateLoginIdException;
import com.glucobite.auth.exception.InvalidAllergenException;
import com.glucobite.auth.exception.InvalidCredentialsException;
import com.glucobite.health.exception.HealthProfileNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ApiErrorResponse response = new ApiErrorResponse(
                "VALIDATION_ERROR",
                "요청 값을 확인해 주세요.",
                fieldErrors
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest() {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of(
                "INVALID_REQUEST",
                "요청 형식 또는 값이 올바르지 않습니다."
        ));
    }

    @ExceptionHandler(InvalidAllergenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidAllergen(InvalidAllergenException exception) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of(
                "INVALID_ALLERGEN",
                exception.getMessage()
        ));
    }

    @ExceptionHandler(DuplicateLoginIdException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateLoginId(DuplicateLoginIdException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiErrorResponse.of(
                "DUPLICATE_LOGIN_ID",
                exception.getMessage()
        ));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException exception
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiErrorResponse.of(
                "INVALID_CREDENTIALS",
                exception.getMessage()
        ));
    }

    @ExceptionHandler(HealthProfileNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleHealthProfileNotFound(
            HealthProfileNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiErrorResponse.of(
                "HEALTH_PROFILE_NOT_FOUND",
                exception.getMessage()
        ));
    }
}
