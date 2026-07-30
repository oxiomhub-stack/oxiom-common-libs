package com.oxiomhub.common.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Shared RFC 7807 {@code application/problem+json} error handling for all OxiomHub services.
 * <p>
 * Note: 401 (unauthenticated) and security-layer 403s are handled by the resource-server filter
 * chain before a request reaches a controller, so they do not pass through this advice.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail onValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation failed");
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.put(fe.getField(), fe.getDefaultMessage()));
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail onBadArgument(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Bad Request");
        return pd;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail onAccessDenied(AccessDeniedException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        pd.setTitle("Forbidden");
        return pd;
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail onNotFound(NoSuchElementException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Not Found");
        return pd;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail onConflict(IllegalStateException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Conflict");
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail onUnexpected(Exception ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Internal Server Error");
        return pd;
    }
}
