package com.example.taskmanagerv3.service;

import com.example.taskmanagerv3.model.Task;
import com.example.taskmanagerv3.model.TaskPriority;
import com.example.taskmanagerv3.model.TaskStatus;
import com.example.taskmanagerv3.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for Admin AI Chatbot functionality
 * Handles admin-specific AI interactions including task creation and assignment
 */
public class AdminAIChatbotService {
    private static final Logger logger = LoggerFactory.getLogger(AdminAIChatbotService.class);

    private final GeminiApiService geminiApiService;
    private final TaskService taskService;
    private final UserService userService;
    private final NotificationService notificationService;

    public AdminAIChatbotService() {
        this.geminiApiService = new GeminiApiService();
        this.taskService = new TaskService();
        this.userService = new UserService();
        this.notificationService = new NotificationService();
    }

    /**
     * Process admin message and generate AI response
     */
    public String processMessage(String userMessage, int adminUserId) {
        try {
            logger.info("Processing admin message: '{}' for admin ID: {}", userMessage, adminUserId);

            // Check if Gemini API is available
            boolean apiAvailable = geminiApiService.isApiAvailable();
            logger.info("Gemini API available: {}", apiAvailable);

            if (apiAvailable) {
                logger.info("Using Gemini API for admin response generation");
                return processWithGeminiApi(userMessage, adminUserId);
            } else {
                logger.warn("Gemini API not configured, falling back to simulated admin responses");
                return processWithFallback(userMessage, adminUserId);
            }

        } catch (Exception e) {
            logger.error("Error processing admin message", e);
            return "I apologize, but I'm having trouble processing your request right now. Please try again later.";
        }
    }

    /**
     * Process message using Gemini API with admin-specific context
     */
    private String processWithGeminiApi(String userMessage, int adminUserId) {
        try {
            // Get admin context
            String adminContext = buildAdminContext(adminUserId);

            // Create admin-specific prompt
            String adminPrompt = String.format("""
                You are an AI assistant for a WorkFlow Manager system, specifically helping an ADMIN user.

                ADMIN CONTEXT:
                %s

                ADMIN CAPABILITIES:
                - Create tasks and assign them to users
                - View team workload and status
                - Manage user assignments
                - Monitor project progress
                - Generate reports and analytics

                TASK CREATION COMMANDS:
                When the admin wants to create a task, look for patterns like:
                - "Create task [title] for [user] with [priority] priority"
                - "Assign [task] to [user] due [date]"
                - "New task: [description] for [username]"

                USER MESSAGE: %s

                If this is a task creation request, respond with:
                "TASK_CREATION_REQUEST: [extracted details]"

                Otherwise, provide helpful admin-focused guidance and information.
                Use markdown formatting for better readability.
                """, adminContext, userMessage);

            String response = geminiApiService.generateResponse(adminPrompt, adminContext);

            // Check if this is a task creation request
            if (response.startsWith("TASK_CREATION_REQUEST:")) {
                return handleTaskCreationRequest(userMessage, adminUserId);
            }

            return response;

        } catch (Exception e) {
            logger.error("Error processing with Gemini API", e);
            return processWithFallback(userMessage, adminUserId);
        }
    }

    /**
     * Process message with fallback responses for admin
     */
    private String processWithFallback(String userMessage, int adminUserId) {
        String normalizedMessage = userMessage.toLowerCase().trim();

        // Check for task creation patterns
        if (isTaskCreationRequest(normalizedMessage)) {
            return handleTaskCreationRequest(userMessage, adminUserId);
        }

        // Check for team status requests
        if (isTeamStatusRequest(normalizedMessage)) {
            return handleTeamStatusRequest(adminUserId);
        }

        // Check for user workload requests
        if (isWorkloadRequest(normalizedMessage)) {
            return handleWorkloadRequest(normalizedMessage, adminUserId);
        }

        // Check for help requests
        if (isHelpRequest(normalizedMessage)) {
            return getAdminHelpResponse();
        }

        // Default admin response
        return generateDefaultAdminResponse(userMessage);
    }

    /**
     * Handle task creation request
     */
    private String handleTaskCreationRequest(String userMessage, int adminUserId) {
        try {
            TaskCreationRequest request = parseTaskCreationRequest(userMessage);

            if (request == null) {
                return """
                    I couldn't parse your task creation request. Please use a format like:

                    **Examples:**
                    - "Create task 'Fix login bug' for John with high priority"
                    - "Assign 'Update documentation' to Sarah due tomorrow"
                    - "New task: Review code for Mike with medium priority due Friday"
                    """;
            }

            // Find user
            Optional<User> targetUser = findUserByName(request.username);
            if (targetUser.isEmpty()) {
                return String.format("""
                    ❌ **User not found**: '%s'

                    **Available users:**
                    %s
                    """, request.username, getAvailableUsersList());
            }

            // Create task
            Task task = new Task();
            task.setTitle(request.title);
            task.setDescription(request.description != null ? request.description : request.title);
            task.setStatus(TaskStatus.TODO);
            task.setPriority(request.priority);
            task.setAssignedUserId(targetUser.get().getUserId());
            task.setCreatedByUserId(adminUserId);
            task.setEstimatedHours(8); // Default

            if (request.dueDate != null) {
                task.setDueDate(LocalDateTime.of(request.dueDate, LocalTime.of(23, 59, 59)));
            }

            // Save task
            if (taskService.createTask(task)) {
                // Send notification
                User admin = userService.getUserById(adminUserId).orElse(null);
                notificationService.sendTaskAssignmentNotification(task, targetUser.get(), admin);

                return String.format("""
                    ✅ **Task created successfully!**

                    **Task Details:**
                    - **Title:** %s
                    - **Assigned to:** %s (%s)
                    - **Priority:** %s
                    - **Due Date:** %s
                    - **Status:** TODO

                    📧 Notification sent to %s
                    """,
                    task.getTitle(),
                    targetUser.get().getFullName(),
                    targetUser.get().getUsername(),
                    task.getPriority(),
                    task.getDueDate() != null ? task.getDueDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "Not set",
                    targetUser.get().getEmail()
                );
            } else {
                return "❌ **Failed to create task.** Please try again or check the system logs.";
            }

        } catch (Exception e) {
            logger.error("Error handling task creation request", e);
            return "❌ **Error creating task:** " + e.getMessage();
        }
    }

    /**
     * Handle team status request
     */
    private String handleTeamStatusRequest(int adminUserId) {
        try {
            List<User> allUsers = userService.getAllActiveUsers();
            StringBuilder response = new StringBuilder("## 👥 **Team Status Overview**\n\n");

            for (User user : allUsers) {
                List<Task> userTasks = taskService.getTasksByUserId(user.getUserId());
                long pendingTasks = userTasks.stream().filter(t -> t.getStatus() != TaskStatus.COMPLETED).count();
                long completedTasks = userTasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();

                response.append(String.format("""
                    **%s** (@%s)
                    - 📋 Active Tasks: %d
                    - ✅ Completed: %d
                    - 📧 %s

                    """,
                    user.getFullName(),
                    user.getUsername(),
                    pendingTasks,
                    completedTasks,
                    user.getEmail()
                ));
            }

            return response.toString();

        } catch (Exception e) {
            logger.error("Error getting team status", e);
            return "❌ **Error getting team status:** " + e.getMessage();
        }
    }

    /**
     * Build admin context for AI
     */
    private String buildAdminContext(int adminUserId) {
        try {
            List<User> allUsers = userService.getAllActiveUsers();
            List<Task> allTasks = taskService.getAllTasks();

            return String.format("""
                Total Users: %d
                Total Tasks: %d
                Available Users: %s
                """,
                allUsers.size(),
                allTasks.size(),
                allUsers.stream().map(User::getUsername).reduce((a, b) -> a + ", " + b).orElse("None")
            );

        } catch (Exception e) {
            logger.error("Error building admin context", e);
            return "Context unavailable";
        }
    }

    /**
     * Parse task creation request from natural language
     */
    private TaskCreationRequest parseTaskCreationRequest(String message) {
        // Pattern for: "Create task 'title' for username with priority priority"
        Pattern pattern1 = Pattern.compile("create\\s+task\\s+['\"]([^'\"]+)['\"]\\s+for\\s+(\\w+)\\s+with\\s+(\\w+)\\s+priority", Pattern.CASE_INSENSITIVE);
        Matcher matcher1 = pattern1.matcher(message);

        if (matcher1.find()) {
            TaskCreationRequest request = new TaskCreationRequest();
            request.title = matcher1.group(1);
            request.username = matcher1.group(2);
            request.priority = parsePriority(matcher1.group(3));
            return request;
        }

        // Pattern for: "Assign 'task' to username due date"
        Pattern pattern2 = Pattern.compile("assign\\s+['\"]([^'\"]+)['\"]\\s+to\\s+(\\w+)(?:\\s+due\\s+(\\w+))?", Pattern.CASE_INSENSITIVE);
        Matcher matcher2 = pattern2.matcher(message);

        if (matcher2.find()) {
            TaskCreationRequest request = new TaskCreationRequest();
            request.title = matcher2.group(1);
            request.username = matcher2.group(2);
            request.priority = TaskPriority.MEDIUM; // Default
            if (matcher2.group(3) != null) {
                request.dueDate = parseDate(matcher2.group(3));
            }
            return request;
        }

        return null;
    }

    /**
     * Parse priority from string
     */
    private TaskPriority parsePriority(String priorityStr) {
        try {
            return TaskPriority.valueOf(priorityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            switch (priorityStr.toLowerCase()) {
                case "low": return TaskPriority.LOW;
                case "medium": case "med": return TaskPriority.MEDIUM;
                case "high": return TaskPriority.HIGH;
                case "urgent": case "critical": return TaskPriority.URGENT;
                default: return TaskPriority.MEDIUM;
            }
        }
    }

    /**
     * Parse date from string
     */
    private LocalDate parseDate(String dateStr) {
        try {
            switch (dateStr.toLowerCase()) {
                case "today": return LocalDate.now();
                case "tomorrow": return LocalDate.now().plusDays(1);
                case "monday": return getNextWeekday(1);
                case "tuesday": return getNextWeekday(2);
                case "wednesday": return getNextWeekday(3);
                case "thursday": return getNextWeekday(4);
                case "friday": return getNextWeekday(5);
                default:
                    return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Get next occurrence of weekday
     */
    private LocalDate getNextWeekday(int targetDayOfWeek) {
        LocalDate today = LocalDate.now();
        int currentDayOfWeek = today.getDayOfWeek().getValue();
        int daysToAdd = (targetDayOfWeek - currentDayOfWeek + 7) % 7;
        if (daysToAdd == 0) daysToAdd = 7; // Next week if today is the target day
        return today.plusDays(daysToAdd);
    }

    /**
     * Find user by name (username or full name)
     */
    private Optional<User> findUserByName(String name) {
        try {
            List<User> allUsers = userService.getAllActiveUsers();
            return allUsers.stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(name) ||
                               user.getFullName().toLowerCase().contains(name.toLowerCase()))
                .findFirst();
        } catch (Exception e) {
            logger.error("Error finding user by name: {}", name, e);
            return Optional.empty();
        }
    }

    /**
     * Get list of available users
     */
    private String getAvailableUsersList() {
        try {
            List<User> allUsers = userService.getAllActiveUsers();
            StringBuilder usersList = new StringBuilder();
            for (User user : allUsers) {
                usersList.append(String.format("- **%s** (@%s)\n", user.getFullName(), user.getUsername()));
            }
            return usersList.toString();
        } catch (Exception e) {
            return "Error loading users list";
        }
    }

    /**
     * Check if message is a task creation request
     */
    private boolean isTaskCreationRequest(String message) {
        return message.contains("create task") ||
               message.contains("assign") ||
               message.contains("new task");
    }

    /**
     * Check if message is a team status request
     */
    private boolean isTeamStatusRequest(String message) {
        return message.contains("team status") ||
               message.contains("team overview") ||
               message.contains("show team");
    }

    /**
     * Check if message is a workload request
     */
    private boolean isWorkloadRequest(String message) {
        return message.contains("workload") ||
               message.contains("user tasks") ||
               message.contains("who has");
    }

    /**
     * Check if message is a help request
     */
    private boolean isHelpRequest(String message) {
        return message.contains("help") ||
               message.contains("how to") ||
               message.contains("commands");
    }

    /**
     * Handle workload request
     */
    private String handleWorkloadRequest(String message, int adminUserId) {
        return "📊 **Workload analysis feature coming soon!** For now, use 'team status' to see basic task counts.";
    }

    /**
     * Get admin help response
     */
    private String getAdminHelpResponse() {
        return """
            # 🤖 **Admin AI Assistant Help**

            ## **Task Management Commands:**
            - `Create task 'Task Title' for username with priority priority`
            - `Assign 'Task Name' to username due date`
            - `New task: Description for username`

            ## **Team Management:**
            - `Show team status` - View all team members and their task counts
            - `Team overview` - Get team productivity summary

            ## **Examples:**
            - `Create task 'Fix login bug' for john with high priority`
            - `Assign 'Update documentation' to sarah due tomorrow`
            - `Show team status`

            ## **Supported Priorities:**
            - Low, Medium, High, Urgent

            ## **Supported Dates:**
            - today, tomorrow, monday, tuesday, etc.
            - YYYY-MM-DD format
            """;
    }

    /**
     * Generate default admin response
     */
    private String generateDefaultAdminResponse(String message) {
        return """
            I'm your **Admin AI Assistant** for WorkFlow Manager!

            I can help you with:
            - 📋 **Creating and assigning tasks**
            - 👥 **Managing team workload**
            - 📊 **Viewing team status**

            Try asking me to:
            - "Create task 'Fix bug' for john with high priority"
            - "Show team status"
            - "Help" for more commands
            """;
    }

    /**
     * Task creation request data class
     */
    private static class TaskCreationRequest {
        String title;
        String description;
        String username;
        TaskPriority priority = TaskPriority.MEDIUM;
        LocalDate dueDate;
    }

    /**
     * Check if Gemini API is available
     */
    public boolean isGeminiApiAvailable() {
        return geminiApiService.isApiAvailable();
    }

    /**
     * Get API status
     */
    public String getApiStatus() {
        if (isGeminiApiAvailable()) {
            return "✅ Connected to Gemini 2.0 Flash API";
        } else {
            return "⚠️ Using fallback responses (Gemini API not configured)";
        }
    }

    /**
     * Clear conversation history
     */
    public void clearConversationHistory() {
        if (geminiApiService != null) {
            geminiApiService.clearConversationHistory();
        }
    }
}
