package com.invoice.automation.dto;

import java.time.LocalDateTime;
import java.util.Base64;

public record PdfResponse(
    String fileName,
    String contentType,
    Long fileSize,
    String base64Data,
    LocalDateTime uploadedAt,
    String invoiceNumber
) {
    
    public PdfResponse {
        // Validation in compact constructor
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }
        if (contentType == null || contentType.trim().isEmpty()) {
            throw new IllegalArgumentException("Content type cannot be null or empty");
        }
        if (fileSize != null && fileSize < 0) {
            throw new IllegalArgumentException("File size cannot be negative");
        }
    }
    
    // Static factory method for PDF metadata only (no data)
    public static PdfResponse metadata(String fileName, String contentType, Long fileSize, 
                                     LocalDateTime uploadedAt, String invoiceNumber) {
        return new PdfResponse(fileName, contentType, fileSize, null, uploadedAt, invoiceNumber);
    }
    
    // Static factory method for full PDF response
    public static PdfResponse full(String fileName, String contentType, Long fileSize, 
                                 byte[] data, LocalDateTime uploadedAt, String invoiceNumber) {
        String base64Data = data != null ? Base64.getEncoder().encodeToString(data) : null;
        return new PdfResponse(fileName, contentType, fileSize, base64Data, uploadedAt, invoiceNumber);
    }
}
