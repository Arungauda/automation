package com.invoice.automation.controller;

import com.invoice.automation.dto.InvoiceItemRequest;
import com.invoice.automation.dto.InvoiceItemResponse;
import com.invoice.automation.entity.InvoiceHeader;
import com.invoice.automation.entity.InvoiceItem;
import com.invoice.automation.service.InvoiceItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.context.request.WebRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for managing invoice items.
 * Provides endpoints for CRUD operations on invoice items.
 */
@RestController
@RequestMapping("/api/v1/invoice-items")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Invoice Item Management", description = "APIs for managing invoice items")
public class InvoiceItemController extends BaseController {

    private final InvoiceItemService invoiceItemService;

    // ========== Basic CRUD Operations ==========

    @Operation(summary = "Create invoice item", description = "Creates a new invoice item")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Invoice item created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    @PostMapping
    public ResponseEntity<InvoiceItemResponse> createInvoiceItem(
            @Valid @RequestBody InvoiceItemRequest itemRequest,
            WebRequest request) {
        
        String operation = "CREATE_INVOICE_ITEM";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        try {
            InvoiceItem invoiceItem = convertToInvoiceItem(itemRequest);
            InvoiceItem savedItem = invoiceItemService.saveInvoiceItem(invoiceItem);
            InvoiceItemResponse response = convertToInvoiceItemResponse(savedItem);
            
            logOperationEnd(operation, user);
            return success(response, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error creating invoice item: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Get all invoice items", description = "Retrieves all invoice items")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invoice items retrieved successfully")
    })
    @GetMapping
    @Cacheable(value = "invoiceItem")
    public ResponseEntity<List<InvoiceItemResponse>> getAllInvoiceItems(WebRequest request) {
        String operation = "GET_ALL_INVOICE_ITEMS";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        List<InvoiceItem> items = invoiceItemService.getAllInvoiceItems();
        List<InvoiceItemResponse> response = items.stream()
                .map(this::convertToInvoiceItemResponse)
                .collect(Collectors.toList());

        logOperationEnd(operation, user);
        return success(response);
    }

    @Operation(summary = "Get invoice item by ID", description = "Retrieves a specific invoice item by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invoice item retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Invoice item not found")
    })
    @GetMapping("/{id}")
    @Cacheable(value = "invoiceItem", key = "#id")
    public ResponseEntity<InvoiceItemResponse> getInvoiceItemById(
            @Parameter(description = "Invoice item ID") @PathVariable UUID id,
            WebRequest request) {
        
        requireNonNull(id, "Invoice item ID");
        String operation = "GET_INVOICE_ITEM_BY_ID";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        InvoiceItem item = invoiceItemService.getInvoiceItemById(id);
        InvoiceItemResponse response = convertToInvoiceItemResponse(item);

        logOperationEnd(operation, user);
        return success(response);
    }

    @Operation(summary = "Update invoice item", description = "Updates an existing invoice item")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invoice item updated successfully"),
            @ApiResponse(responseCode = "404", description = "Invoice item not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PutMapping("/{id}")
    @CacheEvict(value = "invoiceItem", key = "#id")
    public ResponseEntity<InvoiceItemResponse> updateInvoiceItem(
            @Parameter(description = "Invoice item ID") @PathVariable UUID id,
            @Valid @RequestBody InvoiceItemRequest itemRequest,
            WebRequest request) {
        
        requireNonNull(id, "Invoice item ID");
        String operation = "UPDATE_INVOICE_ITEM";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        InvoiceItem itemDetails = convertToInvoiceItem(itemRequest);
        InvoiceItem updatedItem = invoiceItemService.updateInvoiceItem(id, itemDetails);
        InvoiceItemResponse response = convertToInvoiceItemResponse(updatedItem);

        logOperationEnd(operation, user);
        return success(response);
    }

    @Operation(summary = "Delete invoice item", description = "Deletes an invoice item by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Invoice item deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Invoice item not found")
    })
    @DeleteMapping("/{id}")
    @CacheEvict(value = "invoiceItem", key = "#id")
    public ResponseEntity<Void> deleteInvoiceItem(
            @Parameter(description = "Invoice item ID") @PathVariable UUID id,
            WebRequest request) {
        
        requireNonNull(id, "Invoice item ID");
        String operation = "DELETE_INVOICE_ITEM";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        invoiceItemService.deleteInvoiceItem(id);
        logOperationEnd(operation, user);

        return noContent();
    }

    // ========== Invoice-specific Operations ==========

    @Operation(summary = "Get items by invoice ID", description = "Retrieves all items for a specific invoice")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invoice items retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    @GetMapping("/invoice/{invoiceId}")
    @Cacheable(value = "invoiceItem", key = "'invoice_' + #invoiceId")
    public ResponseEntity<List<InvoiceItemResponse>> getItemsByInvoiceId(
            @Parameter(description = "Invoice ID") @PathVariable UUID invoiceId,
            WebRequest request) {
        
        requireNonNull(invoiceId, "Invoice ID");
        String operation = "GET_ITEMS_BY_INVOICE_ID";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        List<InvoiceItem> items = invoiceItemService.getItemsByInvoiceHeaderId(invoiceId);
        List<InvoiceItemResponse> response = items.stream()
                .map(this::convertToInvoiceItemResponse)
                .collect(Collectors.toList());

        logOperationEnd(operation, user);
        return success(response);
    }

    @Operation(summary = "Add item to invoice", description = "Adds a new item to an existing invoice")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Item added to invoice successfully"),
            @ApiResponse(responseCode = "404", description = "Invoice not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping("/invoice/{invoiceId}")
    @CacheEvict(value = {"invoiceItem", "invoiceHeader"}, key = "#invoiceId")
    public ResponseEntity<InvoiceItemResponse> addItemToInvoice(
            @Parameter(description = "Invoice ID") @PathVariable UUID invoiceId,
            @Valid @RequestBody InvoiceItemRequest itemRequest,
            WebRequest request) {
        
        requireNonNull(invoiceId, "Invoice ID");
        String operation = "ADD_ITEM_TO_INVOICE";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        InvoiceItem invoiceItem = convertToInvoiceItem(itemRequest);
        // Set the invoice header reference
        InvoiceHeader header = new InvoiceHeader();
        header.setId(invoiceId);
        invoiceItem.setInvoiceHeader(header);
        InvoiceItem savedItem = invoiceItemService.saveInvoiceItem(invoiceItem);
        InvoiceItemResponse response = convertToInvoiceItemResponse(savedItem);

        logOperationEnd(operation, user);
        return success(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Remove item from invoice", description = "Removes an item from an invoice")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Item removed from invoice successfully"),
            @ApiResponse(responseCode = "404", description = "Invoice item not found")
    })
    @DeleteMapping("/invoice/{invoiceId}/item/{itemId}")
    @CacheEvict(value = {"invoiceItem", "invoiceHeader"}, key = "#invoiceId")
    public ResponseEntity<Void> removeItemFromInvoice(
            @Parameter(description = "Invoice ID") @PathVariable UUID invoiceId,
            @Parameter(description = "Item ID") @PathVariable UUID itemId,
            WebRequest request) {
        
        requireNonNull(invoiceId, "Invoice ID");
        requireNonNull(itemId, "Item ID");
        String operation = "REMOVE_ITEM_FROM_INVOICE";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        invoiceItemService.deleteInvoiceItem(itemId);
        logOperationEnd(operation, user);

        return noContent();
    }

    // ========== Search and Filter Operations ==========

    @Operation(summary = "Search items by description", description = "Searches invoice items by description")
    @GetMapping("/search")
    @Cacheable(value = "queryCache", key = "#description")
    public ResponseEntity<List<InvoiceItemResponse>> searchItemsByDescription(
            @Parameter(description = "Item description to search") @RequestParam @NotNull String description,
            WebRequest request) {
        
        requireNonEmpty(description, "Description");
        String operation = "SEARCH_ITEMS_BY_DESCRIPTION";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        List<InvoiceItem> items = invoiceItemService.getItemsByDescriptionContaining(description);
        List<InvoiceItemResponse> response = items.stream()
                .map(this::convertToInvoiceItemResponse)
                .collect(Collectors.toList());

        logOperationEnd(operation, user);
        return success(response);
    }

    @Operation(summary = "Get items by price range", description = "Retrieves items within a specified price range")
    @GetMapping("/price-range")
    @Cacheable(value = "queryCache", key = "#minPrice + '_' + #maxPrice")
    public ResponseEntity<List<InvoiceItemResponse>> getItemsByPriceRange(
            @Parameter(description = "Minimum price") @RequestParam @NotNull BigDecimal minPrice,
            @Parameter(description = "Maximum price") @RequestParam @NotNull BigDecimal maxPrice,
            WebRequest request) {
        
        String operation = "GET_ITEMS_BY_PRICE_RANGE";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        // Note: getItemsByPriceRange is not available in service interface
        // This method needs to be implemented in the service or use different approach
        throw new UnsupportedOperationException("getItemsByPriceRange method not implemented in service");
    }

    @Operation(summary = "Get items by item code", description = "Retrieves items by their item code")
    @GetMapping("/code/{itemCode}")
    @Cacheable(value = "queryCache", key = "'code_' + #itemCode")
    public ResponseEntity<List<InvoiceItemResponse>> getItemsByItemCode(
            @Parameter(description = "Item code") @PathVariable String itemCode,
            WebRequest request) {
        
        requireNonEmpty(itemCode, "Item code");
        String operation = "GET_ITEMS_BY_ITEM_CODE";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        List<InvoiceItem> items = invoiceItemService.getItemsByItemCode(itemCode);
        List<InvoiceItemResponse> response = items.stream()
                .map(this::convertToInvoiceItemResponse)
                .collect(Collectors.toList());

        logOperationEnd(operation, user);
        return success(response);
    }

    // ========== Bulk Operations ==========

    @Operation(summary = "Create multiple items for invoice", description = "Creates multiple items for an invoice in a single request")
    @PostMapping("/invoice/{invoiceId}/bulk")
    @CacheEvict(value = {"invoiceItem", "invoiceHeader"}, key = "#invoiceId")
    public ResponseEntity<List<InvoiceItemResponse>> createMultipleItems(
            @Parameter(description = "Invoice ID") @PathVariable UUID invoiceId,
            @Valid @RequestBody List<InvoiceItemRequest> itemRequests,
            WebRequest request) {
        
        requireNonNull(invoiceId, "Invoice ID");
        String operation = "CREATE_MULTIPLE_ITEMS";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        List<InvoiceItem> items = itemRequests.stream()
                .map(this::convertToInvoiceItem)
                .collect(Collectors.toList());

        List<InvoiceItem> savedItems = invoiceItemService.saveAllItemsForInvoice(invoiceId, items);
        List<InvoiceItemResponse> response = savedItems.stream()
                .map(this::convertToInvoiceItemResponse)
                .collect(Collectors.toList());

        logOperationEnd(operation, user);
        return success(response, HttpStatus.CREATED);
    }

    // ========== Helper Methods ==========

    private InvoiceItem convertToInvoiceItem(InvoiceItemRequest request) {
        return InvoiceItem.builder()
                .itemDescription(request.itemDescription())
                .quantity(request.quantity())
                .unitPrice(request.unitPrice())
                .totalAmount(request.getTotalAmount())
                .itemCode(request.itemCode())
                .hsnCode(request.hsnCode())
                .build();
    }

    private InvoiceItemResponse convertToInvoiceItemResponse(InvoiceItem item) {
        return new InvoiceItemResponse(
                item.getId(),
                item.getItemDescription(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalAmount(),
                item.getItemCode(),
                item.getHsnCode()
        );
    }
}
