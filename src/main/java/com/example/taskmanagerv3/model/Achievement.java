package com.example.taskmanagerv3.model;

import java.time.LocalDateTime;

/**
 * Achievement model class representing user achievements
 */
public class Achievement {
    private int achievementId;
    private String name;
    private String description;
    private String icon;
    private AchievementType type;
    private int targetValue;
    private int currentValue;
    private boolean isUnlocked;
    private LocalDateTime unlockedAt;
    private int userId;

    // Constructors
    public Achievement() {
        this.isUnlocked = false;
        this.currentValue = 0;
    }

    public Achievement(String name, String description, String icon, AchievementType type, int targetValue) {
        this();
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.type = type;
        this.targetValue = targetValue;
    }

    // Getters and Setters
    public int getAchievementId() {
        return achievementId;
    }

    public void setAchievementId(int achievementId) {
        this.achievementId = achievementId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public AchievementType getType() {
        return type;
    }

    public void setType(AchievementType type) {
        this.type = type;
    }

    public int getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(int targetValue) {
        this.targetValue = targetValue;
    }

    public int getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(int currentValue) {
        this.currentValue = currentValue;
    }

    public boolean isUnlocked() {
        return isUnlocked;
    }

    public void setUnlocked(boolean unlocked) {
        isUnlocked = unlocked;
        if (unlocked && unlockedAt == null) {
            unlockedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getUnlockedAt() {
        return unlockedAt;
    }

    public void setUnlockedAt(LocalDateTime unlockedAt) {
        this.unlockedAt = unlockedAt;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    // Utility methods
    public double getProgress() {
        if (targetValue == 0) return 0.0;
        return Math.min(1.0, (double) currentValue / targetValue);
    }

    public boolean isCompleted() {
        return currentValue >= targetValue;
    }

    public String getProgressText() {
        return String.format("%d/%d", currentValue, targetValue);
    }

    public String getStatusText() {
        if (isUnlocked) {
            return "Achieved";
        } else if (isCompleted()) {
            return "Ready to Unlock";
        } else {
            return "In Progress";
        }
    }

    @Override
    public String toString() {
        return String.format("Achievement{name='%s', progress=%s, unlocked=%s}", 
                           name, getProgressText(), isUnlocked);
    }
}
