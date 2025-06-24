package com.example.taskmanagerv3.controller;

import com.example.taskmanagerv3.model.Task;
import com.example.taskmanagerv3.model.TaskStatus;
import com.example.taskmanagerv3.service.TaskService;
import com.example.taskmanagerv3.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller for Update Progress Dialog
 */
public class UpdateProgressController {
    private static final Logger logger = LoggerFactory.getLogger(UpdateProgressController.class);

    // Task information display
    @FXML private Label taskTitleLabel;
    @FXML private TextArea taskDescriptionArea;
    @FXML private Label currentStatusLabel;
    @FXML private Label priorityLabel;
    @FXML private Label dueDateLabel;

    // Progress update controls
    @FXML private ComboBox<String> statusComboBox;
    @FXML private Slider progressSlider;
    @FXML private Label progressPercentageLabel;
    @FXML private Spinner<Integer> actualHoursSpinner;
    @FXML private TextArea commentsTextArea;

    // Time tracking display
    @FXML private Label estimatedHoursLabel;
    @FXML private Label currentActualHoursLabel;
    @FXML private Label remainingHoursLabel;
    @FXML private ProgressBar timeProgressBar;
    @FXML private Label timeProgressLabel;

    // Action buttons
    @FXML private Button saveButton;
    @FXML private Button saveAndCompleteButton;
    @FXML private Button cancelButton;

    // Progress history
    @FXML private ListView<String> progressHistoryListView;

    private TaskService taskService;
    private SessionManager sessionManager;
    private Task currentTask;

    @FXML
    private void initialize() {
        taskService = new TaskService();
        sessionManager = SessionManager.getInstance();

        setupUI();
        logger.info("Update progress dialog initialized");
    }

    /**
     * Setup UI components and event handlers
     */
    private void setupUI() {
        // Populate ComboBox items
        statusComboBox.getItems().addAll("TODO", "IN_PROGRESS", "REVIEW", "COMPLETED");

        // Setup spinner
        actualHoursSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1000, 0));

        // Setup progress slider
        progressSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int percentage = newVal.intValue();
            progressPercentageLabel.setText(percentage + "%");
            updateTimeTracking();

            // Auto-update status based on progress
            if (percentage == 0 && !statusComboBox.getValue().equals("TODO")) {
                statusComboBox.setValue("TODO");
            } else if (percentage > 0 && percentage < 100 && !statusComboBox.getValue().equals("IN_PROGRESS")) {
                statusComboBox.setValue("IN_PROGRESS");
            } else if (percentage == 100 && !statusComboBox.getValue().equals("COMPLETED")) {
                statusComboBox.setValue("COMPLETED");
            }
        });

        // Setup actual hours spinner listener
        actualHoursSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateTimeTracking();
        });

        // Setup button actions
        saveButton.setOnAction(e -> saveProgress());
        saveAndCompleteButton.setOnAction(e -> saveAndComplete());
        cancelButton.setOnAction(e -> closeDialog());

        // Setup status combo box listener
        statusComboBox.setOnAction(e -> {
            String status = statusComboBox.getValue();
            if ("COMPLETED".equals(status)) {
                progressSlider.setValue(100);
            } else if ("TODO".equals(status)) {
                progressSlider.setValue(0);
            } else if ("IN_PROGRESS".equals(status) && progressSlider.getValue() == 0) {
                progressSlider.setValue(10); // Start with some progress
            }
        });
    }

    /**
     * Set the task to update
     */
    public void setTask(Task task) {
        this.currentTask = task;
        loadTaskData();
    }

    /**
     * Load task data into the form
     */
    private void loadTaskData() {
        if (currentTask == null) return;

        // Display task information
        taskTitleLabel.setText(currentTask.getTitle());
        taskDescriptionArea.setText(currentTask.getDescription() != null ? currentTask.getDescription() : "No description");
        currentStatusLabel.setText(getStatusDisplay(currentTask.getStatus()));
        priorityLabel.setText(getPriorityDisplay(currentTask.getPriority()));
        dueDateLabel.setText(formatDueDate(currentTask.getDueDate()));

        // Set current values
        statusComboBox.setValue(currentTask.getStatus().name());
        progressSlider.setValue(currentTask.getProgressPercentage());
        actualHoursSpinner.getValueFactory().setValue(currentTask.getActualHours());

        if (currentTask.getComments() != null) {
            commentsTextArea.setText(currentTask.getComments());
        }

        // Update time tracking
        estimatedHoursLabel.setText(String.valueOf(currentTask.getEstimatedHours()));
        currentActualHoursLabel.setText(String.valueOf(currentTask.getActualHours()));
        updateTimeTracking();

        // Load progress history
        loadProgressHistory();
    }

    /**
     * Update time tracking display
     */
    private void updateTimeTracking() {
        if (currentTask == null) return;

        int estimatedHours = currentTask.getEstimatedHours();
        int actualHours = actualHoursSpinner.getValue();

        // Calculate remaining hours
        int remainingHours = Math.max(0, estimatedHours - actualHours);
        remainingHoursLabel.setText(String.valueOf(remainingHours));

        // Update progress bar
        if (estimatedHours > 0) {
            double timeProgress = Math.min(1.0, (double) actualHours / estimatedHours);
            timeProgressBar.setProgress(timeProgress);

            int timePercentage = (int) (timeProgress * 100);
            timeProgressLabel.setText(timePercentage + "% of estimated time used");

            // Change color based on progress vs completion
            double taskProgress = progressSlider.getValue() / 100.0;
            if (timeProgress > taskProgress + 0.2) { // Significantly over time
                timeProgressBar.setStyle("-fx-accent: #e74c3c;"); // Red
            } else if (timeProgress > taskProgress) { // Slightly over time
                timeProgressBar.setStyle("-fx-accent: #f39c12;"); // Orange
            } else {
                timeProgressBar.setStyle("-fx-accent: #27ae60;"); // Green
            }
        } else {
            timeProgressBar.setProgress(0);
            timeProgressLabel.setText("No time estimate set");
        }
    }

    /**
     * Load progress history
     */
    private void loadProgressHistory() {
        progressHistoryListView.getItems().clear();

        // Add current status as latest entry
        String currentEntry = String.format("📊 Current: %s (%.0f%%) - %s",
            getStatusDisplay(currentTask.getStatus()),
            currentTask.getProgressPercentage(),
            formatDateTime(currentTask.getUpdatedAt()));
        progressHistoryListView.getItems().add(currentEntry);

        // Add creation entry
        String createdEntry = String.format("🆕 Created: %s - %s",
            getStatusDisplay(TaskStatus.TODO),
            formatDateTime(currentTask.getCreatedAt()));
        progressHistoryListView.getItems().add(createdEntry);

        // Add completion entry if completed
        if (currentTask.getCompletedAt() != null) {
            String completedEntry = String.format("✅ Completed: %s",
                formatDateTime(currentTask.getCompletedAt()));
            progressHistoryListView.getItems().add(0, completedEntry);
        }
    }

    /**
     * Save progress updates
     */
    private void saveProgress() {
        try {
            updateTaskFromForm();

            if (taskService.updateTask(currentTask)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Task progress updated successfully!");
                closeDialog();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update task progress.");
            }

        } catch (Exception e) {
            logger.error("Error saving task progress", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save progress: " + e.getMessage());
        }
    }

    /**
     * Save progress and mark as complete
     */
    private void saveAndComplete() {
        try {
            updateTaskFromForm();
            currentTask.setStatus(TaskStatus.COMPLETED);
            currentTask.setProgressPercentage(100.0);
            currentTask.setCompletedAt(LocalDateTime.now());

            if (taskService.updateTask(currentTask)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Task marked as completed!");
                closeDialog();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to complete task.");
            }

        } catch (Exception e) {
            logger.error("Error completing task", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to complete task: " + e.getMessage());
        }
    }

    /**
     * Update task object from form data
     */
    private void updateTaskFromForm() {
        currentTask.setStatus(TaskStatus.valueOf(statusComboBox.getValue()));
        currentTask.setProgressPercentage(progressSlider.getValue());
        currentTask.setActualHours(actualHoursSpinner.getValue());

        String comments = commentsTextArea.getText();
        if (comments != null && !comments.trim().isEmpty()) {
            // Append new comments to existing ones
            String existingComments = currentTask.getComments();
            if (existingComments != null && !existingComments.trim().isEmpty()) {
                currentTask.setComments(existingComments + "\n\n--- " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) +
                    " ---\n" + comments);
            } else {
                currentTask.setComments(comments);
            }
        }

        currentTask.setUpdatedAt(LocalDateTime.now());

        // Set completion time if completed
        if (currentTask.getStatus() == TaskStatus.COMPLETED && currentTask.getCompletedAt() == null) {
            currentTask.setCompletedAt(LocalDateTime.now());
        }
    }

    /**
     * Close the dialog
     */
    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    // Utility methods
    private String getStatusDisplay(TaskStatus status) {
        return switch (status) {
            case TODO -> "📋 To Do";
            case IN_PROGRESS -> "⚡ In Progress";
            case REVIEW -> "👀 In Review";
            case COMPLETED -> "✅ Completed";
            case CANCELLED -> "❌ Cancelled";
        };
    }

    private String getPriorityDisplay(com.example.taskmanagerv3.model.TaskPriority priority) {
        return switch (priority) {
            case LOW -> "🟢 Low";
            case MEDIUM -> "🟡 Medium";
            case HIGH -> "🟠 High";
            case URGENT -> "🔴 Urgent";
        };
    }

    private String formatDueDate(LocalDateTime dueDate) {
        if (dueDate == null) return "No due date";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        return dueDate.format(formatter);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "Unknown";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        return dateTime.format(formatter);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
