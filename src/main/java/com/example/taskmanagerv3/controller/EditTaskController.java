package com.example.taskmanagerv3.controller;

import com.example.taskmanagerv3.model.Task;
import com.example.taskmanagerv3.model.TaskPriority;
import com.example.taskmanagerv3.model.TaskStatus;
import com.example.taskmanagerv3.model.User;
import com.example.taskmanagerv3.service.TaskService;
import com.example.taskmanagerv3.service.UserService;
import com.example.taskmanagerv3.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Controller for Edit Task Dialog
 */
public class EditTaskController {
    private static final Logger logger = LoggerFactory.getLogger(EditTaskController.class);
    
    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<TaskStatus> statusComboBox;
    @FXML private ComboBox<TaskPriority> priorityComboBox;
    @FXML private ComboBox<User> assignedUserComboBox;
    @FXML private DatePicker dueDatePicker;
    @FXML private Spinner<Integer> estimatedHoursSpinner;
    @FXML private Spinner<Integer> actualHoursSpinner;
    @FXML private Slider progressSlider;
    @FXML private Label progressLabel;
    @FXML private TextArea commentsArea;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    
    @FXML private Label taskIdLabel;
    @FXML private Label createdByLabel;
    @FXML private Label createdAtLabel;
    @FXML private Label updatedAtLabel;
    
    private TaskService taskService;
    private UserService userService;
    private SessionManager sessionManager;
    private Task currentTask;
    private boolean taskUpdated = false;
    
    @FXML
    private void initialize() {
        taskService = new TaskService();
        userService = new UserService();
        sessionManager = SessionManager.getInstance();
        
        setupUI();
        loadUsers();
        
        logger.info("Edit Task dialog initialized");
    }
    
    /**
     * Setup UI components
     */
    private void setupUI() {
        // Setup status combo box
        statusComboBox.getItems().addAll(TaskStatus.values());
        
        // Setup priority combo box
        priorityComboBox.getItems().addAll(TaskPriority.values());
        
        // Setup spinners
        estimatedHoursSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 200, 8));
        actualHoursSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 500, 0));
        
        // Setup progress slider
        progressSlider.setMin(0);
        progressSlider.setMax(100);
        progressSlider.setValue(0);
        progressSlider.setMajorTickUnit(25);
        progressSlider.setMinorTickCount(5);
        progressSlider.setShowTickLabels(true);
        progressSlider.setShowTickMarks(true);
        
        // Progress slider listener
        progressSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double progress = newVal.doubleValue();
            progressLabel.setText(String.format("%.1f%%", progress));
            
            // Auto-update status based on progress
            if (progress == 0) {
                statusComboBox.setValue(TaskStatus.TODO);
            } else if (progress == 100) {
                statusComboBox.setValue(TaskStatus.COMPLETED);
            } else if (progress > 0 && statusComboBox.getValue() == TaskStatus.TODO) {
                statusComboBox.setValue(TaskStatus.IN_PROGRESS);
            }
        });
        
        // Status change listener
        statusComboBox.valueProperty().addListener((obs, oldStatus, newStatus) -> {
            if (newStatus == TaskStatus.COMPLETED && progressSlider.getValue() < 100) {
                progressSlider.setValue(100);
            } else if (newStatus == TaskStatus.TODO && progressSlider.getValue() > 0) {
                progressSlider.setValue(0);
            }
        });
        
        // Setup button actions
        saveButton.setOnAction(e -> handleSaveTask());
        cancelButton.setOnAction(e -> handleCancel());
        
        // Setup validation
        titleField.textProperty().addListener((obs, oldText, newText) -> validateForm());
        assignedUserComboBox.valueProperty().addListener((obs, oldValue, newValue) -> validateForm());
        
        // Initial validation
        validateForm();
    }
    
    /**
     * Load users for assignment
     */
    private void loadUsers() {
        try {
            List<User> users = userService.getAllActiveUsers();
            assignedUserComboBox.getItems().addAll(users);
            
            // Set custom cell factory to display user names
            assignedUserComboBox.setCellFactory(listView -> new ListCell<User>() {
                @Override
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    if (empty || user == null) {
                        setText(null);
                    } else {
                        setText(user.getDisplayName() + " (" + user.getUsername() + ")");
                    }
                }
            });
            
            assignedUserComboBox.setButtonCell(new ListCell<User>() {
                @Override
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    if (empty || user == null) {
                        setText(null);
                    } else {
                        setText(user.getDisplayName() + " (" + user.getUsername() + ")");
                    }
                }
            });
            
        } catch (Exception e) {
            logger.error("Error loading users", e);
            showAlert("Error", "Failed to load users: " + e.getMessage());
        }
    }
    
    /**
     * Set task to edit
     */
    public void setTask(Task task) {
        this.currentTask = task;
        populateFields();
    }
    
    /**
     * Populate form fields with task data
     */
    private void populateFields() {
        if (currentTask == null) return;
        
        // Basic info
        taskIdLabel.setText("Task ID: " + currentTask.getTaskId());
        titleField.setText(currentTask.getTitle());
        descriptionArea.setText(currentTask.getDescription());
        statusComboBox.setValue(currentTask.getStatus());
        priorityComboBox.setValue(currentTask.getPriority());
        
        // Find and set assigned user
        int assignedUserId = currentTask.getAssignedUserId();
        assignedUserComboBox.getItems().stream()
            .filter(user -> user.getUserId() == assignedUserId)
            .findFirst()
            .ifPresent(assignedUserComboBox::setValue);
        
        // Dates
        if (currentTask.getDueDate() != null) {
            dueDatePicker.setValue(currentTask.getDueDate().toLocalDate());
        }
        
        // Hours and progress
        estimatedHoursSpinner.getValueFactory().setValue(currentTask.getEstimatedHours());
        actualHoursSpinner.getValueFactory().setValue(currentTask.getActualHours());
        progressSlider.setValue(currentTask.getProgressPercentage());
        
        // Comments
        commentsArea.setText(currentTask.getComments());
        
        // Metadata
        try {
            User createdByUser = userService.getUserById(currentTask.getCreatedByUserId()).orElse(null);
            if (createdByUser != null) {
                createdByLabel.setText("Created by: " + createdByUser.getDisplayName());
            }
        } catch (Exception e) {
            createdByLabel.setText("Created by: Unknown");
        }
        
        if (currentTask.getCreatedAt() != null) {
            createdAtLabel.setText("Created: " + currentTask.getCreatedAt().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }
        
        if (currentTask.getUpdatedAt() != null) {
            updatedAtLabel.setText("Last updated: " + currentTask.getUpdatedAt().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }
    }
    
    /**
     * Validate form inputs
     */
    private void validateForm() {
        boolean isValid = !titleField.getText().trim().isEmpty() && 
                         assignedUserComboBox.getValue() != null;
        
        saveButton.setDisable(!isValid);
    }
    
    /**
     * Handle save task
     */
    @FXML
    private void handleSaveTask() {
        try {
            // Validate inputs
            String title = titleField.getText().trim();
            if (title.isEmpty()) {
                showAlert("Validation Error", "Task title is required");
                return;
            }
            
            User assignedUser = assignedUserComboBox.getValue();
            if (assignedUser == null) {
                showAlert("Validation Error", "Please select a user to assign the task");
                return;
            }
            
            // Update task object
            currentTask.setTitle(title);
            currentTask.setDescription(descriptionArea.getText().trim());
            currentTask.setStatus(statusComboBox.getValue());
            currentTask.setPriority(priorityComboBox.getValue());
            currentTask.setAssignedUserId(assignedUser.getUserId());
            currentTask.setEstimatedHours(estimatedHoursSpinner.getValue());
            currentTask.setActualHours(actualHoursSpinner.getValue());
            currentTask.setProgressPercentage(progressSlider.getValue());
            currentTask.setComments(commentsArea.getText().trim());
            
            // Set due date if selected
            LocalDate dueDate = dueDatePicker.getValue();
            if (dueDate != null) {
                currentTask.setDueDate(LocalDateTime.of(dueDate, LocalTime.of(23, 59, 59)));
            } else {
                currentTask.setDueDate(null);
            }
            
            // Update task in database
            if (taskService.updateTask(currentTask)) {
                taskUpdated = true;
                showAlert("Success", "Task updated successfully!");
                closeDialog();
            } else {
                showAlert("Error", "Failed to update task. Please try again.");
            }
            
        } catch (Exception e) {
            logger.error("Error updating task", e);
            showAlert("Error", "Failed to update task: " + e.getMessage());
        }
    }
    
    /**
     * Handle cancel
     */
    @FXML
    private void handleCancel() {
        closeDialog();
    }
    
    /**
     * Close dialog
     */
    private void closeDialog() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
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
     * Check if task was updated
     */
    public boolean isTaskUpdated() {
        return taskUpdated;
    }
}
