package com.example.taskmanagerv3.model;

/**
 * Enum representing the different statuses a task can have
 */
public enum TaskStatus {
    TODO("To Do", "#FF6B6B"),
    IN_PROGRESS("In Progress", "#4ECDC4"),
    REVIEW("Under Review", "#45B7D1"),
    COMPLETED("Completed", "#96CEB4"),
    CANCELLED("Cancelled", "#FFEAA7");

    private final String displayName;
    private final String color;

    TaskStatus(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
