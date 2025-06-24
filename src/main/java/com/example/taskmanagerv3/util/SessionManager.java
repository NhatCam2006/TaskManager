package com.example.taskmanagerv3.util;

import com.example.taskmanagerv3.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Session manager to handle user authentication and session state
 */
public class SessionManager {
    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

    private static SessionManager instance;
    private User currentUser;
    private LocalDateTime loginTime;
    private boolean isLoggedIn;

    private SessionManager() {
        this.isLoggedIn = false;
    }

    /**
     * Get singleton instance
     */
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Login user
     */
    public void login(User user) {
        this.currentUser = user;
        this.loginTime = LocalDateTime.now();
        this.isLoggedIn = true;

        logger.info("User logged in: {} ({})", user.getUsername(), user.getRole());
    }

    /**
     * Logout current user
     */
    public void logout() {
        if (currentUser != null) {
            logger.info("User logged out: {}", currentUser.getUsername());
        }

        this.currentUser = null;
        this.loginTime = null;
        this.isLoggedIn = false;
    }

    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return isLoggedIn && currentUser != null;
    }

    /**
     * Get current logged in user
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Update current user information (for profile updates)
     */
    public void updateCurrentUser(User user) {
        if (isLoggedIn() && user != null && user.getUserId() == currentUser.getUserId()) {
            this.currentUser = user;
            logger.info("Current user information updated: {}", user.getUsername());
        }
    }

    /**
     * Get login time
     */
    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    /**
     * Check if current user is admin
     */
    public boolean isCurrentUserAdmin() {
        return isLoggedIn() && currentUser.isAdmin();
    }

    /**
     * Get current user ID
     */
    public int getCurrentUserId() {
        return isLoggedIn() ? currentUser.getUserId() : -1;
    }

    /**
     * Get current username
     */
    public String getCurrentUsername() {
        return isLoggedIn() ? currentUser.getUsername() : null;
    }

    /**
     * Get session duration in minutes
     */
    public long getSessionDurationMinutes() {
        if (loginTime == null) return 0;
        return java.time.Duration.between(loginTime, LocalDateTime.now()).toMinutes();
    }

    /**
     * Check if session is valid (not expired)
     * Session expires after 8 hours of inactivity
     */
    public boolean isSessionValid() {
        if (!isLoggedIn() || loginTime == null) {
            return false;
        }

        long sessionDurationHours = java.time.Duration.between(loginTime, LocalDateTime.now()).toHours();
        return sessionDurationHours < 8; // 8 hours session timeout
    }

    /**
     * Refresh session (update login time)
     */
    public void refreshSession() {
        if (isLoggedIn()) {
            this.loginTime = LocalDateTime.now();
        }
    }

    /**
     * Force logout if session expired
     */
    public void checkSessionExpiry() {
        if (isLoggedIn() && !isSessionValid()) {
            logger.warn("Session expired for user: {}", currentUser.getUsername());
            logout();
        }
    }
}
