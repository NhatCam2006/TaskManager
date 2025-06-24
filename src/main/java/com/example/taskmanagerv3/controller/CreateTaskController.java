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
 * Controller for Create Task Dialog
 */
public class CreateTaskController {
    private static final Logger logger = LoggerFactory.getLogger(CreateTaskController.class);
    
    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<TaskPriority> priorityComboBox;
    @FXML private ComboBox<User> assignedUserComboBox;
    @FXML private DatePicker dueDatePicker;
    @FXML private Spinner<Integer> estimatedHoursSpinner;
    @FXML private TextArea commentsArea;
    @FXML private Button createButton;
    @FXML private Button cancelButton;
    
    private TaskService taskService;
    private UserService userService;
    private SessionManager sessionManager;
    private boolean taskCreated = false;
    
    @FXML
    private void initialize() {
        taskService = new TaskService();
        userService = new UserService();
        sessionManager = SessionManager.getInstance();
        
        setupUI();
        loadUsers();
        
        logger.info("Create Task dialog initialized");
    }
    
    /**
     * Setup UI components
     */
    private void setupUI() {
        // Setup priority combo box
        priorityComboBox.getItems().addAll(TaskPriority.values());
        priorityComboBox.setValue(TaskPriority.MEDIUM);
        
        // Setup estimated hours spinner
        estimatedHoursSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 8));
        
        // Setup button actions
        createButton.setOnAction(e -> handleCreateTask());
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
     * Validate form inputs
     */
    private void validateForm() {
        boolean isValid = !titleField.getText().trim().isEmpty() && 
                         assignedUserComboBox.getValue() != null;
        
        createButton.setDisable(!isValid);
    }
    
    /**
     * Handle create task
     */
    @FXML
    private void handleCreateTask() {
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
            
            // Create task object
            Task task = new Task();
            task.setTitle(title);
            task.setDescription(descriptionArea.getText().trim());
            task.setStatus(TaskStatus.TODO);
            task.setPriority(priorityComboBox.getValue());
            task.setAssignedUserId(assignedUser.getUserId());
            task.setCreatedByUserId(sessionManager.getCurrentUserId());
            task.setEstimatedHours(estimatedHoursSpinner.getValue());
            task.setComments(commentsArea.getText().trim());
            
            // Set due date if selected
            LocalDate dueDate = dueDatePicker.getValue();
            if (dueDate != null) {
                task.setDueDate(LocalDateTime.of(dueDate, LocalTime.of(23, 59, 59)));
            }
            
            // Create task
            if (taskService.createTask(task)) {
                taskCreated = true;
                showAlert("Success", "Task created successfully!");
                closeDialog();
            } else {
                showAlert("Error", "Failed to create task. Please try again.");
            }
            
        } catch (Exception e) {
            logger.error("Error creating task", e);
            showAlert("Error", "Failed to create task: " + e.getMessage());
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
        Stage stage = (Stage) createButton.getScene().getWindow();
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
     * Check if task was created
     */
    public boolean isTaskCreated() {
        return taskCreated;
    }
}
