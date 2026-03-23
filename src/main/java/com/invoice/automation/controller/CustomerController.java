package com.invoice.automation.controller;

import com.invoice.automation.dto.InvoiceResponse;
import com.invoice.automation.dto.InvoiceStatisticsResponse;
import com.invoice.automation.entity.InvoiceHeader;
import com.invoice.automation.service.InvoiceHeaderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for customer-related operations.
 * Provides endpoints for customer management, statistics, and reporting.
 */
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Customer Management", description = "APIs for managing customer-related operations")
public class CustomerController extends BaseController {

    private final InvoiceHeaderService invoiceHeaderService;

    // ========== Customer Invoice Operations ==========

    @Operation(summary = "Get all customers", description = "Retrieves a list of all unique customers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customers retrieved successfully")
    })
    @GetMapping
    @Cacheable(value = "customerStats", key = "'allCustomers'")
    public ResponseEntity<List<String>> getAllCustomers(WebRequest request) {
        String operation = "GET_ALL_CUSTOMERS";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        List<InvoiceHeader> allInvoices = invoiceHeaderService.getAllInvoiceHeaders();
        List<String> customers = allInvoices.stream()
                .map(InvoiceHeader::getCustomerName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        logOperationEnd(operation, user);
        return success(customers);
    }


    // ========== Customer Statistics ==========

    @Operation(summary = "Get customer statistics", description = "Retrieves statistical information for a specific customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer statistics retrieved successfully")
    })
    @GetMapping("/{customerName}/statistics")
    @Cacheable(value = "customerStats", key = "'stats_' + #customerName")
    public ResponseEntity<Map<String, Object>> getCustomerStatistics(
            @Parameter(description = "Customer name") @PathVariable @NotBlank String customerName,
            WebRequest request) {
        
        String operation = "GET_CUSTOMER_STATISTICS";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        List<InvoiceHeader> customerInvoices = invoiceHeaderService.getInvoiceHeadersByCustomerName(customerName);
        
        // Calculate statistics
        long totalInvoices = customerInvoices.size();
        long currentYearInvoices = customerInvoices.stream()
                .filter(invoice -> invoice.getInvoiceDate().getYear() == LocalDate.now().getYear())
                .count();
        
        long currentMonthInvoices = customerInvoices.stream()
                .filter(invoice -> {
                    LocalDate date = invoice.getInvoiceDate();
                    return date.getYear() == LocalDate.now().getYear() && 
                           date.getMonth() == LocalDate.now().getMonth();
                })
                .count();

        Map<String, Object> statistics = Map.of(
                "customerName", customerName,
                "totalInvoices", totalInvoices,
                "currentYearInvoices", currentYearInvoices,
                "currentMonthInvoices", currentMonthInvoices,
                "firstInvoiceDate", customerInvoices.stream()
                        .map(InvoiceHeader::getInvoiceDate)
                        .min(LocalDate::compareTo)
                        .orElse(null),
                "lastInvoiceDate", customerInvoices.stream()
                        .map(InvoiceHeader::getInvoiceDate)
                        .max(LocalDate::compareTo)
                        .orElse(null)
        );

        logOperationEnd(operation, user);
        return success(statistics);
    }



    // ========== Customer Search and Analysis ==========

    @Operation(summary = "Search customers by name", description = "Searches customers by name (partial match)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customers retrieved successfully")
    })
    @GetMapping("/search")
    @Cacheable(value = "queryCache", key = "'search_' + #name")
    public ResponseEntity<List<String>> searchCustomersByName(
            @Parameter(description = "Customer name to search") @RequestParam @NotBlank String name,
            WebRequest request) {
        
        String operation = "SEARCH_CUSTOMERS_BY_NAME";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        List<InvoiceHeader> allInvoices = invoiceHeaderService.getAllInvoiceHeaders();
        List<String> matchingCustomers = allInvoices.stream()
                .map(InvoiceHeader::getCustomerName)
                .distinct()
                .filter(customerName -> customerName.toLowerCase().contains(name.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());

        logOperationEnd(operation, user);
        return success(matchingCustomers);
    }

    @Operation(summary = "Get customer revenue", description = "Calculates total revenue for a customer within a date range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer revenue calculated successfully")
    })
    @GetMapping("/{customerName}/revenue")
    @Cacheable(value = "customerStats", key = "'revenue_' + #customerName + '_' + #startDate + '_' + #endDate")
    public ResponseEntity<Map<String, Object>> getCustomerRevenue(
            @Parameter(description = "Customer name") @PathVariable @NotBlank String customerName,
            @Parameter(description = "Start date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            WebRequest request) {
        
        String operation = "GET_CUSTOMER_REVENUE";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        // Use provided dates or default to all time
        LocalDate start = startDate != null ? startDate : LocalDate.of(2000, 1, 1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        List<InvoiceHeader> customerInvoices = invoiceHeaderService.getInvoiceHeadersByCustomerName(customerName);
        List<InvoiceHeader> filteredInvoices = customerInvoices.stream()
                .filter(invoice -> !invoice.getInvoiceDate().isBefore(start) && !invoice.getInvoiceDate().isAfter(end))
                .collect(Collectors.toList());

        // Calculate revenue from invoice items
        double totalRevenue = filteredInvoices.stream()
                .flatMap(invoice -> invoice.getItems().stream())
                .mapToDouble(item -> item.getTotalAmount().doubleValue())
                .sum();

        Map<String, Object> revenueData = Map.of(
                "customerName", customerName,
                "totalRevenue", totalRevenue,
                "invoiceCount", filteredInvoices.size(),
                "startDate", start,
                "endDate", end,
                "averageRevenuePerInvoice", filteredInvoices.isEmpty() ? 0.0 : totalRevenue / filteredInvoices.size()
        );

        logOperationEnd(operation, user);
        return success(revenueData);
    }

    @Operation(summary = "Get customer activity summary", description = "Provides a comprehensive activity summary for a customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer activity summary retrieved successfully")
    })
    @GetMapping("/{customerName}/activity")
    @Cacheable(value = "customerStats", key = "'activity_' + #customerName")
    public ResponseEntity<Map<String, Object>> getCustomerActivitySummary(
            @Parameter(description = "Customer name") @PathVariable @NotBlank String customerName,
            WebRequest request) {
        
        String operation = "GET_CUSTOMER_ACTIVITY_SUMMARY";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        List<InvoiceHeader> customerInvoices = invoiceHeaderService.getInvoiceHeadersByCustomerName(customerName);
        
        // Calculate various metrics
        long totalInvoices = customerInvoices.size();
        long invoicesWithPdf = customerInvoices.stream()
                .filter(invoice -> invoice.getInvoicePDF() != null)
                .count();
        
        Map<String, Long> companyCodeDistribution = customerInvoices.stream()
                .collect(Collectors.groupingBy(
                        invoice -> invoice.getCompanyCode() != null ? invoice.getCompanyCode() : "N/A",
                        Collectors.counting()
                ));

        Map<String, Object> activitySummary = Map.of(
                "customerName", customerName,
                "totalInvoices", totalInvoices,
                "invoicesWithPdf", invoicesWithPdf,
                "pdfAttachmentRate", totalInvoices > 0 ? (double) invoicesWithPdf / totalInvoices * 100 : 0.0,
                "companyCodeDistribution", companyCodeDistribution,
                "hasMultipleCompanyCodes", companyCodeDistribution.size() > 1,
                "isRegularCustomer", totalInvoices > 5,
                "lastActivityDate", customerInvoices.stream()
                        .map(InvoiceHeader::getInvoiceDate)
                        .max(LocalDate::compareTo)
                        .orElse(null)
        );

        logOperationEnd(operation, user);
        return success(activitySummary);
    }

}
