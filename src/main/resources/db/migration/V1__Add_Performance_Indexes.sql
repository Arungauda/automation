-- Performance Optimization Indexes
-- These indexes are critical for preventing full table scans
-- and ensuring the application doesn't break under load

-- Invoice Header Table Indexes
-- Critical for search operations and reporting
CREATE INDEX idx_invoice_header_customer_name ON invoice_header(customer_name);
CREATE INDEX idx_invoice_header_invoice_date ON invoice_header(invoice_date);
CREATE INDEX idx_invoice_header_invoice_number ON invoice_header(invoice_number);
CREATE INDEX idx_invoice_header_company_code ON invoice_header(company_code);
CREATE INDEX idx_invoice_header_vendor_name ON invoice_header(vendor_name);
CREATE INDEX idx_invoice_header_po_number ON invoice_header(po_number);

-- Composite indexes for common query patterns
CREATE INDEX idx_invoice_header_date_customer ON invoice_header(invoice_date, customer_name);
CREATE INDEX idx_invoice_header_customer_date ON invoice_header(customer_name, invoice_date);

-- Invoice Item Table Indexes
-- Critical for item search operations
CREATE INDEX idx_invoice_item_invoice_header_id ON invoice_item(invoice_header_id);
CREATE INDEX idx_invoice_item_item_code ON invoice_item(item_code);
CREATE INDEX idx_invoice_item_hsn_code ON invoice_item(hsn_code);
CREATE INDEX idx_invoice_item_description ON invoice_item(item_description);
CREATE INDEX idx_invoice_item_unit_price ON invoice_item(unit_price);
CREATE INDEX idx_invoice_item_quantity ON invoice_item(quantity);

-- Composite indexes for item search patterns
CREATE INDEX idx_invoice_item_header_code ON invoice_item(invoice_header_id, item_code);
CREATE INDEX idx_invoice_item_price_range ON invoice_item(unit_price, quantity);

-- Invoice PDF Table Indexes
CREATE INDEX idx_invoice_pdf_header_id ON invoice_pdf(invoice_header_id);

-- Full-text search indexes (if using MySQL 5.7+ or similar)
-- These indexes optimize the LIKE operations in search queries
-- CREATE FULLTEXT INDEX idx_invoice_header_customer_ft ON invoice_header(customer_name);
-- CREATE FULLTEXT INDEX idx_invoice_item_description_ft ON invoice_item(item_description);
-- CREATE FULLTEXT INDEX idx_invoice_item_code_ft ON invoice_item(item_code);

-- Analysis and Optimization
-- Run these commands after creating indexes to update statistics
ANALYZE TABLE invoice_header;
ANALYZE TABLE invoice_item;
ANALYZE TABLE invoice_pdf;
