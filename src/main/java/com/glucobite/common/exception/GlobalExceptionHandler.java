package com.glucobite.common.exception;

import com.glucobite.auth.exception.DuplicateLoginIdException;
import com.glucobite.auth.exception.InvalidAllergenException;
import com.glucobite.auth.exception.InvalidCredentialsException;
import com.glucobite.health.exception.HealthProfileNotFoundException;
import com.glucobite.recipe.exception.IngredientNotFoundException;
import com.glucobite.recipe.exception.InvalidRecipeSubstitutionException;
import com.glucobite.recipe.exception.InvalidSubstituteIngredientException;
import com.glucobite.recipe.exception.RecipeIngredientNotFoundException;
import com.glucobite.recipe.exception.RecipeNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

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

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodValidation(
            HandlerMethodValidationException exception
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ParameterValidationResult result : exception.getParameterValidationResults()) {
            String parameterName = result.getMethodParameter().getParameterName();
            String message = result.getResolvableErrors().stream()
                    .map(MessageSourceResolvable::getDefaultMessage)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse("요청 값을 확인해 주세요.");
            fieldErrors.putIfAbsent(
                    parameterName == null ? "request" : parameterName,
                    message
            );
        }
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                "VALIDATION_ERROR",
                "요청 값을 확인해 주세요.",
                fieldErrors
        ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                "VALIDATION_ERROR",
                "요청 값을 확인해 주세요.",
                Map.of(exception.getName(), "요청 값 형식이 올바르지 않습니다.")
        ));
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

    @ExceptionHandler(RecipeNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleRecipeNotFound(RecipeNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiErrorResponse.of(
                "RECIPE_NOT_FOUND",
                exception.getMessage()
        ));
    }

    @ExceptionHandler(IngredientNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleIngredientNotFound(
            IngredientNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiErrorResponse.of(
                "INGREDIENT_NOT_FOUND",
                exception.getMessage()
        ));
    }

    @ExceptionHandler(RecipeIngredientNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleRecipeIngredientNotFound(
            RecipeIngredientNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiErrorResponse.of(
                "RECIPE_INGREDIENT_NOT_FOUND",
                exception.getMessage()
        ));
    }

    @ExceptionHandler(InvalidSubstituteIngredientException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidSubstituteIngredient(
            InvalidSubstituteIngredientException exception
    ) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of(
                "INVALID_SUBSTITUTE_INGREDIENT",
                exception.getMessage()
        ));
    }

    @ExceptionHandler(InvalidRecipeSubstitutionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRecipeSubstitution(
            InvalidRecipeSubstitutionException exception
    ) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of(
                "INVALID_RECIPE_SUBSTITUTION",
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
