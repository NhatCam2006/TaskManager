package com.example.taskmanagerv3.controller;

import com.example.taskmanagerv3.model.Task;
import com.example.taskmanagerv3.model.TaskStatus;
import com.example.taskmanagerv3.service.TaskService;
import com.example.taskmanagerv3.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Controller for User Calendar View
 */
public class UserCalendarController {
    private static final Logger logger = LoggerFactory.getLogger(UserCalendarController.class);

    // Header controls
    @FXML private Button prevMonthButton;
    @FXML private Button nextMonthButton;
    @FXML private Button todayButton;
    @FXML private Button backToDashboardButton;
    @FXML private Label currentMonthLabel;

    // View options
    @FXML private CheckBox showCompletedCheckBox;
    @FXML private CheckBox showOverdueCheckBox;
    @FXML private CheckBox showFutureCheckBox;

    // Summary labels
    @FXML private Label totalTasksThisMonthLabel;
    @FXML private Label completedTasksThisMonthLabel;
    @FXML private Label pendingTasksThisMonthLabel;
    @FXML private Label overdueTasksThisMonthLabel;

    // Calendar grid
    @FXML private GridPane dayHeadersGrid;
    @FXML private GridPane calendarGrid;

    // Selected date details
    @FXML private Label selectedDateLabel;
    @FXML private VBox selectedDateTasksContainer;

    // Action buttons
    @FXML private Button refreshCalendarButton;
    @FXML private Button exportCalendarButton;

    private TaskService taskService;
    private SessionManager sessionManager;
    private YearMonth currentMonth;
    private LocalDate selectedDate;
    private List<Task> allTasks;

    @FXML
    private void initialize() {
        taskService = new TaskService();
        sessionManager = SessionManager.getInstance();
        currentMonth = YearMonth.now();
        selectedDate = LocalDate.now();

        setupUI();
        loadTasks();

        logger.info("User calendar initialized for user: {}", sessionManager.getCurrentUsername());
    }

    /**
     * Setup UI components and event handlers
     */
    private void setupUI() {
        // Setup navigation buttons
        prevMonthButton.setOnAction(e -> navigateToPreviousMonth());
        nextMonthButton.setOnAction(e -> navigateToNextMonth());
        todayButton.setOnAction(e -> navigateToToday());
        backToDashboardButton.setOnAction(e -> backToDashboard());

        // Setup action buttons
        refreshCalendarButton.setOnAction(e -> loadTasks());
        exportCalendarButton.setOnAction(e -> exportCalendar());

        // Setup view option listeners
        showCompletedCheckBox.setOnAction(e -> refreshCalendarDisplay());
        showOverdueCheckBox.setOnAction(e -> refreshCalendarDisplay());
        showFutureCheckBox.setOnAction(e -> refreshCalendarDisplay());

        // Setup day headers
        setupDayHeaders();
        
        // Update month label
        updateMonthLabel();
    }

    /**
     * Setup day headers (Sun, Mon, Tue, etc.)
     */
    private void setupDayHeaders() {
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        
        for (int i = 0; i < dayNames.length; i++) {
            Label dayHeader = new Label(dayNames[i]);
            dayHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-alignment: center; " +
                             "-fx-background-color: #ecf0f1; -fx-padding: 10; -fx-min-width: 100;");
            dayHeadersGrid.add(dayHeader, i, 0);
        }
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
                    refreshCalendarDisplay();
                    updateMonthlySummary();
                });

            } catch (Exception e) {
                logger.error("Error loading tasks for calendar", e);
                Platform.runLater(() -> {
                    showAlert("Error", "Failed to load tasks: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Refresh calendar display
     */
    private void refreshCalendarDisplay() {
        calendarGrid.getChildren().clear();
        
        LocalDate firstDayOfMonth = currentMonth.atDay(1);
        LocalDate lastDayOfMonth = currentMonth.atEndOfMonth();
        
        // Calculate starting position (day of week for first day)
        int startDayOfWeek = firstDayOfMonth.getDayOfWeek().getValue() % 7; // Sunday = 0
        
        // Add empty cells for days before the first day of month
        int currentRow = 0;
        int currentCol = startDayOfWeek;
        
        // Add days of the month
        for (LocalDate date = firstDayOfMonth; !date.isAfter(lastDayOfMonth); date = date.plusDays(1)) {
            VBox dayCell = createDayCell(date);
            calendarGrid.add(dayCell, currentCol, currentRow);
            
            currentCol++;
            if (currentCol > 6) {
                currentCol = 0;
                currentRow++;
            }
        }
        
        // Update selected date display
        updateSelectedDateDisplay();
    }

    /**
     * Create a day cell for the calendar
     */
    private VBox createDayCell(LocalDate date) {
        VBox dayCell = new VBox(2);
        dayCell.setAlignment(Pos.TOP_CENTER);
        dayCell.setPadding(new Insets(5));
        dayCell.setMinHeight(80);
        dayCell.setMinWidth(100);
        
        // Day number label
        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        // Highlight today
        if (date.equals(LocalDate.now())) {
            dayLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #3498db;");
            dayCell.setStyle("-fx-background-color: #e8f4fd; -fx-border-color: #3498db; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");
        } else if (date.equals(selectedDate)) {
            dayCell.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #95a5a6; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");
        } else {
            dayCell.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");
        }
        
        dayCell.getChildren().add(dayLabel);
        
        // Add tasks for this date
        List<Task> tasksForDate = getTasksForDate(date);
        for (Task task : tasksForDate) {
            if (shouldShowTask(task)) {
                Label taskLabel = new Label(getTaskIcon(task));
                taskLabel.setStyle("-fx-font-size: 12px;");
                taskLabel.setTooltip(new Tooltip(task.getTitle()));
                dayCell.getChildren().add(taskLabel);
            }
        }
        
        // Add click handler
        dayCell.setOnMouseClicked(e -> {
            selectedDate = date;
            refreshCalendarDisplay();
        });
        
        return dayCell;
    }

    /**
     * Get tasks for a specific date
     */
    private List<Task> getTasksForDate(LocalDate date) {
        if (allTasks == null) return List.of();
        
        return allTasks.stream()
            .filter(task -> task.getDueDate() != null && task.getDueDate().toLocalDate().equals(date))
            .collect(Collectors.toList());
    }

    /**
     * Check if task should be shown based on view options
     */
    private boolean shouldShowTask(Task task) {
        if (task.getStatus() == TaskStatus.COMPLETED && !showCompletedCheckBox.isSelected()) {
            return false;
        }
        
        if (task.isOverdue() && !showOverdueCheckBox.isSelected()) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (task.getDueDate() != null && task.getDueDate().isAfter(now) && !showFutureCheckBox.isSelected()) {
            return false;
        }
        
        return true;
    }

    /**
     * Get task icon based on status and urgency
     */
    private String getTaskIcon(Task task) {
        if (task.isOverdue()) {
            return "🔴";
        } else if (task.getDueDate() != null && task.getDueDate().toLocalDate().equals(LocalDate.now())) {
            return "🟠";
        } else if (task.getStatus() == TaskStatus.COMPLETED) {
            return "🟢";
        } else if (task.getDueDate() != null && task.getDueDate().toLocalDate().isBefore(LocalDate.now().plusWeeks(1))) {
            return "🟡";
        } else {
            return "⚪";
        }
    }

    /**
     * Update selected date display
     */
    private void updateSelectedDateDisplay() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        selectedDateLabel.setText(selectedDate.format(formatter));
        
        selectedDateTasksContainer.getChildren().clear();
        
        List<Task> tasksForSelectedDate = getTasksForDate(selectedDate);
        
        if (tasksForSelectedDate.isEmpty()) {
            Label noTasksLabel = new Label("No tasks scheduled for this date");
            noTasksLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #7f8c8d;");
            selectedDateTasksContainer.getChildren().add(noTasksLabel);
        } else {
            for (Task task : tasksForSelectedDate) {
                if (shouldShowTask(task)) {
                    HBox taskRow = createTaskRow(task);
                    selectedDateTasksContainer.getChildren().add(taskRow);
                }
            }
        }
    }

    /**
     * Create a task row for selected date display
     */
    private HBox createTaskRow(Task task) {
        HBox taskRow = new HBox(10);
        taskRow.setAlignment(Pos.CENTER_LEFT);
        taskRow.setPadding(new Insets(5));
        taskRow.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 3;");
        
        Label iconLabel = new Label(getTaskIcon(task));
        Label titleLabel = new Label(task.getTitle());
        titleLabel.setStyle("-fx-font-weight: bold;");
        
        Label statusLabel = new Label(task.getStatus().name());
        statusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");
        
        taskRow.getChildren().addAll(iconLabel, titleLabel, statusLabel);
        return taskRow;
    }

    /**
     * Update monthly summary
     */
    private void updateMonthlySummary() {
        if (allTasks == null) return;
        
        LocalDate startOfMonth = currentMonth.atDay(1);
        LocalDate endOfMonth = currentMonth.atEndOfMonth();
        
        List<Task> monthTasks = allTasks.stream()
            .filter(task -> task.getDueDate() != null)
            .filter(task -> {
                LocalDate taskDate = task.getDueDate().toLocalDate();
                return !taskDate.isBefore(startOfMonth) && !taskDate.isAfter(endOfMonth);
            })
            .collect(Collectors.toList());
        
        int total = monthTasks.size();
        int completed = (int) monthTasks.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count();
        int pending = total - completed;
        int overdue = (int) monthTasks.stream().filter(Task::isOverdue).count();
        
        totalTasksThisMonthLabel.setText(String.valueOf(total));
        completedTasksThisMonthLabel.setText(String.valueOf(completed));
        pendingTasksThisMonthLabel.setText(String.valueOf(pending));
        overdueTasksThisMonthLabel.setText(String.valueOf(overdue));
    }

    /**
     * Navigate to previous month
     */
    private void navigateToPreviousMonth() {
        currentMonth = currentMonth.minusMonths(1);
        updateMonthLabel();
        refreshCalendarDisplay();
        updateMonthlySummary();
    }

    /**
     * Navigate to next month
     */
    private void navigateToNextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        updateMonthLabel();
        refreshCalendarDisplay();
        updateMonthlySummary();
    }

    /**
     * Navigate to current month
     */
    private void navigateToToday() {
        currentMonth = YearMonth.now();
        selectedDate = LocalDate.now();
        updateMonthLabel();
        refreshCalendarDisplay();
        updateMonthlySummary();
    }

    /**
     * Update month label
     */
    private void updateMonthLabel() {
        String monthName = currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        currentMonthLabel.setText(monthName + " " + currentMonth.getYear());
    }

    /**
     * Export calendar (placeholder)
     */
    private void exportCalendar() {
        showAlert("Export Calendar", "Calendar export feature will be implemented in a future update.");
    }

    /**
     * Back to dashboard
     */
    private void backToDashboard() {
        Stage stage = (Stage) backToDashboardButton.getScene().getWindow();
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
