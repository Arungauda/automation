package com.invoice.automation.exception;

public class InvoiceNumberAlreadyExistsException extends RuntimeException {
    
    public InvoiceNumberAlreadyExistsException(String invoiceNumber) {
        super("Invoice number already exists: " + invoiceNumber);
    }
    
    public InvoiceNumberAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
