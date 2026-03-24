package com.invoice.automation.repository;

import com.invoice.automation.entity.InvoicePDF;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoicePDFRepository extends JpaRepository<InvoicePDF, UUID> {
    
    Optional<InvoicePDF> findByInvoiceHeaderId(UUID invoiceHeaderId);
    
    @Modifying
    @Query("DELETE FROM InvoicePDF i WHERE i.invoiceHeader.id = :invoiceHeaderId")
    void deleteByInvoiceHeaderId(@Param("invoiceHeaderId") UUID invoiceHeaderId);
}
