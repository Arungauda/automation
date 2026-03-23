package com.invoice.automation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(exclude = {"items", "invoicePDF"}) // Exclude relationships for caching
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "invoiceHeader")
public class InvoiceHeader {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(unique = true, nullable = false)
    private String invoiceNumber;

    @Column(nullable = false)
    private LocalDate invoiceDate;

    @Column(nullable = false)
    private String customerName;

    @Column()
    private String poNumber;

    @Column()
    private String companyCode;

    @Column()
    private String vendorName;

    @Column()
    private String address;

    @OneToMany(mappedBy = "invoiceHeader", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<InvoiceItem> items;

    @OneToOne(mappedBy = "invoiceHeader", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private InvoicePDF invoicePDF;

}
