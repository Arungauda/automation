package com.invoice.automation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
    UUID id,
    String invoiceNumber,
    LocalDate invoiceDate,
    String customerName,
    String poNumber,
    String companyCode,
    String vendorName,
    String address,
    List<InvoiceItemResponse> items,
    BigDecimal totalAmount,
    boolean hasPdf,
    LocalDate createdAt
) {
    
    // Static factory method for empty invoice response
    public static InvoiceResponse empty() {
        return new InvoiceResponse(
            null, null, null, null, null, null, null, null, 
            null, BigDecimal.ZERO, false, null
        );
    }
    
    // Static factory method for basic invoice response (without items)
    public static InvoiceResponse basic(UUID id, String invoiceNumber, LocalDate invoiceDate, 
                                      String customerName, String poNumber, String companyCode, 
                                      String vendorName, String address, boolean hasPdf) {
        return new InvoiceResponse(
            id, invoiceNumber, invoiceDate, customerName, poNumber, 
            companyCode, vendorName, address, null, BigDecimal.ZERO, hasPdf, null
        );
    }
}
