package com.invoice.automation.controller;

import com.invoice.automation.dto.*;
import com.invoice.automation.entity.InvoiceHeader;
import com.invoice.automation.service.InvoiceHeaderService;
import com.invoice.automation.service.InvoiceItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for managing invoices and their related operations.
 * Handles HTTP concerns only - all business logic is delegated to services.
 */
@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Invoice Management", description = "APIs for managing invoices, items, and PDFs")
public class InvoiceController extends BaseController {

    private final InvoiceHeaderService invoiceHeaderService;
    private final InvoiceItemService invoiceItemService;

    // ========== Basic CRUD Operations ==========

    @Operation(summary = "Create a new invoice", description = "Creates a new invoice with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Invoice created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "Invoice number already exists")
    })
    @PostMapping
    public ResponseEntity<InvoiceResponse> createInvoice(
            @Valid @RequestBody InvoiceRequest invoiceRequest,
            WebRequest request) {
        
        String operation = "CREATE_INVOICE";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        try {
            // Convert request to entity
            InvoiceHeader invoiceHeader = convertToInvoiceHeader(invoiceRequest);
            InvoiceHeader savedInvoice = invoiceHeaderService.createInvoiceWithNumber(invoiceHeader);
            
            InvoiceResponse response = convertToInvoiceResponse(savedInvoice);
            logOperationEnd(operation, user);
            
            return success(response, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error creating invoice: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Get all invoices", description = "Retrieves a list of all invoices")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invoices retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> getAllInvoices(WebRequest request) {
        String operation = "GET_ALL_INVOICES";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        List<InvoiceHeader> invoices = invoiceHeaderService.getAllInvoiceHeaders();
        List<InvoiceResponse> response = invoices.stream()
                .map(this::convertToInvoiceResponse)
                .collect(Collectors.toList());

        logOperationEnd(operation, user);
        return success(response);
    }

    @Operation(summary = "Get invoice by ID", description = "Retrieves a specific invoice by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invoice retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoiceById(
            @Parameter(description = "Invoice ID") @PathVariable UUID id,
            WebRequest request) {
        
        requireNonNull(id, "Invoice ID");
        String operation = "GET_INVOICE_BY_ID";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        InvoiceHeader invoice = invoiceHeaderService.getInvoiceHeaderById(id);
        InvoiceResponse response = convertToInvoiceResponse(invoice);

        logOperationEnd(operation, user);
        return success(response);
    }

    @Operation(summary = "Update invoice", description = "Updates an existing invoice")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invoice updated successfully"),
            @ApiResponse(responseCode = "404", description = "Invoice not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PutMapping("/{id}")
    @CacheEvict(value = {"invoiceHeader", "queryCache"}, key = "#id")
    public ResponseEntity<InvoiceResponse> updateInvoice(
            @Parameter(description = "Invoice ID") @PathVariable UUID id,
            @Valid @RequestBody InvoiceRequest invoiceRequest,
            WebRequest request) {
        
        requireNonNull(id, "Invoice ID");
        String operation = "UPDATE_INVOICE";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        InvoiceHeader invoiceDetails = convertToInvoiceHeader(invoiceRequest);
        InvoiceHeader updatedInvoice = invoiceHeaderService.updateInvoiceHeader(id, invoiceDetails);
        InvoiceResponse response = convertToInvoiceResponse(updatedInvoice);

        logOperationEnd(operation, user);
        return success(response);
    }

    @Operation(summary = "Delete invoice", description = "Deletes an invoice by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Invoice deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    @DeleteMapping("/{id}")
    @CacheEvict(value = {"invoiceHeader", "queryCache"}, key = "#id")
    public ResponseEntity<Void> deleteInvoice(
            @Parameter(description = "Invoice ID") @PathVariable UUID id,
            WebRequest request) {
        
        requireNonNull(id, "Invoice ID");
        String operation = "DELETE_INVOICE";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        invoiceHeaderService.deleteInvoiceHeader(id);
        logOperationEnd(operation, user);

        return noContent();
    }

    // ========== Search and Filter Operations ==========

    @Operation(summary = "Search invoices", description = "Searches invoices based on various criteria")
    @GetMapping("/search")
    public ResponseEntity<List<InvoiceResponse>> searchInvoices(
            @Parameter(description = "Customer name") @RequestParam(required = false) String customerName,
            @Parameter(description = "Start date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Company code") @RequestParam(required = false) String companyCode,
            WebRequest request) {
        
        String operation = "SEARCH_INVOICES";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        List<InvoiceHeader> invoices = invoiceHeaderService.searchInvoices(customerName, startDate, endDate, companyCode);
        List<InvoiceResponse> response = invoices.stream()
                .map(this::convertToInvoiceResponse)
                .collect(Collectors.toList());

        logOperationEnd(operation, user);
        return success(response);
    }

    @Operation(summary = "Get invoices by customer", description = "Retrieves all invoices for a specific customer")
    @GetMapping("/customer/{customerName}")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByCustomer(
            @Parameter(description = "Customer name") @PathVariable String customerName,
            WebRequest request) {
        
        requireNonEmpty(customerName, "Customer name");
        String operation = "GET_INVOICES_BY_CUSTOMER";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        List<InvoiceHeader> invoices = invoiceHeaderService.getInvoiceHeadersByCustomerName(customerName);
        List<InvoiceResponse> response = invoices.stream()
                .map(this::convertToInvoiceResponse)
                .collect(Collectors.toList());

        logOperationEnd(operation, user);
        return success(response);
    }

    @Operation(summary = "Get invoices by date range", description = "Retrieves invoices within a date range")
    @GetMapping("/date-range")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByDateRange(
            @Parameter(description = "Start date") @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            WebRequest request) {
        
        String operation = "GET_INVOICES_BY_DATE_RANGE";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        List<InvoiceHeader> invoices = invoiceHeaderService.getInvoiceHeadersByDateRange(startDate, endDate);
        List<InvoiceResponse> response = invoices.stream()
                .map(this::convertToInvoiceResponse)
                .collect(Collectors.toList());

        logOperationEnd(operation, user);
        return success(response);
    }

    // ========== PDF Management ==========

    @Operation(summary = "Upload PDF for invoice", description = "Uploads and associates a PDF file with an invoice")
    @PostMapping(value = "/{id}/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @CacheEvict(value = "invoiceHeader", key = "#id")
    public ResponseEntity<PdfResponse> uploadPdf(
            @Parameter(description = "Invoice ID") @PathVariable UUID id,
            @Parameter(description = "PDF file") @RequestParam("file") @NotNull MultipartFile file,
            WebRequest request) {
        
        requireNonNull(id, "Invoice ID");
        String operation = "UPLOAD_PDF";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        try {
            byte[] pdfData = file.getBytes();
            String fileName = file.getOriginalFilename();
            
            InvoiceHeader updatedInvoice = invoiceHeaderService.attachPdfToInvoice(id, pdfData, fileName);
            PdfResponse response = new PdfResponse(fileName, pdfData.length, "PDF uploaded successfully");

            logOperationEnd(operation, user);
            return success(response);
        } catch (Exception e) {
            log.error("Error uploading PDF: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Download invoice PDF", description = "Downloads the PDF associated with an invoice")
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadPdf(
            @Parameter(description = "Invoice ID") @PathVariable UUID id,
            WebRequest request) {
        
        requireNonNull(id, "Invoice ID");
        String operation = "DOWNLOAD_PDF";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        InvoiceHeader invoice = invoiceHeaderService.getInvoiceHeaderById(id);
        if (invoice.getInvoicePDF() == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] pdfData = invoice.getInvoicePDF().getPdfData();
        String fileName = invoice.getInvoicePDF().getFileName();

        logOperationEnd(operation, user);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .body(pdfData);
    }

    @Operation(summary = "Delete invoice PDF", description = "Removes the PDF associated with an invoice")
    @DeleteMapping("/{id}/pdf")
    @CacheEvict(value = {"invoicePDF", "invoiceHeader"}, key = "#id")
    public ResponseEntity<Void> deletePdf(
            @Parameter(description = "Invoice ID") @PathVariable UUID id,
            WebRequest request) {
        
        requireNonNull(id, "Invoice ID");
        String operation = "DELETE_PDF";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        invoiceHeaderService.removeInvoicePdf(id);
        logOperationEnd(operation, user);

        return noContent();
    }

    // ========== Statistics and Reporting ==========

    @Operation(summary = "Get invoice statistics", description = "Retrieves statistical information about invoices")
    @GetMapping("/statistics")
    public ResponseEntity<InvoiceStatisticsResponse> getStatistics(WebRequest request) {
        String operation = "GET_STATISTICS";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        Long totalCount = invoiceHeaderService.getTotalInvoiceCount();
        List<InvoiceHeader> todayInvoices = invoiceHeaderService.getInvoicesForToday();
        List<InvoiceHeader> monthInvoices = invoiceHeaderService.getInvoicesForThisMonth();

        InvoiceStatisticsResponse response = new InvoiceStatisticsResponse(
                totalCount,
                (long) todayInvoices.size(),
                (long) monthInvoices.size(),
                invoiceHeaderService.getInvoiceCountByDateRange(
                        LocalDate.now().withDayOfMonth(1),
                        LocalDate.now()
                )
        );

        logOperationEnd(operation, user);
        return success(response);
    }

    // ========== Bulk Operations ==========

    @Operation(summary = "Create multiple invoices", description = "Creates multiple invoices in a single request")
    @PostMapping("/bulk")
    public ResponseEntity<List<InvoiceResponse>> createMultipleInvoices(
            @Valid @RequestBody List<InvoiceRequest> invoiceRequests,
            WebRequest request) {
        
        String operation = "CREATE_MULTIPLE_INVOICES";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        List<InvoiceHeader> invoices = invoiceRequests.stream()
                .map(this::convertToInvoiceHeader)
                .collect(Collectors.toList());

        List<InvoiceHeader> savedInvoices = invoiceHeaderService.saveMultipleInvoices(invoices);
        List<InvoiceResponse> response = savedInvoices.stream()
                .map(this::convertToInvoiceResponse)
                .collect(Collectors.toList());

        logOperationEnd(operation, user);
        return success(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Delete multiple invoices", description = "Deletes multiple invoices in a single request")
    @DeleteMapping("/bulk")
    public ResponseEntity<Void> deleteMultipleInvoices(
            @RequestBody List<UUID> invoiceIds,
            WebRequest request) {
        
        String operation = "DELETE_MULTIPLE_INVOICES";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        invoiceHeaderService.deleteMultipleInvoices(invoiceIds);
        logOperationEnd(operation, user);

        return noContent();
    }

    // ========== Helper Methods ==========

    private InvoiceHeader convertToInvoiceHeader(InvoiceRequest request) {
        return InvoiceHeader.builder()
                .customerName(request.customerName())
                .invoiceDate(request.invoiceDate())
                .poNumber(request.poNumber())
                .companyCode(request.companyCode())
                .vendorName(request.vendorName())
                .address(request.address())
                .build();
    }

    private InvoiceResponse convertToInvoiceResponse(InvoiceHeader invoice) {
        List<InvoiceItemResponse> itemResponses = invoice.getItems() != null ?
                invoice.getItems().stream()
                        .map(this::convertToInvoiceItemResponse)
                        .collect(Collectors.toList()) :
                List.of();

        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getInvoiceDate(),
                invoice.getCustomerName(),
                invoice.getPoNumber(),
                invoice.getCompanyCode(),
                invoice.getVendorName(),
                invoice.getAddress(),
                itemResponses,
                invoice.getInvoicePDF() != null
        );
    }

    private InvoiceItemResponse convertToInvoiceItemResponse(com.invoice.automation.entity.InvoiceItem item) {
        return new InvoiceItemResponse(
                item.getId(),
                item.getItemDescription(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalAmount(),
                item.getItemCode(),
                item.getHsnCode()
        );
    }
}
