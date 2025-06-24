package com.example.taskmanagerv3.service;

import com.example.taskmanagerv3.model.Task;
import com.example.taskmanagerv3.model.TaskStatus;
import com.example.taskmanagerv3.model.TaskPriority;
import com.example.taskmanagerv3.model.User;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for exporting statistical data and charts
 */
public class StatisticsExportService {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsExportService.class);
    
    private final ChartGeneratorService chartGenerator;
    
    public StatisticsExportService() {
        this.chartGenerator = new ChartGeneratorService();
    }
    
    /**
     * Export comprehensive statistics report to Excel
     */
    public boolean exportStatisticsToExcel(File outputFile, List<Task> tasks, List<User> users) {
        try (Workbook workbook = new XSSFWorkbook()) {
            
            // Create summary sheet
            createSummarySheet(workbook, tasks, users);
            
            // Create task details sheet
            createTaskDetailsSheet(workbook, tasks, users);
            
            // Create user statistics sheet
            createUserStatisticsSheet(workbook, tasks, users);
            
            // Create time analysis sheet
            createTimeAnalysisSheet(workbook, tasks);
            
            // Write to file
            try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
                workbook.write(fileOut);
            }
            
            logger.info("Statistics exported to Excel: {}", outputFile.getAbsolutePath());
            return true;
            
        } catch (Exception e) {
            logger.error("Error exporting statistics to Excel", e);
            return false;
        }
    }
    
    /**
     * Export charts as images
     */
    public boolean exportChartsAsImages(String outputDirectory, List<Task> tasks, List<User> users) {
        try {
            File dir = new File(outputDirectory);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // Generate and save charts
            JFreeChart statusChart = chartGenerator.generateTaskStatusChart(tasks);
            ChartUtils.saveChartAsPNG(new File(dir, "task_status_chart.png"), statusChart, 800, 600);
            
            JFreeChart priorityChart = chartGenerator.generateTaskPriorityChart(tasks);
            ChartUtils.saveChartAsPNG(new File(dir, "task_priority_chart.png"), priorityChart, 800, 600);
            
            JFreeChart userChart = chartGenerator.generateTasksPerUserChart(tasks, users);
            ChartUtils.saveChartAsPNG(new File(dir, "tasks_per_user_chart.png"), userChart, 800, 600);
            
            JFreeChart trendChart = chartGenerator.generateTaskCompletionTrendChart(tasks);
            ChartUtils.saveChartAsPNG(new File(dir, "completion_trend_chart.png"), trendChart, 800, 600);
            
            JFreeChart workloadChart = chartGenerator.generateWorkloadDistributionChart(tasks, users);
            ChartUtils.saveChartAsPNG(new File(dir, "workload_distribution_chart.png"), workloadChart, 800, 600);
            
            JFreeChart hoursChart = chartGenerator.generateEstimatedVsActualHoursChart(tasks);
            ChartUtils.saveChartAsPNG(new File(dir, "estimated_vs_actual_hours_chart.png"), hoursChart, 800, 600);
            
            logger.info("Charts exported to directory: {}", outputDirectory);
            return true;
            
        } catch (Exception e) {
            logger.error("Error exporting charts as images", e);
            return false;
        }
    }
    
    /**
     * Create summary sheet with key statistics
     */
    private void createSummarySheet(Workbook workbook, List<Task> tasks, List<User> users) {
        Sheet sheet = workbook.createSheet("Summary");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        
        int rowNum = 0;
        
        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Task Management Statistics Summary");
        titleCell.setCellStyle(headerStyle);
        
        // Generation info
        rowNum++;
        Row dateRow = sheet.createRow(rowNum++);
        dateRow.createCell(0).setCellValue("Generated:");
        dateRow.createCell(1).setCellValue(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // Overall statistics
        rowNum++;
        createStatisticRow(sheet, rowNum++, "Total Tasks", tasks.size(), headerStyle, dataStyle);
        createStatisticRow(sheet, rowNum++, "Total Users", users.size(), headerStyle, dataStyle);
        createStatisticRow(sheet, rowNum++, "Active Users", 
            (int) users.stream().filter(User::isActive).count(), headerStyle, dataStyle);
        
        // Task status breakdown
        rowNum++;
        Row statusHeaderRow = sheet.createRow(rowNum++);
        statusHeaderRow.createCell(0).setCellValue("Task Status Breakdown");
        statusHeaderRow.getCell(0).setCellStyle(headerStyle);
        
        Map<TaskStatus, Long> statusCounts = tasks.stream()
            .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));
        
        for (TaskStatus status : TaskStatus.values()) {
            long count = statusCounts.getOrDefault(status, 0L);
            createStatisticRow(sheet, rowNum++, status.toString(), (int) count, headerStyle, dataStyle);
        }
        
        // Task priority breakdown
        rowNum++;
        Row priorityHeaderRow = sheet.createRow(rowNum++);
        priorityHeaderRow.createCell(0).setCellValue("Task Priority Breakdown");
        priorityHeaderRow.getCell(0).setCellStyle(headerStyle);
        
        Map<TaskPriority, Long> priorityCounts = tasks.stream()
            .collect(Collectors.groupingBy(Task::getPriority, Collectors.counting()));
        
        for (TaskPriority priority : TaskPriority.values()) {
            long count = priorityCounts.getOrDefault(priority, 0L);
            createStatisticRow(sheet, rowNum++, priority.toString(), (int) count, headerStyle, dataStyle);
        }
        
        // Auto-size columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }
    
    /**
     * Create task details sheet
     */
    private void createTaskDetailsSheet(Workbook workbook, List<Task> tasks, List<User> users) {
        Sheet sheet = workbook.createSheet("Task Details");
        CellStyle headerStyle = createHeaderStyle(workbook);
        
        Map<Integer, String> userIdToName = users.stream()
            .collect(Collectors.toMap(User::getUserId, User::getDisplayName));
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Task ID", "Title", "Status", "Priority", "Assigned User", 
                           "Created Date", "Due Date", "Completed Date", "Estimated Hours", "Actual Hours"};
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Add data rows
        int rowNum = 1;
        for (Task task : tasks) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(task.getTaskId());
            row.createCell(1).setCellValue(task.getTitle());
            row.createCell(2).setCellValue(task.getStatus().toString());
            row.createCell(3).setCellValue(task.getPriority().toString());
            row.createCell(4).setCellValue(userIdToName.getOrDefault(task.getAssignedUserId(), "Unknown"));
            row.createCell(5).setCellValue(task.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            row.createCell(6).setCellValue(task.getDueDate() != null ? 
                task.getDueDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
            row.createCell(7).setCellValue(task.getCompletedAt() != null ? 
                task.getCompletedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
            row.createCell(8).setCellValue(task.getEstimatedHours());
            row.createCell(9).setCellValue(task.getActualHours());
        }
        
        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    /**
     * Create user statistics sheet
     */
    private void createUserStatisticsSheet(Workbook workbook, List<Task> tasks, List<User> users) {
        Sheet sheet = workbook.createSheet("User Statistics");
        CellStyle headerStyle = createHeaderStyle(workbook);
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"User", "Total Tasks", "Completed", "In Progress", "Pending", "Overdue"};
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Calculate user statistics
        Map<Integer, List<Task>> tasksByUser = tasks.stream()
            .collect(Collectors.groupingBy(Task::getAssignedUserId));
        
        int rowNum = 1;
        for (User user : users) {
            List<Task> userTasks = tasksByUser.getOrDefault(user.getUserId(), List.of());
            
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(user.getDisplayName());
            row.createCell(1).setCellValue(userTasks.size());
            row.createCell(2).setCellValue((int) userTasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count());
            row.createCell(3).setCellValue((int) userTasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count());
            row.createCell(4).setCellValue((int) userTasks.stream().filter(t -> 
                t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.CANCELLED).count());
            row.createCell(5).setCellValue((int) userTasks.stream().filter(Task::isOverdue).count());
        }
        
        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    /**
     * Create time analysis sheet
     */
    private void createTimeAnalysisSheet(Workbook workbook, List<Task> tasks) {
        Sheet sheet = workbook.createSheet("Time Analysis");
        CellStyle headerStyle = createHeaderStyle(workbook);
        
        List<Task> completedTasks = tasks.stream()
            .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
            .filter(task -> task.getEstimatedHours() > 0 && task.getActualHours() > 0)
            .collect(Collectors.toList());
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Task ID", "Title", "Estimated Hours", "Actual Hours", "Variance", "Variance %"};
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Add data rows
        int rowNum = 1;
        for (Task task : completedTasks) {
            int variance = task.getActualHours() - task.getEstimatedHours();
            double variancePercent = task.getEstimatedHours() > 0 ? 
                (double) variance / task.getEstimatedHours() * 100 : 0;
            
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(task.getTaskId());
            row.createCell(1).setCellValue(task.getTitle());
            row.createCell(2).setCellValue(task.getEstimatedHours());
            row.createCell(3).setCellValue(task.getActualHours());
            row.createCell(4).setCellValue(variance);
            row.createCell(5).setCellValue(String.format("%.1f%%", variancePercent));
        }
        
        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    /**
     * Create header style for Excel
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
    
    /**
     * Create data style for Excel
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }
    
    /**
     * Helper method to create statistic rows
     */
    private void createStatisticRow(Sheet sheet, int rowNum, String label, int value, 
                                   CellStyle headerStyle, CellStyle dataStyle) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(headerStyle);
        
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(dataStyle);
    }
}
