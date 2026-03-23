package com.invoice.automation.exception;

public class InvalidInvoiceNumberException extends RuntimeException {
    
    public InvalidInvoiceNumberException(String invoiceNumber) {
        super("Invalid invoice number format: " + invoiceNumber);
    }
    
    public InvalidInvoiceNumberException(String message, Throwable cause) {
        super(message, cause);
    }
}
