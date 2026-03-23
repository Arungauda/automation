package com.invoice.automation.service.impl;

import com.invoice.automation.exception.PdfStorageException;
import com.invoice.automation.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of FileService providing file management operations.
 */
@Service
@Slf4j
public class FileServiceImpl implements FileService {

    private static final String UPLOAD_DIR = "uploads";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "pdf", "jpg", "jpeg", "png", "doc", "docx", "xls", "xlsx", "txt"
    );

    @Override
    public Map<String, Object> uploadFile(MultipartFile file) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        
        log.debug("Uploading file: {}", file.getOriginalFilename());
        
        validateFile(file);
        
        try {
            String fileName = generateUniqueFileName(file.getOriginalFilename());
            Path uploadPath = createUploadDirectoryIfNotExists();
            Path filePath = uploadPath.resolve(fileName);
            
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Process the uploaded file
            Map<String, Object> processResult = processUploadedFile(filePath.toString());
            
            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("fileName", fileName);
            fileInfo.put("originalFileName", file.getOriginalFilename());
            fileInfo.put("size", file.getSize());
            fileInfo.put("contentType", determineContentType(fileName));
            fileInfo.put("uploadDate", LocalDate.now());
            fileInfo.put("message", "File uploaded successfully");
            fileInfo.put("processingResult", processResult);
            
            log.info("Successfully uploaded file: {} (size: {} bytes)", fileName, file.getSize());
            return fileInfo;
            
        } catch (IOException e) {
            log.error("Error uploading file: {}", e.getMessage(), e);
            throw new PdfStorageException("Failed to upload file: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> uploadMultipleFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("Files array cannot be null or empty");
        }
        
        log.debug("Uploading {} files", files.length);
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                Map<String, Object> result = uploadFile(file);
                results.add(result);
            } catch (Exception e) {
                log.error("Failed to upload file {}: {}", file.getOriginalFilename(), e.getMessage());
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("fileName", file.getOriginalFilename());
                errorResult.put("error", e.getMessage());
                errorResult.put("success", false);
                results.add(errorResult);
            }
        }
        
        log.info("Uploaded {} out of {} files successfully", 
                results.stream().mapToLong(r -> (Boolean) r.getOrDefault("success", true) ? 1 : 0).sum(),
                files.length);
        
        return results;
    }

    @Override
    @Cacheable(value = "fileCache", key = "#fileName")
    public Resource downloadFile(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        
        log.debug("Downloading file: {}", fileName);
        
        try {
            Path uploadPath = createUploadDirectoryIfNotExists();
            Path filePath = uploadPath.resolve(fileName).normalize();
            
            if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
                throw new PdfStorageException("File not found: " + fileName);
            }
            
            Resource resource = new UrlResource(filePath.toUri());
            log.debug("Successfully prepared file for download: {}", fileName);
            return resource;
            
        } catch (IOException e) {
            log.error("Error downloading file {}: {}", fileName, e.getMessage(), e);
            throw new PdfStorageException("Failed to download file: " + e.getMessage());
        }
    }

    @Override
    @Cacheable(value = "fileCache", key = "'view_' + #fileName")
    public Resource viewFile(String fileName) {
        log.debug("Preparing file for inline viewing: {}", fileName);
        return downloadFile(fileName); // Same implementation, different use case
    }

    @Override
    @CacheEvict(value = "fileCache", key = "#fileName")
    public boolean deleteFile(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        
        log.debug("Deleting file: {}", fileName);
        
        try {
            Path uploadPath = createUploadDirectoryIfNotExists();
            Path filePath = uploadPath.resolve(fileName).normalize();
            
            boolean deleted = Files.deleteIfExists(filePath);
            
            if (deleted) {
                log.info("Successfully deleted file: {}", fileName);
            } else {
                log.warn("File not found for deletion: {}", fileName);
            }
            
            return deleted;
            
        } catch (IOException e) {
            log.error("Error deleting file {}: {}", fileName, e.getMessage(), e);
            throw new PdfStorageException("Failed to delete file: " + e.getMessage());
        }
    }

    @Override
    @Cacheable(value = "fileCache", key = "'fileList'")
    public List<Map<String, Object>> listFiles() {
        log.debug("Listing all files in upload directory");
        
        try {
            Path uploadPath = createUploadDirectoryIfNotExists();
            
            List<Map<String, Object>> files;
            try (Stream<Path> paths = Files.list(uploadPath)) {
                files = paths
                        .filter(Files::isRegularFile)
                        .map(this::pathToFileInfo)
                        .filter(Objects::nonNull)
                        .sorted((a, b) -> ((String) a.get("fileName")).compareTo((String) b.get("fileName")))
                        .collect(Collectors.toList());
            }
            
            log.debug("Found {} files in upload directory", files.size());
            return files;
            
        } catch (IOException e) {
            log.error("Error listing files: {}", e.getMessage(), e);
            throw new PdfStorageException("Failed to list files: " + e.getMessage());
        }
    }

    @Override
    @Cacheable(value = "fileCache", key = "'info_' + #fileName")
    public Map<String, Object> getFileInfo(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        
        log.debug("Getting file info for: {}", fileName);
        
        try {
            Path uploadPath = createUploadDirectoryIfNotExists();
            Path filePath = uploadPath.resolve(fileName).normalize();
            
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                throw new PdfStorageException("File not found: " + fileName);
            }
            
            Map<String, Object> fileInfo = pathToFileInfo(filePath);
            
            // Add additional metadata
            Map<String, Object> metadata = extractFileMetadata(fileName);
            fileInfo.put("metadata", metadata);
            
            log.debug("Retrieved file info for: {}", fileName);
            return fileInfo;
            
        } catch (IOException e) {
            log.error("Error getting file info for {}: {}", fileName, e.getMessage(), e);
            throw new PdfStorageException("Failed to get file info: " + e.getMessage());
        }
    }

    @Override
    public boolean fileExists(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }
        
        try {
            Path uploadPath = createUploadDirectoryIfNotExists();
            Path filePath = uploadPath.resolve(fileName).normalize();
            return Files.exists(filePath) && Files.isRegularFile(filePath);
        } catch (IOException e) {
            log.error("Error checking if file exists {}: {}", fileName, e.getMessage());
            return false;
        }
    }

    @Override
    public void validateFile(MultipartFile file) {
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
        
        log.debug("File validation passed for: {}", originalFilename);
    }

    @Override
    public String generateUniqueFileName(String originalFilename) {
        if (originalFilename == null) {
            throw new IllegalArgumentException("Original filename cannot be null");
        }
        
        String extension = getFileExtension(originalFilename);
        String nameWithoutExtension = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
        String uniqueFileName = nameWithoutExtension + "_" + UUID.randomUUID() + "." + extension;
        
        log.debug("Generated unique filename: {} from original: {}", uniqueFileName, originalFilename);
        return uniqueFileName;
    }

    @Override
    public String determineContentType(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }
        
        String extension = getFileExtension(filename).toLowerCase();
        String contentType = switch (extension) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "txt" -> "text/plain";
            default -> "application/octet-stream";
        };
        
        log.debug("Determined content type {} for file: {}", contentType, filename);
        return contentType;
    }

    @Override
    public String getUploadDirectory() {
        return UPLOAD_DIR;
    }

    @Override
    public String createUploadDirectoryIfNotExists() {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadPath.toAbsolutePath());
            }
            return uploadPath.toAbsolutePath().toString();
        } catch (IOException e) {
            log.error("Error creating upload directory: {}", e.getMessage(), e);
            throw new PdfStorageException("Failed to create upload directory: " + e.getMessage());
        }
    }

    @Override
    public int cleanupOldFiles(int daysOld) {
        if (daysOld < 0) {
            throw new IllegalArgumentException("Days old must be non-negative");
        }
        
        log.info("Starting cleanup of files older than {} days", daysOld);
        
        LocalDate cutoffDate = LocalDate.now().minusDays(daysOld);
        int deletedCount = 0;
        
        try {
            Path uploadPath = createUploadDirectoryIfNotExists();
            
            try (Stream<Path> paths = Files.list(uploadPath)) {
                List<Path> filesToDelete = paths
                        .filter(Files::isRegularFile)
                        .filter(path -> isFileOlderThan(path, cutoffDate))
                        .collect(Collectors.toList());
                
                for (Path file : filesToDelete) {
                    try {
                        Files.delete(file);
                        deletedCount++;
                        log.debug("Deleted old file: {}", file.getFileName());
                    } catch (IOException e) {
                        log.error("Failed to delete old file {}: {}", file.getFileName(), e.getMessage());
                    }
                }
            }
            
            log.info("Cleanup completed. Deleted {} files older than {} days", deletedCount, daysOld);
            return deletedCount;
            
        } catch (IOException e) {
            log.error("Error during cleanup: {}", e.getMessage(), e);
            throw new PdfStorageException("Failed to cleanup old files: " + e.getMessage());
        }
    }

    @Override
    @Cacheable(value = "fileCache", key = "'storageStats'")
    public Map<String, Object> getStorageStatistics() {
        log.debug("Calculating storage statistics");
        
        try {
            Path uploadPath = createUploadDirectoryIfNotExists();
            
            long totalFiles = 0;
            long totalSize = 0;
            Map<String, Long> typeDistribution = new HashMap<>();
            
            try (Stream<Path> paths = Files.list(uploadPath)) {
                List<Path> files = paths.filter(Files::isRegularFile).collect(Collectors.toList());
                
                totalFiles = files.size();
                
                for (Path file : files) {
                    try {
                        long size = Files.size(file);
                        totalSize += size;
                        
                        String contentType = determineContentType(file.getFileName().toString());
                        typeDistribution.merge(contentType, size, Long::sum);
                        
                    } catch (IOException e) {
                        log.warn("Error getting size for file {}: {}", file.getFileName(), e.getMessage());
                    }
                }
            }
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalFiles", totalFiles);
            statistics.put("totalSize", totalSize);
            statistics.put("totalSizeMB", totalSize / (1024.0 * 1024.0));
            statistics.put("averageFileSize", totalFiles > 0 ? (double) totalSize / totalFiles : 0.0);
            statistics.put("typeDistribution", typeDistribution);
            statistics.put("uploadDirectory", uploadPath.toAbsolutePath().toString());
            statistics.put("calculatedAt", Instant.now());
            
            log.debug("Storage statistics calculated: {} files, {} bytes total", totalFiles, totalSize);
            return statistics;
            
        } catch (IOException e) {
            log.error("Error calculating storage statistics: {}", e.getMessage(), e);
            throw new PdfStorageException("Failed to calculate storage statistics: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> processUploadedFile(String filePath) {
        log.debug("Processing uploaded file: {}", filePath);
        
        Map<String, Object> processResult = new HashMap<>();
        processResult.put("processed", true);
        processResult.put("processedAt", Instant.now());
        processResult.put("filePath", filePath);
        
        // In a real implementation, you might:
        // - Scan for viruses
        // - Generate thumbnails for images
        // - Extract text content for indexing
        // - Validate file integrity
        
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                String contentType = determineContentType(path.getFileName().toString());
                processResult.put("contentType", contentType);
                
                if (contentType.startsWith("image/")) {
                    // Generate thumbnail for images
                    // String thumbnailPath = generateThumbnail(path.getFileName().toString());
                    // processResult.put("thumbnailPath", thumbnailPath);
                    processResult.put("thumbnailGenerated", false); // Placeholder
                }
            }
            
            log.debug("File processing completed for: {}", filePath);
            
        } catch (Exception e) {
            log.error("Error processing file {}: {}", filePath, e.getMessage());
            processResult.put("processed", false);
            processResult.put("error", e.getMessage());
        }
        
        return processResult;
    }

    @Override
    public String generateThumbnail(String fileName) {
        log.debug("Generating thumbnail for: {}", fileName);
        
        // Placeholder implementation
        // In a real scenario, you would use an image processing library
        // like ImageIO or Thumbnailator to generate thumbnails
        
        String thumbnailPath = "thumbnails/thumb_" + fileName;
        log.debug("Thumbnail generated at: {}", thumbnailPath);
        return thumbnailPath;
    }

    @Override
    public Map<String, Object> extractFileMetadata(String fileName) {
        log.debug("Extracting metadata for: {}", fileName);
        
        Map<String, Object> metadata = new HashMap<>();
        
        try {
            Path filePath = Paths.get(getUploadDirectory()).resolve(fileName);
            if (Files.exists(filePath)) {
                BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
                
                metadata.put("creationTime", attrs.creationTime().toInstant());
                metadata.put("lastModifiedTime", attrs.lastModifiedTime().toInstant());
                metadata.put("lastAccessTime", attrs.lastAccessTime().toInstant());
                metadata.put("size", attrs.size());
                metadata.put("isRegularFile", attrs.isRegularFile());
                metadata.put("extension", getFileExtension(fileName));
                metadata.put("contentType", determineContentType(fileName));
            }
            
        } catch (IOException e) {
            log.error("Error extracting metadata for {}: {}", fileName, e.getMessage());
            metadata.put("error", e.getMessage());
        }
        
        return metadata;
    }

    @Override
    public List<Map<String, Object>> searchFiles(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return listFiles();
        }
        
        log.debug("Searching files with pattern: {}", pattern);
        
        try {
            Path uploadPath = createUploadDirectoryIfNotExists();
            
            try (Stream<Path> paths = Files.list(uploadPath)) {
                return paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase().contains(pattern.toLowerCase()))
                        .map(this::pathToFileInfo)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
            
        } catch (IOException e) {
            log.error("Error searching files: {}", e.getMessage(), e);
            throw new PdfStorageException("Failed to search files: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> filterFilesByType(String fileType) {
        if (fileType == null || fileType.trim().isEmpty()) {
            return listFiles();
        }
        
        log.debug("Filtering files by type: {}", fileType);
        
        return listFiles().stream()
                .filter(fileInfo -> {
                    String contentType = (String) fileInfo.get("contentType");
                    return contentType != null && contentType.contains(fileType.toLowerCase());
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getFilesByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        
        log.debug("Getting files uploaded between {} and {}", startDate, endDate);
        
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        
        return listFiles().stream()
                .filter(fileInfo -> {
                    Map<String, Object> metadata = (Map<String, Object>) fileInfo.get("metadata");
                    if (metadata != null && metadata.containsKey("creationTime")) {
                        Instant creationTime = (Instant) metadata.get("creationTime");
                        return !creationTime.isBefore(startInstant) && creationTime.isBefore(endInstant);
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> createBackup(String backupPath) {
        log.info("Creating backup to: {}", backupPath);
        
        Map<String, Object> backupResult = new HashMap<>();
        backupResult.put("backupPath", backupPath);
        backupResult.put("startedAt", Instant.now());
        
        try {
            Path sourcePath = Paths.get(getUploadDirectory());
            Path targetPath = Paths.get(backupPath);
            
            if (!Files.exists(targetPath)) {
                Files.createDirectories(targetPath);
            }
            
            int backedUpFiles = 0;
            long totalSize = 0;
            
            try (Stream<Path> paths = Files.list(sourcePath)) {
                List<Path> files = paths.filter(Files::isRegularFile).collect(Collectors.toList());
                
                for (Path file : files) {
                    Path targetFile = targetPath.resolve(file.getFileName());
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    backedUpFiles++;
                    totalSize += Files.size(file);
                }
            }
            
            backupResult.put("success", true);
            backupResult.put("filesBackedUp", backedUpFiles);
            backupResult.put("totalSize", totalSize);
            backupResult.put("completedAt", Instant.now());
            
            log.info("Backup completed: {} files, {} bytes", backedUpFiles, totalSize);
            
        } catch (IOException e) {
            log.error("Error creating backup: {}", e.getMessage(), e);
            backupResult.put("success", false);
            backupResult.put("error", e.getMessage());
        }
        
        return backupResult;
    }

    @Override
    public Map<String, Object> restoreFromBackup(String backupPath) {
        log.info("Restoring from backup: {}", backupPath);
        
        Map<String, Object> restoreResult = new HashMap<>();
        restoreResult.put("backupPath", backupPath);
        restoreResult.put("startedAt", Instant.now());
        
        try {
            Path backupDir = Paths.get(backupPath);
            Path uploadDir = Paths.get(getUploadDirectory());
            
            if (!Files.exists(backupDir)) {
                throw new PdfStorageException("Backup directory not found: " + backupPath);
            }
            
            createUploadDirectoryIfNotExists();
            
            int restoredFiles = 0;
            long totalSize = 0;
            
            try (Stream<Path> paths = Files.list(backupDir)) {
                List<Path> files = paths.filter(Files::isRegularFile).collect(Collectors.toList());
                
                for (Path file : files) {
                    Path targetFile = uploadDir.resolve(file.getFileName());
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    restoredFiles++;
                    totalSize += Files.size(file);
                }
            }
            
            restoreResult.put("success", true);
            restoreResult.put("filesRestored", restoredFiles);
            restoreResult.put("totalSize", totalSize);
            restoreResult.put("completedAt", Instant.now());
            
            log.info("Restore completed: {} files, {} bytes", restoredFiles, totalSize);
            
        } catch (IOException e) {
            log.error("Error restoring from backup: {}", e.getMessage(), e);
            restoreResult.put("success", false);
            restoreResult.put("error", e.getMessage());
        }
        
        return restoreResult;
    }

    // ========== Helper Methods ==========

    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    private Map<String, Object> pathToFileInfo(Path path) {
        try {
            Map<String, Object> fileInfo = new HashMap<>();
            String fileName = path.getFileName().toString();
            
            fileInfo.put("fileName", fileName);
            fileInfo.put("size", Files.size(path));
            fileInfo.put("lastModified", Files.getLastModifiedTime(path).toString());
            fileInfo.put("contentType", determineContentType(fileName));
            
            return fileInfo;
            
        } catch (IOException e) {
            log.warn("Error getting file info for {}: {}", path, e.getMessage());
            return null;
        }
    }

    private boolean isFileOlderThan(Path filePath, LocalDate cutoffDate) {
        try {
            Instant fileTime = Files.getLastModifiedTime(filePath).toInstant();
            LocalDate fileDate = fileTime.atZone(ZoneId.systemDefault()).toLocalDate();
            return fileDate.isBefore(cutoffDate);
        } catch (IOException e) {
            log.warn("Error checking file age for {}: {}", filePath, e.getMessage());
            return false;
        }
    }
}
