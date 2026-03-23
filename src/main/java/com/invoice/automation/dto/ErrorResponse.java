package com.invoice.automation.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
    String timestamp,
    int status,
    String error,
    String message,
    String path,
    List<String> details
) {
    
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(
            LocalDateTime.now().toString(),
            status,
            error,
            message,
            path,
            null
        );
    }
    
    public static ErrorResponse of(int status, String error, String message, String path, List<String> details) {
        return new ErrorResponse(
            LocalDateTime.now().toString(),
            status,
            error,
            message,
            path,
            details
        );
    }
}
