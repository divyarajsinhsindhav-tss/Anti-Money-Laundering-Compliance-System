package org.tss.tm.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.tss.tm.common.response.ErrorResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        @ExceptionHandler(ApplicationException.class)
        public ResponseEntity<ErrorResponse> handleApplicationException(
                ApplicationException ex,
                HttpServletRequest request
        ) {

                Map<String, String> errors = new HashMap<>();
                if (ex.getErrorCode() != null) {
                        errors.put("code", ex.getErrorCode());
                }

                ErrorResponse errorResponse = ErrorResponse.of(
                        ex.getHttpStatus().value(),
                        ex.getHttpStatus().getReasonPhrase(),
                        ex.getMessage(),
                        request.getRequestURI(),
                        errors
                );

                log.error("ApplicationException: {}", errorResponse);

                return ResponseEntity
                                .status(ex.getHttpStatus())
                                .body(errorResponse);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationException(
                MethodArgumentNotValidException ex,
                HttpServletRequest request
        ) {

                Map<String, String> fieldErrors = new HashMap<>();

                ex.getBindingResult().getFieldErrors()
                                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

                ErrorResponse errorResponse = ErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "Validation failed",
                        request.getRequestURI(),
                        fieldErrors
                );

                log.error("ValidationException: {}", errorResponse);

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(errorResponse);
        }

        @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
        public ResponseEntity<ErrorResponse> handleBadRequestException(
                RuntimeException ex,
                HttpServletRequest request
        ) {

                ErrorResponse errorResponse = ErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        ex.getMessage(),
                        request.getRequestURI()
                );

                log.error("BadRequestException: {}", errorResponse);

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(errorResponse);
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleSecurityAccessDeniedException(
                AccessDeniedException ex,
                HttpServletRequest request
        ) {

                ErrorResponse errorResponse = ErrorResponse.of(
                        HttpStatus.FORBIDDEN.value(),
                        "Forbidden",
                        "You do not have permission to access this resource",
                        request.getRequestURI()
                );

                log.error("AccessDeniedException for URI {}: {}", request.getRequestURI(), ex.getMessage());

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleBadCredentialsException(
                BadCredentialsException ex,
                HttpServletRequest request
        ) {

                ErrorResponse errorResponse = ErrorResponse.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Unauthorized",
                        ex.getMessage(),
                        request.getRequestURI()
                );

                log.error("BadCredentialsException: {}", ex.getMessage());

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ErrorResponse> handleAuthenticationException(
                AuthenticationException ex,
                HttpServletRequest request
        ) {

                ErrorResponse errorResponse = ErrorResponse.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Unauthorized",
                        ex.getMessage(),
                        request.getRequestURI()
                );

                log.error("AuthenticationException: {}", ex.getMessage());

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(
                        Exception ex,
                        HttpServletRequest request) {

                String errorName = ex.getClass().getSimpleName();
                String exactConsoleMessage = ex.getMessage();

                ErrorResponse errorResponse = ErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                        "An unexpected system error occurred.",
                        request.getRequestURI(),
                        null,
                        List.of("Exception Type: " + errorName, "Details: " + exactConsoleMessage)
                );

                log.error("Unhandled Exception: ", ex);

                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(errorResponse);
        }
}