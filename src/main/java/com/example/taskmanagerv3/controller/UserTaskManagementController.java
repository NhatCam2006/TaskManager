package com.example.taskmanagerv3.controller;

import com.example.taskmanagerv3.model.Task;
import com.example.taskmanagerv3.model.TaskStatus;
import com.example.taskmanagerv3.model.TaskPriority;
import com.example.taskmanagerv3.service.TaskService;
import com.example.taskmanagerv3.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for User Task Management interface
 */
public class UserTaskManagementController {
    private static final Logger logger = LoggerFactory.getLogger(UserTaskManagementController.class);

    // Header components
    @FXML private Button refreshButton;
    @FXML private Button backButton;

    // Filter components
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private ComboBox<String> priorityFilterComboBox;
    @FXML private ComboBox<String> dueDateFilterComboBox;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private Button applyFiltersButton;
    @FXML private Button clearFiltersButton;

    // Search components
    @FXML private TextField searchTextField;
    @FXML private Button searchButton;

    // Statistics labels
    @FXML private Label totalTasksLabel;
    @FXML private Label pendingTasksLabel;
    @FXML private Label inProgressTasksLabel;
    @FXML private Label completedTasksLabel;
    @FXML private Label overdueTasksLabel;

    // Task list container
    @FXML private VBox taskListContainer;

    // Action buttons
    @FXML private Button updateProgressButton;
    @FXML private Button requestHelpButton;
    @FXML private Button viewCalendarButton;

    private TaskService taskService;
    private SessionManager sessionManager;
    private List<Task> allTasks;
    private List<Task> filteredTasks;

    @FXML
    private void initialize() {
        taskService = new TaskService();
        sessionManager = SessionManager.getInstance();

        setupUI();
        loadTasks();

        logger.info("User task management initialized for user: {}",
                   sessionManager.getCurrentUsername());
    }

    /**
     * Setup UI components and event handlers
     */
    private void setupUI() {
        // Populate ComboBox items
        statusFilterComboBox.getItems().addAll("All", "TODO", "IN_PROGRESS", "REVIEW", "COMPLETED");
        priorityFilterComboBox.getItems().addAll("All", "LOW", "MEDIUM", "HIGH", "URGENT");
        dueDateFilterComboBox.getItems().addAll("All", "Overdue", "Today", "This Week", "This Month");
        sortComboBox.getItems().addAll("Due Date", "Priority", "Status", "Created Date", "Title");

        // Set default values
        statusFilterComboBox.setValue("All");
        priorityFilterComboBox.setValue("All");
        dueDateFilterComboBox.setValue("All");
        sortComboBox.setValue("Due Date");

        // Setup button actions
        refreshButton.setOnAction(e -> loadTasks());
        backButton.setOnAction(e -> goBackToDashboard());
        applyFiltersButton.setOnAction(e -> applyFilters());
        clearFiltersButton.setOnAction(e -> clearFilters());
        searchButton.setOnAction(e -> performSearch());

        updateProgressButton.setOnAction(e -> showUpdateProgressDialog());
        requestHelpButton.setOnAction(e -> showRequestHelpDialog());
        viewCalendarButton.setOnAction(e -> showCalendarView());

        // Setup search on Enter key
        searchTextField.setOnAction(e -> performSearch());

        // Setup filter change listeners
        statusFilterComboBox.setOnAction(e -> applyFilters());
        priorityFilterComboBox.setOnAction(e -> applyFilters());
        dueDateFilterComboBox.setOnAction(e -> applyFilters());
        sortComboBox.setOnAction(e -> applyFilters());
    }

    /**
     * Load user's tasks
     */
    private void loadTasks() {
        new Thread(() -> {
            try {
                int currentUserId = sessionManager.getCurrentUserId();
                allTasks = taskService.getTasksByUserId(currentUserId);

                Platform.runLater(() -> {
                    updateStatistics();
                    applyFilters();
                });

            } catch (Exception e) {
                logger.error("Error loading tasks", e);
                Platform.runLater(() -> {
                    showAlert("Error", "Failed to load tasks: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Update task statistics
     */
    private void updateStatistics() {
        if (allTasks == null) return;

        int total = allTasks.size();
        int pending = (int) allTasks.stream()
            .filter(task -> task.getStatus() == TaskStatus.TODO)
            .count();
        int inProgress = (int) allTasks.stream()
            .filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS)
            .count();
        int completed = (int) allTasks.stream()
            .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
            .count();
        int overdue = (int) allTasks.stream()
            .filter(Task::isOverdue)
            .count();

        totalTasksLabel.setText(String.valueOf(total));
        pendingTasksLabel.setText(String.valueOf(pending));
        inProgressTasksLabel.setText(String.valueOf(inProgress));
        completedTasksLabel.setText(String.valueOf(completed));
        overdueTasksLabel.setText(String.valueOf(overdue));
    }

    /**
     * Apply filters and sorting
     */
    private void applyFilters() {
        if (allTasks == null) return;

        filteredTasks = allTasks.stream()
            .filter(this::matchesStatusFilter)
            .filter(this::matchesPriorityFilter)
            .filter(this::matchesDueDateFilter)
            .filter(this::matchesSearchFilter)
            .collect(Collectors.toList());

        // Apply sorting
        String sortBy = sortComboBox.getValue();
        switch (sortBy) {
            case "Due Date" -> filteredTasks.sort((t1, t2) -> {
                if (t1.getDueDate() == null && t2.getDueDate() == null) return 0;
                if (t1.getDueDate() == null) return 1;
                if (t2.getDueDate() == null) return -1;
                return t1.getDueDate().compareTo(t2.getDueDate());
            });
            case "Priority" -> filteredTasks.sort((t1, t2) ->
                Integer.compare(t2.getPriority().getLevel(), t1.getPriority().getLevel()));
            case "Status" -> filteredTasks.sort((t1, t2) ->
                t1.getStatus().compareTo(t2.getStatus()));
            case "Created Date" -> filteredTasks.sort((t1, t2) ->
                t2.getCreatedAt().compareTo(t1.getCreatedAt()));
            case "Title" -> filteredTasks.sort((t1, t2) ->
                t1.getTitle().compareToIgnoreCase(t2.getTitle()));
        }

        displayTasks();
    }

    /**
     * Display filtered tasks
     */
    private void displayTasks() {
        taskListContainer.getChildren().clear();

        if (filteredTasks.isEmpty()) {
            Label noTasksLabel = new Label("No tasks found matching the current filters.");
            noTasksLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-padding: 20;");
            taskListContainer.getChildren().add(noTasksLabel);
            return;
        }

        for (Task task : filteredTasks) {
            VBox taskCard = createTaskCard(task);
            taskListContainer.getChildren().add(taskCard);
        }
    }

    /**
     * Create a task card UI component
     */
    private VBox createTaskCard(Task task) {
        VBox card = new VBox(10);
        card.setStyle(getTaskCardStyle(task));
        card.setPadding(new Insets(15));

        // Title and status row
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label statusLabel = new Label(getStatusIcon(task.getStatus()));
        statusLabel.setStyle("-fx-font-size: 16px;");

        Label titleLabel = new Label(task.getTitle());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label priorityLabel = new Label(getPriorityIcon(task.getPriority()));
        priorityLabel.setStyle("-fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button updateButton = new Button("Update");
        updateButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 3;");
        updateButton.setOnAction(e -> showUpdateProgressDialog(task));

        titleRow.getChildren().addAll(statusLabel, titleLabel, priorityLabel, spacer, updateButton);

        // Description
        Label descriptionLabel = new Label(task.getDescription());
        descriptionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        descriptionLabel.setWrapText(true);

        // Details row
        HBox detailsRow = new HBox(20);
        detailsRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label dueDateLabel = new Label("Due: " + formatDueDate(task.getDueDate()));
        dueDateLabel.setStyle("-fx-font-size: 12px;");

        Label progressLabel = new Label("Progress: " + (int)task.getProgressPercentage() + "%");
        progressLabel.setStyle("-fx-font-size: 12px;");

        detailsRow.getChildren().addAll(dueDateLabel, progressLabel);

        card.getChildren().addAll(titleRow, descriptionLabel, detailsRow);
        return card;
    }

    // Filter helper methods
    private boolean matchesStatusFilter(Task task) {
        String filter = statusFilterComboBox.getValue();
        return "All".equals(filter) || task.getStatus().name().equals(filter);
    }

    private boolean matchesPriorityFilter(Task task) {
        String filter = priorityFilterComboBox.getValue();
        return "All".equals(filter) || task.getPriority().name().equals(filter);
    }

    private boolean matchesDueDateFilter(Task task) {
        String filter = dueDateFilterComboBox.getValue();
        if ("All".equals(filter)) return true;

        LocalDateTime now = LocalDateTime.now();
        return switch (filter) {
            case "Overdue" -> task.isOverdue();
            case "Today" -> task.getDueDate() != null &&
                           task.getDueDate().toLocalDate().equals(now.toLocalDate());
            case "This Week" -> task.getDueDate() != null &&
                              ChronoUnit.DAYS.between(now, task.getDueDate()) <= 7;
            case "This Month" -> task.getDueDate() != null &&
                               ChronoUnit.DAYS.between(now, task.getDueDate()) <= 30;
            default -> true;
        };
    }

    private boolean matchesSearchFilter(Task task) {
        String searchText = searchTextField.getText();
        if (searchText == null || searchText.trim().isEmpty()) return true;

        String lowerSearchText = searchText.toLowerCase();
        return task.getTitle().toLowerCase().contains(lowerSearchText) ||
               (task.getDescription() != null && task.getDescription().toLowerCase().contains(lowerSearchText));
    }

    /**
     * Clear all filters
     */
    private void clearFilters() {
        statusFilterComboBox.setValue("All");
        priorityFilterComboBox.setValue("All");
        dueDateFilterComboBox.setValue("All");
        searchTextField.clear();
        applyFilters();
    }

    /**
     * Perform search
     */
    private void performSearch() {
        applyFilters();
    }

    /**
     * Show update progress dialog
     */
    private void showUpdateProgressDialog() {
        showAlert("Update Progress", "Please select a task to update progress.");
    }

    private void showUpdateProgressDialog(Task task) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/taskmanagerv3/update-progress-dialog.fxml"));
            Scene scene = new Scene(loader.load());

            UpdateProgressController controller = loader.getController();
            controller.setTask(task);

            Stage stage = new Stage();
            stage.setTitle("Update Progress - " + task.getTitle());
            stage.setScene(scene);
            stage.setResizable(false);
            stage.showAndWait();

            // Refresh tasks after update
            loadTasks();

        } catch (IOException e) {
            logger.error("Failed to load update progress dialog", e);
            showAlert("Error", "Failed to open update progress dialog: " + e.getMessage());
        }
    }

    /**
     * Show request help dialog
     */
    private void showRequestHelpDialog() {
        showAlert("Request Help", "Help request feature will be implemented soon.");
    }

    /**
     * Show calendar view
     */
    private void showCalendarView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/taskmanagerv3/user-calendar.fxml"));
            Scene scene = new Scene(loader.load(), 1000, 700);

            Stage stage = new Stage();
            stage.setTitle("Calendar View");
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            logger.error("Failed to load calendar view", e);
            showAlert("Error", "Failed to open calendar view: " + e.getMessage());
        }
    }

    /**
     * Go back to dashboard
     */
    private void goBackToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/taskmanagerv3/user-dashboard.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);

            Stage currentStage = (Stage) backButton.getScene().getWindow();
            currentStage.setTitle("WorkFlow Manager - User Dashboard");
            currentStage.setScene(scene);
            currentStage.centerOnScreen();

        } catch (IOException e) {
            logger.error("Failed to load user dashboard", e);
            showAlert("Error", "Failed to return to dashboard: " + e.getMessage());
        }
    }

    // Utility methods
    private String getStatusIcon(TaskStatus status) {
        return switch (status) {
            case TODO -> "📋";
            case IN_PROGRESS -> "⚡";
            case REVIEW -> "👀";
            case COMPLETED -> "✅";
            case CANCELLED -> "❌";
        };
    }

    private String getPriorityIcon(TaskPriority priority) {
        return switch (priority) {
            case LOW -> "🟢";
            case MEDIUM -> "🟡";
            case HIGH -> "🟠";
            case URGENT -> "🔴";
        };
    }

    private String getTaskCardStyle(Task task) {
        String baseStyle = "-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 5; -fx-background-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 2, 0, 0, 1);";

        if (task.isOverdue()) {
            return baseStyle + " -fx-border-color: #e74c3c; -fx-border-width: 2;";
        }

        return baseStyle;
    }

    private String formatDueDate(LocalDateTime dueDate) {
        if (dueDate == null) return "No due date";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        return dueDate.format(formatter);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
