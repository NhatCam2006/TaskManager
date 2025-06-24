package com.example.taskmanagerv3.service;

import com.example.taskmanagerv3.model.Task;
import com.example.taskmanagerv3.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import javax.mail.*;
// import javax.mail.internet.InternetAddress;
// import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Service for handling notifications (email, in-app, etc.)
 */
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final UserService userService;
    private final List<InAppNotification> inAppNotifications;

    // Email configuration (should be moved to config file in production)
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String EMAIL_USERNAME = "your-email@gmail.com";
    private static final String EMAIL_PASSWORD = "your-app-password";

    public NotificationService() {
        this.userService = new UserService();
        this.inAppNotifications = new ArrayList<>();
    }

    /**
     * Send task assignment notification
     */
    public void sendTaskAssignmentNotification(Task task, User assignedUser, User assignedBy) {
        try {
            // TODO: Send email notification (temporarily disabled for compilation)
            // CompletableFuture.runAsync(() -> {
            //     sendEmailNotification(
            //         assignedUser.getEmail(),
            //         "New Task Assigned: " + task.getTitle(),
            //         createTaskAssignmentEmailContent(task, assignedUser, assignedBy)
            //     );
            // });

            // Add in-app notification
            addInAppNotification(
                assignedUser.getUserId(),
                "New Task Assigned",
                String.format("You have been assigned a new task: %s", task.getTitle()),
                NotificationType.TASK_ASSIGNMENT
            );

            logger.info("Task assignment notification sent to user: {} (email disabled)", assignedUser.getUsername());

        } catch (Exception e) {
            logger.error("Error sending task assignment notification", e);
        }
    }

    /**
     * Send task deadline reminder
     */
    public void sendDeadlineReminder(Task task, User user) {
        try {
            // Send email notification
            CompletableFuture.runAsync(() -> {
                sendEmailNotification(
                    user.getEmail(),
                    "Task Deadline Reminder: " + task.getTitle(),
                    createDeadlineReminderEmailContent(task, user)
                );
            });

            // Add in-app notification
            addInAppNotification(
                user.getUserId(),
                "Deadline Reminder",
                String.format("Task '%s' is due soon!", task.getTitle()),
                NotificationType.DEADLINE_REMINDER
            );

            logger.info("Deadline reminder sent to user: {}", user.getUsername());

        } catch (Exception e) {
            logger.error("Error sending deadline reminder", e);
        }
    }

    /**
     * Send task completion notification
     */
    public void sendTaskCompletionNotification(Task task, User completedBy, User taskCreator) {
        try {
            if (completedBy.getUserId() != taskCreator.getUserId()) {
                // Send email notification to task creator
                CompletableFuture.runAsync(() -> {
                    sendEmailNotification(
                        taskCreator.getEmail(),
                        "Task Completed: " + task.getTitle(),
                        createTaskCompletionEmailContent(task, completedBy, taskCreator)
                    );
                });

                // Add in-app notification
                addInAppNotification(
                    taskCreator.getUserId(),
                    "Task Completed",
                    String.format("Task '%s' has been completed by %s", task.getTitle(), completedBy.getDisplayName()),
                    NotificationType.TASK_COMPLETION
                );
            }

            logger.info("Task completion notification sent for task: {}", task.getTitle());

        } catch (Exception e) {
            logger.error("Error sending task completion notification", e);
        }
    }

    /**
     * Send email notification (temporarily disabled)
     */
    private void sendEmailNotification(String toEmail, String subject, String content) {
        // TODO: Implement email notification when JavaMail is properly configured
        logger.info("Email notification would be sent to: {} with subject: {}", toEmail, subject);
    }

    /**
     * Create task assignment email content
     */
    private String createTaskAssignmentEmailContent(Task task, User assignedUser, User assignedBy) {
        return String.format("""
            Hello %s,

            You have been assigned a new task:

            Task: %s
            Description: %s
            Priority: %s
            Due Date: %s
            Assigned by: %s

            Please log in to the WorkFlow Manager to view more details and start working on this task.

            Best regards,
            WorkFlow Manager System
            """,
            assignedUser.getDisplayName(),
            task.getTitle(),
            task.getDescription(),
            task.getPriority(),
            task.getDueDate() != null ? task.getDueDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")) : "Not set",
            assignedBy.getDisplayName()
        );
    }

    /**
     * Create deadline reminder email content
     */
    private String createDeadlineReminderEmailContent(Task task, User user) {
        return String.format("""
            Hello %s,

            This is a reminder that your task is due soon:

            Task: %s
            Description: %s
            Priority: %s
            Due Date: %s
            Current Status: %s

            Please make sure to complete this task on time.

            Best regards,
            WorkFlow Manager System
            """,
            user.getDisplayName(),
            task.getTitle(),
            task.getDescription(),
            task.getPriority(),
            task.getDueDate() != null ? task.getDueDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")) : "Not set",
            task.getStatus()
        );
    }

    /**
     * Create task completion email content
     */
    private String createTaskCompletionEmailContent(Task task, User completedBy, User taskCreator) {
        return String.format("""
            Hello %s,

            Good news! A task you created has been completed:

            Task: %s
            Description: %s
            Completed by: %s
            Completion Date: %s

            You can view the completed task details in the WorkFlow Manager.

            Best regards,
            WorkFlow Manager System
            """,
            taskCreator.getDisplayName(),
            task.getTitle(),
            task.getDescription(),
            completedBy.getDisplayName(),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
        );
    }

    /**
     * Add in-app notification
     */
    private void addInAppNotification(int userId, String title, String message, NotificationType type) {
        InAppNotification notification = new InAppNotification(
            userId, title, message, type, LocalDateTime.now()
        );
        inAppNotifications.add(notification);

        // Keep only last 100 notifications per user to prevent memory issues
        while (inAppNotifications.size() > 100) {
            inAppNotifications.remove(0);
        }
    }

    /**
     * Get in-app notifications for user
     */
    public List<InAppNotification> getInAppNotifications(int userId) {
        return inAppNotifications.stream()
            .filter(notification -> notification.getUserId() == userId)
            .sorted((n1, n2) -> n2.getTimestamp().compareTo(n1.getTimestamp()))
            .limit(20)
            .toList();
    }

    /**
     * Mark notification as read
     */
    public void markNotificationAsRead(int notificationId) {
        inAppNotifications.stream()
            .filter(notification -> notification.getId() == notificationId)
            .findFirst()
            .ifPresent(notification -> notification.setRead(true));
    }

    /**
     * Get unread notification count for user
     */
    public int getUnreadNotificationCount(int userId) {
        return (int) inAppNotifications.stream()
            .filter(notification -> notification.getUserId() == userId && !notification.isRead())
            .count();
    }

    /**
     * In-app notification class
     */
    public static class InAppNotification {
        private static int nextId = 1;

        private final int id;
        private final int userId;
        private final String title;
        private final String message;
        private final NotificationType type;
        private final LocalDateTime timestamp;
        private boolean isRead;

        public InAppNotification(int userId, String title, String message, NotificationType type, LocalDateTime timestamp) {
            this.id = nextId++;
            this.userId = userId;
            this.title = title;
            this.message = message;
            this.type = type;
            this.timestamp = timestamp;
            this.isRead = false;
        }

        // Getters and setters
        public int getId() { return id; }
        public int getUserId() { return userId; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public NotificationType getType() { return type; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public boolean isRead() { return isRead; }
        public void setRead(boolean read) { isRead = read; }

        public String getFormattedTimestamp() {
            return timestamp.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"));
        }
    }

    /**
     * Notification types
     */
    public enum NotificationType {
        TASK_ASSIGNMENT,
        DEADLINE_REMINDER,
        TASK_COMPLETION,
        SYSTEM_UPDATE,
        GENERAL
    }
}
