package com.example.taskmanagerv3.service;

import com.example.taskmanagerv3.config.GeminiConfig;
import com.example.taskmanagerv3.model.Task;
import com.example.taskmanagerv3.model.TaskPriority;
import com.example.taskmanagerv3.model.TaskStatus;
import com.example.taskmanagerv3.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * AI Chatbot Service using Google Gemini 2.0 Flash API for intelligent task suggestions and assistance
 */
public class AIChatbotService {
    private static final Logger logger = LoggerFactory.getLogger(AIChatbotService.class);

    private final TaskService taskService;
    private final UserService userService;
    private final GeminiApiService geminiApiService;
    private final ObjectMapper objectMapper;
    private final Random random;

    // Fallback responses when Gemini API is not available
    private static final String[] FALLBACK_GREETINGS = {
        "Hello! I'm your AI assistant. How can I help you with your tasks today?",
        "Hi there! I'm here to help you manage your workflow efficiently. What would you like to know?",
        "Welcome! I can help you with task management, suggestions, and productivity tips. How may I assist you?"
    };

    private static final String[] FALLBACK_SUGGESTIONS = {
        "Based on your workload, I suggest prioritizing tasks with approaching deadlines.",
        "Consider breaking down large tasks into smaller, manageable subtasks.",
        "You might want to delegate some tasks to team members to balance the workload.",
        "I notice you have several high-priority tasks. Would you like me to help organize them?",
        "It looks like you're doing great! Keep up the momentum with your current tasks."
    };

    public AIChatbotService() {
        this.taskService = new TaskService();
        this.userService = new UserService();
        this.geminiApiService = new GeminiApiService();
        this.objectMapper = new ObjectMapper();
        this.random = new Random();
    }

    /**
     * Process user message and generate AI response
     */
    public String processMessage(String userMessage, int userId) {
        try {
            logger.info("Processing message: '{}' for user ID: {}", userMessage, userId);

            // Check if Gemini API is available
            boolean apiAvailable = geminiApiService.isApiAvailable();
            logger.info("Gemini API available: {}", apiAvailable);

            if (apiAvailable) {
                logger.info("Using Gemini API for response generation");
                return processWithGeminiApi(userMessage, userId);
            } else {
                logger.warn("Gemini API not configured, falling back to simulated responses");
                return processWithFallback(userMessage, userId);
            }

        } catch (Exception e) {
            logger.error("Error processing AI message: {}", e.getMessage(), e);
            return "I apologize, but I'm having trouble processing your request right now. Please try again later.";
        }
    }

    /**
     * Process message using Gemini API
     */
    private String processWithGeminiApi(String userMessage, int userId) {
        try {
            logger.info("Attempting to use Gemini API for user message: {}", userMessage);

            // Gather context data about the user and their tasks
            String contextData = buildContextData(userId);
            logger.debug("Built context data: {}", contextData);

            // Generate response using Gemini API
            String response = geminiApiService.generateResponse(userMessage, contextData);

            logger.info("Successfully generated response using Gemini API for user {}", userId);
            return response;

        } catch (IOException e) {
            logger.error("Error calling Gemini API, falling back to simulated response. Error: {}", e.getMessage(), e);
            return processWithFallback(userMessage, userId);
        } catch (Exception e) {
            logger.error("Unexpected error with Gemini API: {}", e.getMessage(), e);
            return processWithFallback(userMessage, userId);
        }
    }

    /**
     * Build context data for the AI about the user's current situation
     */
    private String buildContextData(int userId) {
        try {
            StringBuilder context = new StringBuilder();

            // Get user information
            Optional<User> userOpt = userService.getUserById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                context.append("User: ").append(user.getFullName())
                       .append(" (").append(user.getRole()).append(")\n");
            }

            // Get user's tasks
            List<Task> userTasks = taskService.getTasksByUserId(userId);

            if (!userTasks.isEmpty()) {
                context.append("Current Tasks Summary:\n");

                // Task statistics
                long totalTasks = userTasks.size();
                long completedTasks = userTasks.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count();
                long inProgressTasks = userTasks.stream().filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS).count();
                long todoTasks = userTasks.stream().filter(task -> task.getStatus() == TaskStatus.TODO).count();
                long overdueTasks = userTasks.stream().filter(Task::isOverdue).count();
                long highPriorityTasks = userTasks.stream()
                    .filter(task -> task.getPriority() == TaskPriority.HIGH && task.getStatus() != TaskStatus.COMPLETED)
                    .count();

                context.append("- Total tasks: ").append(totalTasks).append("\n");
                context.append("- Completed: ").append(completedTasks).append("\n");
                context.append("- In Progress: ").append(inProgressTasks).append("\n");
                context.append("- To Do: ").append(todoTasks).append("\n");
                context.append("- Overdue: ").append(overdueTasks).append("\n");
                context.append("- High Priority (incomplete): ").append(highPriorityTasks).append("\n");

                if (totalTasks > 0) {
                    double completionRate = (double) completedTasks / totalTasks * 100;
                    context.append("- Completion rate: ").append(String.format("%.1f%%", completionRate)).append("\n");
                }

                // Recent tasks
                context.append("\nRecent tasks:\n");
                userTasks.stream()
                    .sorted((t1, t2) -> t2.getUpdatedAt().compareTo(t1.getUpdatedAt()))
                    .limit(5)
                    .forEach(task -> {
                        context.append("- ").append(task.getTitle())
                               .append(" (").append(task.getStatus())
                               .append(", ").append(task.getPriority())
                               .append(")");
                        if (task.getDueDate() != null) {
                            context.append(" - Due: ").append(task.getDueDate().format(DateTimeFormatter.ofPattern("MMM dd")));
                        }
                        context.append("\n");
                    });

                // Upcoming deadlines
                long upcomingDeadlines = userTasks.stream()
                    .filter(task -> task.getDueDate() != null &&
                           task.getDueDate().isAfter(LocalDateTime.now()) &&
                           task.getDueDate().isBefore(LocalDateTime.now().plusDays(7)) &&
                           task.getStatus() != TaskStatus.COMPLETED)
                    .count();

                if (upcomingDeadlines > 0) {
                    context.append("\nUpcoming deadlines (next 7 days): ").append(upcomingDeadlines).append("\n");
                }

            } else {
                context.append("No tasks currently assigned.\n");
            }

            context.append("\nCurrent time: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));

            return context.toString();

        } catch (Exception e) {
            logger.error("Error building context data for user {}", userId, e);
            return "Unable to gather current task context.";
        }
    }

    /**
     * Process message with fallback simulated responses
     */
    private String processWithFallback(String userMessage, int userId) {
        String normalizedMessage = userMessage.toLowerCase().trim();

        // Handle different types of queries with simulated responses
        if (isGreeting(normalizedMessage)) {
            return getRandomFallbackGreeting();
        } else if (isTaskQuery(normalizedMessage)) {
            return handleTaskQueryFallback(normalizedMessage, userId);
        } else if (isStatusQuery(normalizedMessage)) {
            return handleStatusQueryFallback(userId);
        } else if (isSuggestionRequest(normalizedMessage)) {
            return generateTaskSuggestionsFallback(userId);
        } else if (isHelpRequest(normalizedMessage)) {
            return getHelpResponseFallback();
        } else {
            return generateContextualResponseFallback(userMessage, userId);
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

    /**
     * Check if Gemini API is available
     */
    public boolean isGeminiApiAvailable() {
        return geminiApiService != null && geminiApiService.isApiAvailable();
    }

    /**
     * Get API status information
     */
    public String getApiStatus() {
        if (isGeminiApiAvailable()) {
            return "✅ Connected to Gemini API";
        } else {
            return "⚠️ Using fallback responses (Gemini API not configured)";
        }
    }

    /**
     * Test API connection directly
     */
    public String testApiDirectly() {
        try {
            logger.info("Direct API test initiated");

            if (!geminiApiService.isApiAvailable()) {
                return "❌ API not configured. Check your API key in gemini.properties";
            }

            String result = geminiApiService.testApiConnection();
            return "✅ API Test Successful!\nResponse: " + result;

        } catch (Exception e) {
            logger.error("Direct API test failed", e);
            return "❌ API Test Failed: " + e.getMessage();
        }
    }

    // Helper methods for message classification
    private boolean isGreeting(String message) {
        return message.contains("hello") || message.contains("hi") ||
               message.contains("hey") || message.contains("good morning") ||
               message.contains("good afternoon") || message.contains("good evening");
    }

    private boolean isTaskQuery(String message) {
        return message.contains("task") || message.contains("todo") ||
               message.contains("assignment") || message.contains("work") ||
               message.contains("project") || message.contains("deadline");
    }

    private boolean isStatusQuery(String message) {
        return message.contains("status") || message.contains("progress") ||
               message.contains("how am i doing") || message.contains("summary") ||
               message.contains("overview");
    }

    private boolean isSuggestionRequest(String message) {
        return message.contains("suggest") || message.contains("recommend") ||
               message.contains("advice") || message.contains("tip") ||
               message.contains("help me") || message.contains("what should");
    }

    private boolean isHelpRequest(String message) {
        return message.contains("help") || message.contains("how to") ||
               message.contains("explain") || message.contains("guide") ||
               message.contains("tutorial");
    }

    private String getRandomFallbackGreeting() {
        return FALLBACK_GREETINGS[random.nextInt(FALLBACK_GREETINGS.length)];
    }

    /**
     * Handle task-related queries with fallback responses
     */
    private String handleTaskQueryFallback(String message, int userId) {
        try {
            List<Task> userTasks = taskService.getTasksByUserId(userId);

            if (message.contains("overdue")) {
                long overdueCount = userTasks.stream().filter(Task::isOverdue).count();
                return String.format("You have %d overdue task(s). I recommend addressing these first to get back on track.", overdueCount);
            } else if (message.contains("today") || message.contains("due")) {
                long todayTasks = userTasks.stream()
                    .filter(task -> task.getDueDate() != null &&
                           task.getDueDate().toLocalDate().equals(LocalDateTime.now().toLocalDate()))
                    .count();
                return String.format("You have %d task(s) due today. Would you like me to help you prioritize them?", todayTasks);
            } else if (message.contains("completed")) {
                long completedCount = userTasks.stream()
                    .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                    .count();
                return String.format("Great job! You've completed %d task(s). Keep up the excellent work!", completedCount);
            } else {
                return String.format("You currently have %d active task(s). Would you like me to help you organize or prioritize them?", userTasks.size());
            }

        } catch (Exception e) {
            logger.error("Error handling task query", e);
            return "I'm having trouble accessing your task information right now. Please try again later.";
        }
    }

    /**
     * Handle status queries with fallback responses
     */
    private String handleStatusQueryFallback(int userId) {
        try {
            List<Task> userTasks = taskService.getTasksByUserId(userId);

            long totalTasks = userTasks.size();
            long completedTasks = userTasks.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count();
            long inProgressTasks = userTasks.stream().filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS).count();
            long overdueTasks = userTasks.stream().filter(Task::isOverdue).count();

            StringBuilder status = new StringBuilder();
            status.append("📊 Here's your current status:\n\n");
            status.append(String.format("• Total Tasks: %d\n", totalTasks));
            status.append(String.format("• Completed: %d\n", completedTasks));
            status.append(String.format("• In Progress: %d\n", inProgressTasks));
            status.append(String.format("• Overdue: %d\n", overdueTasks));

            if (totalTasks > 0) {
                double completionRate = (double) completedTasks / totalTasks * 100;
                status.append(String.format("• Completion Rate: %.1f%%\n", completionRate));
            }

            if (overdueTasks > 0) {
                status.append("\n⚠️ You have overdue tasks that need attention!");
            } else if (completedTasks > inProgressTasks) {
                status.append("\n🎉 You're doing great! Keep up the momentum!");
            }

            return status.toString();

        } catch (Exception e) {
            logger.error("Error generating status query", e);
            return "I'm having trouble generating your status report right now. Please try again later.";
        }
    }

    /**
     * Generate task suggestions with fallback responses
     */
    private String generateTaskSuggestionsFallback(int userId) {
        try {
            List<Task> userTasks = taskService.getTasksByUserId(userId);

            if (userTasks.isEmpty()) {
                return "You don't have any tasks yet. I'm here to help you with task management, productivity tips, and workflow optimization whenever you need assistance!";
            }

            StringBuilder suggestions = new StringBuilder();
            suggestions.append("💡 Here are some personalized suggestions:\n\n");

            // Check for overdue tasks
            long overdueCount = userTasks.stream().filter(Task::isOverdue).count();
            if (overdueCount > 0) {
                suggestions.append("• Focus on your overdue tasks first to get back on schedule\n");
            }

            // Check for high priority tasks
            long highPriorityCount = userTasks.stream()
                .filter(task -> task.getPriority() == TaskPriority.HIGH && task.getStatus() != TaskStatus.COMPLETED)
                .count();
            if (highPriorityCount > 0) {
                suggestions.append("• Prioritize your high-priority tasks for maximum impact\n");
            }

            // Check workload
            if (userTasks.size() > 10) {
                suggestions.append("• Consider delegating some tasks to balance your workload\n");
            }

            // Add a random general suggestion
            suggestions.append("• ").append(FALLBACK_SUGGESTIONS[random.nextInt(FALLBACK_SUGGESTIONS.length)]);

            return suggestions.toString();

        } catch (Exception e) {
            logger.error("Error generating task suggestions", e);
            return "I'm having trouble generating suggestions right now. Please try again later.";
        }
    }

    /**
     * Get help response with fallback
     */
    private String getHelpResponseFallback() {
        return """
            🤖 I'm your AI assistant! Here's what I can help you with:

            • Task Management: Ask about your tasks, deadlines, and progress
            • Status Updates: Get summaries of your work status
            • Suggestions: Receive personalized productivity tips
            • Planning: Help organize and prioritize your tasks

            Try asking me things like:
            - "How am I doing?"
            - "What tasks are due today?"
            - "Give me some suggestions"
            - "Show my overdue tasks"

            I'm here to help you be more productive! 🚀
            """;
    }

    /**
     * Generate contextual response for general queries with fallback
     */
    private String generateContextualResponseFallback(String message, int userId) {
        String lowerMessage = message.toLowerCase();

        // Productivity tips
        if (lowerMessage.contains("productivity") || lowerMessage.contains("productive")) {
            return "💡 Here are some productivity tips:\n\n" +
                   "• Break large tasks into smaller, manageable chunks\n" +
                   "• Use the Pomodoro Technique (25 min work, 5 min break)\n" +
                   "• Prioritize tasks by importance and urgency\n" +
                   "• Set clear deadlines and stick to them\n" +
                   "• Take regular breaks to maintain focus\n" +
                   "• Celebrate your achievements, no matter how small!";
        }

        // Time management
        else if (lowerMessage.contains("time") || lowerMessage.contains("manage") || lowerMessage.contains("schedule")) {
            return "⏰ Time management strategies:\n\n" +
                   "• Plan your day the night before\n" +
                   "• Use time-blocking for focused work\n" +
                   "• Identify your most productive hours\n" +
                   "• Eliminate distractions during work time\n" +
                   "• Learn to say 'no' to non-essential tasks\n" +
                   "• Review and adjust your schedule regularly";
        }

        // Stress management
        else if (lowerMessage.contains("stress") || lowerMessage.contains("overwhelm") || lowerMessage.contains("pressure")) {
            return "🧘 Managing stress and overwhelm:\n\n" +
                   "• Take deep breaths and pause when feeling overwhelmed\n" +
                   "• Break down big problems into smaller steps\n" +
                   "• Focus on what you can control\n" +
                   "• Don't hesitate to ask for help when needed\n" +
                   "• Remember: progress, not perfection\n" +
                   "• You've got this! 💪";
        }

        // Motivation and encouragement
        else if (lowerMessage.contains("motivat") || lowerMessage.contains("inspire") || lowerMessage.contains("encourage")) {
            return "🌟 You're doing great! Here's some motivation:\n\n" +
                   "• Every small step forward is progress\n" +
                   "• Your effort today shapes your success tomorrow\n" +
                   "• Challenges are opportunities to grow\n" +
                   "• Focus on progress, not perfection\n" +
                   "• You have the power to achieve your goals!\n\n" +
                   "Keep pushing forward! 🚀";
        }

        // Work-life balance
        else if (lowerMessage.contains("balance") || lowerMessage.contains("life") || lowerMessage.contains("rest")) {
            return "⚖️ Work-life balance tips:\n\n" +
                   "• Set clear boundaries between work and personal time\n" +
                   "• Take regular breaks throughout the day\n" +
                   "• Make time for activities you enjoy\n" +
                   "• Get enough sleep and exercise\n" +
                   "• Disconnect from work during off-hours\n" +
                   "• Remember: you're more than your work!";
        }

        // Default intelligent response
        else {
            try {
                // Try to provide task-related insights even for general queries
                List<Task> userTasks = taskService.getTasksByUserId(userId);
                if (!userTasks.isEmpty()) {
                    long pendingTasks = userTasks.stream()
                        .filter(task -> task.getStatus() != TaskStatus.COMPLETED)
                        .count();

                    return "🤖 I'm here to help with your workflow! I see you have " + pendingTasks +
                           " pending tasks. Here are some things I can help you with:\n\n" +
                           "• 'How am I doing?' - Get your productivity overview\n" +
                           "• 'What tasks are due today?' - Check deadlines\n" +
                           "• 'Give me suggestions' - Get personalized tips\n" +
                           "• 'Show my overdue tasks' - See what needs attention\n\n" +
                           "Feel free to ask me anything about task management or productivity!";
                } else {
                    return "🤖 Welcome! I'm your AI assistant for task management and productivity.\n\n" +
                           "I can help you with:\n" +
                           "• Task analysis and insights\n" +
                           "• Productivity tips and strategies\n" +
                           "• Time management advice\n" +
                           "• Workflow optimization\n\n" +
                           "Try asking me 'How can I be more productive?' or 'Give me some tips!'";
                }
            } catch (Exception e) {
                logger.error("Error in fallback response generation", e);
                return "🤖 I'm your AI assistant for WorkFlow Manager! I can help you with task management, productivity tips, and workflow optimization. Try asking me about your tasks or for suggestions!";
            }
        }
    }
}
