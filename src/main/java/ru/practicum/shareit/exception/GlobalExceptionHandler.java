package ru.practicum.shareit.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DuplicateEmailException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateEmail(DuplicateEmailException ex) {
        log.warn("Email conflict: {}", ex.getMessage());
        return new ErrorResponse("Conflict", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());

        log.warn("Validation failed for {} fields: {}", fieldErrors.size(), fieldErrors);
        return new ValidationErrorResponse("Validation Failed", fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleConstraintViolation(ConstraintViolationException ex) {
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
        List<FieldError> fieldErrors = violations.stream()
                .map(v -> new FieldError(
                        v.getPropertyPath().toString(),
                        v.getMessage()))
                .collect(Collectors.toList());

        log.warn("Constraint violation for {} fields: {}", fieldErrors.size(), fieldErrors);
        return new ValidationErrorResponse("Constraint Violation", fieldErrors);
    }

    @ExceptionHandler({
            ValidationException.class,
            UnavailableItemException.class,
            SelfBookingException.class,
            BookingTimeException.class,
            InvalidPaginationException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(RuntimeException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return new ErrorResponse("Bad Request", ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleForbidden(ForbiddenException ex) {
        log.warn("Forbidden operation: {}", ex.getMessage());
        return new ErrorResponse("Forbidden", ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(NotFoundException ex) {
        log.warn("Not found error: {}", ex.getMessage());
        return new ErrorResponse("Not Found", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Parameter '" + ex.getName() + "' should be " + getExpectedType(ex);
        log.warn("Argument type mismatch: {}", message);
        return new ErrorResponse("Invalid Parameter", message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleInternalError(Exception ex) {
        log.error("Internal server error", ex);
        return new ErrorResponse("Internal Server Error", "An unexpected error occurred");
    }

    private String getExpectedType(MethodArgumentTypeMismatchException ex) {
        if (ex.getRequiredType() == null) {
            return "unknown type";
        }
        return ex.getRequiredType().getSimpleName().toLowerCase();
    }

    public record ErrorResponse(String error, String message) {}

    public record ValidationErrorResponse(
            String error,
            String message,
            List<FieldError> errors
    ) {
        public ValidationErrorResponse(String error, List<FieldError> errors) {
            this(error, "Validation failed for " + errors.size() + " field(s)", errors);
        }
    }

    public record FieldError(String field, String message) {}
}
