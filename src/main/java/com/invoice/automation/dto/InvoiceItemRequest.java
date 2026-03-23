package com.invoice.automation.dto;

import java.math.BigDecimal;

public record InvoiceItemRequest(
    String itemDescription,
    Integer quantity,
    BigDecimal unitPrice,
    String itemCode,
    String hsnCode
) {
    
    public InvoiceItemRequest {
        // Validation in compact constructor
        if (itemDescription == null || itemDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("Item description cannot be null or empty");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unit price must be positive");
        }
    }
    
    // Calculated total amount
    public BigDecimal getTotalAmount() {
        return unitPrice.multiply(new BigDecimal(quantity));
    }
}
