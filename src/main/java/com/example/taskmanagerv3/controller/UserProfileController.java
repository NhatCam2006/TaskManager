package com.example.taskmanagerv3.controller;

import com.example.taskmanagerv3.model.Task;
import com.example.taskmanagerv3.model.Achievement;
import com.example.taskmanagerv3.model.TaskStatus;
import com.example.taskmanagerv3.model.User;
import com.example.taskmanagerv3.service.AchievementService;
import com.example.taskmanagerv3.service.TaskService;
import com.example.taskmanagerv3.service.UserService;
import com.example.taskmanagerv3.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.chart.PieChart;
import javafx.scene.shape.Circle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.ImagePattern;
import javafx.stage.FileChooser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for User Profile Management
 */
public class UserProfileController {
    private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);

    // Profile information fields
    @FXML private TextField usernameField;
    @FXML private TextField displayNameField;
    @FXML private TextField emailField;
    @FXML private TextField roleField;
    @FXML private TextField departmentField;
    @FXML private TextField phoneField;

    // Statistics labels
    @FXML private Label memberSinceLabel;
    @FXML private Label lastLoginLabel;
    @FXML private Label totalTasksLabel;
    @FXML private Label completedTasksLabel;
    @FXML private Label completionRateLabel;
    @FXML private Label averageRatingLabel;
    @FXML private Label streakLabel;

    // Enhanced Statistics Components
    @FXML private PieChart taskStatusPieChart;
    @FXML private ProgressBar completedProgressBar;
    @FXML private ProgressBar inProgressProgressBar;
    @FXML private ProgressBar pendingProgressBar;
    @FXML private ProgressBar overdueProgressBar;
    @FXML private Label completedPercentLabel;
    @FXML private Label inProgressPercentLabel;
    @FXML private Label pendingPercentLabel;
    @FXML private Label overduePercentLabel;

    // Avatar Components
    @FXML private Circle avatarCircle;
    @FXML private Label avatarPlaceholder;
    @FXML private Button uploadAvatarButton;
    @FXML private Label profileDisplayName;
    @FXML private Label profileRole;
    @FXML private Label quickTotalTasks;
    @FXML private Label quickCompletedTasks;
    @FXML private Label quickStreakDays;



    // Enhanced Notifications
    @FXML private CheckBox soundNotificationsCheckBox;
    @FXML private CheckBox desktopNotificationsCheckBox;
    @FXML private ComboBox<String> notificationFrequencyComboBox;

    // Achievements & Goals
    @FXML private ProgressBar dailyGoalProgress;
    @FXML private ProgressBar weeklyGoalProgress;
    @FXML private ProgressBar nextAchievementProgress;
    @FXML private Label dailyGoalLabel;
    @FXML private Label weeklyGoalLabel;

    // Password change fields
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    // Preferences
    @FXML private CheckBox emailNotificationsCheckBox;
    @FXML private CheckBox taskRemindersCheckBox;
    @FXML private CheckBox weeklyReportsCheckBox;
    @FXML private ComboBox<String> timeZoneComboBox;

    // Action buttons
    @FXML private Button saveProfileButton;
    @FXML private Button resetButton;
    @FXML private Button cancelButton;
    @FXML private Button changePasswordButton;

    private UserService userService;
    private TaskService taskService;
    private AchievementService achievementService;
    private SessionManager sessionManager;
    private User currentUser;
    private User originalUser; // To track changes

    @FXML
    private void initialize() {
        userService = new UserService();
        taskService = new TaskService();
        achievementService = new AchievementService();
        sessionManager = SessionManager.getInstance();

        setupUI();
        loadUserProfile();
        loadUserStatistics();

        logger.info("User profile initialized for user: {}", sessionManager.getCurrentUsername());
    }

    /**
     * Setup UI components and event handlers
     */
    private void setupUI() {
        // Setup basic components
        setupBasicComponents();

        // Setup avatar components
        setupAvatarComponents();



        // Setup notification components
        setupNotificationComponents();

        // Setup achievements components
        setupAchievementsComponents();

        // Setup button actions
        saveProfileButton.setOnAction(e -> saveProfile());
        resetButton.setOnAction(e -> resetChanges());
        cancelButton.setOnAction(e -> resetChanges());
        changePasswordButton.setOnAction(e -> changePassword());

        // Setup field validation
        setupFieldValidation();

        // Load saved preferences
        loadSavedPreferences();
    }

    /**
     * Setup basic components
     */
    private void setupBasicComponents() {
        // Populate timezone ComboBox
        timeZoneComboBox.getItems().addAll("UTC", "America/New_York", "America/Los_Angeles",
                                          "Europe/London", "Asia/Tokyo", "Australia/Sydney");
        timeZoneComboBox.setValue("UTC");
    }

    /**
     * Setup avatar components
     */
    private void setupAvatarComponents() {
        // Setup upload avatar button
        uploadAvatarButton.setOnAction(e -> uploadAvatar());

        // Initialize avatar circle
        avatarCircle.setVisible(false); // Hide until image is loaded
        avatarPlaceholder.setVisible(true);
    }



    /**
     * Setup notification components
     */
    private void setupNotificationComponents() {
        // Setup notification frequency ComboBox
        notificationFrequencyComboBox.getItems().addAll(
            "🔔 Immediate", "⏰ Every 15 minutes", "📅 Hourly",
            "🌅 Daily digest", "📊 Weekly summary"
        );
        notificationFrequencyComboBox.setValue("⏰ Every 15 minutes");
    }

    /**
     * Setup achievements components
     */
    private void setupAchievementsComponents() {
        // Initialize progress bars
        dailyGoalProgress.setProgress(0.33); // Example: 1/3 tasks
        weeklyGoalProgress.setProgress(0.53); // Example: 8/15 tasks
        nextAchievementProgress.setProgress(0.6); // Example: 30/50 tasks
    }

    /**
     * Setup field validation
     */
    private void setupFieldValidation() {
        // Email validation
        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty() && !isValidEmail(newVal)) {
                emailField.setStyle("-fx-border-color: red;");
            } else {
                emailField.setStyle("");
            }
        });

        // Phone validation
        phoneField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty() && !isValidPhone(newVal)) {
                phoneField.setStyle("-fx-border-color: red;");
            } else {
                phoneField.setStyle("");
            }
        });

        // Password confirmation validation
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            String newPassword = newPasswordField.getText();
            if (newVal != null && !newVal.isEmpty() && !newVal.equals(newPassword)) {
                confirmPasswordField.setStyle("-fx-border-color: red;");
            } else {
                confirmPasswordField.setStyle("");
            }
        });
    }

    /**
     * Load user profile data
     */
    private void loadUserProfile() {
        currentUser = sessionManager.getCurrentUser();
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No user session found.");
            return;
        }

        // Create a copy for tracking changes
        originalUser = new User();
        originalUser.setUserId(currentUser.getUserId());
        originalUser.setUsername(currentUser.getUsername());
        originalUser.setFullName(currentUser.getFullName());
        originalUser.setEmail(currentUser.getEmail());
        originalUser.setRole(currentUser.getRole());
        originalUser.setDepartment(currentUser.getDepartment());
        originalUser.setPhone(currentUser.getPhone());
        originalUser.setCreatedAt(currentUser.getCreatedAt());
        originalUser.setLastLogin(currentUser.getLastLogin());

        // Populate fields
        usernameField.setText(currentUser.getUsername());
        displayNameField.setText(currentUser.getDisplayName());
        emailField.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        roleField.setText(currentUser.getRole().toString());
        departmentField.setText(currentUser.getDepartment() != null ? currentUser.getDepartment() : "");
        phoneField.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "");

        // Update avatar section
        profileDisplayName.setText(currentUser.getDisplayName());
        profileRole.setText(currentUser.getRole().toString());

        // Set member since and last login
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        if (currentUser.getCreatedAt() != null) {
            memberSinceLabel.setText(currentUser.getCreatedAt().format(formatter));
        }
        if (currentUser.getLastLogin() != null) {
            lastLoginLabel.setText(currentUser.getLastLogin().format(formatter));
        }

        // Load avatar if exists
        loadUserAvatar();
    }

    /**
     * Load user statistics with enhanced dashboard
     */
    private void loadUserStatistics() {
        new Thread(() -> {
            try {
                // Validate session manager and current user
                if (sessionManager == null || currentUser == null) {
                    logger.error("Session manager or current user is null");
                    return;
                }

                int userId = sessionManager.getCurrentUserId();
                if (userId <= 0) {
                    logger.error("Invalid user ID: {}", userId);
                    return;
                }

                List<Task> userTasks = taskService.getTasksByUserId(userId);
                if (userTasks == null) {
                    logger.warn("User tasks list is null, using empty list");
                    userTasks = new ArrayList<>();
                }

                // Calculate basic statistics
                int totalTasks = userTasks.size();
                int completedTasks = (int) userTasks.stream()
                    .filter(task -> task != null && task.getStatus() == TaskStatus.COMPLETED)
                    .count();
                int inProgressTasks = (int) userTasks.stream()
                    .filter(task -> task != null && (task.getStatus() == TaskStatus.IN_PROGRESS || task.getStatus() == TaskStatus.REVIEW))
                    .count();
                int pendingTasks = (int) userTasks.stream()
                    .filter(task -> task != null && task.getStatus() == TaskStatus.TODO)
                    .count();
                int overdueTasks = (int) userTasks.stream()
                    .filter(task -> task != null && task.isOverdue())
                    .count();

                double completionRate = totalTasks > 0 ? (double) completedTasks / totalTasks * 100 : 0;

                // Calculate streak (consecutive days with completed tasks)
                int streak = calculateCompletionStreak(userTasks);

                // Calculate percentages for progress bars
                double completedPercent = totalTasks > 0 ? (double) completedTasks / totalTasks * 100 : 0;
                double inProgressPercent = totalTasks > 0 ? (double) inProgressTasks / totalTasks * 100 : 0;
                double pendingPercent = totalTasks > 0 ? (double) pendingTasks / totalTasks * 100 : 0;
                double overduePercent = totalTasks > 0 ? (double) overdueTasks / totalTasks * 100 : 0;

                Platform.runLater(() -> {
                    try {
                        // Update basic stats with null checks
                        if (totalTasksLabel != null) totalTasksLabel.setText(String.valueOf(totalTasks));
                        if (completedTasksLabel != null) completedTasksLabel.setText(String.valueOf(completedTasks));
                        if (completionRateLabel != null) completionRateLabel.setText(String.format("%.1f%%", completionRate));
                        if (averageRatingLabel != null) averageRatingLabel.setText("N/A");
                        if (streakLabel != null) streakLabel.setText(String.valueOf(streak));

                        // Update quick stats in avatar section with null checks
                        if (quickTotalTasks != null) quickTotalTasks.setText(String.valueOf(totalTasks));
                        if (quickCompletedTasks != null) quickCompletedTasks.setText(String.valueOf(completedTasks));
                        if (quickStreakDays != null) quickStreakDays.setText(String.valueOf(streak));

                        // Update progress bars
                        updateProgressBars(completedPercent, inProgressPercent, pendingPercent, overduePercent);

                        // Update pie chart
                        updatePieChart(completedTasks, inProgressTasks, pendingTasks, overdueTasks);

                        // Update achievements
                        updateAchievements(totalTasks, completedTasks, streak);

                    } catch (Exception e) {
                        logger.error("Error updating UI with statistics", e);
                    }
                });

            } catch (Exception e) {
                logger.error("Error loading user statistics", e);
                Platform.runLater(() -> {
                    try {
                        if (totalTasksLabel != null) totalTasksLabel.setText("Error");
                        if (completedTasksLabel != null) completedTasksLabel.setText("Error");
                        if (completionRateLabel != null) completionRateLabel.setText("Error");
                        if (streakLabel != null) streakLabel.setText("0");
                    } catch (Exception uiError) {
                        logger.error("Error updating UI with error message", uiError);
                    }
                });
            }
        }).start();
    }

    /**
     * Save profile changes
     */
    private void saveProfile() {
        try {
            // Validate fields
            if (!validateFields()) {
                return;
            }

            // Update user object
            currentUser.setFullName(displayNameField.getText().trim());
            currentUser.setEmail(emailField.getText().trim());
            currentUser.setDepartment(departmentField.getText().trim());
            currentUser.setPhone(phoneField.getText().trim());



            // Save notification preferences
            saveNotificationPreferences();

            // Save to database
            if (userService.updateUser(currentUser)) {
                // Update session
                sessionManager.updateCurrentUser(currentUser);

                showAlert(Alert.AlertType.INFORMATION, "Success", "Profile and preferences updated successfully!");

                // Update original user for change tracking
                originalUser.setFullName(currentUser.getFullName());
                originalUser.setEmail(currentUser.getEmail());
                originalUser.setDepartment(currentUser.getDepartment());
                originalUser.setPhone(currentUser.getPhone());

                logger.info("Profile updated successfully for user: {}", currentUser.getUsername());

            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update profile.");
            }

        } catch (Exception e) {
            logger.error("Error saving profile", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save profile: " + e.getMessage());
        }
    }

    /**
     * Change password
     */
    private void changePassword() {
        try {
            String currentPassword = currentPasswordField.getText();
            String newPassword = newPasswordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            // Validate password fields
            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "All password fields are required.");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "New password and confirmation do not match.");
                return;
            }

            if (newPassword.length() < 6) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "New password must be at least 6 characters long.");
                return;
            }

            // Verify current password and update
            if (userService.changePassword(currentUser.getUserId(), currentPassword, newPassword)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Password changed successfully!");

                // Clear password fields
                currentPasswordField.clear();
                newPasswordField.clear();
                confirmPasswordField.clear();

            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to change password. Please check your current password.");
            }

        } catch (Exception e) {
            logger.error("Error changing password", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to change password: " + e.getMessage());
        }
    }

    /**
     * Reset changes to original values
     */
    private void resetChanges() {
        if (originalUser != null) {
            displayNameField.setText(originalUser.getFullName());
            emailField.setText(originalUser.getEmail() != null ? originalUser.getEmail() : "");
            departmentField.setText(originalUser.getDepartment() != null ? originalUser.getDepartment() : "");
            phoneField.setText(originalUser.getPhone() != null ? originalUser.getPhone() : "");

            // Clear password fields
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();

            showAlert(Alert.AlertType.INFORMATION, "Reset", "Changes have been reset to original values.");
        }
    }

    /**
     * Validate form fields
     */
    private boolean validateFields() {
        if (displayNameField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Display name is required.");
            return false;
        }

        String email = emailField.getText().trim();
        if (!email.isEmpty() && !isValidEmail(email)) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter a valid email address.");
            return false;
        }

        String phone = phoneField.getText().trim();
        if (!phone.isEmpty() && !isValidPhone(phone)) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter a valid phone number.");
            return false;
        }

        return true;
    }

    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * Validate phone format
     */
    private boolean isValidPhone(String phone) {
        return phone.matches("^[+]?[0-9\\s\\-\\(\\)]{10,}$");
    }



    /**
     * Calculate completion streak (consecutive days with completed tasks)
     */
    private int calculateCompletionStreak(List<Task> userTasks) {
        try {
            LocalDate today = LocalDate.now();
            int streak = 0;
            LocalDate checkDate = today;

            // Check each day going backwards
            for (int i = 0; i < 30; i++) { // Check last 30 days max
                final LocalDate currentDate = checkDate;
                boolean hasCompletedTask = userTasks.stream()
                    .anyMatch(task -> task.getStatus() == TaskStatus.COMPLETED &&
                                    task.getUpdatedAt() != null &&
                                    task.getUpdatedAt().toLocalDate().equals(currentDate));

                if (hasCompletedTask) {
                    streak++;
                    checkDate = checkDate.minusDays(1);
                } else {
                    break; // Streak broken
                }
            }

            return streak;
        } catch (Exception e) {
            logger.error("Error calculating completion streak", e);
            return 0;
        }
    }

    /**
     * Update progress bars with calculated percentages
     */
    private void updateProgressBars(double completedPercent, double inProgressPercent,
                                   double pendingPercent, double overduePercent) {
        try {
            // Update progress bars
            completedProgressBar.setProgress(completedPercent / 100.0);
            inProgressProgressBar.setProgress(inProgressPercent / 100.0);
            pendingProgressBar.setProgress(pendingPercent / 100.0);
            overdueProgressBar.setProgress(overduePercent / 100.0);

            // Update percentage labels
            completedPercentLabel.setText(String.format("%.1f%%", completedPercent));
            inProgressPercentLabel.setText(String.format("%.1f%%", inProgressPercent));
            pendingPercentLabel.setText(String.format("%.1f%%", pendingPercent));
            overduePercentLabel.setText(String.format("%.1f%%", overduePercent));

        } catch (Exception e) {
            logger.error("Error updating progress bars", e);
        }
    }

    /**
     * Update pie chart with task status data
     */
    private void updatePieChart(int completed, int inProgress, int pending, int overdue) {
        try {
            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

            if (completed > 0) {
                pieChartData.add(new PieChart.Data("Completed (" + completed + ")", completed));
            }
            if (inProgress > 0) {
                pieChartData.add(new PieChart.Data("In Progress (" + inProgress + ")", inProgress));
            }
            if (pending > 0) {
                pieChartData.add(new PieChart.Data("Pending (" + pending + ")", pending));
            }
            if (overdue > 0) {
                pieChartData.add(new PieChart.Data("Overdue (" + overdue + ")", overdue));
            }

            if (pieChartData.isEmpty()) {
                pieChartData.add(new PieChart.Data("No Tasks", 1));
            }

            taskStatusPieChart.setData(pieChartData);
            taskStatusPieChart.setLegendVisible(false);
            taskStatusPieChart.setLabelsVisible(true);

            // Apply custom colors to pie chart
            Platform.runLater(() -> {
                applyPieChartColors();
            });

        } catch (Exception e) {
            logger.error("Error updating pie chart", e);
        }
    }

    /**
     * Apply custom colors to pie chart segments
     */
    private void applyPieChartColors() {
        try {
            taskStatusPieChart.getData().forEach(data -> {
                String dataName = data.getName().toLowerCase();
                if (dataName.contains("completed")) {
                    data.getNode().setStyle("-fx-pie-color: #27ae60;");
                } else if (dataName.contains("progress")) {
                    data.getNode().setStyle("-fx-pie-color: #3498db;");
                } else if (dataName.contains("pending")) {
                    data.getNode().setStyle("-fx-pie-color: #f39c12;");
                } else if (dataName.contains("overdue")) {
                    data.getNode().setStyle("-fx-pie-color: #e74c3c;");
                } else {
                    data.getNode().setStyle("-fx-pie-color: #95a5a6;");
                }
            });
        } catch (Exception e) {
            logger.error("Error applying pie chart colors", e);
        }
    }

    /**
     * Upload avatar image
     */
    private void uploadAvatar() {
        try {
            // Validate current user
            if (currentUser == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "No user session found.");
                return;
            }

            // Validate UI components
            if (uploadAvatarButton == null || uploadAvatarButton.getScene() == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "UI components not properly initialized.");
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Profile Picture");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
            );

            Stage stage = (Stage) uploadAvatarButton.getScene().getWindow();
            File selectedFile = fileChooser.showOpenDialog(stage);

            if (selectedFile != null && selectedFile.exists()) {
                // Validate file size (max 5MB)
                long fileSizeInMB = selectedFile.length() / (1024 * 1024);
                if (fileSizeInMB > 5) {
                    showAlert(Alert.AlertType.WARNING, "Warning", "File size too large. Please select an image smaller than 5MB.");
                    return;
                }

                // Create avatars directory if it doesn't exist
                Path avatarsDir = Paths.get("avatars");
                if (!Files.exists(avatarsDir)) {
                    Files.createDirectories(avatarsDir);
                }

                // Copy file to avatars directory with user ID as filename
                String fileName = "user_" + currentUser.getUserId() + "_avatar.png";
                Path targetPath = avatarsDir.resolve(fileName);
                Files.copy(selectedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                // Update user profile picture path in database
                currentUser.setProfilePicture(targetPath.toString());
                boolean updateSuccess = userService.updateUser(currentUser);

                if (updateSuccess) {
                    // Load and display the avatar
                    loadAvatarImage(targetPath.toString());

                    logger.info("Avatar uploaded successfully for user: {}", currentUser.getUsername());
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Profile picture updated successfully!");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to save profile picture to database.");
                }

            }
        } catch (Exception e) {
            logger.error("Error uploading avatar", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to upload profile picture: " + e.getMessage());
        }
    }

    /**
     * Load user avatar
     */
    private void loadUserAvatar() {
        try {
            if (currentUser.getProfilePicture() != null && !currentUser.getProfilePicture().isEmpty()) {
                loadAvatarImage(currentUser.getProfilePicture());
            }
        } catch (Exception e) {
            logger.error("Error loading user avatar", e);
        }
    }

    /**
     * Load avatar image from path
     */
    private void loadAvatarImage(String imagePath) {
        try {
            // Validate input
            if (imagePath == null || imagePath.trim().isEmpty()) {
                logger.warn("Avatar image path is null or empty");
                return;
            }

            // Validate UI components
            if (avatarCircle == null || avatarPlaceholder == null) {
                logger.warn("Avatar UI components are not initialized");
                return;
            }

            File imageFile = new File(imagePath);
            if (imageFile.exists() && imageFile.isFile()) {
                // Load image with error handling
                Image image = new Image(imageFile.toURI().toString());

                // Check if image loaded successfully
                if (!image.isError()) {
                    ImagePattern imagePattern = new ImagePattern(image);

                    // Update UI on JavaFX Application Thread
                    Platform.runLater(() -> {
                        try {
                            avatarCircle.setFill(imagePattern);
                            avatarCircle.setVisible(true);
                            avatarPlaceholder.setVisible(false);
                            logger.debug("Avatar image loaded successfully: {}", imagePath);
                        } catch (Exception e) {
                            logger.error("Error updating avatar UI", e);
                        }
                    });
                } else {
                    logger.error("Failed to load image: {}", imagePath);
                }
            } else {
                logger.warn("Avatar image file does not exist: {}", imagePath);
            }
        } catch (Exception e) {
            logger.error("Error loading avatar image: {}", imagePath, e);
        }
    }



    /**
     * Update achievements based on user statistics
     */
    private void updateAchievements(int totalTasks, int completedTasks, int streak) {
        try {
            // Get user tasks for achievement calculation
            List<Task> userTasks = taskService.getTasksByUserId(currentUser.getUserId());

            // Calculate achievements using AchievementService
            List<Achievement> achievements = achievementService.calculateAchievements(currentUser.getUserId(), userTasks);

            // Update daily goal (complete 3 tasks today)
            int dailyGoal = 3;
            int tasksCompletedToday = calculateTasksCompletedToday(userTasks);
            if (dailyGoalProgress != null) {
                dailyGoalProgress.setProgress(Math.min(1.0, (double) tasksCompletedToday / dailyGoal));
            }
            if (dailyGoalLabel != null) {
                dailyGoalLabel.setText(String.format("Complete %d tasks today (%d/%d)",
                                                   dailyGoal, tasksCompletedToday, dailyGoal));
            }

            // Update weekly goal (complete 15 tasks this week)
            int weeklyGoal = 15;
            int tasksCompletedThisWeek = calculateTasksCompletedThisWeek(userTasks);
            if (weeklyGoalProgress != null) {
                weeklyGoalProgress.setProgress(Math.min(1.0, (double) tasksCompletedThisWeek / weeklyGoal));
            }
            if (weeklyGoalLabel != null) {
                weeklyGoalLabel.setText(String.format("Complete %d tasks this week (%d/%d)",
                                                    weeklyGoal, tasksCompletedThisWeek, weeklyGoal));
            }

            // Update next achievement progress
            Achievement nextAchievement = achievementService.getNextAchievement(achievements);
            if (nextAchievement != null && nextAchievementProgress != null) {
                nextAchievementProgress.setProgress(nextAchievement.getProgress());

                // Update next achievement label (find it in FXML or create dynamically)
                logger.info("Next achievement: {} - Progress: {}",
                           nextAchievement.getName(), nextAchievement.getProgressText());
            }

            // Log recently unlocked achievements
            List<Achievement> recentlyUnlocked = achievementService.getRecentlyUnlocked(achievements, 7);
            if (!recentlyUnlocked.isEmpty()) {
                logger.info("Recently unlocked achievements: {}",
                           recentlyUnlocked.stream().map(Achievement::getName).toArray());
            }

        } catch (Exception e) {
            logger.error("Error updating achievements", e);
        }
    }

    /**
     * Calculate tasks completed today
     */
    private int calculateTasksCompletedToday(List<Task> userTasks) {
        LocalDate today = LocalDate.now();
        return (int) userTasks.stream()
            .filter(task -> task.getStatus() == TaskStatus.COMPLETED &&
                          task.getCompletedAt() != null &&
                          task.getCompletedAt().toLocalDate().equals(today))
            .count();
    }

    /**
     * Calculate tasks completed this week
     */
    private int calculateTasksCompletedThisWeek(List<Task> userTasks) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);

        return (int) userTasks.stream()
            .filter(task -> task.getStatus() == TaskStatus.COMPLETED &&
                          task.getCompletedAt() != null &&
                          !task.getCompletedAt().toLocalDate().isBefore(startOfWeek))
            .count();
    }



    /**
     * Save notification preferences
     */
    private void saveNotificationPreferences() {
        try {
            // Get notification settings
            boolean emailNotifications = emailNotificationsCheckBox.isSelected();
            boolean taskReminders = taskRemindersCheckBox.isSelected();
            boolean weeklyReports = weeklyReportsCheckBox.isSelected();
            boolean soundNotifications = soundNotificationsCheckBox.isSelected();
            boolean desktopNotifications = desktopNotificationsCheckBox.isSelected();

            // Get frequency and timezone
            String notificationFrequency = notificationFrequencyComboBox.getValue();
            String timeZone = timeZoneComboBox.getValue();

            // Save to preferences (you can implement a preferences service later)
            logger.info("Notification preferences saved - Email: {}, Reminders: {}, Reports: {}, Sound: {}, Desktop: {}, Frequency: {}, TimeZone: {}",
                       emailNotifications, taskReminders, weeklyReports, soundNotifications,
                       desktopNotifications, notificationFrequency, timeZone);

        } catch (Exception e) {
            logger.error("Error saving notification preferences", e);
        }
    }

    /**
     * Load saved preferences
     */
    private void loadSavedPreferences() {
        try {
            // Load notification preferences
            emailNotificationsCheckBox.setSelected(true);
            taskRemindersCheckBox.setSelected(true);
            weeklyReportsCheckBox.setSelected(false);
            soundNotificationsCheckBox.setSelected(true);
            desktopNotificationsCheckBox.setSelected(true);
            notificationFrequencyComboBox.setValue("⏰ Every 15 minutes");
            timeZoneComboBox.setValue("UTC");

            logger.info("Preferences loaded successfully");

        } catch (Exception e) {
            logger.error("Error loading preferences", e);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
