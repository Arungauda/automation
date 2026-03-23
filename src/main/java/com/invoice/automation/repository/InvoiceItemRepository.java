package com.invoice.automation.repository;

import com.invoice.automation.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
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
    void deleteByInvoiceHeaderId(UUID invoiceHeaderId);
    
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
    
    // Advanced search
    @Query("SELECT i FROM InvoiceItem i WHERE " +
           "(:itemCode IS NULL OR LOWER(i.itemCode) LIKE LOWER(CONCAT('%', :itemCode, '%'))) AND " +
           "(:hsnCode IS NULL OR LOWER(i.hsnCode) LIKE LOWER(CONCAT('%', :hsnCode, '%'))) AND " +
           "(:description IS NULL OR LOWER(i.itemDescription) LIKE LOWER(CONCAT('%', :description, '%')))")
    List<InvoiceItem> searchItemsWithFilters(
            @Param("itemCode") String itemCode,
            @Param("hsnCode") String hsnCode,
            @Param("description") String description);
    
    // Find items by price range
    @Query("SELECT i FROM InvoiceItem i WHERE i.unitPrice BETWEEN :minPrice AND :maxPrice")
    List<InvoiceItem> findByUnitPriceBetween(@Param("minPrice") Double minPrice, @Param("maxPrice") Double maxPrice);
    
    // Find most expensive items
    @Query("SELECT i FROM InvoiceItem i ORDER BY i.unitPrice DESC")
    List<InvoiceItem> findMostExpensiveItems();
    
    // Find items by quantity range
    List<InvoiceItem> findByQuantityBetween(Integer minQuantity, Integer maxQuantity);
}
