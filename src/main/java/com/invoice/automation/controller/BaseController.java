package com.invoice.automation.controller;

import com.invoice.automation.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

/**
 * Base controller providing common functionality for all REST controllers.
 * Includes standardized response methods and exception handling utilities.
 */
@Slf4j
public abstract class BaseController {

    /**
     * Creates a successful response with data and HTTP status 200.
     *
     * @param data the response data
     * @param <T>  the type of the response data
     * @return ResponseEntity with the data and HTTP 200 status
     */
    protected <T> ResponseEntity<T> success(T data) {
        return ResponseEntity.ok(data);
    }

    /**
     * Creates a successful response with data and custom HTTP status.
     *
     * @param data   the response data
     * @param status the HTTP status to return
     * @param <T>    the type of the response data
     * @return ResponseEntity with the data and specified status
     */
    protected <T> ResponseEntity<T> success(T data, HttpStatus status) {
        return ResponseEntity.status(status).body(data);
    }

    /**
     * Creates a successful response with HTTP status 204 (No Content).
     *
     * @return ResponseEntity with HTTP 204 status
     */
    protected ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }

    /**
     * Creates an error response with standard error format.
     *
     * @param status  the HTTP status
     * @param error   the error type
     * @param message the error message
     * @param request the web request
     * @return ResponseEntity with error details
     */
    protected ResponseEntity<ErrorResponse> error(HttpStatus status, String error, String message, WebRequest request) {
        log.error("Error response: {} - {}", error, message);
        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                error,
                message,
                request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Creates an error response with detailed error information.
     *
     * @param status  the HTTP status
     * @param error   the error type
     * @param message the error message
     * @param request the web request
     * @param details list of detailed error messages
     * @return ResponseEntity with error details
     */
    protected ResponseEntity<ErrorResponse> error(HttpStatus status, String error, String message, WebRequest request, List<String> details) {
        log.error("Error response with details: {} - {}", error, message);
        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                error,
                message,
                request.getDescription(false).replace("uri=", ""),
                details
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Extracts the current user's information from the request.
     * This can be overridden in subclasses to implement authentication/authorization.
     *
     * @param request the web request
     * @return the current user identifier or "anonymous" if not authenticated
     */
    protected String getCurrentUser(WebRequest request) {
        // TODO: Implement proper user extraction from security context
        return "anonymous";
    }

    /**
     * Logs the start of a controller operation.
     *
     * @param operation the operation being performed
     * @param user      the user performing the operation
     */
    protected void logOperationStart(String operation, String user) {
        log.info("Starting operation: {} by user: {}", operation, user);
    }

    /**
     * Logs the completion of a controller operation.
     *
     * @param operation the operation that was performed
     * @param user      the user who performed the operation
     */
    protected void logOperationEnd(String operation, String user) {
        log.info("Completed operation: {} by user: {}", operation, user);
    }

    /**
     * Validates that a required parameter is not null.
     *
     * @param param     the parameter to validate
     * @param paramName the parameter name for error messages
     * @throws IllegalArgumentException if the parameter is null
     */
    protected void requireNonNull(Object param, String paramName) {
        if (param == null) {
            throw new IllegalArgumentException(paramName + " cannot be null");
        }
    }

    /**
     * Validates that a required string parameter is not null or empty.
     *
     * @param param     the parameter to validate
     * @param paramName the parameter name for error messages
     * @throws IllegalArgumentException if the parameter is null or empty
     */
    protected void requireNonEmpty(String param, String paramName) {
        if (param == null || param.trim().isEmpty()) {
            throw new IllegalArgumentException(paramName + " cannot be null or empty");
        }
    }
}
