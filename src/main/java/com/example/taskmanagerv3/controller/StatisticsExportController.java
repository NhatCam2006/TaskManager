package com.example.taskmanagerv3.controller;

import com.example.taskmanagerv3.model.Task;
import com.example.taskmanagerv3.model.User;
import com.example.taskmanagerv3.service.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.embed.swing.SwingFXUtils;
import java.awt.image.BufferedImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for Statistics and Export functionality
 */
public class StatisticsExportController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsExportController.class);

    // Services
    private final TaskService taskService = new TaskService();
    private final UserService userService = new UserService();
    private final LogExportService logExportService = new LogExportService();
    private final StatisticsExportService statisticsExportService = new StatisticsExportService();
    private final ChartGeneratorService chartGeneratorService = new ChartGeneratorService();

    // UI Components
    @FXML private TabPane mainTabPane;

    // Statistics Tab
    @FXML private VBox chartContainer;
    @FXML private ComboBox<String> chartTypeComboBox;
    @FXML private Button refreshChartsButton;
    @FXML private Button exportChartsButton;

    // Export Tab
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Button exportStatisticsExcelButton;
    @FXML private Button exportStatisticsChartsButton;
    @FXML private ProgressBar exportProgressBar;
    @FXML private Label exportStatusLabel;

    // Log Export Tab
    @FXML private DatePicker logFromDatePicker;
    @FXML private DatePicker logToDatePicker;
    @FXML private ComboBox<String> logFormatComboBox;
    @FXML private Button exportLogsButton;
    @FXML private TextArea logPreviewArea;
    @FXML private Label logStatsLabel;

    // Control buttons
    @FXML private Button closeButton;

    // Data
    private List<Task> allTasks;
    private List<User> allUsers;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        loadData();
    }

    /**
     * Setup UI components and event handlers
     */
    private void setupUI() {
        // Setup chart type combo box
        chartTypeComboBox.getItems().addAll(
            "Task Status Distribution",
            "Task Priority Distribution",
            "Tasks per User",
            "Task Completion Trend",
            "Workload Distribution",
            "Estimated vs Actual Hours"
        );
        chartTypeComboBox.setValue("Task Status Distribution");
        chartTypeComboBox.setOnAction(e -> updateChart());

        // Setup date pickers with default values
        LocalDate today = LocalDate.now();
        fromDatePicker.setValue(today.minusMonths(1));
        toDatePicker.setValue(today);
        logFromDatePicker.setValue(today.minusDays(7));
        logToDatePicker.setValue(today);

        // Setup log format combo box
        logFormatComboBox.getItems().addAll("Excel (.xlsx)", "CSV (.csv)", "Text (.txt)");
        logFormatComboBox.setValue("Excel (.xlsx)");

        // Setup event handlers
        refreshChartsButton.setOnAction(e -> refreshCharts());
        exportChartsButton.setOnAction(e -> exportCharts());
        exportStatisticsExcelButton.setOnAction(e -> exportStatisticsToExcel());
        exportStatisticsChartsButton.setOnAction(e -> exportStatisticsCharts());
        exportLogsButton.setOnAction(e -> exportLogs());
        closeButton.setOnAction(e -> closeWindow());

        // Setup log preview
        logFromDatePicker.setOnAction(e -> updateLogPreview());
        logToDatePicker.setOnAction(e -> updateLogPreview());

        // Initialize progress bar
        exportProgressBar.setVisible(false);
        exportStatusLabel.setText("Ready");
    }

    /**
     * Load data from database
     */
    private void loadData() {
        new Thread(() -> {
            try {
                allTasks = taskService.getAllTasks();
                allUsers = userService.getAllActiveUsers();

                Platform.runLater(() -> {
                    updateChart();
                    updateLogPreview();
                });

            } catch (Exception e) {
                logger.error("Error loading data", e);
                Platform.runLater(() -> {
                    showAlert("Error", "Failed to load data: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Update the displayed chart based on selected type
     */
    private void updateChart() {
        if (allTasks == null || allUsers == null) return;

        String chartType = chartTypeComboBox.getValue();
        if (chartType == null) return;

        try {
            JFreeChart chart = switch (chartType) {
                case "Task Status Distribution" -> chartGeneratorService.generateTaskStatusChart(allTasks);
                case "Task Priority Distribution" -> chartGeneratorService.generateTaskPriorityChart(allTasks);
                case "Tasks per User" -> chartGeneratorService.generateTasksPerUserChart(allTasks, allUsers);
                case "Task Completion Trend" -> chartGeneratorService.generateTaskCompletionTrendChart(allTasks);
                case "Workload Distribution" -> chartGeneratorService.generateWorkloadDistributionChart(allTasks, allUsers);
                case "Estimated vs Actual Hours" -> chartGeneratorService.generateEstimatedVsActualHoursChart(allTasks);
                default -> chartGeneratorService.generateTaskStatusChart(allTasks);
            };

            // Clear previous chart and add new one
            chartContainer.getChildren().clear();

            // Convert JFreeChart to JavaFX Image
            BufferedImage bufferedImage = chart.createBufferedImage(800, 600);
            Image fxImage = SwingFXUtils.toFXImage(bufferedImage, null);

            ImageView imageView = new ImageView(fxImage);
            imageView.setFitWidth(800);
            imageView.setFitHeight(600);
            imageView.setPreserveRatio(true);

            chartContainer.getChildren().add(imageView);

        } catch (Exception e) {
            logger.error("Error updating chart", e);
            showAlert("Error", "Failed to update chart: " + e.getMessage());
        }
    }

    /**
     * Refresh charts with latest data
     */
    private void refreshCharts() {
        loadData();
    }

    /**
     * Export charts as images
     */
    private void exportCharts() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Export Directory");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        Stage stage = (Stage) exportChartsButton.getScene().getWindow();
        File selectedDirectory = directoryChooser.showDialog(stage);

        if (selectedDirectory != null) {
            new Thread(() -> {
                Platform.runLater(() -> {
                    exportProgressBar.setVisible(true);
                    exportStatusLabel.setText("Exporting charts...");
                });

                boolean success = statisticsExportService.exportChartsAsImages(
                    selectedDirectory.getAbsolutePath(), allTasks, allUsers);

                Platform.runLater(() -> {
                    exportProgressBar.setVisible(false);
                    if (success) {
                        exportStatusLabel.setText("Charts exported successfully");
                        showAlert("Success", "Charts exported to: " + selectedDirectory.getAbsolutePath());
                    } else {
                        exportStatusLabel.setText("Export failed");
                        showAlert("Error", "Failed to export charts");
                    }
                });
            }).start();
        }
    }

    /**
     * Export statistics to Excel
     */
    private void exportStatisticsToExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Statistics Report");
        fileChooser.setInitialFileName("task_statistics_" + LocalDate.now() + ".xlsx");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));

        Stage stage = (Stage) exportStatisticsExcelButton.getScene().getWindow();
        File selectedFile = fileChooser.showSaveDialog(stage);

        if (selectedFile != null) {
            new Thread(() -> {
                Platform.runLater(() -> {
                    exportProgressBar.setVisible(true);
                    exportStatusLabel.setText("Exporting statistics...");
                });

                boolean success = statisticsExportService.exportStatisticsToExcel(
                    selectedFile, allTasks, allUsers);

                Platform.runLater(() -> {
                    exportProgressBar.setVisible(false);
                    if (success) {
                        exportStatusLabel.setText("Statistics exported successfully");
                        showAlert("Success", "Statistics exported to: " + selectedFile.getAbsolutePath());
                    } else {
                        exportStatusLabel.setText("Export failed");
                        showAlert("Error", "Failed to export statistics");
                    }
                });
            }).start();
        }
    }

    /**
     * Export statistics charts
     */
    private void exportStatisticsCharts() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Export Directory for Charts");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        Stage stage = (Stage) exportStatisticsChartsButton.getScene().getWindow();
        File selectedDirectory = directoryChooser.showDialog(stage);

        if (selectedDirectory != null) {
            exportCharts(); // Reuse the existing export charts functionality
        }
    }

    /**
     * Export logs based on selected format and date range
     */
    private void exportLogs() {
        LocalDate fromDate = logFromDatePicker.getValue();
        LocalDate toDate = logToDatePicker.getValue();
        String format = logFormatComboBox.getValue();

        if (fromDate == null || toDate == null) {
            showAlert("Error", "Please select both from and to dates");
            return;
        }

        if (fromDate.isAfter(toDate)) {
            showAlert("Error", "From date cannot be after to date");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Log Export");

        String extension = switch (format) {
            case "Excel (.xlsx)" -> ".xlsx";
            case "CSV (.csv)" -> ".csv";
            case "Text (.txt)" -> ".txt";
            default -> ".txt";
        };

        fileChooser.setInitialFileName("application_logs_" + fromDate + "_to_" + toDate + extension);
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(format, "*" + extension));

        Stage stage = (Stage) exportLogsButton.getScene().getWindow();
        File selectedFile = fileChooser.showSaveDialog(stage);

        if (selectedFile != null) {
            new Thread(() -> {
                Platform.runLater(() -> {
                    exportProgressBar.setVisible(true);
                    exportStatusLabel.setText("Exporting logs...");
                });

                LocalDateTime fromDateTime = fromDate.atStartOfDay();
                LocalDateTime toDateTime = toDate.atTime(23, 59, 59);

                boolean success = switch (format) {
                    case "Excel (.xlsx)" -> logExportService.exportLogsToExcel(selectedFile, fromDateTime, toDateTime);
                    case "CSV (.csv)" -> logExportService.exportLogsToCSV(selectedFile, fromDateTime, toDateTime);
                    case "Text (.txt)" -> logExportService.exportLogsToText(selectedFile, fromDateTime, toDateTime);
                    default -> false;
                };

                Platform.runLater(() -> {
                    exportProgressBar.setVisible(false);
                    if (success) {
                        exportStatusLabel.setText("Logs exported successfully");
                        showAlert("Success", "Logs exported to: " + selectedFile.getAbsolutePath());
                    } else {
                        exportStatusLabel.setText("Export failed");
                        showAlert("Error", "Failed to export logs");
                    }
                });
            }).start();
        }
    }

    /**
     * Update log preview and statistics
     */
    private void updateLogPreview() {
        LocalDate fromDate = logFromDatePicker.getValue();
        LocalDate toDate = logToDatePicker.getValue();

        if (fromDate == null || toDate == null) return;

        new Thread(() -> {
            LocalDateTime fromDateTime = fromDate.atStartOfDay();
            LocalDateTime toDateTime = toDate.atTime(23, 59, 59);

            LogExportService.LogStatistics stats = logExportService.getLogStatistics(fromDateTime, toDateTime);

            Platform.runLater(() -> {
                logStatsLabel.setText(String.format(
                    "Total: %d | Errors: %d | Warnings: %d | Info: %d | Debug: %d",
                    stats.getTotalEntries(), stats.getErrorCount(), stats.getWarnCount(),
                    stats.getInfoCount(), stats.getDebugCount()
                ));

                // Show preview of recent log entries
                logPreviewArea.setText("Log entries from " + fromDate + " to " + toDate + "\n" +
                    "Total entries: " + stats.getTotalEntries() + "\n\n" +
                    "Use the export button to save the complete log data.");
            });
        }).start();
    }

    /**
     * Close the window
     */
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
