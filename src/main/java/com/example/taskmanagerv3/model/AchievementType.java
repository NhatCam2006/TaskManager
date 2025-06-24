package com.example.taskmanagerv3.model;

/**
 * Enum representing different types of achievements
 */
public enum AchievementType {
    TASK_COMPLETION("Task Completion", "Complete a certain number of tasks"),
    STREAK("Streak", "Maintain consecutive days of task completion"),
    SPEED("Speed", "Complete tasks quickly"),
    QUALITY("Quality", "Maintain high quality work"),
    MILESTONE("Milestone", "Reach specific milestones"),
    CONSISTENCY("Consistency", "Regular task completion over time"),
    COLLABORATION("Collaboration", "Work effectively with team"),
    EFFICIENCY("Efficiency", "Complete tasks efficiently");

    private final String displayName;
    private final String description;

    AchievementType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
