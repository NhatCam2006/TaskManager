package com.example.taskmanagerv3.controller;

import com.example.taskmanagerv3.model.User;
import com.example.taskmanagerv3.service.PasswordResetService;
import com.example.taskmanagerv3.service.UserService;
import com.example.taskmanagerv3.util.SessionManager;
import com.example.taskmanagerv3.util.UserPreferences;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

/**
 * Controller for login screen
 */
public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;
    @FXML private CheckBox rememberMeCheckBox;
    @FXML private Hyperlink forgotPasswordLink;

    private UserService userService;

    @FXML
    private void initialize() {
        userService = new UserService();

        // Initialize password reset table
        PasswordResetService.initializePasswordResetTable();

        // Set up enter key handling
        passwordField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleLogin();
            }
        });

        usernameField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                passwordField.requestFocus();
            }
        });

        // Clear status label when user starts typing
        usernameField.textProperty().addListener((obs, oldText, newText) -> clearStatus());
        passwordField.textProperty().addListener((obs, oldText, newText) -> clearStatus());

        // Load saved credentials if remember me is enabled
        loadSavedCredentials();

        // Set default focus
        Platform.runLater(() -> {
            if (usernameField.getText().isEmpty()) {
                usernameField.requestFocus();
            } else {
                passwordField.requestFocus();
            }
        });

        logger.info("Login controller initialized");
    }

    @FXML
    private void handleLogin() {


        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Validate input
        if (username.isEmpty()) {
            showError("Please enter username or email");
            usernameField.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            showError("Please enter password");
            passwordField.requestFocus();
            return;
        }

        // Disable login button during authentication
        loginButton.setDisable(true);
        showStatus("Authenticating...", false);

        // Perform authentication in background thread
        new Thread(() -> {
            try {
                Optional<User> userOpt = userService.authenticate(username, password);

                Platform.runLater(() -> {
                    loginButton.setDisable(false);

                    if (userOpt.isPresent()) {
                        User user = userOpt.get();

                        // Handle remember me functionality
                        if (rememberMeCheckBox.isSelected()) {
                            UserPreferences.saveRememberMeCredentials(username, password);
                        } else {
                            UserPreferences.clearRememberMeCredentials();
                        }

                        // Set up session
                        SessionManager.getInstance().login(user);

                        // Navigate to appropriate dashboard
                        navigateToDashboard(user);

                    } else {
                        showError("Invalid username/email or password");
                        passwordField.clear();
                        passwordField.requestFocus();
                    }
                });

            } catch (Exception e) {
                logger.error("Login error", e);
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    showError("Login failed: " + e.getMessage());
                });
            }
        }).start();




    }

    @FXML
    private void handleForgotPassword() {
        try {
            // Load forgot password dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/taskmanagerv3/forgot-password-dialog.fxml"));
            Scene scene = new Scene(loader.load());

            // Create new stage for dialog
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Password Reset Request");
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(forgotPasswordLink.getScene().getWindow());

            // Center dialog on parent window
            dialogStage.centerOnScreen();

            // Show dialog
            dialogStage.showAndWait();

        } catch (IOException e) {
            logger.error("Failed to load forgot password dialog", e);

            // Fallback to simple alert
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Password Recovery");
            alert.setHeaderText("Forgot Password");
            alert.setContentText("Please contact your system administrator to reset your password.\n\n" +
                                "Default admin credentials:\n" +
                                "Username: admin\n" +
                                "Password: admin123");
            alert.showAndWait();
        }
    }

    /**
     * Navigate to appropriate dashboard based on user role
     */
    private void navigateToDashboard(User user) {
        try {
            String fxmlFile;
            String title;

            if (user.isAdmin()) {
                fxmlFile = "admin-dashboard.fxml";
                title = "WorkFlow Manager - Admin Dashboard";
            } else {
                fxmlFile = "user-dashboard.fxml";
                title = "WorkFlow Manager - User Dashboard";
            }

            // Load dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/taskmanagerv3/" + fxmlFile));
            Scene scene = new Scene(loader.load(), 1200, 800);

            // Get current stage
            Stage currentStage = (Stage) loginButton.getScene().getWindow();

            // Set up new stage
            currentStage.setTitle(title);
            currentStage.setScene(scene);
            currentStage.setResizable(true);
            currentStage.centerOnScreen();
            currentStage.setMaximized(true);

            logger.info("Navigated to dashboard for user: {} ({})", user.getUsername(), user.getRole());

        } catch (IOException e) {
            logger.error("Failed to load dashboard", e);
            showError("Failed to load dashboard: " + e.getMessage());
        }
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        showStatus(message, true);
    }

    /**
     * Show status message
     */
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: blue;");
        statusLabel.setVisible(true);
    }

    /**
     * Clear status message
     */
    private void clearStatus() {
        statusLabel.setVisible(false);
    }

    @FXML
    private void handleExit() {
        Platform.exit();
    }

    /**
     * Load saved credentials if remember me is enabled
     */
    private void loadSavedCredentials() {
        if (UserPreferences.isRememberMeEnabled()) {
            String savedUsername = UserPreferences.getSavedUsername();
            String savedPassword = UserPreferences.getSavedPassword();

            if (!savedUsername.isEmpty() && !savedPassword.isEmpty()) {
                usernameField.setText(savedUsername);
                passwordField.setText(savedPassword);
                rememberMeCheckBox.setSelected(true);

                logger.info("Loaded saved credentials for user: {}", savedUsername);
            }
        }
    }
}
