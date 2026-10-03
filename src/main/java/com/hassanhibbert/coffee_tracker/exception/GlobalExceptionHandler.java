package com.hassanhibbert.coffee_tracker.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One place that turns exceptions into HTTP responses, for every controller.
 * Extending ResponseEntityExceptionHandler gives consistent ProblemDetail bodies
 * (RFC 9457: type, title, status, detail, instance) for Spring's own errors too,
 * e.g. malformed JSON or an unknown enum value.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage());
    }

    @ExceptionHandler(ResourceInUseException.class)
    public ProblemDetail handleInUse(ResourceInUseException ex) {
        return problem(HttpStatus.CONFLICT, "Resource in use", ex.getMessage());
    }

    @ExceptionHandler(BrewRuleViolationException.class)
    public ProblemDetail handleBrewRule(BrewRuleViolationException ex) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Brew rule violated", ex.getMessage());
    }

    /**
     * The database backstop fired (a CHECK, NOT NULL, UNIQUE or FK constraint). If validation
     * is doing its job this is rare. Don't echo the SQL error to the client: it leaks schema details.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return problem(HttpStatus.CONFLICT, "Data conflict", "The request conflicts with existing data");
    }

    /** @Valid failed: return every field error, not just the first one. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Validation failed", "One or more fields are invalid");
        body.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
