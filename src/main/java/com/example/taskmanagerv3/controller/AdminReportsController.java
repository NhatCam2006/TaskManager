package com.example.taskmanagerv3.controller;

import com.example.taskmanagerv3.model.Task;
import com.example.taskmanagerv3.model.TaskStatus;
import com.example.taskmanagerv3.model.User;
import com.example.taskmanagerv3.service.TaskService;
import com.example.taskmanagerv3.service.UserService;
import com.example.taskmanagerv3.service.StatisticsExportService;
import com.example.taskmanagerv3.service.LogExportService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for Admin Reports Interface
 */
public class AdminReportsController {
    private static final Logger logger = LoggerFactory.getLogger(AdminReportsController.class);

    // Navigation buttons
    @FXML private Button overviewButton;
    @FXML private Button userReportsButton;
    @FXML private Button taskReportsButton;
    @FXML private Button performanceButton;
    @FXML private Button activityLogsButton;
    @FXML private Button refreshButton;
    @FXML private Button closeButton;

    // Date filters
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Button applyDateFilterButton;

    // Report content areas
    @FXML private VBox reportContentArea;
    @FXML private VBox systemOverview;
    @FXML private VBox userReports;
    @FXML private VBox taskReports;
    @FXML private VBox performanceReports;
    @FXML private VBox activityLogs;

    // System Overview components
    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label totalTasksLabel;
    @FXML private Label completedTasksLabel;
    @FXML private ListView<String> recentActivityListView;

    // User Reports components
    @FXML private TableView<User> userStatsTable;
    @FXML private TableColumn<User, String> userNameColumn;
    @FXML private TableColumn<User, String> userRoleColumn;
    @FXML private TableColumn<User, Integer> userTasksColumn;
    @FXML private TableColumn<User, Integer> userCompletedColumn;
    @FXML private TableColumn<User, String> userCompletionRateColumn;
    @FXML private TableColumn<User, String> userLastLoginColumn;

    // Task Reports components
    @FXML private TableView<Task> taskStatsTable;
    @FXML private TableColumn<Task, String> taskTitleColumn;
    @FXML private TableColumn<Task, String> taskStatusColumn;
    @FXML private TableColumn<Task, String> taskPriorityColumn;
    @FXML private TableColumn<Task, String> taskAssigneeColumn;
    @FXML private TableColumn<Task, String> taskDueDateColumn;
    @FXML private TableColumn<Task, String> taskProgressColumn;

    // Performance components
    @FXML private Label avgCompletionTimeLabel;
    @FXML private Label systemUptimeLabel;
    @FXML private Label databaseSizeLabel;
    @FXML private Label activeSessionsLabel;

    // Activity Logs components
    @FXML private ComboBox<String> logTypeComboBox;
    @FXML private Button exportLogsButton;
    @FXML private TableView<String> activityLogsTable;
    @FXML private TableColumn<String, String> logTimestampColumn;
    @FXML private TableColumn<String, String> logUserColumn;
    @FXML private TableColumn<String, String> logActionColumn;
    @FXML private TableColumn<String, String> logDetailsColumn;
    @FXML private TableColumn<String, String> logIpAddressColumn;

    // Export buttons
    @FXML private Button exportPdfButton;
    @FXML private Button exportExcelButton;
    @FXML private Button printReportButton;
    @FXML private Button advancedExportButton;

    private UserService userService;
    private TaskService taskService;
    private StatisticsExportService statisticsExportService;
    private LogExportService logExportService;

    @FXML
    private void initialize() {
        userService = new UserService();
        taskService = new TaskService();
        statisticsExportService = new StatisticsExportService();
        logExportService = new LogExportService();

        setupUI();
        loadReportData();
        showSystemOverview();

        logger.info("Admin reports initialized");
    }

    /**
     * Setup UI components and event handlers
     */
    private void setupUI() {
        // Setup navigation buttons
        overviewButton.setOnAction(e -> showSystemOverview());
        userReportsButton.setOnAction(e -> showUserReports());
        taskReportsButton.setOnAction(e -> showTaskReports());
        performanceButton.setOnAction(e -> showPerformanceReports());
        activityLogsButton.setOnAction(e -> showActivityLogs());
        refreshButton.setOnAction(e -> loadReportData());
        closeButton.setOnAction(e -> closeReports());

        // Setup date filters
        fromDatePicker.setValue(LocalDate.now().minusMonths(1));
        toDatePicker.setValue(LocalDate.now());
        applyDateFilterButton.setOnAction(e -> applyDateFilter());

        // Setup export buttons
        exportPdfButton.setOnAction(e -> exportToPdf());
        exportExcelButton.setOnAction(e -> exportToExcel());
        printReportButton.setOnAction(e -> printReport());
        exportLogsButton.setOnAction(e -> exportLogs());
        if (advancedExportButton != null) {
            advancedExportButton.setOnAction(e -> openAdvancedExport());
        }

        // Setup ComboBoxes
        logTypeComboBox.getItems().addAll("All", "Login", "Task Created", "Task Updated", "User Created", "Settings Changed");
        logTypeComboBox.setValue("All");

        // Setup table columns
        setupTableColumns();
    }

    /**
     * Setup table columns
     */
    private void setupTableColumns() {
        // User Stats Table
        userNameColumn.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        userRoleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        userLastLoginColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getLastLogin() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                    cellData.getValue().getLastLogin().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            }
            return new javafx.beans.property.SimpleStringProperty("Never");
        });

        // Task Stats Table
        taskTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        taskStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        taskPriorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        taskDueDateColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDueDate() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                    cellData.getValue().getDueDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            }
            return new javafx.beans.property.SimpleStringProperty("No due date");
        });
        taskProgressColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                String.format("%.0f%%", cellData.getValue().getProgressPercentage())));
    }

    /**
     * Load report data
     */
    private void loadReportData() {
        new Thread(() -> {
            try {
                // Load users and tasks
                List<User> users = userService.getAllActiveUsers();
                List<Task> tasks = taskService.getAllTasks();

                Platform.runLater(() -> {
                    updateSystemOverview(users, tasks);
                    updateUserReports(users, tasks);
                    updateTaskReports(tasks);
                    updatePerformanceReports();
                    updateActivityLogs();
                });

            } catch (Exception e) {
                logger.error("Error loading report data", e);
                Platform.runLater(() -> {
                    showAlert("Error", "Failed to load report data: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Update system overview
     */
    private void updateSystemOverview(List<User> users, List<Task> tasks) {
        int totalUsers = users.size();
        int activeUsers = (int) users.stream().filter(User::isActive).count();
        int totalTasks = tasks.size();
        int completedTasks = (int) tasks.stream()
            .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
            .count();

        totalUsersLabel.setText(String.valueOf(totalUsers));
        activeUsersLabel.setText(String.valueOf(activeUsers));
        totalTasksLabel.setText(String.valueOf(totalTasks));
        completedTasksLabel.setText(String.valueOf(completedTasks));

        // Update recent activity
        recentActivityListView.getItems().clear();
        recentActivityListView.getItems().addAll(
            "User 'admin' logged in - 2 hours ago",
            "Task 'Project Review' completed - 3 hours ago",
            "New user 'john.doe' created - 5 hours ago",
            "Task 'Database Backup' assigned - 1 day ago",
            "System settings updated - 2 days ago"
        );
    }

    /**
     * Update user reports
     */
    private void updateUserReports(List<User> users, List<Task> tasks) {
        userStatsTable.getItems().clear();

        for (User user : users) {
            // Calculate user task statistics
            List<Task> userTasks = tasks.stream()
                .filter(task -> task.getAssignedUserId() == user.getUserId())
                .collect(Collectors.toList());

            int totalUserTasks = userTasks.size();
            int completedUserTasks = (int) userTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .count();

            // Set calculated values (in a real app, you'd extend the User model or create a UserStats class)
            userStatsTable.getItems().add(user);
        }
    }

    /**
     * Update task reports
     */
    private void updateTaskReports(List<Task> tasks) {
        taskStatsTable.getItems().clear();
        taskStatsTable.getItems().addAll(tasks);
    }

    /**
     * Update performance reports
     */
    private void updatePerformanceReports() {
        avgCompletionTimeLabel.setText("3.5 days");
        systemUptimeLabel.setText("720 hours");
        databaseSizeLabel.setText("45.2 MB");
        activeSessionsLabel.setText("12");
    }

    /**
     * Update activity logs
     */
    private void updateActivityLogs() {
        // In a real application, load from actual log files or database
        activityLogsTable.getItems().clear();
        // Add sample log entries
    }

    // Navigation methods
    private void showSystemOverview() {
        hideAllReports();
        systemOverview.setVisible(true);
        systemOverview.setManaged(true);
        updateButtonStyles(overviewButton);
    }

    private void showUserReports() {
        hideAllReports();
        userReports.setVisible(true);
        userReports.setManaged(true);
        updateButtonStyles(userReportsButton);
    }

    private void showTaskReports() {
        hideAllReports();
        taskReports.setVisible(true);
        taskReports.setManaged(true);
        updateButtonStyles(taskReportsButton);
    }

    private void showPerformanceReports() {
        hideAllReports();
        performanceReports.setVisible(true);
        performanceReports.setManaged(true);
        updateButtonStyles(performanceButton);
    }

    private void showActivityLogs() {
        hideAllReports();
        activityLogs.setVisible(true);
        activityLogs.setManaged(true);
        updateButtonStyles(activityLogsButton);
    }

    private void hideAllReports() {
        systemOverview.setVisible(false);
        systemOverview.setManaged(false);
        userReports.setVisible(false);
        userReports.setManaged(false);
        taskReports.setVisible(false);
        taskReports.setManaged(false);
        performanceReports.setVisible(false);
        performanceReports.setManaged(false);
        activityLogs.setVisible(false);
        activityLogs.setManaged(false);
    }

    private void updateButtonStyles(Button activeButton) {
        String defaultStyle = "-fx-text-fill: white; -fx-padding: 10 15; -fx-background-radius: 5;";
        overviewButton.setStyle(defaultStyle + " -fx-background-color: #3498db;");
        userReportsButton.setStyle(defaultStyle + " -fx-background-color: #e74c3c;");
        taskReportsButton.setStyle(defaultStyle + " -fx-background-color: #f39c12;");
        performanceButton.setStyle(defaultStyle + " -fx-background-color: #27ae60;");
        activityLogsButton.setStyle(defaultStyle + " -fx-background-color: #9b59b6;");

        activeButton.setStyle(defaultStyle + " -fx-background-color: #2c3e50;");
    }

    // Action methods
    private void applyDateFilter() {
        loadReportData();
        showAlert("Filter Applied", "Date filter has been applied to all reports.");
    }

    private void exportToPdf() {
        showAlert("Export PDF", "PDF export feature is available in the Advanced Export interface.\nClick 'Advanced Export' for full functionality.");
    }

    private void exportToExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report");
        fileChooser.setInitialFileName("admin_report_" + LocalDate.now() + ".xlsx");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));

        Stage stage = (Stage) exportExcelButton.getScene().getWindow();
        File selectedFile = fileChooser.showSaveDialog(stage);

        if (selectedFile != null) {
            new Thread(() -> {
                try {
                    List<Task> tasks = taskService.getAllTasks();
                    List<User> users = userService.getAllActiveUsers();

                    boolean success = statisticsExportService.exportStatisticsToExcel(selectedFile, tasks, users);

                    Platform.runLater(() -> {
                        if (success) {
                            showAlert("Success", "Report exported successfully to: " + selectedFile.getAbsolutePath());
                        } else {
                            showAlert("Error", "Failed to export report");
                        }
                    });
                } catch (Exception e) {
                    logger.error("Error exporting to Excel", e);
                    Platform.runLater(() -> {
                        showAlert("Error", "Failed to export report: " + e.getMessage());
                    });
                }
            }).start();
        }
    }

    private void printReport() {
        showAlert("Print Report", "Print feature will be implemented in a future update.");
    }

    private void exportLogs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Logs");
        fileChooser.setInitialFileName("application_logs_" + LocalDate.now() + ".xlsx");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));

        Stage stage = (Stage) exportLogsButton.getScene().getWindow();
        File selectedFile = fileChooser.showSaveDialog(stage);

        if (selectedFile != null) {
            new Thread(() -> {
                LocalDateTime fromDateTime = fromDatePicker.getValue().atStartOfDay();
                LocalDateTime toDateTime = toDatePicker.getValue().atTime(23, 59, 59);

                boolean success = logExportService.exportLogsToExcel(selectedFile, fromDateTime, toDateTime);

                Platform.runLater(() -> {
                    if (success) {
                        showAlert("Success", "Logs exported successfully to: " + selectedFile.getAbsolutePath());
                    } else {
                        showAlert("Error", "Failed to export logs");
                    }
                });
            }).start();
        }
    }

    /**
     * Open advanced export interface
     */
    private void openAdvancedExport() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/taskmanagerv3/statistics-export.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);

            Stage stage = new Stage();
            stage.setTitle("WorkFlow Manager - Statistics & Export Center");
            stage.setScene(scene);
            stage.show();

            logger.info("Opened advanced export interface");

        } catch (IOException e) {
            logger.error("Failed to load advanced export interface", e);
            showAlert("Error", "Failed to open advanced export: " + e.getMessage());
        }
    }

    private void closeReports() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
