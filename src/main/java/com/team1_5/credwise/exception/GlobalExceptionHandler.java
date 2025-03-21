package com.team1_5.credwise.exception;

import com.team1_5.credwise.dto.ErrorResponse;
import com.team1_5.credwise.service.AuditLogService;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global Exception Handler for centralized error management
 *
 * Provides consistent error responses and logging for various exception scenarios
 *
 * @author Credwise Development Team
 * @version 1.0.0
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final AuditLogService auditLogService;

    public GlobalExceptionHandler(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Handle generic exceptions
     *
     * @param ex Exception to handle
     * @param request Web request context
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            WebRequest request
    ) {
        logger.error("Unexpected error occurred", ex);

        // Log system error
        auditLogService.logSystemError("Unexpected system error", ex);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                request.getDescription(false)
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handle validation exceptions for method arguments
     *
     * @param ex Validation exception
     * @param request Web request context
     * @return ResponseEntity with validation error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            WebRequest request
    ) {
        // Collect validation errors
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null
                                ? fieldError.getDefaultMessage()
                                : "Invalid input"
                ));

        logger.warn("Validation errors: {}", errors);

        // Audit log validation failure
        auditLogService.logEvent(
                AuditLogService.AuditEventType.SYSTEM_ERROR,
                "Validation failed",
                Map.of("errors", errors)
        );

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Validation failed",
                request.getDescription(false)
        );
        errorResponse.setValidationErrors(errors);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle constraint violation exceptions
     *
     * @param ex Constraint violation exception
     * @param request Web request context
     * @return ResponseEntity with validation error details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex,
            WebRequest request
    ) {
        // Collect constraint violation errors
        Map<String, String> errors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage()
                ));

        logger.warn("Constraint violation errors: {}", errors);

        // Audit log constraint violation
        auditLogService.logEvent(
                AuditLogService.AuditEventType.SYSTEM_ERROR,
                "Constraint violation",
                Map.of("errors", errors)
        );

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "CONSTRAINT_VIOLATION",
                "Constraint validation failed",
                request.getDescription(false)
        );
        errorResponse.setValidationErrors(errors);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle specific loan application exceptions
     *
     * @param ex Loan application exception
     * @param request Web request context
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(LoanApplicationException.class)
    public ResponseEntity<ErrorResponse> handleLoanApplicationException(
            LoanApplicationException ex,
            WebRequest request
    ) {
        logger.warn("Loan application error: {}", ex.getMessage());

        // Audit log loan application error
        auditLogService.logEvent(
                AuditLogService.AuditEventType.SYSTEM_ERROR,
                "Loan application error",
                Map.of("message", ex.getMessage())
        );

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "LOAN_APPLICATION_ERROR",
                ex.getMessage(),
                request.getDescription(false)
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle user not found exceptions
     *
     * @param ex User not found exception
     * @param request Web request context
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(
            UserNotFoundException ex,
            WebRequest request
    ) {
        logger.warn("User not found: {}", ex.getMessage());

        // Audit log user not found error
        auditLogService.logEvent(
                AuditLogService.AuditEventType.SYSTEM_ERROR,
                "User not found",
                Map.of("message", ex.getMessage())
        );

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "USER_NOT_FOUND",
                ex.getMessage(),
                request.getDescription(false)
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle unauthorized access exceptions
     *
     * @param ex Unauthorized exception
     * @param request Web request context
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(
            UnauthorizedException ex,
            WebRequest request
    ) {
        logger.warn("Unauthorized access: {}", ex.getMessage());

        // Audit log unauthorized access
        auditLogService.logEvent(
                AuditLogService.AuditEventType.SYSTEM_ERROR,
                "Unauthorized access",
                Map.of("message", ex.getMessage())
        );

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED",
                ex.getMessage(),
                request.getDescription(false)
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }
}