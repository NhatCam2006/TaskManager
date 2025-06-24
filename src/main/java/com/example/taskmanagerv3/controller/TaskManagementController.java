package com.example.taskmanagerv3.controller;

import com.example.taskmanagerv3.model.Task;
import com.example.taskmanagerv3.model.TaskStatus;
import com.example.taskmanagerv3.model.User;
import com.example.taskmanagerv3.service.TaskService;
import com.example.taskmanagerv3.service.UserService;
import com.example.taskmanagerv3.util.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for Task Management Interface
 */
public class TaskManagementController {
    private static final Logger logger = LoggerFactory.getLogger(TaskManagementController.class);

    @FXML private TableView<Task> tasksTable;
    @FXML private TableColumn<Task, Integer> idColumn;
    @FXML private TableColumn<Task, String> titleColumn;
    @FXML private TableColumn<Task, String> statusColumn;
    @FXML private TableColumn<Task, String> priorityColumn;
    @FXML private TableColumn<Task, String> assignedUserColumn;
    @FXML private TableColumn<Task, String> dueDateColumn;
    @FXML private TableColumn<Task, Double> progressColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<TaskStatus> statusFilterComboBox;
    @FXML private Button refreshButton;
    @FXML private Button createTaskButton;
    @FXML private Button editTaskButton;
    @FXML private Button deleteTaskButton;
    @FXML private Button closeButton;

    @FXML private Label totalTasksLabel;
    @FXML private Label selectedTaskLabel;

    private TaskService taskService;
    private UserService userService;
    private SessionManager sessionManager;
    private ObservableList<Task> allTasks;
    private ObservableList<Task> filteredTasks;
    private Map<Integer, String> userIdToNameMap;

    @FXML
    private void initialize() {
        taskService = new TaskService();
        userService = new UserService();
        sessionManager = SessionManager.getInstance();

        allTasks = FXCollections.observableArrayList();
        filteredTasks = FXCollections.observableArrayList();

        setupTable();
        setupFilters();
        setupButtons();
        loadData();

        logger.info("Task Management interface initialized");
    }

    /**
     * Setup table columns and properties
     */
    private void setupTable() {
        // Setup columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("taskId"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));

        // Custom cell factories for better display
        statusColumn.setCellValueFactory(cellData -> {
            TaskStatus status = cellData.getValue().getStatus();
            return new javafx.beans.property.SimpleStringProperty(status.getDisplayName());
        });

        priorityColumn.setCellValueFactory(cellData -> {
            return new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getPriority().getDisplayName());
        });

        assignedUserColumn.setCellValueFactory(cellData -> {
            int userId = cellData.getValue().getAssignedUserId();
            String userName = userIdToNameMap != null ? userIdToNameMap.get(userId) : "Unknown";
            return new javafx.beans.property.SimpleStringProperty(userName);
        });

        dueDateColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDueDate() != null) {
                String formattedDate = cellData.getValue().getDueDate()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                return new javafx.beans.property.SimpleStringProperty(formattedDate);
            }
            return new javafx.beans.property.SimpleStringProperty("-");
        });

        progressColumn.setCellValueFactory(new PropertyValueFactory<>("progressPercentage"));

        // Custom cell factory for progress column
        progressColumn.setCellFactory(column -> new TableCell<Task, Double>() {
            @Override
            protected void updateItem(Double progress, boolean empty) {
                super.updateItem(progress, empty);
                if (empty || progress == null) {
                    setText(null);
                } else {
                    setText(String.format("%.1f%%", progress));
                }
            }
        });

        // Set table data
        tasksTable.setItems(filteredTasks);

        // Selection listener
        tasksTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            updateSelectedTaskInfo(newSelection);
            updateButtonStates();
        });

        // Double-click to edit
        tasksTable.setRowFactory(tv -> {
            TableRow<Task> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    editSelectedTask();
                }
            });
            return row;
        });
    }

    /**
     * Setup filters and search
     */
    private void setupFilters() {
        // Status filter
        statusFilterComboBox.getItems().add(null); // "All" option
        statusFilterComboBox.getItems().addAll(TaskStatus.values());
        statusFilterComboBox.setValue(null);

        // Custom cell factory for status filter
        statusFilterComboBox.setCellFactory(listView -> new ListCell<TaskStatus>() {
            @Override
            protected void updateItem(TaskStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText("All Tasks");
                } else {
                    setText(status.getDisplayName());
                }
            }
        });

        statusFilterComboBox.setButtonCell(new ListCell<TaskStatus>() {
            @Override
            protected void updateItem(TaskStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText("All Tasks");
                } else {
                    setText(status.getDisplayName());
                }
            }
        });

        // Filter listeners
        searchField.textProperty().addListener((obs, oldText, newText) -> applyFilters());
        statusFilterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
    }

    /**
     * Setup button actions and states
     */
    private void setupButtons() {
        refreshButton.setOnAction(e -> loadData());
        createTaskButton.setOnAction(e -> createNewTask());
        editTaskButton.setOnAction(e -> editSelectedTask());
        deleteTaskButton.setOnAction(e -> deleteSelectedTask());
        closeButton.setOnAction(e -> closeWindow());

        // Initial button states
        updateButtonStates();
    }

    /**
     * Load data from database
     */
    private void loadData() {
        new Thread(() -> {
            try {
                // Load users for mapping
                List<User> users = userService.getAllActiveUsers();
                userIdToNameMap = users.stream()
                    .collect(Collectors.toMap(User::getUserId, User::getDisplayName));

                // Load tasks
                List<Task> tasks = taskService.getAllTasks();

                Platform.runLater(() -> {
                    allTasks.clear();
                    allTasks.addAll(tasks);
                    applyFilters();
                    updateTaskCount();
                });

            } catch (Exception e) {
                logger.error("Error loading task data", e);
                Platform.runLater(() -> {
                    showAlert("Error", "Failed to load task data: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Apply search and status filters
     */
    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase().trim();
        TaskStatus statusFilter = statusFilterComboBox.getValue();

        List<Task> filtered = allTasks.stream()
            .filter(task -> {
                // Search filter
                if (!searchText.isEmpty()) {
                    return task.getTitle().toLowerCase().contains(searchText) ||
                           task.getDescription().toLowerCase().contains(searchText);
                }
                return true;
            })
            .filter(task -> {
                // Status filter
                return statusFilter == null || task.getStatus() == statusFilter;
            })
            .collect(Collectors.toList());

        filteredTasks.clear();
        filteredTasks.addAll(filtered);
        updateTaskCount();
    }

    /**
     * Update task count label
     */
    private void updateTaskCount() {
        totalTasksLabel.setText(String.format("Total: %d tasks", filteredTasks.size()));
    }

    /**
     * Update selected task info
     */
    private void updateSelectedTaskInfo(Task task) {
        if (task != null) {
            selectedTaskLabel.setText(String.format("Selected: %s", task.getTitle()));
        } else {
            selectedTaskLabel.setText("No task selected");
        }
    }

    /**
     * Update button states based on selection
     */
    private void updateButtonStates() {
        boolean hasSelection = tasksTable.getSelectionModel().getSelectedItem() != null;
        editTaskButton.setDisable(!hasSelection);
        deleteTaskButton.setDisable(!hasSelection);
    }

    /**
     * Create new task
     */
    @FXML
    private void createNewTask() {
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

            if (controller.isTaskCreated()) {
                loadData();
                showAlert("Success", "Task created successfully!");
            }

        } catch (IOException e) {
            logger.error("Failed to open create task dialog", e);
            showAlert("Error", "Failed to open create task dialog: " + e.getMessage());
        }
    }

    /**
     * Edit selected task
     */
    @FXML
    private void editSelectedTask() {
        Task selectedTask = tasksTable.getSelectionModel().getSelectedItem();
        if (selectedTask == null) {
            showAlert("No Selection", "Please select a task to edit");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/taskmanagerv3/edit-task-dialog.fxml"));
            VBox dialogContent = loader.load();

            EditTaskController controller = loader.getController();
            controller.setTask(selectedTask);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Task - " + selectedTask.getTitle());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(editTaskButton.getScene().getWindow());
            dialogStage.setScene(new Scene(dialogContent, 600, 700));
            dialogStage.setResizable(false);
            dialogStage.centerOnScreen();

            dialogStage.showAndWait();

            if (controller.isTaskUpdated()) {
                loadData();
                showAlert("Success", "Task updated successfully!");
            }

        } catch (IOException e) {
            logger.error("Failed to open edit task dialog", e);
            showAlert("Error", "Failed to open edit task dialog: " + e.getMessage());
        }
    }

    /**
     * Delete selected task
     */
    @FXML
    private void deleteSelectedTask() {
        Task selectedTask = tasksTable.getSelectionModel().getSelectedItem();
        if (selectedTask == null) {
            showAlert("No Selection", "Please select a task to delete");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Task");
        confirmAlert.setHeaderText("Are you sure you want to delete this task?");
        confirmAlert.setContentText("Task: " + selectedTask.getTitle() + "\nThis action cannot be undone.");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (taskService.deleteTask(selectedTask.getTaskId())) {
                loadData();
                showAlert("Success", "Task deleted successfully!");
            } else {
                showAlert("Error", "Failed to delete task. Please try again.");
            }
        }
    }

    /**
     * Close window
     */
    @FXML
    private void closeWindow() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
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
}
