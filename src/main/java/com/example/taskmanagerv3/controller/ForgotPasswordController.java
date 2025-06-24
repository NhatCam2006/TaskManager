package com.example.taskmanagerv3.controller;

import com.example.taskmanagerv3.service.PasswordResetService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for forgot password dialog
 */
public class ForgotPasswordController {
    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordController.class);

    @FXML private TextField usernameOrEmailField;
    @FXML private TextArea reasonTextArea;
    @FXML private Button submitButton;
    @FXML private Button cancelButton;
    @FXML private Label statusLabel;

    private PasswordResetService passwordResetService;

    @FXML
    private void initialize() {
        passwordResetService = new PasswordResetService();
        
        // Set up validation
        usernameOrEmailField.textProperty().addListener((obs, oldText, newText) -> {
            clearStatus();
            validateForm();
        });
        
        reasonTextArea.textProperty().addListener((obs, oldText, newText) -> {
            clearStatus();
            validateForm();
        });
        
        // Initial validation
        validateForm();
        
        logger.info("Forgot password controller initialized");
    }

    @FXML
    private void handleSubmit() {
        String usernameOrEmail = usernameOrEmailField.getText().trim();
        String reason = reasonTextArea.getText().trim();

        // Validate input
        if (usernameOrEmail.isEmpty()) {
            showError("Please enter your username or email");
            usernameOrEmailField.requestFocus();
            return;
        }

        if (reason.isEmpty()) {
            showError("Please provide a reason for password reset");
            reasonTextArea.requestFocus();
            return;
        }

        if (reason.length() < 10) {
            showError("Please provide a more detailed reason (at least 10 characters)");
            reasonTextArea.requestFocus();
            return;
        }

        // Disable submit button during processing
        submitButton.setDisable(true);
        showStatus("Submitting request...", false);

        // Submit request in background thread
        new Thread(() -> {
            try {
                boolean success = passwordResetService.submitPasswordResetRequest(usernameOrEmail, reason);

                javafx.application.Platform.runLater(() -> {
                    submitButton.setDisable(false);

                    if (success) {
                        showSuccessDialog();
                        closeDialog();
                    } else {
                        showError("Failed to submit request. User not found or you already have a pending request.");
                    }
                });

            } catch (Exception e) {
                logger.error("Error submitting password reset request", e);
                javafx.application.Platform.runLater(() -> {
                    submitButton.setDisable(false);
                    showError("An error occurred while submitting your request. Please try again.");
                });
            }
        }).start();
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    /**
     * Validate form and enable/disable submit button
     */
    private void validateForm() {
        boolean isValid = !usernameOrEmailField.getText().trim().isEmpty() &&
                         !reasonTextArea.getText().trim().isEmpty() &&
                         reasonTextArea.getText().trim().length() >= 10;
        
        submitButton.setDisable(!isValid);
    }

    /**
     * Show success dialog
     */
    private void showSuccessDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Request Submitted");
        alert.setHeaderText("Password Reset Request Submitted Successfully");
        alert.setContentText("Your password reset request has been submitted to the administrators. " +
                            "You will be notified once your request is processed.\n\n" +
                            "Please contact your system administrator if you need urgent assistance.");
        alert.showAndWait();
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

    /**
     * Close the dialog
     */
    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
