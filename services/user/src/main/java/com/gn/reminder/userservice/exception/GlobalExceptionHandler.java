package com.gn.reminder.userservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    Logger log = LoggerFactory.getLogger(this.getClass());

    @ExceptionHandler(value = {UserNotFoundException.class})
    public ResponseEntity<Object> userNotFoundExceptionHandler(UserNotFoundException ex) {
        log.error("got exception due to: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(value = {DuplicateKeyException.class})
    public ResponseEntity<Object> duplicateKeyExceptionHandler(DuplicateKeyException ex, WebRequest request) {
        log.error("value already present, request: {}, exception: {}", request, ex.getMessage());
        return ResponseEntity.badRequest().body("duplicate entry not allowed");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> genericExceptionHandler(RuntimeException ex) {
        log.error("Exception due to: {}", ex.getMessage());
        return ResponseEntity.badRequest().build();
    }
}
