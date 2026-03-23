package com.invoice.automation.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceItemResponse(
    UUID id,
    String itemDescription,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal totalAmount,
    String itemCode,
    String hsnCode
) {
    
    // Static factory method from entity
    public static InvoiceItemResponse fromEntity(UUID id, String itemDescription, Integer quantity, 
                                               BigDecimal unitPrice, BigDecimal totalAmount, 
                                               String itemCode, String hsnCode) {
        return new InvoiceItemResponse(
            id, itemDescription, quantity, unitPrice, totalAmount, itemCode, hsnCode
        );
    }
    
    // Static factory method for new item
    public static InvoiceItemResponse of(String itemDescription, Integer quantity, 
                                        BigDecimal unitPrice, String itemCode, String hsnCode) {
        BigDecimal totalAmount = unitPrice.multiply(new BigDecimal(quantity));
        return new InvoiceItemResponse(
            null, itemDescription, quantity, unitPrice, totalAmount, itemCode, hsnCode
        );
    }
}
