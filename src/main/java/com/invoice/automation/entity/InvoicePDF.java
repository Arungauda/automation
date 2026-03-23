package com.invoice.automation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(exclude = {"invoiceHeader"}) // Exclude relationship for caching
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "invoicePDF")
public class InvoicePDF {
    
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_header_id", nullable = false, unique = true)
    private InvoiceHeader invoiceHeader;
    
    @Lob
    @Column(columnDefinition = "LONGBLOB", nullable = false)
    private byte[] pdfData;
    
    @Column(nullable = false)
    private String fileName;
    
    @Column(nullable = false)
    private String contentType = "application/pdf";
    
    private Long fileSize;
    
    @Column(nullable = false)
    private java.time.LocalDateTime uploadedAt;
    
    public InvoicePDF(InvoiceHeader invoiceHeader, byte[] pdfData, String fileName) {
        this.invoiceHeader = invoiceHeader;
        this.pdfData = pdfData;
        this.fileName = fileName;
        this.fileSize = (long) pdfData.length;
        this.uploadedAt = java.time.LocalDateTime.now();
    }
}
