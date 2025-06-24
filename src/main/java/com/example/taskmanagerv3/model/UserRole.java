package com.example.taskmanagerv3.model;

/**
 * Enum representing user roles in the system
 */
public enum UserRole {
    ADMIN("Administrator"),
    USER("User");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
