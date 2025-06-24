package com.example.taskmanagerv3.controller;

import com.example.taskmanagerv3.model.Task;
import com.example.taskmanagerv3.service.TaskService;
import com.example.taskmanagerv3.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for Request Help Dialog
 */
public class RequestHelpController {
    private static final Logger logger = LoggerFactory.getLogger(RequestHelpController.class);

    // Form fields
    @FXML private ComboBox<String> requestTypeComboBox;
    @FXML private ComboBox<String> priorityComboBox;
    @FXML private ComboBox<String> relatedTaskComboBox;
    @FXML private TextField subjectField;
    @FXML private TextArea descriptionArea;

    // Contact preference radio buttons
    @FXML private RadioButton emailContactRadio;
    @FXML private RadioButton phoneContactRadio;
    @FXML private RadioButton inPersonContactRadio;

    // Action buttons
    @FXML private Button sendRequestButton;
    @FXML private Button saveAsDraftButton;
    @FXML private Button cancelButton;

    // Help request history
    @FXML private ListView<String> helpRequestHistoryListView;

    private TaskService taskService;
    private SessionManager sessionManager;
    private ToggleGroup contactMethodGroup;

    @FXML
    private void initialize() {
        taskService = new TaskService();
        sessionManager = SessionManager.getInstance();

        setupUI();
        loadUserTasks();
        loadHelpRequestHistory();

        logger.info("Request help dialog initialized for user: {}", sessionManager.getCurrentUsername());
    }

    /**
     * Setup UI components and event handlers
     */
    private void setupUI() {
        // Populate ComboBox items
        requestTypeComboBox.getItems().addAll("Task Assistance", "Technical Support", "Resource Request",
                                             "Deadline Extension", "Training Request", "General Question", "Other");
        priorityComboBox.getItems().addAll("Low", "Medium", "High", "Urgent");
        relatedTaskComboBox.getItems().add("None");

        // Set default values
        requestTypeComboBox.setValue("General Question");
        priorityComboBox.setValue("Medium");
        relatedTaskComboBox.setValue("None");

        // Setup contact method radio button group
        contactMethodGroup = new ToggleGroup();
        emailContactRadio.setToggleGroup(contactMethodGroup);
        phoneContactRadio.setToggleGroup(contactMethodGroup);
        inPersonContactRadio.setToggleGroup(contactMethodGroup);

        // Setup button actions
        sendRequestButton.setOnAction(e -> sendHelpRequest());
        saveAsDraftButton.setOnAction(e -> saveAsDraft());
        cancelButton.setOnAction(e -> closeDialog());

        // Setup field validation
        setupFieldValidation();
    }

    /**
     * Setup field validation
     */
    private void setupFieldValidation() {
        // Subject field validation
        subjectField.textProperty().addListener((obs, oldVal, newVal) -> {
            updateSendButtonState();
        });

        // Description area validation
        descriptionArea.textProperty().addListener((obs, oldVal, newVal) -> {
            updateSendButtonState();
        });

        // Initial button state
        updateSendButtonState();
    }

    /**
     * Update send button state based on form validation
     */
    private void updateSendButtonState() {
        boolean isValid = !subjectField.getText().trim().isEmpty() &&
                         !descriptionArea.getText().trim().isEmpty();
        sendRequestButton.setDisable(!isValid);
    }

    /**
     * Load user's tasks for the related task dropdown
     */
    private void loadUserTasks() {
        new Thread(() -> {
            try {
                int currentUserId = sessionManager.getCurrentUserId();
                List<Task> userTasks = taskService.getTasksByUserId(currentUserId);

                Platform.runLater(() -> {
                    relatedTaskComboBox.getItems().clear();
                    relatedTaskComboBox.getItems().add("None");

                    for (Task task : userTasks) {
                        String taskDisplay = String.format("[%s] %s",
                            task.getStatus().name(), task.getTitle());
                        relatedTaskComboBox.getItems().add(taskDisplay);
                    }
                });

            } catch (Exception e) {
                logger.error("Error loading user tasks", e);
            }
        }).start();
    }

    /**
     * Load help request history (placeholder implementation)
     */
    private void loadHelpRequestHistory() {
        helpRequestHistoryListView.getItems().clear();

        // Add some sample history entries
        helpRequestHistoryListView.getItems().addAll(
            "📧 Task Assistance - Resolved (2 days ago)",
            "🔧 Technical Support - In Progress (1 week ago)",
            "📚 Training Request - Completed (2 weeks ago)"
        );
    }

    /**
     * Send help request
     */
    private void sendHelpRequest() {
        try {
            // Validate form
            if (!validateForm()) {
                return;
            }

            // Create help request data
            String requestType = requestTypeComboBox.getValue();
            String priority = priorityComboBox.getValue();
            String relatedTask = relatedTaskComboBox.getValue();
            String subject = subjectField.getText().trim();
            String description = descriptionArea.getText().trim();
            String contactMethod = getSelectedContactMethod();

            // Format help request message
            String helpRequestMessage = formatHelpRequest(requestType, priority, relatedTask,
                                                        subject, description, contactMethod);

            // In a real implementation, this would be sent to administrators
            // For now, we'll just log it and show a success message
            logger.info("Help request submitted by user {}: {}",
                       sessionManager.getCurrentUsername(), helpRequestMessage);

            // Show success message
            showAlert(Alert.AlertType.INFORMATION, "Request Sent",
                     "Your help request has been sent successfully!\n\n" +
                     "An administrator will review your request and contact you using your preferred method.\n\n" +
                     "Request ID: HR-" + System.currentTimeMillis());

            // Add to history
            String historyEntry = String.format("📤 %s - Submitted (%s)",
                requestType, LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")));
            helpRequestHistoryListView.getItems().add(0, historyEntry);

            // Clear form
            clearForm();

        } catch (Exception e) {
            logger.error("Error sending help request", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to send help request: " + e.getMessage());
        }
    }

    /**
     * Save request as draft
     */
    private void saveAsDraft() {
        try {
            // In a real implementation, this would save to database
            showAlert(Alert.AlertType.INFORMATION, "Draft Saved",
                     "Your help request has been saved as a draft.\n\n" +
                     "You can continue editing and send it later.");

            logger.info("Help request saved as draft by user: {}", sessionManager.getCurrentUsername());

        } catch (Exception e) {
            logger.error("Error saving draft", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save draft: " + e.getMessage());
        }
    }

    /**
     * Validate form fields
     */
    private boolean validateForm() {
        if (subjectField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Subject is required.");
            subjectField.requestFocus();
            return false;
        }

        if (descriptionArea.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Description is required.");
            descriptionArea.requestFocus();
            return false;
        }

        if (contactMethodGroup.getSelectedToggle() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please select a contact method.");
            return false;
        }

        return true;
    }

    /**
     * Get selected contact method
     */
    private String getSelectedContactMethod() {
        RadioButton selected = (RadioButton) contactMethodGroup.getSelectedToggle();
        return selected != null ? selected.getText() : "Email";
    }

    /**
     * Format help request message
     */
    private String formatHelpRequest(String requestType, String priority, String relatedTask,
                                   String subject, String description, String contactMethod) {
        StringBuilder sb = new StringBuilder();
        sb.append("Help Request Details:\n");
        sb.append("===================\n");
        sb.append("User: ").append(sessionManager.getCurrentUsername()).append("\n");
        sb.append("Request Type: ").append(requestType).append("\n");
        sb.append("Priority: ").append(priority).append("\n");
        sb.append("Related Task: ").append(relatedTask).append("\n");
        sb.append("Subject: ").append(subject).append("\n");
        sb.append("Contact Method: ").append(contactMethod).append("\n");
        sb.append("Submitted: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        sb.append("Description:\n");
        sb.append(description);

        return sb.toString();
    }

    /**
     * Clear form fields
     */
    private void clearForm() {
        requestTypeComboBox.setValue("General Question");
        priorityComboBox.setValue("Medium");
        relatedTaskComboBox.setValue("None");
        subjectField.clear();
        descriptionArea.clear();
        emailContactRadio.setSelected(true);
    }

    /**
     * Close dialog
     */
    private void closeDialog() {
        // Check if there are unsaved changes
        if (hasUnsavedChanges()) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Unsaved Changes");
            confirmAlert.setHeaderText("You have unsaved changes.");
            confirmAlert.setContentText("Do you want to save as draft before closing?");

            ButtonType saveButton = new ButtonType("Save as Draft");
            ButtonType discardButton = new ButtonType("Discard");
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            confirmAlert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == saveButton) {
                    saveAsDraft();
                    closeWindow();
                } else if (response == discardButton) {
                    closeWindow();
                }
                // If cancel, do nothing (dialog stays open)
            });
        } else {
            closeWindow();
        }
    }

    /**
     * Check if there are unsaved changes
     */
    private boolean hasUnsavedChanges() {
        return !subjectField.getText().trim().isEmpty() ||
               !descriptionArea.getText().trim().isEmpty();
    }

    /**
     * Close the window
     */
    private void closeWindow() {
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
