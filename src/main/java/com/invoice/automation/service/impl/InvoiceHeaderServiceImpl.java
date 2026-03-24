package com.invoice.automation.service.impl;

import com.invoice.automation.entity.InvoiceHeader;
import com.invoice.automation.entity.InvoicePDF;
import com.invoice.automation.exception.InvoiceNotFoundException;
import com.invoice.automation.exception.InvoiceNumberAlreadyExistsException;
import com.invoice.automation.exception.InvalidInvoiceNumberException;
import com.invoice.automation.exception.PdfStorageException;
import com.invoice.automation.repository.InvoiceHeaderRepository;
import com.invoice.automation.repository.InvoicePDFRepository;
import com.invoice.automation.service.InvoiceHeaderService;
import com.invoice.automation.service.InvoiceNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InvoiceHeaderServiceImpl implements InvoiceHeaderService {

    private final InvoiceHeaderRepository invoiceHeaderRepository;
    private final InvoicePDFRepository invoicePDFRepository;
    private final InvoiceNumberGenerator invoiceNumberGenerator;


    // Basic CRUD operations
    @Override
    public InvoiceHeader saveInvoiceHeader(InvoiceHeader invoiceHeader) {
        log.info("Saving invoice header for customer: {}", invoiceHeader.getCustomerName());
        return invoiceHeaderRepository.save(invoiceHeader);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceHeader> getAllInvoiceHeaders() {
        log.info("Fetching all invoice headers");
        return invoiceHeaderRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceHeader> getAllInvoicesWithItems() {
        log.info("Fetching all invoices with items (optimized for N+1 prevention)");
        return invoiceHeaderRepository.findAllWithItemsAndPDF();
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceHeader getInvoiceHeaderById(UUID id) {
        log.info("Fetching invoice header by id: {}", id);
        return invoiceHeaderRepository.findById(id)
                .orElseThrow(() -> new InvoiceNotFoundException(id));
    }

    @Override
    public void deleteInvoiceHeader(UUID id) {
        log.info("Deleting invoice header with id: {}", id);
        if (!invoiceHeaderRepository.existsById(id)) {
            throw new InvoiceNotFoundException(id);
        }
        invoiceHeaderRepository.deleteById(id);
    }

    @Override
    public InvoiceHeader updateInvoiceHeader(UUID id, InvoiceHeader invoiceHeader) {
        log.info("Updating invoice header with id: {}", id);
        InvoiceHeader existingInvoice = getInvoiceHeaderById(id);
        
        existingInvoice.setCustomerName(invoiceHeader.getCustomerName());
        existingInvoice.setInvoiceDate(invoiceHeader.getInvoiceDate());
        existingInvoice.setPoNumber(invoiceHeader.getPoNumber());
        existingInvoice.setCompanyCode(invoiceHeader.getCompanyCode());
        existingInvoice.setVendorName(invoiceHeader.getVendorName());
        existingInvoice.setAddress(invoiceHeader.getAddress());
        
        return invoiceHeaderRepository.save(existingInvoice);
    }

    // Search and filter operations
    @Override
    @Transactional(readOnly = true)
    public List<InvoiceHeader> getInvoiceHeadersByCustomerName(String customerName) {
        log.info("Fetching invoices for customer: {}", customerName);
        return invoiceHeaderRepository.findByCustomerNameContainingIgnoreCase(customerName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceHeader> getInvoiceHeadersByDateRange(LocalDate startDate, LocalDate endDate) {
        log.info("Fetching invoices between {} and {}", startDate, endDate);
        return invoiceHeaderRepository.findByInvoiceDateBetween(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceHeader> getInvoiceHeadersByInvoiceNumber(String invoiceNumber) {
        log.info("Fetching invoices by invoice number: {}", invoiceNumber);
        return invoiceHeaderRepository.findByInvoiceNumberContainingIgnoreCase(invoiceNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceHeader> getInvoiceHeadersByCompanyCode(String companyCode) {
        log.info("Fetching invoices by company code: {}", companyCode);
        return invoiceHeaderRepository.findByCompanyCodeContainingIgnoreCase(companyCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceHeader> getInvoiceHeadersByVendorName(String vendorName) {
        log.info("Fetching invoices by vendor name: {}", vendorName);
        return invoiceHeaderRepository.findByVendorNameContainingIgnoreCase(vendorName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceHeader> getInvoiceHeadersByPoNumber(String poNumber) {
        log.info("Fetching invoices by PO number: {}", poNumber);
        return invoiceHeaderRepository.findByPoNumberContainingIgnoreCase(poNumber);
    }

    // Business logic operations
    @Override
    public InvoiceHeader createInvoiceWithNumber(InvoiceHeader invoiceHeader) {
        log.info("Creating invoice with auto-generated number");
        String invoiceNumber = invoiceNumberGenerator.generateInvoiceNumber();
        invoiceHeader.setInvoiceNumber(invoiceNumber);
        return saveInvoiceHeader(invoiceHeader);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceHeader> getInvoicesForToday() {
        LocalDate today = LocalDate.now();
        log.info("Fetching invoices for today: {}", today);
        return invoiceHeaderRepository.findByInvoiceDate(today);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceHeader> getInvoicesForThisMonth() {
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();
        log.info("Fetching invoices for current month: {} to {}", startDate, endDate);
        return getInvoiceHeadersByDateRange(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceHeader> getInvoicesForThisYear() {
        LocalDate startDate = LocalDate.now().withDayOfYear(1);
        LocalDate endDate = LocalDate.now().withDayOfYear(365);
        log.info("Fetching invoices for current year: {} to {}", startDate, endDate);
        return getInvoiceHeadersByDateRange(startDate, endDate);
    }

    // PDF operations
    @Override
    public InvoiceHeader attachPdfToInvoice(UUID invoiceId, byte[] pdfData, String fileName) {
        log.info("Attaching PDF to invoice: {}", invoiceId);
        try {
            InvoiceHeader invoice = getInvoiceHeaderById(invoiceId);
            
            if (pdfData == null || pdfData.length == 0) {
                throw new PdfStorageException("PDF data cannot be null or empty");
            }
            
            // Remove existing PDF if any
            invoicePDFRepository.deleteByInvoiceHeaderId(invoiceId);
            
            // Create new PDF
            InvoicePDF invoicePDF = new InvoicePDF(invoice, pdfData, fileName);
            invoicePDFRepository.save(invoicePDF);
            
            invoice.setInvoicePDF(invoicePDF);
            return invoice;
        } catch (Exception e) {
            log.error("Error attaching PDF to invoice: {}", invoiceId, e);
            throw new PdfStorageException("Failed to attach PDF to invoice: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InvoicePDF> getInvoicePdf(UUID invoiceId) {
        log.info("Fetching PDF for invoice: {}", invoiceId);
        return invoicePDFRepository.findByInvoiceHeaderId(invoiceId);
    }

    @Override
    public void removeInvoicePdf(UUID invoiceId) {
        log.info("Removing PDF from invoice: {}", invoiceId);
        invoicePDFRepository.deleteByInvoiceHeaderId(invoiceId);
    }

    // Validation operations
    @Override
    @Transactional(readOnly = true)
    public boolean validateInvoiceNumber(String invoiceNumber) {
        log.info("Validating invoice number: {}", invoiceNumber);
        Pattern pattern = Pattern.compile("^INV-\\d{8}-\\d{4}$");
        boolean isValid = pattern.matcher(invoiceNumber).matches();
        if (!isValid) {
            throw new InvalidInvoiceNumberException(invoiceNumber);
        }
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInvoiceNumberUnique(String invoiceNumber) {
        log.info("Checking uniqueness of invoice number: {}", invoiceNumber);
        if (invoiceHeaderRepository.existsByInvoiceNumber(invoiceNumber)) {
            throw new InvoiceNumberAlreadyExistsException(invoiceNumber);
        }
        return true;
    }

    // Statistics and reporting
    @Override
    @Transactional(readOnly = true)
    public Long getTotalInvoiceCount() {
        log.info("Getting total invoice count");
        return invoiceHeaderRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Long getInvoiceCountByDateRange(LocalDate startDate, LocalDate endDate) {
        log.info("Getting invoice count between {} and {}", startDate, endDate);
        return invoiceHeaderRepository.countByInvoiceDateBetween(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getTotalRevenueByDateRange(LocalDate startDate, LocalDate endDate) {
        log.info("Calculating total revenue between {} and {}", startDate, endDate);
        // This would require a custom query or calculation from items
        // For now, returning a placeholder
        return 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> findTopCustomersByInvoiceCountData(int limit) {
        log.info("Getting top {} customers by invoice count data", limit);
        return invoiceHeaderRepository.findTopCustomersByInvoiceCountData(limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceHeader> getTopCustomersByInvoiceCount(int limit) {
        log.info("Getting top {} customers by invoice count", limit);
        // This would require a custom query with grouping
        // For now, returning all invoices sorted by customer
        // Note: This method might need clarification on requirements
        // as it returns InvoiceHeader objects but the business logic
        // suggests returning customer aggregation data
        return invoiceHeaderRepository.findAll(); // Placeholder implementation
    }

    // Advanced search
    @Override
    @Transactional(readOnly = true)
    public List<InvoiceHeader> searchInvoices(String customerName, LocalDate startDate, 
                                             LocalDate endDate, String companyCode) {
        log.info("Advanced search - Customer: {}, Date range: {} to {}, Company: {}", 
                customerName, startDate, endDate, companyCode);
        // This would require a complex custom query or specification
        // For now, implementing basic search
        if (startDate != null && endDate != null) {
            return getInvoiceHeadersByDateRange(startDate, endDate);
        } else if (customerName != null) {
            return getInvoiceHeadersByCustomerName(customerName);
        } else {
            return getAllInvoiceHeaders();
        }
    }

    // Bulk operations
    @Override
    public List<InvoiceHeader> saveMultipleInvoices(List<InvoiceHeader> invoiceHeaders) {
        log.info("Saving {} invoices", invoiceHeaders.size());
        return invoiceHeaderRepository.saveAll(invoiceHeaders);
    }

    @Override
    public void deleteMultipleInvoices(List<UUID> invoiceIds) {
        log.info("Deleting {} invoices", invoiceIds.size());
        invoiceIds.forEach(this::deleteInvoiceHeader);
    }
}
