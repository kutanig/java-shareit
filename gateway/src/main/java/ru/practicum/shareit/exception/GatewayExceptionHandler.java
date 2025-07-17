package ru.practicum.shareit.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GatewayExceptionHandler {
    // 1. Обработка ошибок валидации @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());

        log.warn("Validation failed for {} fields: {}", fieldErrors.size(), fieldErrors);
        return new ValidationErrorResponse("Validation Failed", fieldErrors);
    }

    // 2. Ошибки валидации параметров запроса
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldError> fieldErrors = ex.getConstraintViolations().stream()
                .map(v -> new FieldError(
                        v.getPropertyPath().toString(),
                        v.getMessage()))
                .collect(Collectors.toList());

        log.warn("Constraint violation for {} fields: {}", fieldErrors.size(), fieldErrors);
        return new ValidationErrorResponse("Constraint Violation", fieldErrors);
    }

    // 3. Неверный тип параметра
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Parameter '%s' should be of type %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        log.warn("Argument type mismatch: {}", message);
        return new ErrorResponse("Invalid Parameter", message);
    }

    // 4. Общие ошибки формата запроса
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid request format: {}", ex.getMessage());
        return new ErrorResponse("Invalid Request", ex.getMessage());
    }

    // 5. Ошибки доступа
    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleForbidden(ForbiddenException ex) {
        log.warn("Forbidden operation: {}", ex.getMessage());
        return new ErrorResponse("Forbidden", ex.getMessage());
    }

    // Record для ошибок валидации
    public record ValidationErrorResponse(
            String error,
            String message,
            List<FieldError> errors
    ) {
        public ValidationErrorResponse(String error, List<FieldError> errors) {
            this(error, "Validation failed for " + errors.size() + " field(s)", errors);
        }
    }

    // Record для полевых ошибок
    public record FieldError(String field, String message) {
    }

    // Record для обычных ошибок
    public record ErrorResponse(String error, String message) {
    }
}
