package com.example.taskmanagerv3.websocket;

import java.io.Serializable;

/**
 * Data for user connection message
 */
public class UserConnectData implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;
    private String username;
    private boolean isAdmin;

    public UserConnectData() {}

    public UserConnectData(int userId, String username, boolean isAdmin) {
        this.userId = userId;
        this.username = username;
        this.isAdmin = isAdmin;
    }

    // Getters and Setters
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    @Override
    public String toString() {
        return String.format("UserConnectData{userId=%d, username='%s', isAdmin=%s}", 
                userId, username, isAdmin);
    }
}
