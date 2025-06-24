package com.example.taskmanagerv3.service;

import com.example.taskmanagerv3.model.Achievement;
import com.example.taskmanagerv3.model.AchievementType;
import com.example.taskmanagerv3.model.Task;
import com.example.taskmanagerv3.model.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for managing achievements
 */
public class AchievementService {
    private static final Logger logger = LoggerFactory.getLogger(AchievementService.class);
    
    private TaskService taskService;

    public AchievementService() {
        this.taskService = new TaskService();
    }

    /**
     * Initialize default achievements for a user
     */
    public List<Achievement> initializeDefaultAchievements(int userId) {
        List<Achievement> achievements = new ArrayList<>();

        // Task Completion Achievements
        achievements.add(new Achievement("First Task", "Complete your first task", "🥇", 
                                       AchievementType.TASK_COMPLETION, 1));
        achievements.add(new Achievement("Task Novice", "Complete 5 tasks", "📝", 
                                       AchievementType.TASK_COMPLETION, 5));
        achievements.add(new Achievement("Task Expert", "Complete 25 tasks", "⭐", 
                                       AchievementType.TASK_COMPLETION, 25));
        achievements.add(new Achievement("Task Master", "Complete 50 tasks", "🏆", 
                                       AchievementType.TASK_COMPLETION, 50));
        achievements.add(new Achievement("Task Legend", "Complete 100 tasks", "👑", 
                                       AchievementType.TASK_COMPLETION, 100));

        // Streak Achievements
        achievements.add(new Achievement("Getting Started", "Complete tasks for 3 consecutive days", "🔥", 
                                       AchievementType.STREAK, 3));
        achievements.add(new Achievement("Week Warrior", "Complete tasks for 7 consecutive days", "⚡", 
                                       AchievementType.STREAK, 7));
        achievements.add(new Achievement("Consistency King", "Complete tasks for 30 consecutive days", "💎", 
                                       AchievementType.STREAK, 30));

        // Speed Achievements
        achievements.add(new Achievement("Speed Demon", "Complete 5 tasks in one day", "🚀", 
                                       AchievementType.SPEED, 5));
        achievements.add(new Achievement("Lightning Fast", "Complete 10 tasks in one day", "⚡", 
                                       AchievementType.SPEED, 10));

        // Milestone Achievements
        achievements.add(new Achievement("Perfect Week", "Complete all assigned tasks in a week", "🎯", 
                                       AchievementType.MILESTONE, 1));
        achievements.add(new Achievement("Zero Overdue", "Have no overdue tasks for 7 days", "✨", 
                                       AchievementType.MILESTONE, 7));

        // Set user ID for all achievements
        achievements.forEach(achievement -> achievement.setUserId(userId));

        return achievements;
    }

    /**
     * Calculate and update achievements for a user
     */
    public List<Achievement> calculateAchievements(int userId, List<Task> userTasks) {
        try {
            List<Achievement> achievements = initializeDefaultAchievements(userId);
            
            if (userTasks == null || userTasks.isEmpty()) {
                return achievements;
            }

            // Calculate task completion achievements
            updateTaskCompletionAchievements(achievements, userTasks);
            
            // Calculate streak achievements
            updateStreakAchievements(achievements, userTasks);
            
            // Calculate speed achievements
            updateSpeedAchievements(achievements, userTasks);
            
            // Calculate milestone achievements
            updateMilestoneAchievements(achievements, userTasks);

            // Auto-unlock completed achievements
            achievements.forEach(achievement -> {
                if (achievement.isCompleted() && !achievement.isUnlocked()) {
                    achievement.setUnlocked(true);
                    logger.info("Achievement unlocked for user {}: {}", userId, achievement.getName());
                }
            });

            return achievements;

        } catch (Exception e) {
            logger.error("Error calculating achievements for user: {}", userId, e);
            return initializeDefaultAchievements(userId);
        }
    }

    /**
     * Update task completion achievements
     */
    private void updateTaskCompletionAchievements(List<Achievement> achievements, List<Task> userTasks) {
        int completedTasks = (int) userTasks.stream()
            .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
            .count();

        achievements.stream()
            .filter(a -> a.getType() == AchievementType.TASK_COMPLETION)
            .forEach(achievement -> achievement.setCurrentValue(completedTasks));
    }

    /**
     * Update streak achievements
     */
    private void updateStreakAchievements(List<Achievement> achievements, List<Task> userTasks) {
        int currentStreak = calculateStreak(userTasks);

        achievements.stream()
            .filter(a -> a.getType() == AchievementType.STREAK)
            .forEach(achievement -> achievement.setCurrentValue(currentStreak));
    }

    /**
     * Update speed achievements
     */
    private void updateSpeedAchievements(List<Achievement> achievements, List<Task> userTasks) {
        int maxTasksInOneDay = calculateMaxTasksInOneDay(userTasks);

        achievements.stream()
            .filter(a -> a.getType() == AchievementType.SPEED)
            .forEach(achievement -> achievement.setCurrentValue(maxTasksInOneDay));
    }

    /**
     * Update milestone achievements
     */
    private void updateMilestoneAchievements(List<Achievement> achievements, List<Task> userTasks) {
        // Perfect Week: Complete all assigned tasks in a week (simplified)
        boolean hasPerfectWeek = calculatePerfectWeek(userTasks);
        
        // Zero Overdue: No overdue tasks for 7 days (simplified)
        int daysWithoutOverdue = calculateDaysWithoutOverdue(userTasks);

        achievements.stream()
            .filter(a -> a.getType() == AchievementType.MILESTONE)
            .forEach(achievement -> {
                if (achievement.getName().equals("Perfect Week")) {
                    achievement.setCurrentValue(hasPerfectWeek ? 1 : 0);
                } else if (achievement.getName().equals("Zero Overdue")) {
                    achievement.setCurrentValue(daysWithoutOverdue);
                }
            });
    }

    /**
     * Calculate consecutive days streak
     */
    private int calculateStreak(List<Task> userTasks) {
        LocalDate today = LocalDate.now();
        int streak = 0;
        LocalDate checkDate = today;

        for (int i = 0; i < 30; i++) { // Check last 30 days max
            final LocalDate currentDate = checkDate;
            boolean hasCompletedTask = userTasks.stream()
                .anyMatch(task -> task.getStatus() == TaskStatus.COMPLETED &&
                                task.getCompletedAt() != null &&
                                task.getCompletedAt().toLocalDate().equals(currentDate));

            if (hasCompletedTask) {
                streak++;
                checkDate = checkDate.minusDays(1);
            } else {
                break;
            }
        }

        return streak;
    }

    /**
     * Calculate maximum tasks completed in one day
     */
    private int calculateMaxTasksInOneDay(List<Task> userTasks) {
        return userTasks.stream()
            .filter(task -> task.getStatus() == TaskStatus.COMPLETED && task.getCompletedAt() != null)
            .collect(Collectors.groupingBy(task -> task.getCompletedAt().toLocalDate()))
            .values()
            .stream()
            .mapToInt(List::size)
            .max()
            .orElse(0);
    }

    /**
     * Check if user had a perfect week (simplified)
     */
    private boolean calculatePerfectWeek(List<Task> userTasks) {
        // Simplified: if completion rate > 90%
        long totalTasks = userTasks.size();
        long completedTasks = userTasks.stream()
            .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
            .count();
        
        return totalTasks > 0 && (double) completedTasks / totalTasks > 0.9;
    }

    /**
     * Calculate days without overdue tasks
     */
    private int calculateDaysWithoutOverdue(List<Task> userTasks) {
        // Simplified: if no current overdue tasks, return 7
        boolean hasOverdue = userTasks.stream().anyMatch(Task::isOverdue);
        return hasOverdue ? 0 : 7;
    }

    /**
     * Get next achievement to unlock
     */
    public Achievement getNextAchievement(List<Achievement> achievements) {
        return achievements.stream()
            .filter(a -> !a.isUnlocked())
            .min((a1, a2) -> Double.compare(a2.getProgress(), a1.getProgress()))
            .orElse(null);
    }

    /**
     * Get recently unlocked achievements
     */
    public List<Achievement> getRecentlyUnlocked(List<Achievement> achievements, int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return achievements.stream()
            .filter(a -> a.isUnlocked() && a.getUnlockedAt() != null && a.getUnlockedAt().isAfter(cutoff))
            .collect(Collectors.toList());
    }
}
