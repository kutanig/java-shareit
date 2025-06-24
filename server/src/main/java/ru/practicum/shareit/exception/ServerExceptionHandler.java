package ru.practicum.shareit.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ServerExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ServerExceptionHandler.class);

    // 1. Ошибки "Не найдено"
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(NotFoundException ex) {
        log.warn("Not found error: {}", ex.getMessage());
        return new ErrorResponse("Not Found", ex.getMessage());
    }

    // 2. Конфликты данных
    @ExceptionHandler(DuplicateEmailException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateEmail(DuplicateEmailException ex) {
        log.warn("Data conflict: {}", ex.getMessage());
        return new ErrorResponse("Conflict", ex.getMessage());
    }

    // 3. Ошибки бизнес-логики
    @ExceptionHandler({
            UnavailableItemException.class,
            SelfBookingException.class,
            BookingTimeException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBusinessRules(RuntimeException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return new ErrorResponse("Business Rule Violation", ex.getMessage());
    }

    // 4. Ошибки интеграции с БД
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Data integrity violation", ex);
        return new ErrorResponse("Data Conflict", "Database operation failed");
    }

    // 5. Необработанные исключения
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleInternalError(Exception ex) {
        log.error("Internal server error", ex);
        return new ErrorResponse("Internal Server Error", "Please contact support");
    }

    // Record для ошибок
    public record ErrorResponse(String error, String message) {}
}
