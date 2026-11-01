package com.shruti.EmpManage.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(EmployeeNotFoundException.class)
        public ResponseEntity<Map<String, Object>> handleNotFound(EmployeeNotFoundException ex) {
            return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
        }

        @ExceptionHandler(DuplicateEmailException.class)
        public ResponseEntity<Map<String, Object>> handleDuplicateEmail(DuplicateEmailException ex) {
            return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
            Map<String, String> fieldErrors = new LinkedHashMap<>();
            ex.getBindingResult().getFieldErrors()
                    .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));
            return buildResponse(HttpStatus.BAD_REQUEST, fieldErrors);
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException ex) {
            return buildResponse(HttpStatus.BAD_REQUEST,
                    "Invalid or malformed request body (check field types and that dates use yyyy-MM-dd)");
        }
        @ExceptionHandler(InvalidRequestException.class)
        public ResponseEntity<Map<String, Object>> handleInvalidRequest(InvalidRequestException ex) {
            return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
            return buildResponse(HttpStatus.BAD_REQUEST,
                "Invalid value for parameter '" + ex.getName() + "'");
        }
        private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, Object message) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("timestamp", LocalDateTime.now());
            body.put("status", status.value());
            body.put("error", status.getReasonPhrase());
            body.put("message", message);
            return ResponseEntity.status(status).body(body);
        }
}
