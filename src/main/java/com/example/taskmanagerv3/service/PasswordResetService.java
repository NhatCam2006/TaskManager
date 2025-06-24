package com.example.taskmanagerv3.service;

import com.example.taskmanagerv3.config.DatabaseConfig;
import com.example.taskmanagerv3.model.PasswordResetRequest;
import com.example.taskmanagerv3.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for handling password reset requests
 */
public class PasswordResetService {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    /**
     * Create password reset request table if not exists
     */
    public static void initializePasswordResetTable() {
        String createTableQuery = """
            IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='PasswordResetRequests' AND xtype='U')
            CREATE TABLE PasswordResetRequests (
                request_id INT IDENTITY(1,1) PRIMARY KEY,
                user_id INT NOT NULL,
                username NVARCHAR(50) NOT NULL,
                email NVARCHAR(100) NOT NULL,
                full_name NVARCHAR(100) NOT NULL,
                reason NVARCHAR(500),
                status NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
                requested_at DATETIME2 DEFAULT GETDATE(),
                processed_at DATETIME2,
                processed_by INT,
                admin_notes NVARCHAR(1000),
                FOREIGN KEY (user_id) REFERENCES Users(user_id),
                FOREIGN KEY (processed_by) REFERENCES Users(user_id)
            )
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(createTableQuery);
            logger.info("PasswordResetRequests table initialized successfully");
            
        } catch (SQLException e) {
            logger.error("Failed to initialize PasswordResetRequests table", e);
            throw new RuntimeException("Failed to initialize password reset table", e);
        }
    }

    /**
     * Submit a password reset request
     */
    public boolean submitPasswordResetRequest(String usernameOrEmail, String reason) {
        // First, find the user
        UserService userService = new UserService();
        Optional<User> userOpt = findUserByUsernameOrEmail(usernameOrEmail);
        
        if (userOpt.isEmpty()) {
            logger.warn("Password reset request for non-existent user: {}", usernameOrEmail);
            return false;
        }
        
        User user = userOpt.get();
        
        // Check if there's already a pending request for this user
        if (hasPendingRequest(user.getUserId())) {
            logger.warn("User {} already has a pending password reset request", user.getUsername());
            return false;
        }
        
        String insertQuery = """
            INSERT INTO PasswordResetRequests (user_id, username, email, full_name, reason)
            VALUES (?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
            
            stmt.setInt(1, user.getUserId());
            stmt.setString(2, user.getUsername());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getFullName());
            stmt.setString(5, reason);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("Password reset request submitted for user: {}", user.getUsername());
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error submitting password reset request for user: {}", usernameOrEmail, e);
        }
        
        return false;
    }
    
    /**
     * Get all pending password reset requests (for admin)
     */
    public List<PasswordResetRequest> getPendingRequests() {
        List<PasswordResetRequest> requests = new ArrayList<>();
        String query = """
            SELECT request_id, user_id, username, email, full_name, reason, status,
                   requested_at, processed_at, processed_by, admin_notes
            FROM PasswordResetRequests
            WHERE status = 'PENDING'
            ORDER BY requested_at ASC
            """;
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                requests.add(mapResultSetToPasswordResetRequest(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error getting pending password reset requests", e);
        }
        
        return requests;
    }
    
    /**
     * Process password reset request (approve/reject)
     */
    public boolean processPasswordResetRequest(int requestId, PasswordResetRequest.RequestStatus status, 
                                             int processedBy, String adminNotes) {
        String updateQuery = """
            UPDATE PasswordResetRequests 
            SET status = ?, processed_at = GETDATE(), processed_by = ?, admin_notes = ?
            WHERE request_id = ?
            """;
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
            
            stmt.setString(1, status.name());
            stmt.setInt(2, processedBy);
            stmt.setString(3, adminNotes);
            stmt.setInt(4, requestId);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("Password reset request {} processed with status: {}", requestId, status);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error processing password reset request: {}", requestId, e);
        }
        
        return false;
    }
    
    /**
     * Check if user has pending password reset request
     */
    private boolean hasPendingRequest(int userId) {
        String query = "SELECT COUNT(*) FROM PasswordResetRequests WHERE user_id = ? AND status = 'PENDING'";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            logger.error("Error checking pending requests for user: {}", userId, e);
        }
        
        return false;
    }
    
    /**
     * Find user by username or email
     */
    private Optional<User> findUserByUsernameOrEmail(String usernameOrEmail) {
        String query = """
            SELECT user_id, username, email, password_hash, full_name, role, is_active,
                   created_at, last_login, profile_picture, department, phone_number
            FROM Users
            WHERE (username = ? OR email = ?) AND is_active = 1
            """;
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, usernameOrEmail);
            stmt.setString(2, usernameOrEmail);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                UserService userService = new UserService();
                return Optional.of(userService.mapResultSetToUser(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error finding user by username or email: {}", usernameOrEmail, e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Map ResultSet to PasswordResetRequest object
     */
    private PasswordResetRequest mapResultSetToPasswordResetRequest(ResultSet rs) throws SQLException {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setRequestId(rs.getInt("request_id"));
        request.setUserId(rs.getInt("user_id"));
        request.setUsername(rs.getString("username"));
        request.setEmail(rs.getString("email"));
        request.setFullName(rs.getString("full_name"));
        request.setReason(rs.getString("reason"));
        request.setStatus(PasswordResetRequest.RequestStatus.valueOf(rs.getString("status")));
        
        Timestamp requestedAt = rs.getTimestamp("requested_at");
        if (requestedAt != null) {
            request.setRequestedAt(requestedAt.toLocalDateTime());
        }
        
        Timestamp processedAt = rs.getTimestamp("processed_at");
        if (processedAt != null) {
            request.setProcessedAt(processedAt.toLocalDateTime());
        }
        
        request.setProcessedBy(rs.getInt("processed_by"));
        request.setAdminNotes(rs.getString("admin_notes"));
        
        return request;
    }
}
