package com.invoice.automation.repository;

import com.invoice.automation.entity.InvoiceHeader;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceHeaderRepository extends JpaRepository<InvoiceHeader, java.util.UUID> {

    // Search methods - CACHE: Frequently accessed, read-heavy queries
    @Cacheable
    List<InvoiceHeader> findByCustomerNameContainingIgnoreCase(String customerName);
    
    @Cacheable
    List<InvoiceHeader> findByInvoiceDateBetween(LocalDate startDate, LocalDate endDate);
    
    @Cacheable
    List<InvoiceHeader> findByInvoiceNumberContainingIgnoreCase(String invoiceNumber);
    
    @Cacheable
    List<InvoiceHeader> findByCompanyCodeContainingIgnoreCase(String companyCode);
    
    @Cacheable
    List<InvoiceHeader> findByVendorNameContainingIgnoreCase(String vendorName);
    
    // NO CACHE: PO numbers change frequently, low benefit
    List<InvoiceHeader> findByPoNumberContainingIgnoreCase(String poNumber);
    
    // NO CACHE: Date-specific queries, high variability
    List<InvoiceHeader> findByInvoiceDate(LocalDate invoiceDate);
    
    // Count methods
    Long countByInvoiceDateBetween(LocalDate startDate, LocalDate endDate);
    
    boolean existsByInvoiceNumber(String invoiceNumber);
    
    // Performance optimized methods for revenue calculations
    @Query("SELECT DISTINCT i FROM InvoiceHeader i LEFT JOIN FETCH i.items LEFT JOIN FETCH i.invoicePDF")
    List<InvoiceHeader> findAllWithItemsAndPDF();
    
    @Query("SELECT DISTINCT i FROM InvoiceHeader i LEFT JOIN FETCH i.items WHERE i.id IN :ids")
    List<InvoiceHeader> findByIdsWithItems(@Param("ids") List<UUID> ids);

    // Statistics methods - CACHE: Expensive aggregation queries
    @Cacheable
    @Query("SELECT i.customerName, COUNT(i) FROM InvoiceHeader i GROUP BY i.customerName ORDER BY COUNT(i) DESC LIMIT :limit")
    List<Object[]> findTopCustomersByInvoiceCountData(@Param("limit") int limit);
    
    // Advanced search (optimized for index usage)
    @Query("SELECT i FROM InvoiceHeader i WHERE " +
           "(:customerName IS NULL OR i.customerName LIKE CONCAT(:customerName, '%')) AND " +
           "(:startDate IS NULL OR i.invoiceDate >= :startDate) AND " +
           "(:endDate IS NULL OR i.invoiceDate <= :endDate) AND " +
           "(:companyCode IS NULL OR i.companyCode LIKE CONCAT(:companyCode, '%'))")
    List<InvoiceHeader> findInvoicesWithFilters(
            @Param("customerName") String customerName,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("companyCode") String companyCode);

}
