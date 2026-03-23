package com.invoice.automation.dto;

import java.time.LocalDate;

public record InvoiceSearchRequest(
    String customerName,
    String invoiceNumber,
    String companyCode,
    String vendorName,
    String poNumber,
    LocalDate startDate,
    LocalDate endDate,
    Integer page,
    Integer size,
    String sortBy,
    String sortDirection
) {
    
    public InvoiceSearchRequest {
        // Default values in compact constructor
        if (page == null || page < 0) {
            page = 0;
        }
        if (size == null || size <= 0) {
            size = 10;
        }
        if (sortBy == null) {
            sortBy = "invoiceDate";
        }
        if (sortDirection == null) {
            sortDirection = "desc";
        }
    }
    
    // Convenience constructor for basic search
    public InvoiceSearchRequest(String customerName, LocalDate startDate, LocalDate endDate) {
        this(customerName, null, null, null, null, startDate, endDate, 0, 10, "invoiceDate", "desc");
    }
    
    // Convenience constructor for customer search only
    public InvoiceSearchRequest(String customerName) {
        this(customerName, null, null, null, null, null, null, 0, 10, "invoiceDate", "desc");
    }
}
