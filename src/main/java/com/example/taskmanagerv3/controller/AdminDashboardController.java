package com.example.taskmanagerv3.controller;

import com.example.taskmanagerv3.controller.CreateTaskController;
import com.example.taskmanagerv3.controller.CreateUserController;
import com.example.taskmanagerv3.model.PasswordResetRequest;
import com.example.taskmanagerv3.model.User;
import com.example.taskmanagerv3.service.PasswordResetService;
import com.example.taskmanagerv3.service.UserService;
import com.example.taskmanagerv3.service.TaskService;
import com.example.taskmanagerv3.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for Admin Dashboard
 */
public class AdminDashboardController {
    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardController.class);

    // Header components
    @FXML private Label welcomeLabel;
    @FXML private Label currentTimeLabel;
    @FXML private Button logoutButton;

    // Statistics cards
    @FXML private Label totalUsersLabel;
    @FXML private Label totalTasksLabel;
    @FXML private Label completedTasksLabel;
    @FXML private Label pendingTasksLabel;

    // Navigation buttons
    @FXML private Button userManagementButton;
    @FXML private Button taskManagementButton;
    @FXML private Button passwordResetButton;
    @FXML private Button reportsButton;
    @FXML private Button settingsButton;
    @FXML private Button chatButton;
    @FXML private Button aiChatbotButton;

    // Main content area
    @FXML private VBox mainContentContainer;
    @FXML private VBox adminDashboardContent;

    // Dynamic content containers
    @FXML private VBox userManagementContent;
    @FXML private VBox taskManagementContent;
    @FXML private VBox passwordResetContent;
    @FXML private VBox reportsContent;
    @FXML private VBox settingsContent;
    @FXML private VBox chatContent;
    @FXML private VBox aiChatbotContent;

    // Quick actions
    @FXML private Button createTaskButton;
    @FXML private Button createUserButton;
    @FXML private Button viewReportsButton;
    @FXML private Button statisticsExportButton;

    // Recent activities
    @FXML private ListView<String> recentActivitiesListView;

    private UserService userService;
    private TaskService taskService;
    private PasswordResetService passwordResetService;
    private SessionManager sessionManager;

    @FXML
    private void initialize() {
        userService = new UserService();
        taskService = new TaskService();
        passwordResetService = new PasswordResetService();
        sessionManager = SessionManager.getInstance();

        setupUI();
        loadDashboardData();
        startTimeUpdater();

        logger.info("Admin dashboard initialized for user: {}",
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
        userManagementButton.setOnAction(e -> showUserManagement());
        taskManagementButton.setOnAction(e -> showTaskManagement());
        passwordResetButton.setOnAction(e -> showPasswordResetManagement());
        reportsButton.setOnAction(e -> showReports());
        settingsButton.setOnAction(e -> showSettings());
        chatButton.setOnAction(e -> showAdminChat());
        aiChatbotButton.setOnAction(e -> showAIChatbot());

        createTaskButton.setOnAction(e -> showCreateTask());
        createUserButton.setOnAction(e -> showCreateUser());
        viewReportsButton.setOnAction(e -> showReports());
        statisticsExportButton.setOnAction(e -> showStatisticsExport());

        logoutButton.setOnAction(e -> handleLogout());
    }

    /**
     * Load dashboard statistics and data
     */
    private void loadDashboardData() {
        // Load in background thread to avoid blocking UI
        new Thread(() -> {
            try {
                // Get user statistics
                List<User> allUsers = userService.getAllActiveUsers();
                int totalUsers = allUsers.size();

                // Get task statistics
                TaskService.TaskStatistics taskStats = taskService.getTaskStatistics();
                int totalTasks = taskStats.getTotalTasks();
                int completedTasks = taskStats.getCompletedTasks();
                int pendingTasks = taskStats.getPendingTasks();

                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    totalUsersLabel.setText(String.valueOf(totalUsers));
                    totalTasksLabel.setText(String.valueOf(totalTasks));
                    completedTasksLabel.setText(String.valueOf(completedTasks));
                    pendingTasksLabel.setText(String.valueOf(pendingTasks));

                    loadRecentActivities();
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
     * Load recent activities
     */
    private void loadRecentActivities() {
        recentActivitiesListView.getItems().clear();

        // Add sample activities (will be replaced with real data)
        recentActivitiesListView.getItems().addAll(
            "System started successfully",
            "Admin user logged in",
            "Database connection established",
            "Ready for task management"
        );
    }

    /**
     * Start time updater thread
     */
    private void startTimeUpdater() {
        Thread timeUpdater = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Platform.runLater(() -> {
                        currentTimeLabel.setText(
                            java.time.LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        );
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
     * Show user management interface inline
     */
    @FXML
    private void showUserManagement() {
        try {
            // Hide all other content
            hideAllContent();

            // Load user management content
            loadUserManagementContent();

            // Show user management content
            userManagementContent.setVisible(true);
            userManagementContent.setManaged(true);

            logger.info("Switched to inline user management view");

        } catch (Exception e) {
            logger.error("Failed to load user management interface", e);
            showAlert("Error", "Failed to load user management interface: " + e.getMessage());
        }
    }

    /**
     * Show task management interface inline
     */
    @FXML
    private void showTaskManagement() {
        try {
            // Hide all other content
            hideAllContent();

            // Load task management content
            loadTaskManagementContent();

            // Show task management content
            taskManagementContent.setVisible(true);
            taskManagementContent.setManaged(true);

            logger.info("Switched to inline task management view");

        } catch (Exception e) {
            logger.error("Failed to load task management interface", e);
            showAlert("Error", "Failed to load task management interface: " + e.getMessage());
        }
    }

    /**
     * Show password reset management interface inline
     */
    @FXML
    private void showPasswordResetManagement() {
        try {
            // Hide all other content
            hideAllContent();

            // Load password reset management content
            loadPasswordResetContent();

            // Show password reset content
            passwordResetContent.setVisible(true);
            passwordResetContent.setManaged(true);

            logger.info("Switched to inline password reset management view");

        } catch (Exception e) {
            logger.error("Failed to load password reset management interface", e);
            showAlert("Error", "Failed to load password reset management interface: " + e.getMessage());
        }
    }

    /**
     * Show reports interface inline
     */
    @FXML
    private void showReports() {
        try {
            // Hide all other content
            hideAllContent();

            // Load reports content
            loadReportsContent();

            // Show reports content
            reportsContent.setVisible(true);
            reportsContent.setManaged(true);

            logger.info("Switched to inline reports view");

        } catch (Exception e) {
            logger.error("Failed to load reports interface", e);
            showAlert("Error", "Failed to load reports interface: " + e.getMessage());
        }
    }

    /**
     * Show statistics and export interface inline
     */
    @FXML
    private void showStatisticsExport() {
        try {
            // Hide all other content
            hideAllContent();

            // Load reports content (statistics export is part of reports)
            loadReportsContent();

            // Show reports content
            reportsContent.setVisible(true);
            reportsContent.setManaged(true);

            logger.info("Switched to inline statistics export view");

        } catch (Exception e) {
            logger.error("Failed to load statistics and export interface", e);
            showAlert("Error", "Failed to load Statistics & Export: " + e.getMessage());
        }
    }

    /**
     * Show settings interface inline
     */
    @FXML
    private void showSettings() {
        try {
            // Hide all other content
            hideAllContent();

            // Load settings content
            loadSettingsContent();

            // Show settings content
            settingsContent.setVisible(true);
            settingsContent.setManaged(true);

            logger.info("Switched to inline settings view");

        } catch (Exception e) {
            logger.error("Failed to load settings interface", e);
            showAlert("Error", "Failed to load settings interface: " + e.getMessage());
        }
    }

    /**
     * Show Admin Chat interface inline
     */
    @FXML
    private void showAdminChat() {
        try {
            // Hide all other content
            hideAllContent();

            // Load chat content
            loadChatContent();

            // Show chat content
            chatContent.setVisible(true);
            chatContent.setManaged(true);

            logger.info("Switched to inline admin chat view");

        } catch (Exception e) {
            logger.error("Failed to load admin chat interface", e);
            showAlert("Error", "Failed to load Admin Chat: " + e.getMessage());
        }
    }

    /**
     * Show AI Chatbot interface inline for admin
     */
    @FXML
    private void showAIChatbot() {
        try {
            // Hide all other content
            hideAllContent();

            // Load AI chatbot content
            loadAIChatbotContent();

            // Show AI chatbot content
            aiChatbotContent.setVisible(true);
            aiChatbotContent.setManaged(true);

            logger.info("Switched to inline admin AI chatbot view");

        } catch (Exception e) {
            logger.error("Failed to load admin AI chatbot interface", e);
            showAlert("Error", "Failed to load AI Assistant: " + e.getMessage());
        }
    }

    /**
     * Show create task dialog
     */
    @FXML
    private void showCreateTask() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/taskmanagerv3/create-task-dialog.fxml"));
            Parent dialogContent = loader.load();

            CreateTaskController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Create New Task");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(createTaskButton.getScene().getWindow());
            dialogStage.setScene(new Scene(dialogContent, 500, 600));
            dialogStage.setResizable(false);
            dialogStage.centerOnScreen();

            dialogStage.showAndWait();

            // Refresh dashboard if task was created
            if (controller.isTaskCreated()) {
                loadDashboardData();
                showAlert("Success", "Task created successfully! Dashboard refreshed.");
            }

        } catch (IOException e) {
            logger.error("Failed to open create task dialog", e);
            showAlert("Error", "Failed to open create task dialog: " + e.getMessage());
        }
    }

    /**
     * Show create user dialog
     */
    @FXML
    private void showCreateUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/taskmanagerv3/create-user-dialog.fxml"));
            Scene scene = new Scene(loader.load());

            CreateUserController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Create New User");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.showAndWait();

            if (controller.isUserCreated()) {
                loadDashboardData();
                showAlert("Success", "User created successfully! Dashboard refreshed.");
            }

        } catch (IOException e) {
            logger.error("Failed to open create user dialog", e);
            showAlert("Error", "Failed to open create user dialog: " + e.getMessage());
        }
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
     * Hide all content containers
     */
    private void hideAllContent() {
        adminDashboardContent.setVisible(false);
        adminDashboardContent.setManaged(false);
        userManagementContent.setVisible(false);
        userManagementContent.setManaged(false);
        taskManagementContent.setVisible(false);
        taskManagementContent.setManaged(false);
        passwordResetContent.setVisible(false);
        passwordResetContent.setManaged(false);
        reportsContent.setVisible(false);
        reportsContent.setManaged(false);
        settingsContent.setVisible(false);
        settingsContent.setManaged(false);
        chatContent.setVisible(false);
        chatContent.setManaged(false);
        aiChatbotContent.setVisible(false);
        aiChatbotContent.setManaged(false);
    }

    /**
     * Show admin dashboard (go back from other views)
     */
    public void showAdminDashboard() {
        hideAllContent();
        adminDashboardContent.setVisible(true);
        adminDashboardContent.setManaged(true);

        // Force layout refresh
        Platform.runLater(() -> {
            mainContentContainer.requestLayout();
            adminDashboardContent.requestLayout();
        });

        logger.info("Switched back to admin dashboard view");
    }

    /**
     * Load user management content into the inline container
     */
    private void loadUserManagementContent() {
        try {
            // Clear existing content
            userManagementContent.getChildren().clear();

            // Create header with back button
            HBox header = createContentHeader("👥 User Management", "Manage system users and permissions");

            // Load the actual user management content from FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/taskmanagerv3/user-management.fxml"));
            Parent content = loader.load();

            // Add components to container
            userManagementContent.getChildren().addAll(header, content);

            logger.info("User management content loaded successfully");

        } catch (Exception e) {
            logger.error("Error loading user management content", e);
            // Create fallback content
            userManagementContent.getChildren().clear();
            HBox header = createContentHeader("👥 User Management", "Manage system users and permissions");
            Label errorLabel = new Label("Error loading user management content: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red; -fx-padding: 20;");
            userManagementContent.getChildren().addAll(header, errorLabel);
        }
    }

    /**
     * Load task management content into the inline container
     */
    private void loadTaskManagementContent() {
        try {
            // Clear existing content
            taskManagementContent.getChildren().clear();

            // Create header with back button
            HBox header = createContentHeader("📋 Task Management", "Manage all system tasks");

            // Load the actual task management content from FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/taskmanagerv3/task-management.fxml"));
            Parent content = loader.load();

            // Add components to container
            taskManagementContent.getChildren().addAll(header, content);

            logger.info("Task management content loaded successfully");

        } catch (Exception e) {
            logger.error("Error loading task management content", e);
            // Create fallback content
            taskManagementContent.getChildren().clear();
            HBox header = createContentHeader("📋 Task Management", "Manage all system tasks");
            Label errorLabel = new Label("Error loading task management content: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red; -fx-padding: 20;");
            taskManagementContent.getChildren().addAll(header, errorLabel);
        }
    }

    /**
     * Load reports content into the inline container
     */
    private void loadReportsContent() {
        try {
            // Clear existing content
            reportsContent.getChildren().clear();

            // Create header with back button
            HBox header = createContentHeader("📊 Reports & Analytics", "View system reports and analytics");

            // Load the actual reports content from FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/taskmanagerv3/admin-reports.fxml"));
            Parent content = loader.load();

            // Add components to container
            reportsContent.getChildren().addAll(header, content);

            logger.info("Reports content loaded successfully");

        } catch (Exception e) {
            logger.error("Error loading reports content", e);
            // Create fallback content
            reportsContent.getChildren().clear();
            HBox header = createContentHeader("📊 Reports & Analytics", "View system reports and analytics");
            Label errorLabel = new Label("Error loading reports content: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red; -fx-padding: 20;");
            reportsContent.getChildren().addAll(header, errorLabel);
        }
    }

    /**
     * Load settings content into the inline container
     */
    private void loadSettingsContent() {
        try {
            // Clear existing content
            settingsContent.getChildren().clear();

            // Create header with back button
            HBox header = createContentHeader("⚙️ System Settings", "Configure system settings");

            // Load the actual settings content from FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/taskmanagerv3/admin-settings.fxml"));
            Parent content = loader.load();

            // Add components to container
            settingsContent.getChildren().addAll(header, content);

            logger.info("Settings content loaded successfully");

        } catch (Exception e) {
            logger.error("Error loading settings content", e);
            // Create fallback content
            settingsContent.getChildren().clear();
            HBox header = createContentHeader("⚙️ System Settings", "Configure system settings");
            Label errorLabel = new Label("Error loading settings content: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red; -fx-padding: 20;");
            settingsContent.getChildren().addAll(header, errorLabel);
        }
    }

    /**
     * Load chat content into the inline container
     */
    private void loadChatContent() {
        try {
            // Clear existing content
            chatContent.getChildren().clear();

            // Create header with back button
            HBox header = createContentHeader("💬 Admin Chat Center", "Manage chat communications");

            // Load the actual chat content from FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/taskmanagerv3/admin-chat.fxml"));
            Parent content = loader.load();

            // Add components to container
            chatContent.getChildren().addAll(header, content);

            logger.info("Chat content loaded successfully");

        } catch (Exception e) {
            logger.error("Error loading chat content", e);
            // Create fallback content
            chatContent.getChildren().clear();
            HBox header = createContentHeader("💬 Admin Chat Center", "Manage chat communications");
            Label errorLabel = new Label("Error loading chat content: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red; -fx-padding: 20;");
            chatContent.getChildren().addAll(header, errorLabel);
        }
    }

    /**
     * Load AI chatbot content into the inline container
     */
    private void loadAIChatbotContent() {
        try {
            // Clear existing content
            aiChatbotContent.getChildren().clear();

            // Create header with back button
            HBox header = createContentHeader("🤖 AI Assistant", "Admin AI Assistant");

            // Load the actual AI chatbot content from FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/taskmanagerv3/admin-ai-chatbot.fxml"));
            Parent content = loader.load();

            // Add components to container
            aiChatbotContent.getChildren().addAll(header, content);

            logger.info("AI chatbot content loaded successfully");

        } catch (Exception e) {
            logger.error("Error loading AI chatbot content", e);
            // Create fallback content
            aiChatbotContent.getChildren().clear();
            HBox header = createContentHeader("🤖 AI Assistant", "Admin AI Assistant");
            Label errorLabel = new Label("Error loading AI chatbot content: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red; -fx-padding: 20;");
            aiChatbotContent.getChildren().addAll(header, errorLabel);
        }
    }

    /**
     * Create a header for content sections with back button
     */
    private HBox createContentHeader(String title, String subtitle) {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(15);
        header.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 10 10 0 0; " +
                       "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        // Back button
        Button backButton = new Button("← Back to Dashboard");
        backButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-padding: 8 15; " +
                           "-fx-background-radius: 5; -fx-cursor: hand;");
        backButton.setOnAction(e -> showAdminDashboard());

        // Title section
        VBox titleSection = new VBox();
        titleSection.setSpacing(5);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #6c757d;");

        titleSection.getChildren().addAll(titleLabel, subtitleLabel);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(backButton, titleSection, spacer);

        return header;
    }

    /**
     * Load password reset management content
     */
    private void loadPasswordResetContent() {
        passwordResetContent.getChildren().clear();

        HBox header = createContentHeader("🔐 Password Reset Requests", "Manage user password reset requests");

        // Load pending requests
        List<PasswordResetRequest> pendingRequests = passwordResetService.getPendingRequests();

        VBox content = new VBox(15);
        content.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 0 0 10 10; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        Button refreshButton = new Button("🔄 Refresh");
        refreshButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 8 15; -fx-background-radius: 5;");
        refreshButton.setOnAction(e -> loadPasswordResetContent());

        VBox requestsBox = new VBox(10);

        if (pendingRequests.isEmpty()) {
            Label noRequestsLabel = new Label("No pending password reset requests.");
            noRequestsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-padding: 20;");
            requestsBox.getChildren().add(noRequestsLabel);
        } else {
            Label requestsCountLabel = new Label("Pending Requests: " + pendingRequests.size());
            requestsCountLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            requestsBox.getChildren().add(requestsCountLabel);

            for (PasswordResetRequest request : pendingRequests) {
                VBox requestCard = createPasswordResetRequestCard(request);
                requestsBox.getChildren().add(requestCard);
            }
        }

        ScrollPane scrollPane = new ScrollPane(requestsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        content.getChildren().addAll(refreshButton, scrollPane);
        passwordResetContent.getChildren().addAll(header, content);
    }

    /**
     * Create a card for password reset request
     */
    private VBox createPasswordResetRequestCard(PasswordResetRequest request) {
        VBox card = new VBox(10);
        card.setStyle("-fx-padding: 15; -fx-border-color: #e74c3c; -fx-border-radius: 8; -fx-background-color: #fff5f5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        // Header with user info
        Label userLabel = new Label("User: " + request.getFullName() + " (" + request.getUsername() + ")");
        userLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label emailLabel = new Label("Email: " + request.getEmail());
        emailLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        Label dateLabel = new Label("Requested: " + request.getRequestedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        // Reason
        Label reasonTitleLabel = new Label("Reason:");
        reasonTitleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label reasonLabel = new Label(request.getReason());
        reasonLabel.setWrapText(true);
        reasonLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #34495e; -fx-padding: 5; -fx-background-color: #f8f9fa; -fx-background-radius: 3;");

        // Action buttons
        Button approveButton = new Button("✓ Approve & Reset Password");
        approveButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 8 15; -fx-background-radius: 5;");
        approveButton.setOnAction(e -> handlePasswordResetApproval(request));

        Button rejectButton = new Button("✗ Reject Request");
        rejectButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 8 15; -fx-background-radius: 5;");
        rejectButton.setOnAction(e -> handlePasswordResetRejection(request));

        HBox actionButtons = new HBox(10);
        actionButtons.getChildren().addAll(approveButton, rejectButton);

        card.getChildren().addAll(userLabel, emailLabel, dateLabel, reasonTitleLabel, reasonLabel, actionButtons);

        return card;
    }

    /**
     * Handle password reset approval
     */
    private void handlePasswordResetApproval(PasswordResetRequest request) {
        TextInputDialog dialog = new TextInputDialog("TempPass123!");
        dialog.setTitle("Approve Password Reset");
        dialog.setHeaderText("Set New Password for " + request.getUsername());
        dialog.setContentText("Enter new password:");

        dialog.showAndWait().ifPresent(newPassword -> {
            if (newPassword.trim().length() < 6) {
                showAlert("Error", "Password must be at least 6 characters long.");
                return;
            }

            try {
                // Update user password
                boolean passwordUpdated = userService.updateUserPassword(request.getUserId(), newPassword);

                if (passwordUpdated) {
                    // Mark request as approved
                    boolean requestProcessed = passwordResetService.processPasswordResetRequest(
                        request.getRequestId(),
                        PasswordResetRequest.RequestStatus.APPROVED,
                        sessionManager.getCurrentUser().getUserId(),
                        "Password reset approved and new password set."
                    );

                    if (requestProcessed) {
                        showAlert("Success", "Password reset approved successfully!\n\nNew password: " + newPassword +
                                "\n\nPlease inform the user of their new password.");
                        loadPasswordResetContent(); // Refresh the list
                    } else {
                        showAlert("Error", "Failed to update request status.");
                    }
                } else {
                    showAlert("Error", "Failed to update user password.");
                }

            } catch (Exception e) {
                logger.error("Error processing password reset approval", e);
                showAlert("Error", "An error occurred while processing the request: " + e.getMessage());
            }
        });
    }

    /**
     * Handle password reset rejection
     */
    private void handlePasswordResetRejection(PasswordResetRequest request) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reject Password Reset");
        dialog.setHeaderText("Reject Password Reset Request for " + request.getUsername());
        dialog.setContentText("Enter rejection reason:");

        dialog.showAndWait().ifPresent(reason -> {
            if (reason.trim().isEmpty()) {
                showAlert("Error", "Please provide a reason for rejection.");
                return;
            }

            try {
                boolean requestProcessed = passwordResetService.processPasswordResetRequest(
                    request.getRequestId(),
                    PasswordResetRequest.RequestStatus.REJECTED,
                    sessionManager.getCurrentUser().getUserId(),
                    reason
                );

                if (requestProcessed) {
                    showAlert("Success", "Password reset request rejected successfully.");
                    loadPasswordResetContent(); // Refresh the list
                } else {
                    showAlert("Error", "Failed to update request status.");
                }

            } catch (Exception e) {
                logger.error("Error processing password reset rejection", e);
                showAlert("Error", "An error occurred while processing the request: " + e.getMessage());
            }
        });
    }
}
