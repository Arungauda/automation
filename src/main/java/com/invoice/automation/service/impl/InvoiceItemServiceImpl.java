package com.invoice.automation.service.impl;

import com.invoice.automation.entity.InvoiceHeader;
import com.invoice.automation.entity.InvoiceItem;
import com.invoice.automation.exception.InvoiceItemNotFoundException;
import com.invoice.automation.exception.InvoiceNotFoundException;
import com.invoice.automation.repository.InvoiceHeaderRepository;
import com.invoice.automation.repository.InvoiceItemRepository;
import com.invoice.automation.service.InvoiceItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InvoiceItemServiceImpl implements InvoiceItemService {

    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceHeaderRepository invoiceHeaderRepository;

    // Basic CRUD operations
    @Override
    public InvoiceItem saveInvoiceItem(InvoiceItem invoiceItem) {
        log.info("Saving invoice item: {}", invoiceItem.getItemDescription());
        
        // Validate invoice header exists
        if (invoiceItem.getInvoiceHeader() != null && 
            invoiceItem.getInvoiceHeader().getId() != null) {
            InvoiceHeader header = invoiceHeaderRepository.findById(invoiceItem.getInvoiceHeader().getId())
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceItem.getInvoiceHeader().getId()));
            invoiceItem.setInvoiceHeader(header);
        }
        
        // Calculate total amount
        calculateTotalAmount(invoiceItem);
        
        return invoiceItemRepository.save(invoiceItem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceItem> getAllInvoiceItems() {
        log.info("Fetching all invoice items");
        return invoiceItemRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceItem getInvoiceItemById(UUID id) {
        log.info("Fetching invoice item by id: {}", id);
        return invoiceItemRepository.findById(id)
                .orElseThrow(() -> new InvoiceItemNotFoundException(id));
    }

    @Override
    public void deleteInvoiceItem(UUID id) {
        log.info("Deleting invoice item with id: {}", id);
        if (!invoiceItemRepository.existsById(id)) {
            throw new InvoiceItemNotFoundException(id);
        }
        invoiceItemRepository.deleteById(id);
    }

    @Override
    public InvoiceItem updateInvoiceItem(UUID id, InvoiceItem invoiceItem) {
        log.info("Updating invoice item with id: {}", id);
        InvoiceItem existingItem = getInvoiceItemById(id);
        
        existingItem.setItemDescription(invoiceItem.getItemDescription());
        existingItem.setQuantity(invoiceItem.getQuantity());
        existingItem.setUnitPrice(invoiceItem.getUnitPrice());
        existingItem.setItemCode(invoiceItem.getItemCode());
        existingItem.setHsnCode(invoiceItem.getHsnCode());
        
        // Recalculate total amount
        calculateTotalAmount(existingItem);
        
        return invoiceItemRepository.save(existingItem);
    }

    // Invoice-specific operations
    @Override
    @Transactional(readOnly = true)
    public List<InvoiceItem> getItemsByInvoiceHeaderId(UUID invoiceHeaderId) {
        log.info("Fetching items for invoice header: {}", invoiceHeaderId);
        
        if (!invoiceHeaderRepository.existsById(invoiceHeaderId)) {
            throw new InvoiceNotFoundException(invoiceHeaderId);
        }
        
        return invoiceItemRepository.findByInvoiceHeaderId(invoiceHeaderId);
    }

    @Override
    public List<InvoiceItem> saveAllItemsForInvoice(UUID invoiceHeaderId, List<InvoiceItem> items) {
        log.info("Saving {} items for invoice: {}", items.size(), invoiceHeaderId);
        
        InvoiceHeader header = invoiceHeaderRepository.findById(invoiceHeaderId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceHeaderId));
        
        // Remove existing items
        deleteAllItemsForInvoice(invoiceHeaderId);
        
        // Set header and calculate totals for new items
        items.forEach(item -> {
            item.setInvoiceHeader(header);
            calculateTotalAmount(item);
        });
        
        return invoiceItemRepository.saveAll(items);
    }

    @Override
    public void deleteAllItemsForInvoice(UUID invoiceHeaderId) {
        log.info("Deleting all items for invoice: {}", invoiceHeaderId);
        
        if (!invoiceHeaderRepository.existsById(invoiceHeaderId)) {
            throw new InvoiceNotFoundException(invoiceHeaderId);
        }
        
        invoiceItemRepository.deleteByInvoiceHeaderId(invoiceHeaderId);
    }

    // Business logic operations
    @Override
    @Transactional(readOnly = true)
    public List<InvoiceItem> getItemsByItemCode(String itemCode) {
        log.info("Fetching items by item code: {}", itemCode);
        return invoiceItemRepository.findByItemCodeContainingIgnoreCase(itemCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceItem> getItemsByHsnCode(String hsnCode) {
        log.info("Fetching items by HSN code: {}", hsnCode);
        return invoiceItemRepository.findByHsnCodeContainingIgnoreCase(hsnCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceItem> getItemsByDescriptionContaining(String description) {
        log.info("Fetching items by description: {}", description);
        return invoiceItemRepository.findByItemDescriptionContainingIgnoreCase(description);
    }

    // Calculation operations
    @Override
    @Transactional(readOnly = true)
    public Double calculateTotalAmountForInvoice(UUID invoiceHeaderId) {
        log.info("Calculating total amount for invoice: {}", invoiceHeaderId);
        
        List<InvoiceItem> items = getItemsByInvoiceHeaderId(invoiceHeaderId);
        return items.stream()
                .map(InvoiceItem::getTotalAmount)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalQuantityForInvoice(UUID invoiceHeaderId) {
        log.info("Calculating total quantity for invoice: {}", invoiceHeaderId);
        
        List<InvoiceItem> items = getItemsByInvoiceHeaderId(invoiceHeaderId);
        return items.stream()
                .mapToInt(InvoiceItem::getQuantity)
                .sum();
    }

    // Bulk operations
    @Override
    public List<InvoiceItem> saveMultipleItems(List<InvoiceItem> items) {
        log.info("Saving {} invoice items", items.size());
        
        // Calculate totals for all items
        items.forEach(this::calculateTotalAmount);
        
        return invoiceItemRepository.saveAll(items);
    }

    @Override
    public void deleteMultipleItems(List<UUID> itemIds) {
        log.info("Deleting {} invoice items", itemIds.size());
        
        // Validate all items exist
        itemIds.forEach(id -> {
            if (!invoiceItemRepository.existsById(id)) {
                throw new InvoiceItemNotFoundException(id);
            }
        });
        
        invoiceItemRepository.deleteAllById(itemIds);
    }

    // Helper method to calculate total amount
    private void calculateTotalAmount(InvoiceItem item) {
        if (item.getQuantity() != null && item.getUnitPrice() != null) {
            BigDecimal totalAmount = item.getUnitPrice().multiply(new BigDecimal(item.getQuantity()));
            item.setTotalAmount(totalAmount);
        }
    }
}
