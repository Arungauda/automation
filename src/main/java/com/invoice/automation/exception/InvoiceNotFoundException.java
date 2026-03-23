package com.invoice.automation.exception;

import java.util.UUID;

public class InvoiceNotFoundException extends RuntimeException {
    
    public InvoiceNotFoundException(UUID id) {
        super("Invoice not found with id: " + id);
    }
    
    public InvoiceNotFoundException(String invoiceNumber) {
        super("Invoice not found with invoice number: " + invoiceNumber);
    }
    
    public InvoiceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
