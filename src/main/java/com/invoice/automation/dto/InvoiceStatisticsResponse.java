package com.invoice.automation.dto;

import java.time.LocalDate;
import java.util.List;

public record InvoiceStatisticsResponse(
    Long totalInvoices,
    Long invoicesThisMonth,
    Long invoicesThisYear,
    Double totalRevenue,
    Double revenueThisMonth,
    Double revenueThisYear,
    List<CustomerStatistics> topCustomers,
    LocalDate generatedAt
) {
    
    public record CustomerStatistics(
        String customerName,
        Long invoiceCount,
        Double totalRevenue
    ) {
        public CustomerStatistics {
            if (customerName == null || customerName.trim().isEmpty()) {
                throw new IllegalArgumentException("Customer name cannot be null or empty");
            }
        }
    }
    
    // Static factory method for empty statistics
    public static InvoiceStatisticsResponse empty() {
        return new InvoiceStatisticsResponse(
            0L, 0L, 0L, 0.0, 0.0, 0.0, List.of(), LocalDate.now()
        );
    }
}
