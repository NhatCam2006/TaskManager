package com.example.taskmanagerv3.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Model class for chat file attachments
 */
public class ChatFile implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int fileId;
    private int messageId;
    private String fileName;
    private String originalFileName;
    private String filePath;
    private String fileType;
    private long fileSize;
    private LocalDateTime uploadedAt;
    private int uploadedBy;
    
    // Constructors
    public ChatFile() {
        this.uploadedAt = LocalDateTime.now();
    }
    
    public ChatFile(String fileName, String filePath, String fileType, long fileSize) {
        this();
        this.fileName = fileName;
        this.originalFileName = fileName;
        this.filePath = filePath;
        this.fileType = fileType;
        this.fileSize = fileSize;
    }
    
    // Getters and Setters
    public int getFileId() {
        return fileId;
    }
    
    public void setFileId(int fileId) {
        this.fileId = fileId;
    }
    
    public int getMessageId() {
        return messageId;
    }
    
    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getOriginalFileName() {
        return originalFileName;
    }
    
    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
    
    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
    
    public int getUploadedBy() {
        return uploadedBy;
    }
    
    public void setUploadedBy(int uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
    
    // Utility methods
    public boolean isImage() {
        return fileType != null && fileType.toLowerCase().startsWith("image/");
    }
    
    public boolean isDocument() {
        return fileType != null && (
            fileType.toLowerCase().contains("pdf") ||
            fileType.toLowerCase().contains("doc") ||
            fileType.toLowerCase().contains("txt") ||
            fileType.toLowerCase().contains("excel") ||
            fileType.toLowerCase().contains("powerpoint")
        );
    }
    
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }
    
    public String getFileExtension() {
        if (originalFileName != null && originalFileName.contains(".")) {
            return originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toLowerCase();
        }
        return "";
    }
    
    @Override
    public String toString() {
        return String.format("ChatFile{fileName='%s', fileType='%s', size=%s}", 
                           originalFileName, fileType, getFormattedFileSize());
    }
}
