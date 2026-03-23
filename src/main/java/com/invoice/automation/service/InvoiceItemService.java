package com.invoice.automation.service;

import com.invoice.automation.entity.InvoiceItem;

import java.util.List;
import java.util.UUID;

public interface InvoiceItemService {
    
    // Basic CRUD operations
    InvoiceItem saveInvoiceItem(InvoiceItem invoiceItem);
    
    List<InvoiceItem> getAllInvoiceItems();
    
    InvoiceItem getInvoiceItemById(UUID id);
    
    void deleteInvoiceItem(UUID id);
    
    InvoiceItem updateInvoiceItem(UUID id, InvoiceItem invoiceItem);
    
    // Invoice-specific operations
    List<InvoiceItem> getItemsByInvoiceHeaderId(UUID invoiceHeaderId);
    
    List<InvoiceItem> saveAllItemsForInvoice(UUID invoiceHeaderId, List<InvoiceItem> items);
    
    void deleteAllItemsForInvoice(UUID invoiceHeaderId);
    
    // Business logic operations
    List<InvoiceItem> getItemsByItemCode(String itemCode);
    
    List<InvoiceItem> getItemsByHsnCode(String hsnCode);
    
    List<InvoiceItem> getItemsByDescriptionContaining(String description);
    
    // Calculation operations
    Double calculateTotalAmountForInvoice(UUID invoiceHeaderId);
    
    Integer getTotalQuantityForInvoice(UUID invoiceHeaderId);
    
    // Bulk operations
    List<InvoiceItem> saveMultipleItems(List<InvoiceItem> items);
    
    void deleteMultipleItems(List<UUID> itemIds);
    
}
