package com.example.taskmanagerv3.controller;

import com.example.taskmanagerv3.model.Task;
import com.example.taskmanagerv3.model.TaskStatus;
import com.example.taskmanagerv3.model.User;
import com.example.taskmanagerv3.model.UserRole;
import com.example.taskmanagerv3.service.TaskService;
import com.example.taskmanagerv3.service.UserService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for Edit User Dialog
 */
public class EditUserController {
    private static final Logger logger = LoggerFactory.getLogger(EditUserController.class);

    // Form fields
    @FXML private TextField usernameField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private ComboBox<UserRole> roleComboBox;
    @FXML private TextField departmentField;
    @FXML private TextField phoneField;
    @FXML private CheckBox activeCheckBox;

    // Password fields
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button generatePasswordButton;

    // Statistics labels
    @FXML private Label memberSinceLabel;
    @FXML private Label lastLoginLabel;
    @FXML private Label totalTasksLabel;
    @FXML private Label completedTasksLabel;

    // Action buttons
    @FXML private Button saveButton;
    @FXML private Button resetPasswordButton;
    @FXML private Button cancelButton;

    private UserService userService;
    private TaskService taskService;
    private User currentUser;
    private boolean userUpdated = false;

    @FXML
    private void initialize() {
        userService = new UserService();
        taskService = new TaskService();

        setupUI();
        logger.info("Edit user dialog initialized");
    }

    /**
     * Setup UI components and event handlers
     */
    private void setupUI() {
        // Populate role ComboBox
        roleComboBox.getItems().addAll(UserRole.values());

        // Setup button actions
        saveButton.setOnAction(e -> saveUser());
        resetPasswordButton.setOnAction(e -> resetPassword());
        cancelButton.setOnAction(e -> closeDialog());
        generatePasswordButton.setOnAction(e -> generateAndSetRandomPassword());

        // Setup field validation
        setupFieldValidation();
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
            if (!newPassword.isEmpty() && !newVal.equals(newPassword)) {
                confirmPasswordField.setStyle("-fx-border-color: red;");
            } else {
                confirmPasswordField.setStyle("");
            }
        });
    }

    /**
     * Set the user to edit
     */
    public void setUser(User user) {
        this.currentUser = user;
        loadUserData();
        loadUserStatistics();
    }

    /**
     * Load user data into form
     */
    private void loadUserData() {
        if (currentUser == null) return;

        usernameField.setText(currentUser.getUsername());
        fullNameField.setText(currentUser.getFullName() != null ? currentUser.getFullName() : "");
        emailField.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        roleComboBox.setValue(currentUser.getRole());
        departmentField.setText(currentUser.getDepartment() != null ? currentUser.getDepartment() : "");
        phoneField.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "");
        activeCheckBox.setSelected(currentUser.isActive());

        // Set member since and last login
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        if (currentUser.getCreatedAt() != null) {
            memberSinceLabel.setText(currentUser.getCreatedAt().format(formatter));
        }
        if (currentUser.getLastLogin() != null) {
            lastLoginLabel.setText(currentUser.getLastLogin().format(formatter));
        } else {
            lastLoginLabel.setText("Never");
        }
    }

    /**
     * Load user statistics
     */
    private void loadUserStatistics() {
        new Thread(() -> {
            try {
                List<Task> userTasks = taskService.getTasksByUserId(currentUser.getUserId());
                int totalTasks = userTasks.size();
                int completedTasks = (int) userTasks.stream()
                    .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                    .count();

                Platform.runLater(() -> {
                    totalTasksLabel.setText(String.valueOf(totalTasks));
                    completedTasksLabel.setText(String.valueOf(completedTasks));
                });

            } catch (Exception e) {
                logger.error("Error loading user statistics", e);
                Platform.runLater(() -> {
                    totalTasksLabel.setText("Error");
                    completedTasksLabel.setText("Error");
                });
            }
        }).start();
    }

    /**
     * Save user changes
     */
    private void saveUser() {
        try {
            // Validate form
            if (!validateForm()) {
                return;
            }

            // Update user object
            currentUser.setFullName(fullNameField.getText().trim());
            currentUser.setEmail(emailField.getText().trim());
            currentUser.setRole(roleComboBox.getValue());
            currentUser.setDepartment(departmentField.getText().trim());
            currentUser.setPhone(phoneField.getText().trim());
            currentUser.setActive(activeCheckBox.isSelected());

            // Handle password change if provided
            String newPassword = newPasswordField.getText();
            if (!newPassword.isEmpty()) {
                if (!newPassword.equals(confirmPasswordField.getText())) {
                    showAlert(Alert.AlertType.WARNING, "Validation Error", "Password confirmation does not match.");
                    return;
                }

                if (newPassword.length() < 6) {
                    showAlert(Alert.AlertType.WARNING, "Validation Error", "Password must be at least 6 characters long.");
                    return;
                }

                // Update password
                currentUser.setPasswordHash(org.mindrot.jbcrypt.BCrypt.hashpw(newPassword, org.mindrot.jbcrypt.BCrypt.gensalt()));
            }

            // Save to database
            if (userService.updateUser(currentUser)) {
                userUpdated = true;
                showAlert(Alert.AlertType.INFORMATION, "Success", "User updated successfully!");
                closeDialog();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update user.");
            }

        } catch (Exception e) {
            logger.error("Error saving user", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save user: " + e.getMessage());
        }
    }

    /**
     * Reset user password
     */
    private void resetPassword() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Reset Password");
        confirmAlert.setHeaderText("Reset User Password");
        confirmAlert.setContentText("Are you sure you want to reset the password for user: " + currentUser.getUsername() + "?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String newPassword = generateRandomPassword();

                try {
                    if (userService.changePassword(currentUser.getUserId(), "", newPassword)) {
                        showAlert(Alert.AlertType.INFORMATION, "Password Reset",
                                 "Password has been reset successfully!\n\nNew password: " + newPassword +
                                 "\n\nPlease provide this password to the user securely.");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to reset password.");
                    }
                } catch (Exception e) {
                    logger.error("Error resetting password", e);
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to reset password: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Generate and set random password
     */
    private void generateAndSetRandomPassword() {
        String password = generateRandomPassword();
        newPasswordField.setText(password);
        confirmPasswordField.setText(password);

        showAlert(Alert.AlertType.INFORMATION, "Password Generated",
                 "Random password generated: " + password + "\n\nPlease copy this password before saving.");
    }

    /**
     * Generate a random password
     */
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        return password.toString();
    }

    /**
     * Validate form fields
     */
    private boolean validateForm() {
        if (fullNameField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Full name is required.");
            return false;
        }

        if (roleComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Role is required.");
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
     * Check if user was updated
     */
    public boolean isUserUpdated() {
        return userUpdated;
    }

    /**
     * Close dialog
     */
    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
