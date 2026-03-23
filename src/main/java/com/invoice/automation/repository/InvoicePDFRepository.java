package com.invoice.automation.repository;

import com.invoice.automation.entity.InvoicePDF;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoicePDFRepository extends JpaRepository<InvoicePDF, UUID> {
    
    Optional<InvoicePDF> findByInvoiceHeaderId(UUID invoiceHeaderId);
    
    void deleteByInvoiceHeaderId(UUID invoiceHeaderId);
}
