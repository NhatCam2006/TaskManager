package com.example.taskmanagerv3.service;

import com.example.taskmanagerv3.config.DatabaseConfig;
import com.example.taskmanagerv3.model.User;
import com.example.taskmanagerv3.model.UserRole;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service class for user-related operations
 */
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    /**
     * Authenticate user with username/email and password
     */
    public Optional<User> authenticate(String usernameOrEmail, String password) {
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
                String storedHash = rs.getString("password_hash");

                // Verify password
                if (BCrypt.checkpw(password, storedHash)) {
                    User user = mapResultSetToUser(rs);

                    // Update last login time
                    updateLastLogin(user.getUserId());

                    logger.info("User authenticated successfully: {}", user.getUsername());
                    return Optional.of(user);
                }
            }

        } catch (SQLException e) {
            logger.error("Error during user authentication", e);
        }

        return Optional.empty();
    }

    /**
     * Create new user
     */
    public boolean createUser(User user, String password) {
        String query = """
            INSERT INTO Users (username, email, password_hash, full_name, role, department, phone_number)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            // Hash password
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, hashedPassword);
            stmt.setString(4, user.getFullName());
            stmt.setString(5, user.getRole().name());
            stmt.setString(6, user.getDepartment());
            stmt.setString(7, user.getPhoneNumber());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    user.setUserId(generatedKeys.getInt(1));
                }
                logger.info("User created successfully: {}", user.getUsername());
                return true;
            }

        } catch (SQLException e) {
            logger.error("Error creating user: {}", user.getUsername(), e);
        }

        return false;
    }

    /**
     * Get user by ID
     */
    public Optional<User> getUserById(int userId) {
        String query = """
            SELECT user_id, username, email, password_hash, full_name, role, is_active,
                   created_at, last_login, profile_picture, department, phone_number
            FROM Users
            WHERE user_id = ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting user by ID: {}", userId, e);
        }

        return Optional.empty();
    }

    /**
     * Get all active users
     */
    public List<User> getAllActiveUsers() {
        List<User> users = new ArrayList<>();
        String query = """
            SELECT user_id, username, email, password_hash, full_name, role, is_active,
                   created_at, last_login, profile_picture, department, phone_number
            FROM Users
            WHERE is_active = 1
            ORDER BY full_name
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting all active users", e);
        }

        return users;
    }

    /**
     * Update user profile
     */
    public boolean updateUser(User user) {
        String query = """
            UPDATE Users
            SET full_name = ?, email = ?, department = ?, phone_number = ?, profile_picture = ?
            WHERE user_id = ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getDepartment());
            stmt.setString(4, user.getPhoneNumber());
            stmt.setString(5, user.getProfilePicture());
            stmt.setInt(6, user.getUserId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("User updated successfully: {}", user.getUsername());
                return true;
            }

        } catch (SQLException e) {
            logger.error("Error updating user: {}", user.getUsername(), e);
        }

        return false;
    }

    /**
     * Check if username exists
     */
    public boolean usernameExists(String username) {
        String query = "SELECT COUNT(*) FROM Users WHERE username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            logger.error("Error checking username existence: {}", username, e);
        }

        return false;
    }

    /**
     * Check if email exists
     */
    public boolean emailExists(String email) {
        String query = "SELECT COUNT(*) FROM Users WHERE email = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            logger.error("Error checking email existence: {}", email, e);
        }

        return false;
    }

    /**
     * Change user password
     */
    public boolean changePassword(int userId, String currentPassword, String newPassword) {
        // First verify current password
        String verifyQuery = "SELECT password_hash FROM Users WHERE user_id = ?";
        String updateQuery = "UPDATE Users SET password_hash = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection()) {
            // Verify current password
            try (PreparedStatement verifyStmt = conn.prepareStatement(verifyQuery)) {
                verifyStmt.setInt(1, userId);
                ResultSet rs = verifyStmt.executeQuery();

                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");

                    // Check if current password is correct
                    if (!BCrypt.checkpw(currentPassword, storedHash)) {
                        logger.warn("Password change failed - incorrect current password for user ID: {}", userId);
                        return false;
                    }
                } else {
                    logger.error("User not found for password change: {}", userId);
                    return false;
                }
            }

            // Update password
            try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                String newHashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
                updateStmt.setString(1, newHashedPassword);
                updateStmt.setInt(2, userId);

                int affectedRows = updateStmt.executeUpdate();

                if (affectedRows > 0) {
                    logger.info("Password changed successfully for user ID: {}", userId);
                    return true;
                }
            }

        } catch (SQLException e) {
            logger.error("Error changing password for user ID: {}", userId, e);
        }

        return false;
    }

    /**
     * Update last login time
     */
    private void updateLastLogin(int userId) {
        String query = "UPDATE Users SET last_login = GETDATE() WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            logger.error("Error updating last login for user ID: {}", userId, e);
        }
    }

    /**
     * Update user password
     */
    public boolean updateUserPassword(int userId, String newPassword) {
        String query = "UPDATE Users SET password_hash = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            // Hash the new password
            String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());

            stmt.setString(1, hashedPassword);
            stmt.setInt(2, userId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("Password updated successfully for user ID: {}", userId);
                return true;
            }

        } catch (SQLException e) {
            logger.error("Error updating password for user ID: {}", userId, e);
        }

        return false;
    }

    /**
     * Map ResultSet to User object
     */
    public User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setRole(UserRole.valueOf(rs.getString("role")));
        user.setActive(rs.getBoolean("is_active"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null) {
            user.setLastLogin(lastLogin.toLocalDateTime());
        }

        user.setProfilePicture(rs.getString("profile_picture"));
        user.setDepartment(rs.getString("department"));
        user.setPhoneNumber(rs.getString("phone_number"));

        return user;
    }
}
