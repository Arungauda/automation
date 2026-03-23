package com.invoice.automation.exception;

import java.util.UUID;

public class InvoiceItemNotFoundException extends RuntimeException {
    
    public InvoiceItemNotFoundException(UUID id) {
        super("Invoice item not found with id: " + id);
    }
    
    public InvoiceItemNotFoundException(String message) {
        super(message);
    }
    
    public InvoiceItemNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
