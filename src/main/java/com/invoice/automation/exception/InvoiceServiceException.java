package com.invoice.automation.exception;

public class InvoiceServiceException extends RuntimeException {
    
    public InvoiceServiceException(String message) {
        super(message);
    }
    
    public InvoiceServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
