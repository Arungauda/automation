package com.invoice.automation.repository;

import com.invoice.automation.entity.InvoiceItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, UUID> {
    
    // Basic invoice item operations - NO CACHE: Rely on Hibernate Level 2 entity caching
    List<InvoiceItem> findByInvoiceHeaderId(UUID invoiceHeaderId);
    
    // NO CACHE: Single item lookup, minimal benefit
    Optional<InvoiceItem> findByInvoiceHeaderIdAndId(UUID invoiceHeaderId, UUID itemId);
    
    // NO CACHE: Delete operation, never cached
    @Modifying
    @Query("DELETE FROM InvoiceItem i WHERE i.invoiceHeader.id = :invoiceHeaderId")
    void deleteByInvoiceHeaderId(@Param("invoiceHeaderId") UUID invoiceHeaderId);
    
    // Search operations - NO CACHE: Rely on Hibernate Level 2 entity caching
    List<InvoiceItem> findByItemCodeContainingIgnoreCase(String itemCode);
    
    List<InvoiceItem> findByHsnCodeContainingIgnoreCase(String hsnCode);
    
    List<InvoiceItem> findByItemDescriptionContainingIgnoreCase(String description);
    
    // Count operations
    Long countByInvoiceHeaderId(UUID invoiceHeaderId);
    
    // Calculation operations
    @Query("SELECT SUM(i.totalAmount) FROM InvoiceItem i WHERE i.invoiceHeader.id = :invoiceHeaderId")
    Double sumTotalAmountByInvoiceHeaderId(@Param("invoiceHeaderId") UUID invoiceHeaderId);
    
    @Query("SELECT SUM(i.quantity) FROM InvoiceItem i WHERE i.invoiceHeader.id = :invoiceHeaderId")
    Integer sumQuantityByInvoiceHeaderId(@Param("invoiceHeaderId") UUID invoiceHeaderId);
    
    // Advanced search (optimized for index usage)
    @Query("SELECT i FROM InvoiceItem i WHERE " +
           "(:itemCode IS NULL OR i.itemCode LIKE CONCAT(:itemCode, '%')) AND " +
           "(:hsnCode IS NULL OR i.hsnCode LIKE CONCAT(:hsnCode, '%')) AND " +
           "(:description IS NULL OR i.itemDescription LIKE CONCAT(:description, '%'))")
    List<InvoiceItem> searchItemsWithFilters(
            @Param("itemCode") String itemCode,
            @Param("hsnCode") String hsnCode,
            @Param("description") String description);
    
    // Find items by price range
    @Query("SELECT i FROM InvoiceItem i WHERE i.unitPrice BETWEEN :minPrice AND :maxPrice")
    List<InvoiceItem> findByUnitPriceBetween(@Param("minPrice") Double minPrice, @Param("maxPrice") Double maxPrice);
    
    // Find most expensive items (paginated to prevent memory issues)
    @Query("SELECT i FROM InvoiceItem i ORDER BY i.unitPrice DESC")
    Page<InvoiceItem> findMostExpensiveItems(Pageable pageable);
    
    // Find items by quantity range
    List<InvoiceItem> findByQuantityBetween(Integer minQuantity, Integer maxQuantity);
}
