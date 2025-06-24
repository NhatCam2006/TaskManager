package com.example.taskmanagerv3.service;

import com.example.taskmanagerv3.model.ChatFile;
import com.example.taskmanagerv3.model.ChatMessage;
import com.example.taskmanagerv3.model.User;
import com.example.taskmanagerv3.model.UserRole;
import com.example.taskmanagerv3.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service class for managing chat functionality
 */
public class ChatService {
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    private static final String CHAT_FILES_DIR = "chat_files";

    public ChatService() {
        createChatFilesDirectory();
        initializeChatTables();
    }

    /**
     * Create chat files directory if it doesn't exist
     */
    private void createChatFilesDirectory() {
        try {
            Path chatFilesPath = Paths.get(CHAT_FILES_DIR);
            if (!Files.exists(chatFilesPath)) {
                Files.createDirectories(chatFilesPath);
                logger.info("Created chat files directory: {}", CHAT_FILES_DIR);
            }
        } catch (IOException e) {
            logger.error("Error creating chat files directory", e);
        }
    }

    /**
     * Initialize chat tables in database
     */
    private void initializeChatTables() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Create chat_messages table
            String createMessagesTable = """
                IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='chat_messages' AND xtype='U')
                CREATE TABLE chat_messages (
                    message_id INT IDENTITY(1,1) PRIMARY KEY,
                    sender_id INT NOT NULL,
                    sender_name NVARCHAR(100) NOT NULL,
                    sender_role NVARCHAR(20) NOT NULL,
                    receiver_id INT NOT NULL,
                    receiver_name NVARCHAR(100),
                    message NTEXT NOT NULL,
                    timestamp DATETIME2 DEFAULT GETDATE(),
                    is_read BIT DEFAULT 0,
                    has_attachments BIT DEFAULT 0,
                    FOREIGN KEY (sender_id) REFERENCES users(user_id),
                    FOREIGN KEY (receiver_id) REFERENCES users(user_id)
                )
            """;

            // Create chat_files table
            String createFilesTable = """
                IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='chat_files' AND xtype='U')
                CREATE TABLE chat_files (
                    file_id INT IDENTITY(1,1) PRIMARY KEY,
                    message_id INT NOT NULL,
                    file_name NVARCHAR(255) NOT NULL,
                    original_file_name NVARCHAR(255) NOT NULL,
                    file_path NVARCHAR(500) NOT NULL,
                    file_type NVARCHAR(100),
                    file_size BIGINT,
                    uploaded_at DATETIME2 DEFAULT GETDATE(),
                    uploaded_by INT NOT NULL,
                    FOREIGN KEY (message_id) REFERENCES chat_messages(message_id),
                    FOREIGN KEY (uploaded_by) REFERENCES users(user_id)
                )
            """;

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createMessagesTable);
                stmt.execute(createFilesTable);
                logger.info("Chat tables initialized successfully");
            }

        } catch (SQLException e) {
            logger.error("Error initializing chat tables", e);
        }
    }

    /**
     * Save chat message to database
     */
    public ChatMessage saveChatMessage(ChatMessage message) {
        String sql = """
            INSERT INTO chat_messages (sender_id, sender_name, sender_role, receiver_id, receiver_name,
                                     message, timestamp, is_read, has_attachments)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Disable auto-commit for explicit transaction control
            conn.setAutoCommit(false);

            pstmt.setInt(1, message.getSenderId());
            pstmt.setString(2, message.getSenderName());
            pstmt.setString(3, message.getSenderRole().toString());
            pstmt.setInt(4, message.getReceiverId());
            pstmt.setString(5, message.getReceiverName());
            pstmt.setString(6, message.getMessage());
            pstmt.setTimestamp(7, Timestamp.valueOf(message.getTimestamp()));
            pstmt.setBoolean(8, message.isRead());
            pstmt.setBoolean(9, message.isHasAttachments());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        message.setMessageId(generatedKeys.getInt(1));
                    }
                }

                // Explicit commit
                conn.commit();
                logger.debug("Chat message saved and committed: {}", message.getMessageId());
            } else {
                conn.rollback();
                logger.error("No rows affected when saving message");
                return null;
            }

            return message;

        } catch (SQLException e) {
            logger.error("Error saving chat message", e);
            // Try to rollback if connection is still available
            try (Connection conn = DatabaseConfig.getConnection()) {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                logger.error("Error during rollback", rollbackEx);
            }
            return null;
        }
    }

    /**
     * Get chat messages between two users
     */
    public List<ChatMessage> getChatMessages(int userId1, int userId2, int limit) {
        List<ChatMessage> messages = new ArrayList<>();
        logger.info("Loading chat messages between user {} and user {}, limit: {}", userId1, userId2, limit);

        // Load recent messages and then reverse for chronological display
        String sql = """
            SELECT * FROM (
                SELECT TOP (?) message_id, sender_id, sender_name, sender_role, receiver_id, receiver_name,
                       message, timestamp, is_read, has_attachments
                FROM chat_messages
                WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)
                ORDER BY message_id DESC
            ) AS recent_messages
            ORDER BY message_id ASC
        """;

        // Use manual connection management to avoid premature closure
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConfig.getConnection();
            logger.debug("Database connection established for loading messages");

            pstmt = conn.prepareStatement(sql);
            logger.debug("Preparing SQL query with parameters: limit={}, userId1={}, userId2={}", limit, userId1, userId2);

            pstmt.setInt(1, limit);
            pstmt.setInt(2, userId1);
            pstmt.setInt(3, userId2);
            pstmt.setInt(4, userId2);
            pstmt.setInt(5, userId1);

            logger.debug("Executing SQL query...");
            rs = pstmt.executeQuery();
            logger.debug("SQL query executed successfully, processing results...");

            // Load all data into memory first to avoid connection issues
            List<Object[]> rawData = new ArrayList<>();
            while (rs.next()) {
                Object[] row = new Object[10];
                row[0] = rs.getInt("message_id");
                row[1] = rs.getInt("sender_id");
                row[2] = rs.getString("sender_name");
                row[3] = rs.getString("sender_role");
                row[4] = rs.getInt("receiver_id");
                row[5] = rs.getString("receiver_name");
                row[6] = rs.getString("message");
                row[7] = rs.getTimestamp("timestamp");
                row[8] = rs.getBoolean("is_read");
                row[9] = rs.getBoolean("has_attachments");
                rawData.add(row);
            }

            logger.info("Loaded {} raw message records from database", rawData.size());

            // Close resources early
            rs.close();
            pstmt.close();
            conn.close();

            // Process data into ChatMessage objects
            for (int i = 0; i < rawData.size(); i++) {
                Object[] row = rawData.get(i);

                ChatMessage message = new ChatMessage();
                message.setMessageId((Integer) row[0]);
                message.setSenderId((Integer) row[1]);
                message.setSenderName((String) row[2]);

                // Handle role conversion safely
                String roleString = (String) row[3];
                UserRole role;
                try {
                    role = UserRole.valueOf(roleString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Fallback for old data
                    role = roleString.equalsIgnoreCase("admin") ? UserRole.ADMIN : UserRole.USER;
                }
                message.setSenderRole(role);

                message.setReceiverId((Integer) row[4]);
                message.setReceiverName((String) row[5]);
                message.setMessage((String) row[6]);
                message.setTimestamp(((Timestamp) row[7]).toLocalDateTime());
                message.setRead((Boolean) row[8]);
                message.setHasAttachments((Boolean) row[9]);

                logger.debug("Processing message {}: ID={}, Sender={}, Text='{}'",
                           i + 1, message.getMessageId(), message.getSenderId(),
                           message.getMessage() != null && message.getMessage().length() > 30 ?
                           message.getMessage().substring(0, 30) + "..." : message.getMessage());

                // Load attachments if any (using fresh connection)
                if (message.isHasAttachments()) {
                    message.setAttachments(getChatFiles(message.getMessageId()));
                }

                messages.add(message);
            }

            logger.info("Successfully processed {} chat messages", messages.size());

        } catch (SQLException e) {
            logger.error("Error getting chat messages between user {} and user {}", userId1, userId2, e);
            return new ArrayList<>(); // Return empty list on error
        } finally {
            // Ensure all resources are closed
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                logger.error("Error closing database resources", e);
            }
        }

        // Messages are already in chronological order (oldest first) due to ORDER BY message_id ASC
        // This is correct for chat UI where oldest messages appear at top and newest at bottom
        logger.info("Returning {} messages in chronological order (oldest first)", messages.size());

        return messages;
    }

    /**
     * Save chat file
     */
    public ChatFile saveChatFile(int messageId, String originalFileName, byte[] fileData, String fileType, int uploadedBy) {
        try {
            // Generate unique file name
            String fileExtension = getFileExtension(originalFileName);
            String fileName = "chat_" + System.currentTimeMillis() + "_" + uploadedBy + "." + fileExtension;
            Path filePath = Paths.get(CHAT_FILES_DIR, fileName);

            // Save file to disk
            Files.write(filePath, fileData);

            // Save file info to database
            String sql = """
                INSERT INTO chat_files (message_id, file_name, original_file_name, file_path,
                                      file_type, file_size, uploaded_by)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                pstmt.setInt(1, messageId);
                pstmt.setString(2, fileName);
                pstmt.setString(3, originalFileName);
                pstmt.setString(4, filePath.toString());
                pstmt.setString(5, fileType);
                pstmt.setLong(6, fileData.length);
                pstmt.setInt(7, uploadedBy);

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            ChatFile chatFile = new ChatFile(fileName, filePath.toString(), fileType, fileData.length);
                            chatFile.setFileId(generatedKeys.getInt(1));
                            chatFile.setMessageId(messageId);
                            chatFile.setOriginalFileName(originalFileName);
                            chatFile.setUploadedBy(uploadedBy);

                            logger.debug("Chat file saved: {}", chatFile.getFileId());
                            return chatFile;
                        }
                    }
                }
            }

        } catch (IOException | SQLException e) {
            logger.error("Error saving chat file", e);
        }

        return null;
    }

    /**
     * Get chat files for a message
     */
    public List<ChatFile> getChatFiles(int messageId) {
        String sql = "SELECT * FROM chat_files WHERE message_id = ?";
        List<ChatFile> files = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, messageId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ChatFile file = new ChatFile();
                    file.setFileId(rs.getInt("file_id"));
                    file.setMessageId(rs.getInt("message_id"));
                    file.setFileName(rs.getString("file_name"));
                    file.setOriginalFileName(rs.getString("original_file_name"));
                    file.setFilePath(rs.getString("file_path"));
                    file.setFileType(rs.getString("file_type"));
                    file.setFileSize(rs.getLong("file_size"));
                    file.setUploadedAt(rs.getTimestamp("uploaded_at").toLocalDateTime());
                    file.setUploadedBy(rs.getInt("uploaded_by"));

                    files.add(file);
                }
            }

        } catch (SQLException e) {
            logger.error("Error getting chat files", e);
        }

        return files;
    }

    /**
     * Update message has_attachments flag
     */
    public void updateMessageAttachments(int messageId, boolean hasAttachments) {
        String sql = "UPDATE chat_messages SET has_attachments = ? WHERE message_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBoolean(1, hasAttachments);
            pstmt.setInt(2, messageId);

            int updatedRows = pstmt.executeUpdate();
            if (updatedRows > 0) {
                logger.debug("Updated message {} has_attachments to {}", messageId, hasAttachments);
            }

        } catch (SQLException e) {
            logger.error("Error updating message attachments flag", e);
        }
    }

    /**
     * Check if a message is duplicate based on sender, receiver, content and timestamp
     * This prevents saving the same message multiple times
     */
    public boolean isDuplicateMessage(int senderId, int receiverId, String message, LocalDateTime timestamp) {
        String sql = """
            SELECT COUNT(*) FROM chat_messages
            WHERE sender_id = ? AND receiver_id = ? AND message = ?
            AND ABS(DATEDIFF(SECOND, timestamp, ?)) < 5
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, senderId);
            pstmt.setInt(2, receiverId);
            pstmt.setString(3, message);
            pstmt.setTimestamp(4, Timestamp.valueOf(timestamp));

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    boolean isDuplicate = count > 0;

                    if (isDuplicate) {
                        logger.warn("Duplicate message detected: Sender={}, Receiver={}, Message='{}', Count={}",
                                   senderId, receiverId,
                                   message.length() > 50 ? message.substring(0, 50) + "..." : message,
                                   count);
                    }

                    return isDuplicate;
                }
            }

        } catch (SQLException e) {
            logger.error("Error checking for duplicate message", e);
            // In case of error, assume not duplicate to avoid blocking legitimate messages
            return false;
        }

        return false;
    }

    /**
     * Get chat statistics for debugging and monitoring
     */
    public ChatStatistics getChatStatistics() {
        ChatStatistics stats = new ChatStatistics();

        try (Connection conn = DatabaseConfig.getConnection()) {
            // Count messages
            String messageCountSql = "SELECT COUNT(*) FROM chat_messages";
            try (PreparedStatement pstmt = conn.prepareStatement(messageCountSql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    stats.totalMessages = rs.getInt(1);
                }
            }

            // Count files
            String fileCountSql = "SELECT COUNT(*) FROM chat_files";
            try (PreparedStatement pstmt = conn.prepareStatement(fileCountSql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    stats.totalFiles = rs.getInt(1);
                }
            }

            // Count messages with attachments
            String attachmentCountSql = "SELECT COUNT(*) FROM chat_messages WHERE has_attachments = 1";
            try (PreparedStatement pstmt = conn.prepareStatement(attachmentCountSql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    stats.messagesWithAttachments = rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            logger.error("Error getting chat statistics", e);
        }

        return stats;
    }

    /**
     * Inner class for chat statistics
     */
    public static class ChatStatistics {
        public int totalMessages = 0;
        public int totalFiles = 0;
        public int messagesWithAttachments = 0;
        public long totalFileSize = 0;
        public LocalDateTime earliestMessage;
        public LocalDateTime latestMessage;

        public String getFormattedFileSize() {
            if (totalFileSize < 1024) {
                return totalFileSize + " B";
            } else if (totalFileSize < 1024 * 1024) {
                return String.format("%.1f KB", totalFileSize / 1024.0);
            } else {
                return String.format("%.1f MB", totalFileSize / (1024.0 * 1024.0));
            }
        }

        @Override
        public String toString() {
            return String.format("Messages: %d, Files: %d, Messages with attachments: %d, Total file size: %s",
                               totalMessages, totalFiles, messagesWithAttachments, getFormattedFileSize());
        }
    }

    /**
     * Fix database - Update has_attachments for all messages that have files
     */
    public void fixDatabaseAttachments() {
        String sql = """
            UPDATE chat_messages
            SET has_attachments = 1
            WHERE message_id IN (
                SELECT DISTINCT message_id FROM chat_files
            ) AND has_attachments = 0
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int updatedRows = pstmt.executeUpdate();
            logger.info("Fixed database: Updated {} messages to has_attachments = true", updatedRows);

        } catch (SQLException e) {
            logger.error("Error fixing database attachments", e);
        }
    }

    /**
     * DELETE ALL CHAT HISTORY - Xóa hết mọi thứ về lịch sử tin nhắn
     * WARNING: This will permanently delete all chat messages and files!
     */
    public boolean deleteAllChatHistory() {
        logger.warn("=== DELETING ALL CHAT HISTORY ===");
        logger.warn("This will permanently delete all chat messages and files!");

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false); // Start transaction

            try {
                // Step 1: Get all file paths before deleting from database
                List<String> filePaths = new ArrayList<>();
                String getFilesSQL = "SELECT file_path FROM chat_files";
                try (PreparedStatement pstmt = conn.prepareStatement(getFilesSQL);
                     ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        filePaths.add(rs.getString("file_path"));
                    }
                }
                logger.info("Found {} files to delete from disk", filePaths.size());

                // Step 2: Delete from chat_files table (must delete first due to foreign key)
                String deleteFilesSQL = "DELETE FROM chat_files";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteFilesSQL)) {
                    int deletedFiles = pstmt.executeUpdate();
                    logger.info("Deleted {} records from chat_files table", deletedFiles);
                }

                // Step 3: Delete from chat_messages table
                String deleteMessagesSQL = "DELETE FROM chat_messages";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteMessagesSQL)) {
                    int deletedMessages = pstmt.executeUpdate();
                    logger.info("Deleted {} records from chat_messages table", deletedMessages);
                }

                // Step 4: Reset identity columns (restart IDs from 1)
                String resetMessagesIdentity = "DBCC CHECKIDENT ('chat_messages', RESEED, 0)";
                String resetFilesIdentity = "DBCC CHECKIDENT ('chat_files', RESEED, 0)";
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(resetMessagesIdentity);
                    stmt.execute(resetFilesIdentity);
                    logger.info("Reset identity columns for both tables");
                }

                // Commit transaction
                conn.commit();
                logger.info("Database cleanup completed successfully");

                // Step 5: Delete physical files from disk
                int deletedFileCount = 0;
                for (String filePath : filePaths) {
                    try {
                        Path path = Paths.get(filePath);
                        if (Files.exists(path)) {
                            Files.delete(path);
                            deletedFileCount++;
                        }
                    } catch (IOException e) {
                        logger.warn("Failed to delete file: {}", filePath, e);
                    }
                }
                logger.info("Deleted {} physical files from disk", deletedFileCount);

                // Step 6: Clean up chat files directory if empty
                try {
                    Path chatFilesDir = Paths.get(CHAT_FILES_DIR);
                    if (Files.exists(chatFilesDir) && Files.isDirectory(chatFilesDir)) {
                        // Delete empty subdirectories and clean up
                        Files.walk(chatFilesDir)
                            .filter(Files::isRegularFile)
                            .forEach(file -> {
                                try {
                                    Files.delete(file);
                                } catch (IOException e) {
                                    logger.warn("Failed to delete remaining file: {}", file, e);
                                }
                            });
                        logger.info("Cleaned up chat files directory");
                    }
                } catch (IOException e) {
                    logger.warn("Failed to clean up chat files directory", e);
                }

                logger.warn("=== ALL CHAT HISTORY DELETED SUCCESSFULLY ===");
                return true;

            } catch (SQLException e) {
                conn.rollback();
                logger.error("Error during chat history deletion, transaction rolled back", e);
                return false;
            }

        } catch (SQLException e) {
            logger.error("Error connecting to database for chat history deletion", e);
            return false;
        }
    }



    /**
     * Mark messages as read
     */
    public void markMessagesAsRead(int senderId, int receiverId) {
        String sql = "UPDATE chat_messages SET is_read = 1 WHERE sender_id = ? AND receiver_id = ? AND is_read = 0";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, senderId);
            pstmt.setInt(2, receiverId);

            int updatedRows = pstmt.executeUpdate();
            if (updatedRows > 0) {
                logger.debug("Marked {} messages as read", updatedRows);
            }

        } catch (SQLException e) {
            logger.error("Error marking messages as read", e);
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        return "txt";
    }

    /**
     * Get file data for download
     */
    public byte[] getFileData(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                return Files.readAllBytes(path);
            }
        } catch (IOException e) {
            logger.error("Error reading file: {}", filePath, e);
        }
        return null;
    }

    /**
     * Get admin users (for chat recipient selection)
     */
    public List<User> getAdminUsers() {
        String sql = "SELECT * FROM users WHERE role = 'ADMIN'";
        List<User> admins = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                User admin = new User();
                admin.setUserId(rs.getInt("user_id"));
                admin.setUsername(rs.getString("username"));
                admin.setFullName(rs.getString("full_name"));
                admin.setRole(UserRole.valueOf(rs.getString("role")));
                admins.add(admin);
            }

        } catch (SQLException e) {
            logger.error("Error getting admin users", e);
        }

        return admins;
    }

}
