package com.invoice.automation.controller;

import com.invoice.automation.dto.PdfResponse;
import com.invoice.automation.exception.PdfStorageException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for file management operations.
 * Provides endpoints for uploading, downloading, and managing files.
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "File Management", description = "APIs for managing file operations")
public class FileController extends BaseController {

    private static final String UPLOAD_DIR = "uploads";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "jpg", "jpeg", "png", "doc", "docx");

    // ========== File Upload Operations ==========

    @Operation(summary = "Upload file", description = "Uploads a file to the server")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file or file too large"),
            @ApiResponse(responseCode = "415", description = "Unsupported file type")
    })
    @PostMapping("/upload")
    public ResponseEntity<PdfResponse> uploadFile(
            @Parameter(description = "File to upload") @RequestParam("file") @NotNull MultipartFile file,
            WebRequest request) {
        
        String operation = "UPLOAD_FILE";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        try {
            validateFile(file);
            
            String fileName = generateUniqueFileName(file.getOriginalFilename());
            Path uploadPath = createUploadDirectoryIfNotExists();
            Path filePath = uploadPath.resolve(fileName);
            
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            PdfResponse response = new PdfResponse(fileName, file.getSize(), "File uploaded successfully");
            
            logOperationEnd(operation, user);
            return success(response);
            
        } catch (IOException e) {
            log.error("Error uploading file: {}", e.getMessage(), e);
            throw new PdfStorageException("Failed to upload file: " + e.getMessage());
        }
    }

    @Operation(summary = "Upload multiple files", description = "Uploads multiple files to the server")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Files uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid files or files too large")
    })
    @PostMapping("/upload/bulk")
    public ResponseEntity<List<PdfResponse>> uploadMultipleFiles(
            @Parameter(description = "Files to upload") @RequestParam("files") @NotNull MultipartFile[] files,
            WebRequest request) {
        
        String operation = "UPLOAD_MULTIPLE_FILES";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        try {
            List<PdfResponse> responses = Arrays.stream(files)
                    .map(file -> {
                        try {
                            validateFile(file);
                            String fileName = generateUniqueFileName(file.getOriginalFilename());
                            Path uploadPath = createUploadDirectoryIfNotExists();
                            Path filePath = uploadPath.resolve(fileName);
                            
                            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                            
                            return new PdfResponse(fileName, file.getSize(), "File uploaded successfully");
                        } catch (IOException e) {
                            log.error("Error uploading file {}: {}", file.getOriginalFilename(), e.getMessage());
                            throw new PdfStorageException("Failed to upload file: " + file.getOriginalFilename());
                        }
                    })
                    .collect(Collectors.toList());
            
            logOperationEnd(operation, user);
            return success(responses);
            
        } catch (Exception e) {
            log.error("Error uploading multiple files: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ========== File Download Operations ==========

    @Operation(summary = "Download file", description = "Downloads a file by its filename")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    @GetMapping("/download/{fileName:.+}")
    @Cacheable(value = "fileCache", key = "#fileName")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "Filename") @PathVariable String fileName,
            WebRequest request) {
        
        String operation = "DOWNLOAD_FILE";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        try {
            Path uploadPath = createUploadDirectoryIfNotExists();
            Path filePath = uploadPath.resolve(fileName).normalize();
            
            if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new UrlResource(filePath.toUri());
            String contentType = determineContentType(fileName);
            
            logOperationEnd(operation, user);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
                    
        } catch (IOException e) {
            log.error("Error downloading file: {}", e.getMessage(), e);
            throw new PdfStorageException("Failed to download file: " + e.getMessage());
        }
    }

    @Operation(summary = "View file", description = "Views a file inline (for supported formats)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    @GetMapping("/view/{fileName:.+}")
    @Cacheable(value = "fileCache", key = "'view_' + #fileName")
    public ResponseEntity<Resource> viewFile(
            @Parameter(description = "Filename") @PathVariable String fileName,
            WebRequest request) {
        
        String operation = "VIEW_FILE";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        try {
            Path uploadPath = createUploadDirectoryIfNotExists();
            Path filePath = uploadPath.resolve(fileName).normalize();
            
            if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new UrlResource(filePath.toUri());
            String contentType = determineContentType(fileName);
            
            logOperationEnd(operation, user);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);
                    
        } catch (IOException e) {
            log.error("Error viewing file: {}", e.getMessage(), e);
            throw new PdfStorageException("Failed to view file: " + e.getMessage());
        }
    }

    // ========== File Management Operations ==========

    @Operation(summary = "Delete file", description = "Deletes a file by its filename")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "File deleted successfully"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    @DeleteMapping("/{fileName:.+}")
    @CacheEvict(value = "fileCache", key = "#fileName")
    public ResponseEntity<Void> deleteFile(
            @Parameter(description = "Filename") @PathVariable String fileName,
            WebRequest request) {
        
        String operation = "DELETE_FILE";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        try {
            Path uploadPath = createUploadDirectoryIfNotExists();
            Path filePath = uploadPath.resolve(fileName).normalize();
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            Files.delete(filePath);
            logOperationEnd(operation, user);
            
            return noContent();
            
        } catch (IOException e) {
            log.error("Error deleting file: {}", e.getMessage(), e);
            throw new PdfStorageException("Failed to delete file: " + e.getMessage());
        }
    }

    @Operation(summary = "List all files", description = "Retrieves a list of all uploaded files")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Files listed successfully")
    })
    @GetMapping
    public ResponseEntity<List<FileInfo>> listFiles(WebRequest request) {
        String operation = "LIST_FILES";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        try {
            Path uploadPath = createUploadDirectoryIfNotExists();
            
            List<FileInfo> files = Files.list(uploadPath)
                    .filter(Files::isRegularFile)
                    .map(path -> {
                        try {
                            return new FileInfo(
                                    path.getFileName().toString(),
                                    Files.size(path),
                                    Files.getLastModifiedTime(path).toString(),
                                    determineContentType(path.getFileName().toString())
                            );
                        } catch (IOException e) {
                            log.warn("Error getting file info for {}: {}", path, e.getMessage());
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
            
            logOperationEnd(operation, user);
            return success(files);
            
        } catch (IOException e) {
            log.error("Error listing files: {}", e.getMessage(), e);
            throw new PdfStorageException("Failed to list files: " + e.getMessage());
        }
    }

    @Operation(summary = "Get file info", description = "Retrieves information about a specific file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File info retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    @GetMapping("/{fileName:.+}/info")
    @Cacheable(value = "fileCache", key = "'info_' + #fileName")
    public ResponseEntity<FileInfo> getFileInfo(
            @Parameter(description = "Filename") @PathVariable String fileName,
            WebRequest request) {
        
        String operation = "GET_FILE_INFO";
        String user = getCurrentUser(request);
        logOperationStart(operation, user);

        try {
            Path uploadPath = createUploadDirectoryIfNotExists();
            Path filePath = uploadPath.resolve(fileName).normalize();
            
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            FileInfo fileInfo = new FileInfo(
                    fileName,
                    Files.size(filePath),
                    Files.getLastModifiedTime(filePath).toString(),
                    determineContentType(fileName)
            );
            
            logOperationEnd(operation, user);
            return success(fileInfo);
            
        } catch (IOException e) {
            log.error("Error getting file info: {}", e.getMessage(), e);
            throw new PdfStorageException("Failed to get file info: " + e.getMessage());
        }
    }

    // ========== Helper Methods ==========

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 10MB");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("Invalid filename");
        }
        
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("File type not supported. Allowed types: " + ALLOWED_EXTENSIONS);
        }
    }

    private String generateUniqueFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String nameWithoutExtension = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
        return nameWithoutExtension + "_" + UUID.randomUUID() + "." + extension;
    }

    private Path createUploadDirectoryIfNotExists() throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        return uploadPath;
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    private String determineContentType(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }

    // ========== DTO Classes ==========

    public record FileInfo(
            String fileName,
            long size,
            String lastModified,
            String contentType
    ) {}
}
