# Invoice Automation API Documentation Guide

## Overview

The Invoice Automation API provides comprehensive REST endpoints for managing invoices, invoice items, file operations, and generating reports. This API is fully documented using OpenAPI 3.0 specification and accessible through Swagger UI.

## Accessing API Documentation

### Swagger UI
- **Local Development**: `http://localhost:8080/swagger-ui.html`
- **Alternative UI**: `http://localhost:8080/swagger-ui/index.html`

### OpenAPI Specification
- **JSON Format**: `http://localhost:8080/v3/api-docs`
- **YAML Format**: `http://localhost:8080/v3/api-docs.yaml`

## API Base URL
- **Local**: `http://localhost:8080/api/v1`
- **Production**: `https://api.invoiceautomation.com/v1`

## API Categories

### 1. Invoice Management
**Base Path**: `/api/v1/invoices`

#### Core Operations
- `POST /` - Create new invoice
- `GET /` - Get all invoices
- `GET /{id}` - Get invoice by ID
- `PUT /{id}` - Update invoice
- `DELETE /{id}` - Delete invoice

#### Search & Filter
- `GET /search` - Search invoices by criteria
- `GET /customer/{customerName}` - Get invoices by customer
- `GET /date-range` - Get invoices by date range

#### PDF Management
- `POST /{id}/pdf` - Upload PDF for invoice
- `GET /{id}/pdf` - Download invoice PDF
- `DELETE /{id}/pdf` - Delete invoice PDF

#### Bulk Operations
- `POST /bulk` - Create multiple invoices
- `DELETE /bulk` - Delete multiple invoices

### 2. Invoice Item Management
**Base Path**: `/api/v1/invoice-items`

#### Core Operations
- `POST /` - Create invoice item
- `GET /` - Get all invoice items
- `GET /{id}` - Get invoice item by ID
- `PUT /{id}` - Update invoice item
- `DELETE /{id}` - Delete invoice item

#### Invoice-specific Operations
- `GET /invoice/{invoiceId}` - Get items by invoice ID
- `POST /invoice/{invoiceId}` - Add item to invoice
- `DELETE /invoice/{invoiceId}/item/{itemId}` - Remove item from invoice

#### Search & Filter
- `GET /search` - Search items by description
- `GET /price-range` - Get items by price range
- `GET /code/{itemCode}` - Get items by item code

#### Bulk Operations
- `POST /invoice/{invoiceId}/bulk` - Create multiple items for invoice

### 3. File Management
**Base Path**: `/api/v1/files`

#### Upload Operations
- `POST /upload` - Upload single file
- `POST /upload/bulk` - Upload multiple files

#### Download Operations
- `GET /download/{fileName}` - Download file
- `GET /view/{fileName}` - View file inline

#### Management Operations
- `DELETE /{fileName}` - Delete file
- `GET /` - List all files
- `GET /{fileName}/info` - Get file information

### 4. Reports and Analytics
**Base Path**: `/api/v1/reports`

#### Summary Reports
- `GET /dashboard` - Get dashboard summary
- `GET /monthly/{year}/{month}` - Get monthly report
- `GET /yearly/{year}` - Get yearly report

#### Customer Reports
- `GET /customers/performance` - Get customer performance report
- `GET /customers/trends` - Analyze customer trends

#### Financial Reports
- `GET /revenue` - Generate revenue report
- `GET /financial-summary` - Get financial summary

#### Export Operations
- `GET /export/{reportType}` - Export report to CSV

### 5. General Operations
**Base Path**: `/`

#### System Operations
- `GET /` - Application welcome message
- `GET /health` - Health check endpoint

## Request/Response Formats

### Common Response Structure
```json
{
  "success": true,
  "data": {},
  "message": "Operation completed successfully",
  "timestamp": "2024-03-24T01:22:00Z"
}
```

### Error Response Structure
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input data",
    "details": []
  },
  "timestamp": "2024-03-24T01:22:00Z"
}
```

## Data Models

### Invoice
```json
{
  "id": "uuid",
  "invoiceNumber": "string",
  "invoiceDate": "date",
  "customerName": "string",
  "poNumber": "string",
  "companyCode": "string",
  "vendorName": "string",
  "address": "string",
  "items": [],
  "totalAmount": "decimal",
  "hasPdf": "boolean"
}
```

### Invoice Item
```json
{
  "id": "uuid",
  "itemDescription": "string",
  "quantity": "decimal",
  "unitPrice": "decimal",
  "totalAmount": "decimal",
  "itemCode": "string",
  "hsnCode": "string"
}
```

### File Info
```json
{
  "fileName": "string",
  "size": "long",
  "lastModified": "string",
  "contentType": "string"
}
```

## Authentication

Currently, the API uses basic request logging. Future versions will implement:
- JWT Bearer Token Authentication
- API Key Authentication
- OAuth 2.0

## Rate Limiting

- **Default**: 100 requests per minute per IP
- **File Upload**: 10 requests per minute per IP
- **Reports**: 20 requests per minute per IP

## File Upload Specifications

### Supported File Types
- PDF: `.pdf`
- Images: `.jpg`, `.jpeg`, `.png`
- Documents: `.doc`, `.docx`
- Spreadsheets: `.xls`, `.xlsx`
- Text: `.txt`

### File Size Limits
- **Maximum**: 10MB per file
- **Bulk Upload**: Maximum 50 files per request

## Pagination

List endpoints support pagination via query parameters:
- `page`: Page number (default: 0)
- `size`: Page size (default: 20, maximum: 100)
- `sort`: Sort field and direction (e.g., `createdAt,desc`)

## Caching

The API implements caching for performance:
- **File Operations**: 15 minutes cache
- **Invoice Data**: 30 minutes cache
- **Reports**: 1 hour cache
- **Search Results**: 10 minutes cache

## Error Handling

### HTTP Status Codes
- `200` - Success
- `201` - Created
- `204` - No Content
- `400` - Bad Request
- `401` - Unauthorized
- `403` - Forbidden
- `404` - Not Found
- `409` - Conflict
- `415` - Unsupported Media Type
- `422` - Unprocessable Entity
- `500` - Internal Server Error

### Common Error Codes
- `VALIDATION_ERROR` - Input validation failed
- `RESOURCE_NOT_FOUND` - Requested resource not found
- `DUPLICATE_RESOURCE` - Resource already exists
- `FILE_TOO_LARGE` - File exceeds size limit
- `UNSUPPORTED_FILE_TYPE` - File type not supported

## Testing with Swagger UI

1. Navigate to `http://localhost:8080/swagger-ui.html`
2. Expand any endpoint category
3. Click on an endpoint to view details
4. Use "Try it out" to test the endpoint
5. Fill in required parameters
6. Click "Execute" to send the request
7. View the response in the interface

## SDK and Client Libraries

Coming soon:
- Java SDK
- JavaScript/TypeScript SDK
- Python SDK
- Postman Collection

## Support

For API support and questions:
- **Email**: support@invoiceautomation.com
- **Documentation**: https://docs.invoiceautomation.com
- **GitHub Issues**: https://github.com/invoiceautomation/api/issues

## Changelog

### v1.0.0 (Current)
- Initial API release
- Basic CRUD operations for invoices and items
- File management capabilities
- Comprehensive reporting and analytics
- Full Swagger/OpenAPI documentation

---

*This documentation is automatically updated with each API release. Last updated: March 24, 2024*
