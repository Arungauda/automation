package com.invoice.automation.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Service interface for file management operations.
 * Provides methods for file upload, download, and management.
 */
public interface FileService {

    // ========== File Upload Operations ==========

    /**
     * Uploads a single file to the server.
     *
     * @param file the file to upload
     * @return file information including name and size
     */
    Map<String, Object> uploadFile(MultipartFile file);

    /**
     * Uploads multiple files to the server.
     *
     * @param files array of files to upload
     * @return list of file information for each uploaded file
     */
    List<Map<String, Object>> uploadMultipleFiles(MultipartFile[] files);

    // ========== File Download Operations ==========

    /**
     * Downloads a file by its filename.
     *
     * @param fileName the filename to download
     * @return the file resource
     */
    Resource downloadFile(String fileName);

    /**
     * Gets a file resource for inline viewing.
     *
     * @param fileName the filename to view
     * @return the file resource
     */
    Resource viewFile(String fileName);

    // ========== File Management Operations ==========

    /**
     * Deletes a file by its filename.
     *
     * @param fileName the filename to delete
     * @return true if file was deleted, false if file didn't exist
     */
    boolean deleteFile(String fileName);

    /**
     * Lists all uploaded files.
     *
     * @return list of file information
     */
    List<Map<String, Object>> listFiles();

    /**
     * Gets detailed information about a specific file.
     *
     * @param fileName the filename
     * @return file information
     */
    Map<String, Object> getFileInfo(String fileName);

    /**
     * Checks if a file exists.
     *
     * @param fileName the filename to check
     * @return true if file exists, false otherwise
     */
    boolean fileExists(String fileName);

    // ========== File Validation and Security ==========

    /**
     * Validates a file for upload.
     *
     * @param file the file to validate
     * @throws IllegalArgumentException if file is invalid
     */
    void validateFile(MultipartFile file);

    /**
     * Generates a unique filename to prevent conflicts.
     *
     * @param originalFilename the original filename
     * @return a unique filename
     */
    String generateUniqueFileName(String originalFilename);

    /**
     * Determines the content type of a file based on its extension.
     *
     * @param filename the filename
     * @return the content type
     */
    String determineContentType(String filename);

    // ========== File Storage Management ==========

    /**
     * Gets the upload directory path.
     *
     * @return the upload directory path
     */
    String getUploadDirectory();

    /**
     * Creates the upload directory if it doesn't exist.
     *
     * @return the path to the upload directory
     */
    String createUploadDirectoryIfNotExists();

    /**
     * Cleans up old files based on age criteria.
     *
     * @param daysOld files older than this many days will be deleted
     * @return number of files deleted
     */
    int cleanupOldFiles(int daysOld);

    /**
     * Gets storage usage statistics.
     *
     * @return storage statistics including total size and file count
     */
    Map<String, Object> getStorageStatistics();

    // ========== File Processing ==========

    /**
     * Processes an uploaded file (e.g., virus scan, thumbnail generation).
     *
     * @param filePath the path to the uploaded file
     * @return processing result
     */
    Map<String, Object> processUploadedFile(String filePath);

    /**
     * Generates a thumbnail for an image file.
     *
     * @param fileName the image filename
     * @return path to the generated thumbnail
     */
    String generateThumbnail(String fileName);

    /**
     * Extracts metadata from a file.
     *
     * @param fileName the filename
     * @return file metadata
     */
    Map<String, Object> extractFileMetadata(String fileName);

    // ========== File Search and Filtering ==========

    /**
     * Searches files by name pattern.
     *
     * @param pattern the search pattern
     * @return list of matching files
     */
    List<Map<String, Object>> searchFiles(String pattern);

    /**
     * Filters files by type.
     *
     * @param fileType the file type to filter by
     * @return list of files of the specified type
     */
    List<Map<String, Object>> filterFilesByType(String fileType);

    /**
     * Gets files uploaded within a date range.
     *
     * @param startDate start date
     * @param endDate   end date
     * @return list of files uploaded in the date range
     */
    List<Map<String, Object>> getFilesByDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate);

    // ========== File Backup and Recovery ==========

    /**
     * Creates a backup of uploaded files.
     *
     * @param backupPath the path where backup will be stored
     * @return backup operation result
     */
    Map<String, Object> createBackup(String backupPath);

    /**
     * Restores files from a backup.
     *
     * @param backupPath the path to the backup
     * @return restore operation result
     */
    Map<String, Object> restoreFromBackup(String backupPath);
}
