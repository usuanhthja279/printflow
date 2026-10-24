package com.printflow.shared;

import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<Map<String,Object>> workflow(org.springframework.web.server.ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).cacheControl(CacheControl.noStore())
            .body(Map.of("code","ACTION_NOT_AVAILABLE","message",ex.getReason()==null?"This action is unavailable":ex.getReason(),"timestamp",Instant.now().toString()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> validation(MethodArgumentNotValidException ex) {
        return error(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","Check input fields");
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,Object>> invalid(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST,"INVALID_REQUEST",ex.getMessage());
    }
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String,Object>> missing(NoSuchElementException ex) {
        return error(HttpStatus.NOT_FOUND,"NOT_FOUND","Resource not found");
    }
    private ResponseEntity<Map<String,Object>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore())
            .body(Map.of("code",code,"message",message,"timestamp",Instant.now().toString()));
    }
}
