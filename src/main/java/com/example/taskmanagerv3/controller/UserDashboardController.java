package com.example.taskmanagerv3.controller;

import com.example.taskmanagerv3.controller.UpdateProgressController;
import com.example.taskmanagerv3.model.Task;
import com.example.taskmanagerv3.model.TaskStatus;
import com.example.taskmanagerv3.model.TaskPriority;
import com.example.taskmanagerv3.model.User;
import com.example.taskmanagerv3.service.TaskService;

import com.example.taskmanagerv3.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.stage.Stage;
import javafx.stage.Modality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Controller for User Dashboard
 */
public class UserDashboardController {
    private static final Logger logger = LoggerFactory.getLogger(UserDashboardController.class);

    // Header components
    @FXML private Label welcomeLabel;
    @FXML private Label currentTimeLabel;
    @FXML private Button logoutButton;

    // Statistics cards
    @FXML private Label myTasksLabel;
    @FXML private Label completedTasksLabel;
    @FXML private Label pendingTasksLabel;
    @FXML private Label overdueTasksLabel;

    // Navigation buttons
    @FXML private Button myTasksButton;
    @FXML private Button calendarButton;
    @FXML private Button profileButton;
    @FXML private Button chatButton;
    @FXML private Button aiChatbotButton;

    // Main content area
    @FXML private VBox mainContentArea;

    // Charts components (replaced Quick Actions)
    @FXML private ProgressBar completedProgressBar;
    @FXML private ProgressBar inProgressBar;
    @FXML private ProgressBar pendingProgressBar;
    @FXML private ProgressBar overdueProgressBar;

    // Progress chart percentage labels
    @FXML private Label completedPercentLabel;
    @FXML private Label inProgressPercentLabel;
    @FXML private Label pendingPercentLabel;
    @FXML private Label overduePercentLabel;

    // Weekly Activity Chart components
    @FXML private VBox mondayBar;
    @FXML private VBox tuesdayBar;
    @FXML private VBox wednesdayBar;
    @FXML private VBox thursdayBar;
    @FXML private VBox fridayBar;
    @FXML private VBox saturdayBar;
    @FXML private VBox sundayBar;

    @FXML private Label mondayCountLabel;
    @FXML private Label tuesdayCountLabel;
    @FXML private Label wednesdayCountLabel;
    @FXML private Label thursdayCountLabel;
    @FXML private Label fridayCountLabel;
    @FXML private Label saturdayCountLabel;
    @FXML private Label sundayCountLabel;

    // My tasks list
    @FXML private ListView<String> myTasksListView;

    // Recent activities
    @FXML private ListView<String> recentActivitiesListView;

    // Dynamic content containers
    @FXML private VBox mainContentContainer;
    @FXML private VBox dashboardContent;
    @FXML private VBox taskManagementContent;
    @FXML private VBox calendarContent;
    @FXML private VBox profileContent;
    @FXML private VBox chatContent;
    @FXML private VBox aiChatbotContent;

    // Cards
    @FXML private VBox myTasksCard;

    private TaskService taskService;
    private SessionManager sessionManager;

    // Auto-refresh timer
    private Thread autoRefreshThread;

    // Calendar fields
    private YearMonth currentMonth;
    private LocalDate selectedDate;
    private List<Task> allTasks;

    // Calendar view options
    private CheckBox showCompletedCheckBox;
    private CheckBox showOverdueCheckBox;
    private CheckBox showFutureCheckBox;

    // Calendar summary labels
    private Label totalTasksThisMonthLabel;
    private Label completedTasksThisMonthLabel;
    private Label pendingTasksThisMonthLabel;
    private Label overdueTasksThisMonthLabel;

    // Calendar components references
    private VBox calendarArea;
    private VBox selectedDateDetailsContainer;
    private Label currentMonthHeaderLabel;

    @FXML
    private void initialize() {
        taskService = new TaskService();
        sessionManager = SessionManager.getInstance();

        // Initialize calendar fields
        currentMonth = YearMonth.now();
        selectedDate = LocalDate.now();



        setupUI();
        initializeCharts();
        loadDashboardData();
        startTimeUpdater();
        startAutoRefresh();

        logger.info("User dashboard initialized for user: {}",
                   sessionManager.getCurrentUsername());
    }

    /**
     * Setup UI components
     */
    private void setupUI() {
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser != null) {
            welcomeLabel.setText("Welcome, " + currentUser.getDisplayName());
        }

        // Setup button actions
        myTasksButton.setOnAction(e -> showMyTasks());
        calendarButton.setOnAction(e -> showCalendar());
        profileButton.setOnAction(e -> showProfile());
        chatButton.setOnAction(e -> showChat());
        aiChatbotButton.setOnAction(e -> showAIChatbot());

        logoutButton.setOnAction(e -> handleLogout());
    }

    /**
     * Initialize charts with default values
     */
    private void initializeCharts() {
        // Initialize progress bars to 0%
        updateProgressCharts(0, 0, 0, 0, 0);

        // Initialize weekly activity chart with 0 values
        VBox[] bars = {
            mondayBar, tuesdayBar, wednesdayBar,
            thursdayBar, fridayBar, saturdayBar, sundayBar
        };

        Label[] countLabels = {
            mondayCountLabel, tuesdayCountLabel, wednesdayCountLabel,
            thursdayCountLabel, fridayCountLabel, saturdayCountLabel, sundayCountLabel
        };

        for (int i = 0; i < 7; i++) {
            if (bars[i] != null) {
                bars[i].setPrefHeight(0.0);
                bars[i].setMinHeight(0.0);
                bars[i].setMaxHeight(0.0);
            }
            if (countLabels[i] != null) {
                countLabels[i].setText("0");
            }
        }

        logger.debug("Charts initialized with default values");
    }

    /**
     * Load dashboard statistics and data
     */
    private void loadDashboardData() {
        // Load in background thread to avoid blocking UI
        new Thread(() -> {
            try {
                int currentUserId = sessionManager.getCurrentUserId();

                // Get user's tasks
                java.util.List<Task> userTasks = taskService.getTasksByUserId(currentUserId);

                // Calculate statistics
                int myTasks = userTasks.size();
                int completedTasks = (int) userTasks.stream()
                    .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                    .count();
                int overdueTasks = (int) userTasks.stream()
                    .filter(Task::isOverdue)
                    .count();
                int inProgressTasks = (int) userTasks.stream()
                    .filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS || task.getStatus() == TaskStatus.REVIEW)
                    .count();
                int pendingTasks = (int) userTasks.stream()
                    .filter(task -> task.getStatus() == TaskStatus.TODO && !task.isOverdue())
                    .count();

                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    myTasksLabel.setText(String.valueOf(myTasks));
                    completedTasksLabel.setText(String.valueOf(completedTasks));
                    pendingTasksLabel.setText(String.valueOf(pendingTasks));
                    overdueTasksLabel.setText(String.valueOf(overdueTasks));

                    // Update charts with real data
                    updateProgressCharts(myTasks, completedTasks, inProgressTasks, pendingTasks, overdueTasks);

                    // Update weekly activity chart
                    updateWeeklyActivityChart(currentUserId);

                    loadMyTasks(userTasks);
                    loadRecentActivities(userTasks);
                });

            } catch (Exception e) {
                logger.error("Error loading dashboard data", e);
                Platform.runLater(() -> {
                    showAlert("Error", "Failed to load dashboard data: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Load user's tasks
     */
    private void loadMyTasks(java.util.List<Task> userTasks) {
        myTasksListView.getItems().clear();

        if (userTasks.isEmpty()) {
            myTasksListView.getItems().addAll(
                "🎉 No tasks assigned yet!",
                "💼 You're all caught up!",
                "📞 Contact your admin for new assignments"
            );
        } else {
            // Show recent and important tasks
            userTasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.COMPLETED)
                .sorted((t1, t2) -> {
                    // Sort by priority and due date
                    int priorityCompare = Integer.compare(t2.getPriority().getLevel(), t1.getPriority().getLevel());
                    if (priorityCompare != 0) return priorityCompare;

                    if (t1.getDueDate() == null && t2.getDueDate() == null) return 0;
                    if (t1.getDueDate() == null) return 1;
                    if (t2.getDueDate() == null) return -1;
                    return t1.getDueDate().compareTo(t2.getDueDate());
                })
                .limit(5)
                .forEach(task -> {
                    String taskDisplay = formatTaskDisplay(task);
                    myTasksListView.getItems().add(taskDisplay);
                });
        }
    }

    /**
     * Load recent activities
     */
    private void loadRecentActivities(java.util.List<Task> userTasks) {
        recentActivitiesListView.getItems().clear();

        // Add login activity
        recentActivitiesListView.getItems().add("🔐 Logged in successfully");

        if (!userTasks.isEmpty()) {
            // Show recent task activities
            userTasks.stream()
                .filter(task -> task.getUpdatedAt() != null)
                .sorted((t1, t2) -> t2.getUpdatedAt().compareTo(t1.getUpdatedAt()))
                .limit(4)
                .forEach(task -> {
                    String activityIcon = getActivityIcon("updated");
                    String timeAgo = formatRelativeTime(task.getUpdatedAt());

                    String activity = String.format("%s %s - %s",
                        activityIcon, task.getTitle(), timeAgo);

                    recentActivitiesListView.getItems().add(activity);
                });
        }

        // Add welcome message
        recentActivitiesListView.getItems().add("🎯 Ready to be productive!");
    }

    /**
     * Start time updater thread
     */
    private void startTimeUpdater() {
        Thread timeUpdater = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Platform.runLater(() -> {
                        currentTimeLabel.setText(getEnhancedTimeDisplay());
                    });
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        timeUpdater.setDaemon(true);
        timeUpdater.start();
    }

    /**
     * Start auto-refresh thread for dashboard data
     */
    private void startAutoRefresh() {
        autoRefreshThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Wait 30 seconds between refreshes
                    Thread.sleep(30000);

                    Platform.runLater(() -> {
                        try {
                            // Refresh dashboard data if we're on dashboard view
                            if (dashboardContent.isVisible()) {
                                loadDashboardData();
                                logger.debug("Auto-refreshed dashboard data");
                            }
                            // Refresh task management data if we're on task management view
                            else if (taskManagementContent.isVisible()) {
                                refreshTaskManagementContent();
                                logger.debug("Auto-refreshed task management data");
                            }
                            // Refresh calendar data if we're on calendar view
                            else if (calendarContent.isVisible()) {
                                loadCalendarTasks();
                                logger.debug("Auto-refreshed calendar data");
                            }
                            // Profile content doesn't need auto-refresh as it's mostly static
                        } catch (Exception e) {
                            logger.warn("Error during auto-refresh: {}", e.getMessage());
                        }
                    });

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        autoRefreshThread.setDaemon(true);
        autoRefreshThread.start();

        logger.info("Auto-refresh started with 30-second interval");
    }



    /**
     * Show my tasks interface inline in dashboard
     */
    private void showMyTasksInline() {
        try {
            // Hide all other content first
            dashboardContent.setVisible(false);
            dashboardContent.setManaged(false);
            calendarContent.setVisible(false);
            calendarContent.setManaged(false);
            profileContent.setVisible(false);
            profileContent.setManaged(false);
            chatContent.setVisible(false);
            chatContent.setManaged(false);
            aiChatbotContent.setVisible(false);
            aiChatbotContent.setManaged(false);

            // Load task management content
            loadTaskManagementContent();

            // Show task management content
            taskManagementContent.setVisible(true);
            taskManagementContent.setManaged(true);

            // Force layout refresh to prevent display issues
            Platform.runLater(() -> {
                mainContentContainer.requestLayout();
                taskManagementContent.requestLayout();
            });

            logger.info("Switched to inline task management view");

        } catch (Exception e) {
            logger.error("Failed to load inline task management", e);
            showAlert("Error", "Failed to load task management: " + e.getMessage());
        }
    }

    /**
     * Show my tasks interface (original method for button)
     */
    @FXML
    private void showMyTasks() {
        // Use inline view instead of opening new window
        showMyTasksInline();
    }

    /**
     * Show calendar interface inline in dashboard
     */
    private void showCalendarInline() {
        try {
            // Hide all other content first
            dashboardContent.setVisible(false);
            dashboardContent.setManaged(false);
            taskManagementContent.setVisible(false);
            taskManagementContent.setManaged(false);
            profileContent.setVisible(false);
            profileContent.setManaged(false);
            chatContent.setVisible(false);
            chatContent.setManaged(false);
            aiChatbotContent.setVisible(false);
            aiChatbotContent.setManaged(false);

            // Load calendar content
            loadCalendarContent();

            // Show calendar content
            calendarContent.setVisible(true);
            calendarContent.setManaged(true);

            // Force layout refresh to prevent display issues
            Platform.runLater(() -> {
                mainContentContainer.requestLayout();
                calendarContent.requestLayout();
            });

            logger.info("Switched to inline calendar view");

        } catch (Exception e) {
            logger.error("Failed to load inline calendar", e);
            showAlert("Error", "Failed to load calendar: " + e.getMessage());
        }
    }

    /**
     * Show calendar interface (original method for button)
     */
    @FXML
    private void showCalendar() {
        // Use inline view instead of opening new window
        showCalendarInline();
    }

    /**
     * Show profile interface inline in dashboard
     */
    private void showProfileInline() {
        try {
            // Hide other content
            dashboardContent.setVisible(false);
            dashboardContent.setManaged(false);
            taskManagementContent.setVisible(false);
            taskManagementContent.setManaged(false);
            calendarContent.setVisible(false);
            calendarContent.setManaged(false);
            chatContent.setVisible(false);
            chatContent.setManaged(false);
            aiChatbotContent.setVisible(false);
            aiChatbotContent.setManaged(false);

            // Load profile content
            loadProfileContent();

            profileContent.setVisible(true);
            profileContent.setManaged(true);

            logger.info("Switched to inline profile view");

        } catch (Exception e) {
            logger.error("Failed to load inline profile", e);
            showAlert("Error", "Failed to load profile: " + e.getMessage());
        }
    }

    /**
     * Show profile interface (original method for button)
     */
    @FXML
    private void showProfile() {
        // Use inline view instead of opening new window
        showProfileInline();
    }



    /**
     * Show chat interface inline in dashboard
     */
    private void showChatInline() {
        try {
            // Hide other content
            dashboardContent.setVisible(false);
            dashboardContent.setManaged(false);
            taskManagementContent.setVisible(false);
            taskManagementContent.setManaged(false);
            calendarContent.setVisible(false);
            calendarContent.setManaged(false);
            profileContent.setVisible(false);
            profileContent.setManaged(false);
            aiChatbotContent.setVisible(false);
            aiChatbotContent.setManaged(false);

            // Load chat content
            loadChatContent();

            chatContent.setVisible(true);
            chatContent.setManaged(true);

            logger.info("Switched to inline chat view");

        } catch (Exception e) {
            logger.error("Failed to load inline chat", e);
            showAlert("Error", "Failed to load chat: " + e.getMessage());
        }
    }

    /**
     * Show chat interface (original method for button)
     */
    @FXML
    private void showChat() {
        // Use inline view instead of opening new window
        showChatInline();
    }

    /**
     * Show AI Chatbot interface inline in dashboard
     */
    private void showAIChatbotInline() {
        try {
            // Hide other content
            dashboardContent.setVisible(false);
            dashboardContent.setManaged(false);
            taskManagementContent.setVisible(false);
            taskManagementContent.setManaged(false);
            calendarContent.setVisible(false);
            calendarContent.setManaged(false);
            profileContent.setVisible(false);
            profileContent.setManaged(false);
            chatContent.setVisible(false);
            chatContent.setManaged(false);

            // Load AI chatbot content
            loadAIChatbotContent();

            aiChatbotContent.setVisible(true);
            aiChatbotContent.setManaged(true);

            // Force layout refresh to prevent display issues
            Platform.runLater(() -> {
                mainContentContainer.requestLayout();
                aiChatbotContent.requestLayout();
            });

            logger.info("Switched to inline AI chatbot view");

        } catch (Exception e) {
            logger.error("Failed to load inline AI chatbot", e);
            showAlert("Error", "Failed to load AI chatbot: " + e.getMessage());
        }
    }

    /**
     * Show AI Chatbot interface (original method for button)
     */
    @FXML
    private void showAIChatbot() {
        // Use inline view instead of opening new window
        showAIChatbotInline();
    }

    /**
     * Update progress charts with real data
     */
    private void updateProgressCharts(int totalTasks, int completedTasks, int inProgressTasks, int pendingTasks, int overdueTasks) {
        if (totalTasks == 0) {
            // No tasks - set all progress bars to 0%
            updateProgressBar(completedProgressBar, 0);
            updateProgressBar(inProgressBar, 0);
            updateProgressBar(pendingProgressBar, 0);
            updateProgressBar(overdueProgressBar, 0);

            // Update labels to show 0%
            updateProgressLabels(0, 0, 0, 0);
            return;
        }

        // Calculate percentages
        double completedPercentage = (double) completedTasks / totalTasks * 100;
        double inProgressPercentage = (double) inProgressTasks / totalTasks * 100;
        double pendingPercentage = (double) pendingTasks / totalTasks * 100;
        double overduePercentage = (double) overdueTasks / totalTasks * 100;

        // Update progress bars
        updateProgressBar(completedProgressBar, completedPercentage);
        updateProgressBar(inProgressBar, inProgressPercentage);
        updateProgressBar(pendingProgressBar, pendingPercentage);
        updateProgressBar(overdueProgressBar, overduePercentage);

        // Update percentage labels in the UI
        updateProgressLabels(completedPercentage, inProgressPercentage, pendingPercentage, overduePercentage);

        logger.info("Updated progress charts - Total: {}, Completed: {}%, In Progress: {}%, Pending: {}%, Overdue: {}%",
                   totalTasks, Math.round(completedPercentage), Math.round(inProgressPercentage),
                   Math.round(pendingPercentage), Math.round(overduePercentage));
    }

    /**
     * Update individual progress bar
     */
    private void updateProgressBar(ProgressBar progressBar, double percentage) {
        if (progressBar != null) {
            try {
                // Convert percentage to progress value (0.0 to 1.0)
                double progress = Math.max(0.0, Math.min(1.0, percentage / 100.0));

                // Set the progress value
                progressBar.setProgress(progress);

                logger.debug("Updated progress bar: {}% -> progress: {}", percentage, progress);

            } catch (Exception e) {
                logger.warn("Error updating progress bar: {}", e.getMessage());
            }
        }
    }

    /**
     * Update percentage labels in the progress chart
     */
    private void updateProgressLabels(double completed, double inProgress, double pending, double overdue) {
        try {
            // Update percentage labels directly using FXML references
            if (completedPercentLabel != null) {
                completedPercentLabel.setText(Math.round(completed) + "%");
            }
            if (inProgressPercentLabel != null) {
                inProgressPercentLabel.setText(Math.round(inProgress) + "%");
            }
            if (pendingPercentLabel != null) {
                pendingPercentLabel.setText(Math.round(pending) + "%");
            }
            if (overduePercentLabel != null) {
                overduePercentLabel.setText(Math.round(overdue) + "%");
            }
        } catch (Exception e) {
            logger.warn("Could not update percentage labels: {}", e.getMessage());
        }

        logger.info("Progress percentages - Completed: {}%, In Progress: {}%, Pending: {}%, Overdue: {}%",
                   Math.round(completed), Math.round(inProgress), Math.round(pending), Math.round(overdue));
    }

    /**
     * Handle logout
     */
    @FXML
    private void handleLogout() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Logout Confirmation");
        confirmAlert.setHeaderText("Are you sure you want to logout?");
        confirmAlert.setContentText("You will be redirected to the login screen.");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Clear session
                sessionManager.logout();

                // Navigate back to login
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/taskmanagerv3/login-view.fxml"));
                    Scene scene = new Scene(loader.load());

                    Stage currentStage = (Stage) logoutButton.getScene().getWindow();
                    currentStage.setTitle("WorkFlow Manager - Login");
                    currentStage.setScene(scene);
                    currentStage.setResizable(false);
                    currentStage.centerOnScreen();
                    currentStage.setMaximized(false);

                    logger.info("User logged out successfully");

                } catch (IOException e) {
                    logger.error("Failed to load login screen", e);
                    showAlert("Error", "Failed to return to login screen: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Show alert dialog
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Refresh dashboard data
     */
    @FXML
    private void refreshDashboard() {
        loadDashboardData();
        showAlert("Refresh", "Dashboard data refreshed successfully!");
    }

    /**
     * Get status icon for task
     */
    private String getStatusIcon(TaskStatus status) {
        return switch (status) {
            case TODO -> "[ ]";
            case IN_PROGRESS -> "[~]";
            case REVIEW -> "[?]";
            case COMPLETED -> "[✓]";
            case CANCELLED -> "[X]";
        };
    }

    /**
     * Get priority icon for task
     */
    private String getPriorityIcon(String priority) {
        if (priority == null) return "[!]";
        return switch (priority.toUpperCase()) {
            case "HIGH", "URGENT" -> "[!!!]";
            case "MEDIUM", "NORMAL" -> "[!!]";
            case "LOW" -> "[!]";
            default -> "[!]";
        };
    }

    /**
     * Get progress icon based on completion percentage
     */
    private String getProgressIcon(int progressPercentage) {
        if (progressPercentage >= 100) return "[100%]";
        if (progressPercentage >= 75) return "[75%+]";
        if (progressPercentage >= 50) return "[50%+]";
        if (progressPercentage >= 25) return "[25%+]";
        if (progressPercentage > 0) return "[<25%]";
        return "[0%]";
    }

    /**
     * Get category icon for task
     */
    private String getCategoryIcon(String category) {
        if (category == null) return "[FILE]";
        return switch (category.toLowerCase()) {
            case "development", "coding" -> "[CODE]";
            case "design", "ui/ux" -> "[DESIGN]";
            case "testing", "qa" -> "[TEST]";
            case "meeting", "discussion" -> "[MEET]";
            case "documentation" -> "[DOC]";
            case "bug", "issue" -> "[BUG]";
            case "feature" -> "[FEAT]";
            case "research" -> "[RESEARCH]";
            case "deployment" -> "[DEPLOY]";
            default -> "[FILE]";
        };
    }

    /**
     * Get time-related icon based on urgency
     */
    private String getTimeIcon(LocalDateTime dueDate) {
        if (dueDate == null) return "[SCHED]";

        LocalDateTime now = LocalDateTime.now();
        long hoursUntilDue = ChronoUnit.HOURS.between(now, dueDate);

        if (hoursUntilDue < 0) return "[OVERDUE]"; // Overdue
        if (hoursUntilDue <= 2) return "[URGENT]"; // Due very soon
        if (hoursUntilDue <= 24) return "[TODAY]"; // Due today
        if (hoursUntilDue <= 72) return "[SOON]"; // Due in 3 days
        return "[SCHED]"; // Due later
    }

    /**
     * Format relative time (e.g., "2 hours ago", "yesterday")
     */
    private String formatRelativeTime(LocalDateTime dateTime) {
        if (dateTime == null) return "Unknown";

        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        long hours = ChronoUnit.HOURS.between(dateTime, now);
        long days = ChronoUnit.DAYS.between(dateTime, now);

        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
        if (hours < 24) return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        if (days == 1) return "Yesterday";
        if (days < 7) return days + " day" + (days == 1 ? "" : "s") + " ago";
        if (days < 30) return (days / 7) + " week" + (days / 7 == 1 ? "" : "s") + " ago";
        if (days < 365) return (days / 30) + " month" + (days / 30 == 1 ? "" : "s") + " ago";
        return (days / 365) + " year" + (days / 365 == 1 ? "" : "s") + " ago";
    }

    /**
     * Format due date with urgency indicators
     */
    private String formatDueDate(LocalDateTime dueDate) {
        if (dueDate == null) return "No due date";

        LocalDateTime now = LocalDateTime.now();
        long hoursUntilDue = ChronoUnit.HOURS.between(now, dueDate);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm");

        String formattedDate = dueDate.format(formatter);

        if (hoursUntilDue < 0) {
            long hoursOverdue = Math.abs(hoursUntilDue);
            return "🚨 OVERDUE (" + hoursOverdue + "h) - " + formattedDate;
        }
        if (hoursUntilDue <= 2) return "⏰ DUE SOON - " + formattedDate;
        if (hoursUntilDue <= 24) return "⏳ DUE TODAY - " + formattedDate;
        if (hoursUntilDue <= 72) return "📅 DUE IN " + (hoursUntilDue / 24) + " DAYS - " + formattedDate;

        return "🗓️ " + formattedDate;
    }

    /**
     * Get time status icon with text
     */
    private String getTimeStatusIcon(LocalDateTime dueDate) {
        if (dueDate == null) return "📅 No deadline";

        LocalDateTime now = LocalDateTime.now();
        long hoursUntilDue = ChronoUnit.HOURS.between(now, dueDate);

        if (hoursUntilDue < 0) return "🚨 Overdue";
        if (hoursUntilDue <= 2) return "⏰ Due very soon";
        if (hoursUntilDue <= 24) return "⏳ Due today";
        if (hoursUntilDue <= 72) return "📅 Due soon";
        return "🗓️ Scheduled";
    }

    /**
     * Format duration in human-readable format
     */
    private String formatDuration(Duration duration) {
        if (duration == null) return "Unknown";

        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();

        if (hours == 0 && minutes == 0) return "< 1 minute";
        if (hours == 0) return minutes + " minute" + (minutes == 1 ? "" : "s");
        if (minutes == 0) return hours + " hour" + (hours == 1 ? "" : "s");
        return hours + "h " + minutes + "m";
    }

    /**
     * Format task display with icons and time information
     */
    private String formatTaskDisplay(Task task) {
        if (task == null) return "Invalid task";

        StringBuilder display = new StringBuilder();

        // Add status icon
        display.append(getStatusIcon(task.getStatus())).append(" ");

        // Add priority icon if available
        if (task.getPriority() != null) {
            display.append(getPriorityIcon(task.getPriority().toString())).append(" ");
        }

        // Add task title
        display.append(task.getTitle());

        // Add due date information
        if (task.getDueDate() != null) {
            display.append(" - ").append(getTimeStatusIcon(task.getDueDate()));
        }

        return display.toString();
    }

    /**
     * Get dynamic task card style based on status and priority
     */
    private String getTaskCardStyle(Task task) {
        if (task == null) return "";

        String baseStyle = "-fx-padding: 10; -fx-margin: 5; -fx-background-radius: 5; ";

        // Style based on status
        String statusStyle = switch (task.getStatus()) {
            case COMPLETED -> "-fx-background-color: #d4edda; -fx-border-color: #27ae60; ";
            case IN_PROGRESS -> "-fx-background-color: #cce5ff; -fx-border-color: #3498db; ";
            case REVIEW -> "-fx-background-color: #fff3cd; -fx-border-color: #f39c12; ";
            case CANCELLED -> "-fx-background-color: #f8d7da; -fx-border-color: #e74c3c; ";
            default -> "-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; ";
        };

        // Add urgency style for overdue tasks
        if (task.isOverdue()) {
            statusStyle += "-fx-border-width: 2; -fx-effect: dropshadow(three-pass-box, rgba(231,76,60,0.3), 5, 0, 0, 0); ";
        }

        return baseStyle + statusStyle;
    }

    /**
     * Get enhanced time display with multiple formats
     */
    private String getEnhancedTimeDisplay() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy");

        return "🕐 " + now.format(timeFormatter) + " | 📅 " + now.format(dateFormatter);
    }

    /**
     * Get activity icon based on activity type
     */
    private String getActivityIcon(String activityType) {
        if (activityType == null) return "📝";
        return switch (activityType.toLowerCase()) {
            case "created" -> "➕";
            case "updated" -> "✏️";
            case "completed" -> "✅";
            case "assigned" -> "👤";
            case "commented" -> "💬";
            case "deleted" -> "🗑️";
            case "started" -> "▶️";
            case "paused" -> "⏸️";
            case "resumed" -> "▶️";
            default -> "📝";
        };
    }

    /**
     * Update Weekly Activity Chart with real data
     */
    private void updateWeeklyActivityChart(int userId) {
        try {
            // Get weekly task creation statistics
            int[] weeklyStats = taskService.getCurrentWeekTaskStats(userId);

            // Find the maximum value for scaling
            int maxTasks = 0;
            for (int count : weeklyStats) {
                if (count > maxTasks) {
                    maxTasks = count;
                }
            }

            // If no tasks, set a minimum scale of 1 to avoid division by zero
            if (maxTasks == 0) {
                maxTasks = 1;
            }

            // Maximum height for bars (in pixels)
            final double MAX_BAR_HEIGHT = 100.0;

            // Array of bars and labels for easy iteration
            VBox[] bars = {
                mondayBar, tuesdayBar, wednesdayBar,
                thursdayBar, fridayBar, saturdayBar, sundayBar
            };

            Label[] countLabels = {
                mondayCountLabel, tuesdayCountLabel, wednesdayCountLabel,
                thursdayCountLabel, fridayCountLabel, saturdayCountLabel, sundayCountLabel
            };

            String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

            // Update each day's bar height and label
            for (int i = 0; i < 7; i++) {
                int taskCount = weeklyStats[i];
                double heightRatio = (double) taskCount / maxTasks;
                double barHeight = heightRatio * MAX_BAR_HEIGHT;

                // Update bar height
                if (bars[i] != null) {
                    bars[i].setPrefHeight(barHeight);
                    bars[i].setMinHeight(barHeight);
                    bars[i].setMaxHeight(barHeight);
                }

                // Update count label
                if (countLabels[i] != null) {
                    countLabels[i].setText(String.valueOf(taskCount));
                }

                logger.debug("Updated {} - Tasks: {}, Height: {}px", dayNames[i], taskCount, barHeight);
            }

            logger.info("Weekly activity chart updated successfully. Max tasks in a day: {}", maxTasks);

        } catch (Exception e) {
            logger.error("Error updating weekly activity chart for user: {}", userId, e);

            // Set default values in case of error
            VBox[] bars = {
                mondayBar, tuesdayBar, wednesdayBar,
                thursdayBar, fridayBar, saturdayBar, sundayBar
            };

            Label[] countLabels = {
                mondayCountLabel, tuesdayCountLabel, wednesdayCountLabel,
                thursdayCountLabel, fridayCountLabel, saturdayCountLabel, sundayCountLabel
            };

            for (int i = 0; i < 7; i++) {
                if (bars[i] != null) {
                    bars[i].setPrefHeight(0.0);
                    bars[i].setMinHeight(0.0);
                    bars[i].setMaxHeight(0.0);
                }
                if (countLabels[i] != null) {
                    countLabels[i].setText("0");
                }
            }
        }
    }

    /**
     * Refresh weekly activity chart manually
     */
    @FXML
    private void refreshWeeklyActivity() {
        int currentUserId = sessionManager.getCurrentUserId();
        updateWeeklyActivityChart(currentUserId);
        logger.info("Weekly activity chart refreshed manually");
    }

    /**
     * Load task management content into the inline container
     */
    private void loadTaskManagementContent() {
        try {
            // Clear existing content
            taskManagementContent.getChildren().clear();

            // Create header
            HBox header = createTaskManagementHeader();

            // Create main content
            VBox mainContent = createTaskManagementMainContent();

            // Add components to container
            taskManagementContent.getChildren().addAll(header, mainContent);

            logger.info("Task management content loaded successfully");

        } catch (Exception e) {
            logger.error("Error loading task management content", e);
            throw e;
        }
    }

    /**
     * Create task management header
     */
    private HBox createTaskManagementHeader() {
        HBox header = new HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setSpacing(20);
        header.setStyle("-fx-background-color: #2c3e50; -fx-padding: 15; -fx-background-radius: 8;");

        Label titleLabel = new Label("My Tasks");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: bold;");

        // Auto-refresh indicator
        Label autoRefreshLabel = new Label("🔄 Auto-refresh: ON");
        autoRefreshLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 12;");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button backButton = new Button("Back to Dashboard");
        backButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-padding: 8 15; -fx-background-radius: 5;");
        backButton.setOnAction(e -> showDashboardContent());

        header.getChildren().addAll(titleLabel, autoRefreshLabel, spacer, backButton);

        return header;
    }

    /**
     * Create task management main content
     */
    private VBox createTaskManagementMainContent() {
        VBox mainContent = new VBox();
        mainContent.setSpacing(20);
        mainContent.setStyle("-fx-padding: 20;");

        // Create search section
        HBox searchSection = createSearchSection();

        // Create statistics section
        HBox statisticsSection = createTaskStatisticsSection();

        // Create task list section
        ScrollPane taskListSection = createTaskListSection();

        mainContent.getChildren().addAll(searchSection, statisticsSection, taskListSection);

        return mainContent;
    }

    /**
     * Create search section
     */
    private HBox createSearchSection() {
        HBox searchSection = new HBox();
        searchSection.setSpacing(10);
        searchSection.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // TextField với style mới
        TextField searchField = new TextField();
        searchField.setPromptText("Search tasks...");
        searchField.setPrefWidth(300);
        searchField.setStyle("-fx-background-color: transparent; -fx-border-color: #7D2CE0; -fx-border-width: 2; -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 10 15;");

        // Button với icon kính lúp
        Button searchButton = new Button("🔍");
        searchButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #7D2CE0; -fx-padding: 8 12; -fx-border-radius: 20; -fx-background-radius: 20; -fx-border-color: #7D2CE0; -fx-border-width: 2; -fx-font-size: 14px;");

        // Thêm hover effect cho button
        searchButton.setOnMouseEntered(e -> searchButton.setStyle("-fx-background-color: #7D2CE0; -fx-text-fill: white; -fx-padding: 8 12; -fx-border-radius: 20; -fx-background-radius: 20; -fx-border-color: #7D2CE0; -fx-border-width: 2; -fx-font-size: 14px;"));
        searchButton.setOnMouseExited(e -> searchButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #7D2CE0; -fx-padding: 8 12; -fx-border-radius: 20; -fx-background-radius: 20; -fx-border-color: #7D2CE0; -fx-border-width: 2; -fx-font-size: 14px;"));

        // Thêm search functionality
        searchButton.setOnAction(e -> performTaskSearch(searchField.getText()));
        searchField.setOnAction(e -> performTaskSearch(searchField.getText())); // Search khi nhấn Enter

        searchSection.getChildren().addAll(searchField, searchButton);

        return searchSection;
    }

    /**
     * Perform task search
     */
    private void performTaskSearch(String searchQuery) {
        try {
            if (searchQuery == null || searchQuery.trim().isEmpty()) {
                // Nếu search query rỗng, reload toàn bộ task management content
                refreshTaskManagementContent();
                return;
            }

            String query = searchQuery.trim().toLowerCase();
            logger.info("Performing task search with query: {}", query);

            // Get current user's tasks
            int currentUserId = sessionManager.getCurrentUserId();
            java.util.List<Task> allUserTasks = taskService.getTasksByUserId(currentUserId);

            // Filter tasks based on search query
            java.util.List<Task> filteredTasks = allUserTasks.stream()
                .filter(task -> matchesSearchQuery(task, query))
                .collect(java.util.stream.Collectors.toList());

            // Update task management content with filtered results
            updateTaskManagementWithFilteredTasks(filteredTasks, query);

            logger.info("Search completed. Found {} tasks matching '{}'", filteredTasks.size(), query);

        } catch (Exception e) {
            logger.error("Error performing task search", e);
            showAlert("Search Error", "Failed to search tasks: " + e.getMessage());
        }
    }

    /**
     * Check if task matches search query
     */
    private boolean matchesSearchQuery(Task task, String query) {
        if (task == null || query == null || query.isEmpty()) {
            return false;
        }

        // Search in title
        if (task.getTitle() != null && task.getTitle().toLowerCase().contains(query)) {
            return true;
        }

        // Search in description
        if (task.getDescription() != null && task.getDescription().toLowerCase().contains(query)) {
            return true;
        }

        // Search in status
        if (task.getStatus() != null && task.getStatus().toString().toLowerCase().contains(query)) {
            return true;
        }

        // Search in priority
        if (task.getPriority() != null && task.getPriority().toString().toLowerCase().contains(query)) {
            return true;
        }

        return false;
    }



    /**
     * Update task management with filtered tasks
     */
    private void updateTaskManagementWithFilteredTasks(java.util.List<Task> filteredTasks, String searchQuery) {
        try {
            // Clear current content
            taskManagementContent.getChildren().clear();

            // Create header with search info
            VBox headerWithSearch = createTaskManagementHeaderWithSearch(searchQuery, filteredTasks.size());

            // Create main content with filtered tasks
            VBox mainContent = createTaskManagementMainContentWithTasks(filteredTasks, searchQuery);

            taskManagementContent.getChildren().addAll(headerWithSearch, mainContent);

            logger.info("Task management updated with {} filtered tasks", filteredTasks.size());

        } catch (Exception e) {
            logger.error("Error updating task management with filtered tasks", e);
            showAlert("Update Error", "Failed to update filtered tasks: " + e.getMessage());
        }
    }

    /**
     * Create task management header with search info
     */
    private VBox createTaskManagementHeaderWithSearch(String searchQuery, int resultCount) {
        VBox headerContainer = new VBox();
        headerContainer.setSpacing(10);

        // Original header
        HBox originalHeader = createTaskManagementHeader();

        // Search result info
        HBox searchInfo = new HBox();
        searchInfo.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        searchInfo.setSpacing(15);
        searchInfo.setStyle("-fx-background-color: #e8f4fd; -fx-padding: 10 15; -fx-background-radius: 5;");

        Label searchResultLabel = new Label("🔍 Search results for: \"" + searchQuery + "\"");
        searchResultLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label resultCountLabel = new Label("Found " + resultCount + " task(s)");
        resultCountLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #7f8c8d;");

        Button clearSearchButton = new Button("✕ Clear Search");
        clearSearchButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 3; -fx-font-size: 11;");
        clearSearchButton.setOnAction(e -> refreshTaskManagementContent());

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        searchInfo.getChildren().addAll(searchResultLabel, resultCountLabel, spacer, clearSearchButton);

        headerContainer.getChildren().addAll(originalHeader, searchInfo);

        return headerContainer;
    }

    /**
     * Create task management main content with specific tasks
     */
    private VBox createTaskManagementMainContentWithTasks(java.util.List<Task> tasks, String searchQuery) {
        VBox mainContent = new VBox();
        mainContent.setSpacing(20);
        mainContent.setStyle("-fx-padding: 20;");

        // Create search section (keep the search bar)
        HBox searchSection = createSearchSection();

        // Create statistics section for filtered tasks
        HBox statisticsSection = createTaskStatisticsSectionForTasks(tasks);

        // Create task list section with filtered tasks
        ScrollPane taskListSection = createTaskListSectionWithTasks(tasks);

        mainContent.getChildren().addAll(searchSection, statisticsSection, taskListSection);

        return mainContent;
    }

    /**
     * Create task statistics section
     */
    private HBox createTaskStatisticsSection() {
        HBox statsSection = new HBox();
        statsSection.setSpacing(20);
        statsSection.setStyle("-fx-padding: 10; -fx-background-color: #f8f9fa; -fx-background-radius: 5;");

        // Get current user's tasks for statistics
        int currentUserId = sessionManager.getCurrentUserId();
        java.util.List<Task> userTasks = taskService.getTasksByUserId(currentUserId);

        int totalTasks = userTasks.size();
        int pendingTasks = (int) userTasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO).count();
        int inProgressTasks = (int) userTasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS || t.getStatus() == TaskStatus.REVIEW).count();
        int completedTasks = (int) userTasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        int overdueTasks = (int) userTasks.stream().filter(Task::isOverdue).count();

        VBox totalBox = createStatBox("Total Tasks", String.valueOf(totalTasks), "#2c3e50");
        VBox pendingBox = createStatBox("Pending", String.valueOf(pendingTasks), "#f39c12");
        VBox inProgressBox = createStatBox("In Progress", String.valueOf(inProgressTasks), "#3498db");
        VBox completedBox = createStatBox("Completed", String.valueOf(completedTasks), "#27ae60");
        VBox overdueBox = createStatBox("Overdue", String.valueOf(overdueTasks), "#e74c3c");

        statsSection.getChildren().addAll(totalBox, pendingBox, inProgressBox, completedBox, overdueBox);

        return statsSection;
    }

    /**
     * Create task statistics section for specific tasks
     */
    private HBox createTaskStatisticsSectionForTasks(java.util.List<Task> tasks) {
        HBox statsSection = new HBox();
        statsSection.setSpacing(20);
        statsSection.setStyle("-fx-padding: 10; -fx-background-color: #f8f9fa; -fx-background-radius: 5;");

        // Calculate statistics for filtered tasks
        int totalTasks = tasks.size();
        int pendingTasks = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO).count();
        int inProgressTasks = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        int completedTasks = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        int overdueTasks = (int) tasks.stream().filter(Task::isOverdue).count();

        // Create stat cards
        VBox totalCard = createStatBox("Total Tasks", String.valueOf(totalTasks), "#2c3e50");
        VBox pendingCard = createStatBox("Pending", String.valueOf(pendingTasks), "#f39c12");
        VBox inProgressCard = createStatBox("In Progress", String.valueOf(inProgressTasks), "#3498db");
        VBox completedCard = createStatBox("Completed", String.valueOf(completedTasks), "#27ae60");
        VBox overdueCard = createStatBox("Overdue", String.valueOf(overdueTasks), "#e74c3c");

        statsSection.getChildren().addAll(totalCard, pendingCard, inProgressCard, completedCard, overdueCard);

        return statsSection;
    }

    /**
     * Create task list section with specific tasks
     */
    private ScrollPane createTaskListSectionWithTasks(java.util.List<Task> tasks) {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        scrollPane.setPrefHeight(400);

        VBox taskListContainer = new VBox();
        taskListContainer.setSpacing(10);
        taskListContainer.setStyle("-fx-padding: 10;");

        if (tasks.isEmpty()) {
            Label noTasksLabel = new Label("🔍 No tasks found matching your search criteria.\n💡 Try different keywords or clear the search to see all tasks.");
            noTasksLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #6c757d; -fx-text-alignment: center;");
            taskListContainer.getChildren().add(noTasksLabel);
        } else {
            // Create task cards for filtered tasks
            for (Task task : tasks) {
                VBox taskCard = createTaskCard(task);
                taskListContainer.getChildren().add(taskCard);
            }
        }

        scrollPane.setContent(taskListContainer);

        return scrollPane;
    }

    /**
     * Create individual stat box
     */
    private VBox createStatBox(String label, String value, String color) {
        VBox statBox = new VBox();
        statBox.setAlignment(javafx.geometry.Pos.CENTER);
        statBox.setSpacing(5);

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        Label labelText = new Label(label);
        labelText.setStyle("-fx-font-size: 12; -fx-text-fill: #7f8c8d;");

        statBox.getChildren().addAll(valueLabel, labelText);

        return statBox;
    }

    /**
     * Create task list section
     */
    private ScrollPane createTaskListSection() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        scrollPane.setPrefHeight(400);

        VBox taskListContainer = new VBox();
        taskListContainer.setSpacing(10);
        taskListContainer.setStyle("-fx-padding: 10;");

        // Load user's tasks
        int currentUserId = sessionManager.getCurrentUserId();
        java.util.List<Task> userTasks = taskService.getTasksByUserId(currentUserId);

        if (userTasks.isEmpty()) {
            Label noTasksLabel = new Label("🎉 No tasks assigned yet!\n💼 You're all caught up!\n📞 Contact your admin for new assignments");
            noTasksLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #6c757d; -fx-text-alignment: center;");
            taskListContainer.getChildren().add(noTasksLabel);
        } else {
            // Create task cards
            for (Task task : userTasks) {
                VBox taskCard = createTaskCard(task);
                taskListContainer.getChildren().add(taskCard);
            }
        }

        scrollPane.setContent(taskListContainer);

        return scrollPane;
    }

    /**
     * Create individual task card
     */
    private VBox createTaskCard(Task task) {
        VBox taskCard = new VBox();
        taskCard.setSpacing(8);
        taskCard.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2); " +
                        "-fx-border-color: #e9ecef; -fx-border-radius: 8; -fx-cursor: hand;");

        // Add double-click handler to open task edit window
        taskCard.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                openTaskEditWindow(task);
            }
        });

        // Task title and status
        HBox titleRow = new HBox();
        titleRow.setSpacing(10);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label titleLabel = new Label(task.getTitle());
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label statusLabel = new Label(task.getStatus().toString());
        statusLabel.setStyle(getStatusLabelStyle(task.getStatus()));

        javafx.scene.layout.Region titleSpacer = new javafx.scene.layout.Region();
        HBox.setHgrow(titleSpacer, javafx.scene.layout.Priority.ALWAYS);

        titleRow.getChildren().addAll(titleLabel, titleSpacer, statusLabel);

        // Task description
        if (task.getDescription() != null && !task.getDescription().trim().isEmpty()) {
            Label descLabel = new Label(task.getDescription());
            descLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #6c757d;");
            descLabel.setWrapText(true);
            taskCard.getChildren().add(descLabel);
        }

        // Task details row
        HBox detailsRow = new HBox();
        detailsRow.setSpacing(15);
        detailsRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        if (task.getPriority() != null) {
            Label priorityLabel = new Label("Priority: " + task.getPriority().toString());
            priorityLabel.setStyle("-fx-font-size: 11; -fx-text-fill: " + getPriorityColor(task.getPriority()) + ";");
            detailsRow.getChildren().add(priorityLabel);
        }

        if (task.getDueDate() != null) {
            Label dueDateLabel = new Label("Due: " + formatDueDate(task.getDueDate()));
            dueDateLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #6c757d;");
            detailsRow.getChildren().add(dueDateLabel);
        }

        // Add double-click hint
        Label hintLabel = new Label("💡 Double-click to update progress");
        hintLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #95a5a6; -fx-font-style: italic;");

        taskCard.getChildren().addAll(titleRow, detailsRow, hintLabel);

        return taskCard;
    }

    /**
     * Get status label style
     */
    private String getStatusLabelStyle(TaskStatus status) {
        String baseStyle = "-fx-font-size: 10; -fx-padding: 4 8; -fx-background-radius: 12; -fx-text-fill: white; ";
        return baseStyle + switch (status) {
            case TODO -> "-fx-background-color: #6c757d;";
            case IN_PROGRESS -> "-fx-background-color: #3498db;";
            case REVIEW -> "-fx-background-color: #f39c12;";
            case COMPLETED -> "-fx-background-color: #27ae60;";
            case CANCELLED -> "-fx-background-color: #e74c3c;";
        };
    }

    /**
     * Get priority color
     */
    private String getPriorityColor(TaskPriority priority) {
        return switch (priority) {
            case LOW -> "#27ae60";
            case MEDIUM -> "#f39c12";
            case HIGH -> "#e67e22";
            case URGENT -> "#e74c3c";
        };
    }

    /**
     * Show dashboard content
     */
    private void showDashboardContent() {
        taskManagementContent.setVisible(false);
        taskManagementContent.setManaged(false);

        calendarContent.setVisible(false);
        calendarContent.setManaged(false);

        dashboardContent.setVisible(true);
        dashboardContent.setManaged(true);

        logger.info("Switched back to dashboard view");
    }

    /**
     * Refresh task management content
     */
    private void refreshTaskManagementContent() {
        loadTaskManagementContent();
        logger.info("Task management content refreshed");
    }

    /**
     * Load calendar content into the inline container
     */
    private void loadCalendarContent() {
        try {
            // Clear existing content
            calendarContent.getChildren().clear();

            // Create header
            HBox header = createCalendarHeader();

            // Create main content
            VBox mainContent = createCalendarMainContent();

            // Add components to container
            calendarContent.getChildren().addAll(header, mainContent);

            logger.info("Calendar content loaded successfully");

        } catch (Exception e) {
            logger.error("Error loading calendar content", e);
            throw e;
        }
    }

    /**
     * Refresh calendar content
     */
    private void refreshCalendarContent() {
        loadCalendarContent();
        logger.info("Calendar content refreshed");
    }

    /**
     * Refresh only calendar display (grid and selected date details)
     */
    private void refreshCalendarDisplay() {
        try {
            // Use stored references to update components directly
            if (calendarArea != null && selectedDateDetailsContainer != null) {
                // Replace calendar grid in calendarArea
                GridPane newCalendarGrid = createCalendarGrid();
                calendarArea.getChildren().clear();
                calendarArea.getChildren().add(newCalendarGrid);

                // Replace selected date details
                VBox newSelectedDateDetails = createSelectedDateDetails();

                // Find parent and replace the selectedDateDetailsContainer
                if (selectedDateDetailsContainer.getParent() instanceof VBox parentVBox) {
                    int index = parentVBox.getChildren().indexOf(selectedDateDetailsContainer);
                    if (index >= 0) {
                        parentVBox.getChildren().set(index, newSelectedDateDetails);
                        selectedDateDetailsContainer = newSelectedDateDetails; // Update reference
                    }
                }

                // Update monthly summary in sidebar
                updateMonthlySummary();

                logger.debug("Calendar display refreshed using stored references");
            } else {
                // Fallback to full refresh if references not available
                logger.warn("Calendar component references not available, falling back to full refresh");
                refreshCalendarContent();
            }
        } catch (Exception e) {
            logger.error("Error refreshing calendar display", e);
            // Fallback to full refresh
            refreshCalendarContent();
        }
    }

    /**
     * Create calendar header
     */
    private HBox createCalendarHeader() {
        HBox header = new HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setSpacing(20);
        header.setStyle("-fx-background-color: #9b59b6; -fx-padding: 15; -fx-background-radius: 8;");

        Label titleLabel = new Label("Task Calendar");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: bold;");

        // Navigation controls
        Button prevMonthButton = new Button("<");
        prevMonthButton.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 5;");
        prevMonthButton.setOnAction(e -> navigateToPreviousMonth());

        currentMonthHeaderLabel = new Label();
        updateMonthHeaderLabel();
        currentMonthHeaderLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold;");

        Button nextMonthButton = new Button(">");
        nextMonthButton.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 5;");
        nextMonthButton.setOnAction(e -> navigateToNextMonth());

        Button todayButton = new Button("Today");
        todayButton.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 5;");
        todayButton.setOnAction(e -> navigateToToday());

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button backButton = new Button("Back to Dashboard");
        backButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-padding: 8 15; -fx-background-radius: 5;");
        backButton.setOnAction(e -> showDashboardContent());

        header.getChildren().addAll(titleLabel, prevMonthButton, currentMonthHeaderLabel, nextMonthButton, todayButton, spacer, backButton);

        return header;
    }

    /**
     * Create calendar main content
     */
    private VBox createCalendarMainContent() {
        VBox mainContent = new VBox();
        mainContent.setSpacing(20);
        mainContent.setStyle("-fx-padding: 20;");

        // Load user's tasks for calendar first
        loadCalendarTasksSync();

        // Create horizontal layout with sidebar and calendar
        HBox calendarLayout = new HBox();
        calendarLayout.setSpacing(20);

        // Create sidebar
        VBox sidebar = createCalendarSidebar();
        sidebar.setPrefWidth(200); // Giảm từ 250 xuống 200

        // Create calendar area and store reference
        calendarArea = createCalendarArea();

        calendarLayout.getChildren().addAll(sidebar, calendarArea);

        // Create selected date details and store reference
        selectedDateDetailsContainer = createSelectedDateDetails();

        mainContent.getChildren().addAll(calendarLayout, selectedDateDetailsContainer);

        return mainContent;
    }

    /**
     * Load user's tasks for calendar (synchronous)
     */
    private void loadCalendarTasksSync() {
        try {
            int currentUserId = sessionManager.getCurrentUserId();
            allTasks = taskService.getTasksByUserId(currentUserId);
            updateMonthlySummary();
            logger.debug("Calendar tasks loaded successfully: {} tasks", allTasks.size());
        } catch (Exception e) {
            logger.error("Error loading tasks for calendar", e);
            allTasks = new java.util.ArrayList<>(); // Initialize empty list on error
        }
    }

    /**
     * Load user's tasks for calendar (asynchronous for refresh)
     */
    private void loadCalendarTasks() {
        new Thread(() -> {
            try {
                int currentUserId = sessionManager.getCurrentUserId();
                allTasks = taskService.getTasksByUserId(currentUserId);

                Platform.runLater(() -> {
                    // Only refresh the calendar grid, not the entire content
                    if (calendarContent.isVisible()) {
                        refreshCalendarDisplay();
                    }
                });

            } catch (Exception e) {
                logger.error("Error loading tasks for calendar", e);
            }
        }).start();
    }

    /**
     * Create calendar sidebar with legend, view options, and summary
     */
    private VBox createCalendarSidebar() {
        VBox sidebar = new VBox();
        sidebar.setSpacing(10); // Giảm từ 15 xuống 10
        sidebar.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; -fx-background-radius: 8;"); // Giảm padding từ 20 xuống 15

        // Legend section
        VBox legendSection = createLegendSection();

        // Separator
        Separator separator1 = new Separator();

        // View options section
        VBox viewOptionsSection = createViewOptionsSection();

        // Separator
        Separator separator2 = new Separator();

        // Summary section
        VBox summarySection = createSummarySection();

        // Separator
        Separator separator3 = new Separator();

        // Quick actions section
        VBox quickActionsSection = createQuickActionsSection();

        sidebar.getChildren().addAll(
            legendSection, separator1,
            viewOptionsSection, separator2,
            summarySection, separator3,
            quickActionsSection
        );

        return sidebar;
    }

    /**
     * Create calendar area with grid
     */
    private VBox createCalendarArea() {
        VBox calendarArea = new VBox();
        calendarArea.setSpacing(10);

        // Create calendar grid
        GridPane calendarGrid = createCalendarGrid();

        calendarArea.getChildren().add(calendarGrid);

        return calendarArea;
    }

    /**
     * Create legend section
     */
    private VBox createLegendSection() {
        VBox legendSection = new VBox();
        legendSection.setSpacing(5); // Giảm từ 10 xuống 5

        Label legendTitle = new Label("Legend");
        legendTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;"); // Giảm font size

        // Tạo legend compact hơn với 2 cột
        GridPane legendGrid = new GridPane();
        legendGrid.setHgap(8);
        legendGrid.setVgap(3);

        // Row 1
        legendGrid.add(new Label("🔴"), 0, 0);
        legendGrid.add(new Label("Overdue"), 1, 0);
        legendGrid.add(new Label("🟠"), 2, 0);
        legendGrid.add(new Label("Today"), 3, 0);

        // Row 2
        legendGrid.add(new Label("🟡"), 0, 1);
        legendGrid.add(new Label("This Week"), 1, 1);
        legendGrid.add(new Label("🟢"), 2, 1);
        legendGrid.add(new Label("Done"), 3, 1);

        // Row 3
        legendGrid.add(new Label("⚪"), 0, 2);
        legendGrid.add(new Label("Future"), 1, 2);

        // Style cho tất cả labels
        legendGrid.getChildren().forEach(node -> {
            if (node instanceof Label label && !label.getText().matches("[🔴🟠🟡🟢⚪]")) {
                label.setStyle("-fx-font-size: 10px;");
            }
        });

        legendSection.getChildren().addAll(legendTitle, legendGrid);

        return legendSection;
    }

    /**
     * Create view options section
     */
    private VBox createViewOptionsSection() {
        VBox viewOptionsSection = new VBox();
        viewOptionsSection.setSpacing(5); // Giảm spacing

        Label viewOptionsTitle = new Label("View Options");
        viewOptionsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;"); // Giảm font size

        showCompletedCheckBox = new CheckBox("Completed");
        showCompletedCheckBox.setSelected(true);
        showCompletedCheckBox.setStyle("-fx-font-size: 10px;"); // Giảm font size
        showCompletedCheckBox.setOnAction(e -> refreshCalendarDisplay());

        showOverdueCheckBox = new CheckBox("Overdue");
        showOverdueCheckBox.setSelected(true);
        showOverdueCheckBox.setStyle("-fx-font-size: 10px;");
        showOverdueCheckBox.setOnAction(e -> refreshCalendarDisplay());

        showFutureCheckBox = new CheckBox("Future");
        showFutureCheckBox.setSelected(true);
        showFutureCheckBox.setStyle("-fx-font-size: 10px;");
        showFutureCheckBox.setOnAction(e -> refreshCalendarDisplay());

        viewOptionsSection.getChildren().addAll(
            viewOptionsTitle, showCompletedCheckBox, showOverdueCheckBox, showFutureCheckBox
        );

        return viewOptionsSection;
    }

    /**
     * Create summary section
     */
    private VBox createSummarySection() {
        VBox summarySection = new VBox();
        summarySection.setSpacing(5); // Giảm spacing

        Label summaryTitle = new Label("Month Summary");
        summaryTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;"); // Giảm font size và text

        // Tạo summary compact với grid 2x2
        GridPane summaryGrid = new GridPane();
        summaryGrid.setHgap(8);
        summaryGrid.setVgap(3);

        // Row 1
        Label totalLabel = new Label("Total:");
        totalLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
        totalTasksThisMonthLabel = new Label("0");
        totalTasksThisMonthLabel.setStyle("-fx-font-size: 10px;");

        Label completedLabel = new Label("Done:");
        completedLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
        completedTasksThisMonthLabel = new Label("0");
        completedTasksThisMonthLabel.setStyle("-fx-font-size: 10px;");

        // Row 2
        Label pendingLabel = new Label("Pending:");
        pendingLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
        pendingTasksThisMonthLabel = new Label("0");
        pendingTasksThisMonthLabel.setStyle("-fx-font-size: 10px;");

        Label overdueLabel = new Label("Overdue:");
        overdueLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
        overdueTasksThisMonthLabel = new Label("0");
        overdueTasksThisMonthLabel.setStyle("-fx-font-size: 10px;");

        summaryGrid.add(totalLabel, 0, 0);
        summaryGrid.add(totalTasksThisMonthLabel, 1, 0);
        summaryGrid.add(completedLabel, 2, 0);
        summaryGrid.add(completedTasksThisMonthLabel, 3, 0);

        summaryGrid.add(pendingLabel, 0, 1);
        summaryGrid.add(pendingTasksThisMonthLabel, 1, 1);
        summaryGrid.add(overdueLabel, 2, 1);
        summaryGrid.add(overdueTasksThisMonthLabel, 3, 1);

        summarySection.getChildren().addAll(summaryTitle, summaryGrid);

        return summarySection;
    }

    /**
     * Create quick actions section
     */
    private VBox createQuickActionsSection() {
        VBox quickActionsSection = new VBox();
        quickActionsSection.setSpacing(5); // Giảm spacing

        Label quickActionsTitle = new Label("Actions");
        quickActionsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;"); // Giảm font size và text

        Button refreshButton = new Button("🔄 Refresh");
        refreshButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 5; -fx-font-size: 10px;");
        refreshButton.setPrefWidth(180); // Giảm width
        refreshButton.setOnAction(e -> {
            loadCalendarTasks();
            showAlert("Refresh", "Calendar refreshed successfully!");
        });

        Button exportButton = new Button("📤 Export");
        exportButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 5; -fx-font-size: 10px;");
        exportButton.setPrefWidth(180); // Giảm width
        exportButton.setOnAction(e -> showAlert("Export Calendar", "Calendar export feature will be implemented in a future update."));

        quickActionsSection.getChildren().addAll(quickActionsTitle, refreshButton, exportButton);

        return quickActionsSection;
    }

    /**
     * Create calendar grid
     */
    private GridPane createCalendarGrid() {
        GridPane calendarGrid = new GridPane();
        calendarGrid.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        calendarGrid.setHgap(1);
        calendarGrid.setVgap(1);

        // Add day headers
        String[] dayHeaders = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < dayHeaders.length; i++) {
            Label dayHeader = new Label(dayHeaders[i]);
            dayHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #6c757d; -fx-padding: 5; -fx-alignment: center; -fx-font-size: 11px;");
            dayHeader.setPrefWidth(80); // Giảm từ 100 xuống 80
            calendarGrid.add(dayHeader, i, 0);
        }

        // Add calendar days
        refreshCalendarGrid(calendarGrid);

        return calendarGrid;
    }

    /**
     * Refresh calendar grid with current month data
     */
    private void refreshCalendarGrid(GridPane calendarGrid) {
        // Clear existing day cells (keep headers)
        calendarGrid.getChildren().removeIf(node -> GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) > 0);

        LocalDate firstDayOfMonth = currentMonth.atDay(1);
        LocalDate lastDayOfMonth = currentMonth.atEndOfMonth();

        // Calculate starting position (day of week for first day)
        int startDayOfWeek = firstDayOfMonth.getDayOfWeek().getValue() % 7; // Sunday = 0

        // Add days of the month
        int currentRow = 1;
        int currentCol = startDayOfWeek;

        for (LocalDate date = firstDayOfMonth; !date.isAfter(lastDayOfMonth); date = date.plusDays(1)) {
            VBox dayCell = createDayCell(date);
            calendarGrid.add(dayCell, currentCol, currentRow);

            currentCol++;
            if (currentCol > 6) {
                currentCol = 0;
                currentRow++;
            }
        }
    }

    /**
     * Create day cell for calendar
     */
    private VBox createDayCell(LocalDate date) {
        VBox dayCell = new VBox();
        dayCell.setSpacing(1); // Giảm spacing
        dayCell.setPrefWidth(80); // Giảm từ 100 xuống 80
        dayCell.setPrefHeight(60); // Giảm từ 80 xuống 60
        dayCell.setStyle("-fx-padding: 3; -fx-border-color: #e9ecef; -fx-border-width: 1; -fx-background-color: " +
                        (date.equals(selectedDate) ? "#e3f2fd" : "#ffffff") + ";"); // Giảm padding

        // Day number
        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;"); // Giảm font size

        // Highlight today
        if (date.equals(LocalDate.now())) {
            dayLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #2196f3;");
        }

        dayCell.getChildren().add(dayLabel);

        // Add tasks for this date
        if (allTasks != null) {
            List<Task> tasksForDate = getTasksForDate(date);
            int displayedTasks = 0;
            for (Task task : tasksForDate) {
                if (shouldShowTask(task) && displayedTasks < 2) { // Giảm từ 3 xuống 2 tasks
                    Label taskLabel = new Label(getCalendarTaskIcon(task));
                    taskLabel.setStyle("-fx-font-size: 10px;"); // Giảm font size
                    taskLabel.setTooltip(new Tooltip(task.getTitle()));
                    dayCell.getChildren().add(taskLabel);
                    displayedTasks++;
                }
            }

            long filteredTasksCount = tasksForDate.stream().filter(this::shouldShowTask).count();
            if (filteredTasksCount > 2) { // Cập nhật logic cho 2 tasks
                Label moreLabel = new Label("+" + (filteredTasksCount - 2));
                moreLabel.setStyle("-fx-font-size: 7px; -fx-text-fill: #6c757d;"); // Giảm font size
                dayCell.getChildren().add(moreLabel);
            }
        }

        // Add click handler
        dayCell.setOnMouseClicked(e -> {
            selectedDate = date;
            refreshCalendarDisplay();
        });

        return dayCell;
    }

    /**
     * Create selected date details section
     */
    private VBox createSelectedDateDetails() {
        VBox selectedDateDetails = new VBox();
        selectedDateDetails.setSpacing(8); // Giảm spacing
        selectedDateDetails.setStyle("-fx-background-color: #ecf0f1; -fx-padding: 10; -fx-background-radius: 8;"); // Giảm padding

        Label selectedDateLabel = new Label("Selected: " + selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))); // Rút gọn text và format
        selectedDateLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;"); // Giảm font size

        ScrollPane taskScrollPane = new ScrollPane();
        taskScrollPane.setPrefHeight(80); // Giảm từ 120 xuống 80
        taskScrollPane.setFitToWidth(true);
        taskScrollPane.setStyle("-fx-background-color: transparent;");

        VBox tasksContainer = new VBox();
        tasksContainer.setSpacing(5);
        tasksContainer.setStyle("-fx-padding: 5;");

        // Add tasks for selected date
        if (allTasks != null) {
            List<Task> tasksForSelectedDate = getTasksForDate(selectedDate);

            List<Task> filteredTasks = tasksForSelectedDate.stream()
                .filter(this::shouldShowTask)
                .collect(Collectors.toList());

            if (filteredTasks.isEmpty()) {
                Label noTasksLabel = new Label("No tasks scheduled for this date");
                noTasksLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #7f8c8d;");
                tasksContainer.getChildren().add(noTasksLabel);
            } else {
                for (Task task : filteredTasks) {
                    HBox taskRow = createTaskRow(task);
                    tasksContainer.getChildren().add(taskRow);
                }
            }
        }

        taskScrollPane.setContent(tasksContainer);
        selectedDateDetails.getChildren().addAll(selectedDateLabel, taskScrollPane);

        return selectedDateDetails;
    }

    /**
     * Create task row for selected date details
     */
    private HBox createTaskRow(Task task) {
        HBox taskRow = new HBox();
        taskRow.setSpacing(10);
        taskRow.setStyle("-fx-padding: 5; -fx-background-color: white; -fx-background-radius: 5;");

        Label statusLabel = new Label(getCalendarTaskIcon(task));
        statusLabel.setStyle("-fx-font-size: 12px;");

        Label titleLabel = new Label(task.getTitle());
        titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        Label priorityLabel = new Label(task.getPriority().toString());
        priorityLabel.setStyle("-fx-font-size: 10px; -fx-padding: 2 6; -fx-background-radius: 10; -fx-background-color: " +
                              getPriorityColor(task.getPriority()) + "; -fx-text-fill: white;");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        taskRow.getChildren().addAll(statusLabel, titleLabel, spacer, priorityLabel);

        // Add double-click handler to show task details
        taskRow.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                showTaskDetailsDialog(task);
            }
        });

        // Add hover effect
        taskRow.setOnMouseEntered(e -> taskRow.setStyle("-fx-padding: 5; -fx-background-color: #e8f4fd; -fx-background-radius: 5; -fx-cursor: hand;"));
        taskRow.setOnMouseExited(e -> taskRow.setStyle("-fx-padding: 5; -fx-background-color: white; -fx-background-radius: 5;"));

        return taskRow;
    }

    /**
     * Get tasks for specific date
     */
    private List<Task> getTasksForDate(LocalDate date) {
        if (allTasks == null) return List.of();

        return allTasks.stream()
            .filter(task -> task.getDueDate() != null)
            .filter(task -> task.getDueDate().toLocalDate().equals(date))
            .collect(Collectors.toList());
    }

    /**
     * Get task icon based on status
     */
    private String getTaskIcon(Task task) {
        return switch (task.getStatus()) {
            case TODO -> "⚪";
            case IN_PROGRESS -> "🔵";
            case REVIEW -> "🟡";
            case COMPLETED -> "🟢";
            case CANCELLED -> "🔴";
        };
    }

    /**
     * Navigate to previous month
     */
    private void navigateToPreviousMonth() {
        currentMonth = currentMonth.minusMonths(1);
        updateMonthHeaderLabel();
        refreshCalendarDisplay();
        updateMonthlySummary();
    }

    /**
     * Navigate to next month
     */
    private void navigateToNextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        updateMonthHeaderLabel();
        refreshCalendarDisplay();
        updateMonthlySummary();
    }

    /**
     * Navigate to current month
     */
    private void navigateToToday() {
        currentMonth = YearMonth.now();
        selectedDate = LocalDate.now();
        updateMonthHeaderLabel();
        refreshCalendarDisplay();
        updateMonthlySummary();
    }

    /**
     * Update month label
     */
    private void updateMonthLabel(Label monthLabel) {
        String monthName = currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        monthLabel.setText(monthName + " " + currentMonth.getYear());
    }

    /**
     * Update month header label
     */
    private void updateMonthHeaderLabel() {
        if (currentMonthHeaderLabel != null) {
            String monthName = currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            currentMonthHeaderLabel.setText(monthName + " " + currentMonth.getYear());
        }
    }

    /**
     * Check if task should be shown based on view options
     */
    private boolean shouldShowTask(Task task) {
        if (showCompletedCheckBox == null || showOverdueCheckBox == null || showFutureCheckBox == null) {
            return true; // Show all tasks if checkboxes not initialized
        }

        if (task.getStatus() == TaskStatus.COMPLETED && !showCompletedCheckBox.isSelected()) {
            return false;
        }

        if (task.isOverdue() && !showOverdueCheckBox.isSelected()) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (task.getDueDate() != null && task.getDueDate().isAfter(now) && !showFutureCheckBox.isSelected()) {
            return false;
        }

        return true;
    }

    /**
     * Get task icon for calendar based on status and urgency
     */
    private String getCalendarTaskIcon(Task task) {
        if (task.isOverdue()) {
            return "🔴";
        } else if (task.getDueDate() != null && task.getDueDate().toLocalDate().equals(LocalDate.now())) {
            return "🟠";
        } else if (task.getStatus() == TaskStatus.COMPLETED) {
            return "🟢";
        } else if (task.getDueDate() != null && task.getDueDate().toLocalDate().isBefore(LocalDate.now().plusWeeks(1))) {
            return "🟡";
        } else {
            return "⚪";
        }
    }

    /**
     * Update monthly summary
     */
    private void updateMonthlySummary() {
        if (allTasks == null || totalTasksThisMonthLabel == null) return;

        LocalDate startOfMonth = currentMonth.atDay(1);
        LocalDate endOfMonth = currentMonth.atEndOfMonth();

        List<Task> monthTasks = allTasks.stream()
            .filter(task -> task.getDueDate() != null)
            .filter(task -> {
                LocalDate taskDate = task.getDueDate().toLocalDate();
                return !taskDate.isBefore(startOfMonth) && !taskDate.isAfter(endOfMonth);
            })
            .collect(Collectors.toList());

        int total = monthTasks.size();
        int completed = (int) monthTasks.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count();
        int pending = total - completed;
        int overdue = (int) monthTasks.stream().filter(Task::isOverdue).count();

        totalTasksThisMonthLabel.setText(String.valueOf(total));
        completedTasksThisMonthLabel.setText(String.valueOf(completed));
        pendingTasksThisMonthLabel.setText(String.valueOf(pending));
        overdueTasksThisMonthLabel.setText(String.valueOf(overdue));
    }

    /**
     * Show task details dialog
     */
    private void showTaskDetailsDialog(Task task) {
        try {
            // Create dialog
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Task Details");
            dialog.setHeaderText(null);

            // Create content
            VBox content = createTaskDetailsContent(task);

            // Set dialog content
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().setPrefWidth(500);
            dialog.getDialogPane().setPrefHeight(400);

            // Add buttons
            ButtonType closeButtonType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().add(closeButtonType);

            // Style the dialog
            dialog.getDialogPane().setStyle("-fx-background-color: #f8f9fa;");

            // Show dialog
            dialog.showAndWait();

        } catch (Exception e) {
            logger.error("Error showing task details dialog", e);
            showAlert("Error", "Failed to show task details: " + e.getMessage());
        }
    }

    /**
     * Create task details content
     */
    private VBox createTaskDetailsContent(Task task) {
        VBox content = new VBox();
        content.setSpacing(15);
        content.setStyle("-fx-padding: 20;");

        // Task title
        Label titleLabel = new Label("Task Title");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

        Label titleValue = new Label(task.getTitle());
        titleValue.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #34495e;");
        titleValue.setWrapText(true);

        // Task description
        Label descLabel = new Label("Description");
        descLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

        Label descValue = new Label(task.getDescription() != null && !task.getDescription().trim().isEmpty()
                                   ? task.getDescription() : "No description provided");
        descValue.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        descValue.setWrapText(true);

        // Task details grid
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(20);
        detailsGrid.setVgap(10);
        detailsGrid.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 3, 0, 0, 1);");

        // Row 0: Status and Priority
        Label statusLabel = new Label("Status:");
        statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        Label statusValue = new Label(task.getStatus().toString());
        statusValue.setStyle("-fx-font-size: 12px; -fx-padding: 3 8; -fx-background-radius: 10; -fx-background-color: " + task.getStatus().getColor() + "; -fx-text-fill: white;");

        Label priorityLabel = new Label("Priority:");
        priorityLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        Label priorityValue = new Label(task.getPriority().toString());
        priorityValue.setStyle("-fx-font-size: 12px; -fx-padding: 3 8; -fx-background-radius: 10; -fx-background-color: " + getPriorityColor(task.getPriority()) + "; -fx-text-fill: white;");

        detailsGrid.add(statusLabel, 0, 0);
        detailsGrid.add(statusValue, 1, 0);
        detailsGrid.add(priorityLabel, 2, 0);
        detailsGrid.add(priorityValue, 3, 0);

        // Row 1: Due Date and Created Date
        Label dueDateLabel = new Label("Due Date:");
        dueDateLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        Label dueDateValue = new Label(task.getDueDate() != null
                                      ? task.getDueDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
                                      : "Not set");
        dueDateValue.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (task.isOverdue() ? "#e74c3c" : "#34495e") + ";");

        Label createdLabel = new Label("Created:");
        createdLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        Label createdValue = new Label(task.getCreatedAt() != null
                                      ? task.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
                                      : "Unknown");
        createdValue.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        detailsGrid.add(dueDateLabel, 0, 1);
        detailsGrid.add(dueDateValue, 1, 1);
        detailsGrid.add(createdLabel, 2, 1);
        detailsGrid.add(createdValue, 3, 1);

        // Row 2: Progress and Assignee
        Label progressLabel = new Label("Progress:");
        progressLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        ProgressBar progressBar = new ProgressBar(task.getProgressPercentage() / 100.0);
        progressBar.setPrefWidth(100);
        Label progressValue = new Label((int)task.getProgressPercentage() + "%");
        progressValue.setStyle("-fx-font-size: 12px;");

        HBox progressBox = new HBox(5);
        progressBox.getChildren().addAll(progressBar, progressValue);

        Label assigneeLabel = new Label("Assignee:");
        assigneeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        Label assigneeValue = new Label(getAssigneeDisplayName(task.getAssignedUserId()));
        assigneeValue.setStyle("-fx-font-size: 12px; -fx-text-fill: #34495e;");

        detailsGrid.add(progressLabel, 0, 2);
        detailsGrid.add(progressBox, 1, 2);
        detailsGrid.add(assigneeLabel, 2, 2);
        detailsGrid.add(assigneeValue, 3, 2);

        content.getChildren().addAll(titleLabel, titleValue, descLabel, descValue, detailsGrid);

        return content;
    }

    /**
     * Get assignee display name from user ID
     */
    private String getAssigneeDisplayName(int userId) {
        if (userId <= 0) {
            return "Unassigned";
        }

        try {
            // For now, just return "User #ID" - in a real app, you'd query the database
            // You could add a UserService to get actual username
            return "User #" + userId;
        } catch (Exception e) {
            logger.error("Error getting assignee display name for user ID: {}", userId, e);
            return "Unknown User";
        }
    }

    /**
     * Open task update progress dialog when double-clicking on a task card
     */
    private void openTaskEditWindow(Task task) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/taskmanagerv3/update-progress-dialog.fxml"));
            Scene scene = new Scene(loader.load());

            // Get the controller and set the task
            UpdateProgressController controller = loader.getController();
            controller.setTask(task);

            Stage stage = new Stage();
            stage.setTitle("Update Progress - " + task.getTitle());
            stage.setScene(scene);
            stage.setResizable(false);

            // Set stage as modal
            stage.initModality(Modality.APPLICATION_MODAL);

            // Center on parent window
            Stage parentStage = (Stage) taskManagementContent.getScene().getWindow();
            if (parentStage != null) {
                stage.initOwner(parentStage);
                // Center the dialog (500x500 as per updated FXML)
                stage.setX(parentStage.getX() + (parentStage.getWidth() - 500) / 2);
                stage.setY(parentStage.getY() + (parentStage.getHeight() - 500) / 2);
            }

            // Refresh task management content when window closes
            stage.setOnHidden(e -> {
                Platform.runLater(() -> {
                    refreshTaskManagementContent();
                    logger.info("Refreshed task management after update progress dialog closed");
                });
            });

            stage.showAndWait(); // Use showAndWait for modal behavior

            logger.info("Opened update progress dialog for task: {} (ID: {})", task.getTitle(), task.getTaskId());

        } catch (IOException e) {
            logger.error("Failed to open update progress dialog for task: {}", task.getTitle(), e);
            showAlert("Error", "Failed to open update progress dialog: " + e.getMessage());
        }
    }

    /**
     * Load profile content inline
     */
    private void loadProfileContent() {
        try {
            // Clear existing content
            profileContent.getChildren().clear();

            // Load profile FXML content
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/taskmanagerv3/user-profile.fxml"));
            ScrollPane profileScrollPane = loader.load(); // Load as ScrollPane since that's the root element

            // Create header with back button
            HBox header = new HBox();
            header.setSpacing(15);
            header.setStyle("-fx-padding: 20 20 10 20; -fx-alignment: center-left;");

            Button backButton = new Button("← Back to Dashboard");
            backButton.setStyle("-fx-background-color: #6f42c1; -fx-text-fill: white; -fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;");
            backButton.setOnAction(e -> showDashboard());

            Label titleLabel = new Label("My Profile");
            titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

            header.getChildren().addAll(backButton, titleLabel);

            // Add header and profile content
            profileContent.getChildren().addAll(header, profileScrollPane);

            logger.info("Profile content loaded inline");

        } catch (IOException e) {
            logger.error("Failed to load profile content", e);
            showAlert("Error", "Failed to load profile content: " + e.getMessage());
        }
    }

    /**
     * Load chat content inline
     */
    private void loadChatContent() {
        try {
            // Clear existing content
            chatContent.getChildren().clear();

            // Load chat FXML content
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/taskmanagerv3/chat.fxml"));
            VBox chatView = loader.load();

            // Create header with back button
            HBox header = new HBox();
            header.setSpacing(15);
            header.setStyle("-fx-padding: 20 20 10 20; -fx-alignment: center-left;");

            Button backButton = new Button("← Back to Dashboard");
            backButton.setStyle("-fx-background-color: #6f42c1; -fx-text-fill: white; -fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;");
            backButton.setOnAction(e -> showDashboard());

            Label titleLabel = new Label("💬 Chat with Admin");
            titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

            header.getChildren().addAll(backButton, titleLabel);

            // Add header and chat content
            chatContent.getChildren().addAll(header, chatView);

            logger.info("Chat content loaded inline");

        } catch (IOException e) {
            logger.error("Failed to load chat content", e);
            showAlert("Error", "Failed to load chat content: " + e.getMessage());
        }
    }

    /**
     * Load AI chatbot content inline
     */
    private void loadAIChatbotContent() {
        try {
            // Clear existing content
            aiChatbotContent.getChildren().clear();

            // Load AI chatbot FXML content
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/taskmanagerv3/ai-chatbot.fxml"));
            BorderPane aiChatbotView = loader.load(); // Load as BorderPane since that's the root element

            // Create header with back button
            HBox header = new HBox();
            header.setSpacing(15);
            header.setStyle("-fx-padding: 20 20 10 20; -fx-alignment: center-left;");

            Button backButton = new Button("← Back to Dashboard");
            backButton.setStyle("-fx-background-color: #6f42c1; -fx-text-fill: white; -fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;");
            backButton.setOnAction(e -> showDashboard());

            Label titleLabel = new Label("🤖 AI Assistant");
            titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

            header.getChildren().addAll(backButton, titleLabel);

            // Add header and AI chatbot content
            aiChatbotContent.getChildren().addAll(header, aiChatbotView);

            logger.info("AI chatbot content loaded inline");

        } catch (IOException e) {
            logger.error("Failed to load AI chatbot content", e);
            showAlert("Error", "Failed to load AI chatbot content: " + e.getMessage());
        }
    }

    /**
     * Show dashboard (go back from other views)
     */
    public void showDashboard() {
        // Hide all other content
        taskManagementContent.setVisible(false);
        taskManagementContent.setManaged(false);
        calendarContent.setVisible(false);
        calendarContent.setManaged(false);
        profileContent.setVisible(false);
        profileContent.setManaged(false);
        chatContent.setVisible(false);
        chatContent.setManaged(false);
        aiChatbotContent.setVisible(false);
        aiChatbotContent.setManaged(false);

        // Show dashboard content
        dashboardContent.setVisible(true);
        dashboardContent.setManaged(true);

        // Force layout refresh
        Platform.runLater(() -> {
            mainContentContainer.requestLayout();
            dashboardContent.requestLayout();
        });

        // Refresh dashboard data
        loadDashboardData();

        logger.info("Returned to dashboard view");
    }

    /**
     * Stop auto-refresh thread when controller is destroyed
     */
    public void shutdown() {
        if (autoRefreshThread != null && !autoRefreshThread.isInterrupted()) {
            autoRefreshThread.interrupt();
            logger.info("Auto-refresh thread stopped");
        }
    }


}
