package com.invoice.automation.dto;

import java.time.LocalDate;
import java.util.List;

public record InvoiceRequest(
    String customerName,
    LocalDate invoiceDate,
    String poNumber,
    String companyCode,
    String vendorName,
    String address,
    List<InvoiceItemRequest> items
) {
    
    public InvoiceRequest {
        // Validation in compact constructor
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        if (invoiceDate == null) {
            throw new IllegalArgumentException("Invoice date cannot be null");
        }
    }
    
    // Convenience constructor for invoice without items
    public InvoiceRequest(String customerName, LocalDate invoiceDate, String poNumber, 
                         String companyCode, String vendorName, String address) {
        this(customerName, invoiceDate, poNumber, companyCode, vendorName, address, null);
    }
}
