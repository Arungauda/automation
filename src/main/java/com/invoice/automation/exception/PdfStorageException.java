package com.invoice.automation.exception;

public class PdfStorageException extends RuntimeException {
    
    public PdfStorageException(String message) {
        super(message);
    }
    
    public PdfStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
