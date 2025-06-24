package com.example.taskmanagerv3.model;

/**
 * Enum representing task priority levels
 */
public enum TaskPriority {
    LOW("Low", "#74B9FF", 1),
    MEDIUM("Medium", "#FDCB6E", 2),
    HIGH("High", "#E17055", 3),
    URGENT("Urgent", "#D63031", 4);

    private final String displayName;
    private final String color;
    private final int level;

    TaskPriority(String displayName, String color, int level) {
        this.displayName = displayName;
        this.color = color;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
