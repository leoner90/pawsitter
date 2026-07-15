package lv.pawsitter.exception;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler  {

    @ExceptionHandler({
            UserNotFoundException.class,
            BookingNotFoundException.class,
            PetNotFoundException.class,
            ReviewNotFoundException.class,
            AvailabilityNotFoundException.class
    })

    public ResponseEntity<ErrorResponse> handleUserNotFoundException(RuntimeException e) {
        log.warn("Not found: {} ", e.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({
            EmailNotUniqueException.class,
            InvalidBookingOperationException.class,
            InvalidReviewOperationException.class,
            InvalidSitterOperationException.class
    })
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException exception)
    {
        log.warn("Conflict: {}", exception.getMessage());
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
    }


    @ExceptionHandler({
            AccessDeniedException.class,
            SecurityException.class
    })
    public ResponseEntity<ErrorResponse> handleForbidden(RuntimeException exception)
    {
        log.warn("Forbidden: {}", exception.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, exception.getMessage());
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException exception)
    {
        log.warn("Bad request: {}", exception.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception)
    {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        log.warn("Validation failed: {}", fieldErrors);
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception)
    {
        log.error("Unexpected error", exception);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message)
    {
        ErrorResponse body = new ErrorResponse(LocalDateTime.now(), status.value(), message, null);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException exception)
    {
        log.error("Illegal state: {}", exception.getMessage());
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    @Getter
    @AllArgsConstructor
    public static class ErrorResponse
    {
        private LocalDateTime timestamp;
        private int status;
        private String message;
        private Map<String, String> fieldErrors;
    }
}

    



