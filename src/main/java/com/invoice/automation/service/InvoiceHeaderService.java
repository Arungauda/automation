package com.invoice.automation.service;

import com.invoice.automation.entity.InvoiceHeader;
import com.invoice.automation.entity.InvoicePDF;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceHeaderService {

    // Basic CRUD operations
    InvoiceHeader saveInvoiceHeader(InvoiceHeader invoiceHeader);

    List<InvoiceHeader> getAllInvoiceHeaders();

    InvoiceHeader getInvoiceHeaderById(UUID id);

    void deleteInvoiceHeader(UUID id);

    InvoiceHeader updateInvoiceHeader(UUID id, InvoiceHeader invoiceHeader);

    // Search and filter operations
    List<InvoiceHeader> getInvoiceHeadersByCustomerName(String customerName);

    List<InvoiceHeader> getInvoiceHeadersByDateRange(LocalDate startDate, LocalDate endDate);

    List<InvoiceHeader> getInvoiceHeadersByInvoiceNumber(String invoiceNumber);

    List<InvoiceHeader> getInvoiceHeadersByCompanyCode(String companyCode);

    List<InvoiceHeader> getInvoiceHeadersByVendorName(String vendorName);

    List<InvoiceHeader> getInvoiceHeadersByPoNumber(String poNumber);

    // Business logic operations
    InvoiceHeader createInvoiceWithNumber(InvoiceHeader invoiceHeader);

    List<InvoiceHeader> getInvoicesForToday();

    List<InvoiceHeader> getInvoicesForThisMonth();

    List<InvoiceHeader> getInvoicesForThisYear();

    // PDF operations
    InvoiceHeader attachPdfToInvoice(UUID invoiceId, byte[] pdfData, String fileName);

    Optional<InvoicePDF> getInvoicePdf(UUID invoiceId);

    void removeInvoicePdf(UUID invoiceId);

    // Validation operations
    boolean validateInvoiceNumber(String invoiceNumber);

    boolean isInvoiceNumberUnique(String invoiceNumber);

    // Statistics and reporting
    Long getTotalInvoiceCount();

    Long getInvoiceCountByDateRange(LocalDate startDate, LocalDate endDate);

    Double getTotalRevenueByDateRange(LocalDate startDate, LocalDate endDate);

    List<InvoiceHeader> getTopCustomersByInvoiceCount(int limit);

    // Advanced search
    List<InvoiceHeader> searchInvoices(String customerName, LocalDate startDate, 
                                      LocalDate endDate, String companyCode);

    // Bulk operations
    List<InvoiceHeader> saveMultipleInvoices(List<InvoiceHeader> invoiceHeaders);

    void deleteMultipleInvoices(List<UUID> invoiceIds);

}
