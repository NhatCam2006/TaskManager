package com.example.taskmanagerv3.service;

import com.example.taskmanagerv3.model.Task;
import com.example.taskmanagerv3.model.TaskStatus;
import com.example.taskmanagerv3.model.TaskPriority;
import com.example.taskmanagerv3.model.User;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for generating statistical charts
 */
public class ChartGeneratorService {
    private static final Logger logger = LoggerFactory.getLogger(ChartGeneratorService.class);
    
    /**
     * Generate task status distribution pie chart
     */
    public JFreeChart generateTaskStatusChart(List<Task> tasks) {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        
        Map<TaskStatus, Long> statusCounts = tasks.stream()
            .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));
        
        for (TaskStatus status : TaskStatus.values()) {
            long count = statusCounts.getOrDefault(status, 0L);
            if (count > 0) {
                dataset.setValue(status.toString(), count);
            }
        }
        
        JFreeChart chart = ChartFactory.createPieChart(
            "Task Status Distribution",
            dataset,
            true,  // legend
            true,  // tooltips
            false  // URLs
        );
        
        customizePieChart(chart);
        return chart;
    }
    
    /**
     * Generate task priority distribution pie chart
     */
    public JFreeChart generateTaskPriorityChart(List<Task> tasks) {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        
        Map<TaskPriority, Long> priorityCounts = tasks.stream()
            .collect(Collectors.groupingBy(Task::getPriority, Collectors.counting()));
        
        for (TaskPriority priority : TaskPriority.values()) {
            long count = priorityCounts.getOrDefault(priority, 0L);
            if (count > 0) {
                dataset.setValue(priority.toString(), count);
            }
        }
        
        JFreeChart chart = ChartFactory.createPieChart(
            "Task Priority Distribution",
            dataset,
            true,  // legend
            true,  // tooltips
            false  // URLs
        );
        
        customizePieChart(chart);
        return chart;
    }
    
    /**
     * Generate tasks per user bar chart
     */
    public JFreeChart generateTasksPerUserChart(List<Task> tasks, List<User> users) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        Map<Integer, String> userIdToName = users.stream()
            .collect(Collectors.toMap(User::getUserId, User::getDisplayName));
        
        Map<Integer, Long> taskCounts = tasks.stream()
            .collect(Collectors.groupingBy(Task::getAssignedUserId, Collectors.counting()));
        
        for (Map.Entry<Integer, Long> entry : taskCounts.entrySet()) {
            String userName = userIdToName.getOrDefault(entry.getKey(), "Unknown User");
            dataset.addValue(entry.getValue(), "Tasks", userName);
        }
        
        JFreeChart chart = ChartFactory.createBarChart(
            "Tasks per User",
            "Users",
            "Number of Tasks",
            dataset
        );
        
        customizeBarChart(chart);
        return chart;
    }
    
    /**
     * Generate task completion trend over time
     */
    public JFreeChart generateTaskCompletionTrendChart(List<Task> tasks) {
        TimeSeries completedSeries = new TimeSeries("Completed Tasks");
        TimeSeries createdSeries = new TimeSeries("Created Tasks");
        
        Map<LocalDate, Long> completedByDate = tasks.stream()
            .filter(task -> task.getCompletedAt() != null)
            .collect(Collectors.groupingBy(
                task -> task.getCompletedAt().toLocalDate(),
                Collectors.counting()
            ));
        
        Map<LocalDate, Long> createdByDate = tasks.stream()
            .collect(Collectors.groupingBy(
                task -> task.getCreatedAt().toLocalDate(),
                Collectors.counting()
            ));
        
        // Add data points for the last 30 days
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Day day = new Day(java.util.Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            
            long completedCount = completedByDate.getOrDefault(date, 0L);
            long createdCount = createdByDate.getOrDefault(date, 0L);
            
            completedSeries.add(day, completedCount);
            createdSeries.add(day, createdCount);
        }
        
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(completedSeries);
        dataset.addSeries(createdSeries);
        
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "Task Completion Trend (Last 30 Days)",
            "Date",
            "Number of Tasks",
            dataset,
            true,  // legend
            true,  // tooltips
            false  // URLs
        );
        
        customizeTimeSeriesChart(chart);
        return chart;
    }
    
    /**
     * Generate workload distribution chart
     */
    public JFreeChart generateWorkloadDistributionChart(List<Task> tasks, List<User> users) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        Map<Integer, String> userIdToName = users.stream()
            .collect(Collectors.toMap(User::getUserId, User::getDisplayName));
        
        Map<Integer, Map<TaskStatus, Long>> userTaskStatus = tasks.stream()
            .collect(Collectors.groupingBy(
                Task::getAssignedUserId,
                Collectors.groupingBy(Task::getStatus, Collectors.counting())
            ));
        
        for (Map.Entry<Integer, Map<TaskStatus, Long>> userEntry : userTaskStatus.entrySet()) {
            String userName = userIdToName.getOrDefault(userEntry.getKey(), "Unknown User");
            
            for (TaskStatus status : TaskStatus.values()) {
                long count = userEntry.getValue().getOrDefault(status, 0L);
                if (count > 0) {
                    dataset.addValue(count, status.toString(), userName);
                }
            }
        }
        
        JFreeChart chart = ChartFactory.createStackedBarChart(
            "Workload Distribution by User",
            "Users",
            "Number of Tasks",
            dataset
        );
        
        customizeBarChart(chart);
        return chart;
    }
    
    /**
     * Generate estimated vs actual hours chart
     */
    public JFreeChart generateEstimatedVsActualHoursChart(List<Task> tasks) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        List<Task> completedTasks = tasks.stream()
            .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
            .filter(task -> task.getEstimatedHours() > 0 || task.getActualHours() > 0)
            .limit(20) // Show only top 20 tasks to avoid cluttering
            .collect(Collectors.toList());
        
        for (int i = 0; i < completedTasks.size(); i++) {
            Task task = completedTasks.get(i);
            String taskLabel = "Task " + (i + 1);
            
            dataset.addValue(task.getEstimatedHours(), "Estimated Hours", taskLabel);
            dataset.addValue(task.getActualHours(), "Actual Hours", taskLabel);
        }
        
        JFreeChart chart = ChartFactory.createBarChart(
            "Estimated vs Actual Hours (Completed Tasks)",
            "Tasks",
            "Hours",
            dataset
        );
        
        customizeBarChart(chart);
        return chart;
    }
    
    /**
     * Customize pie chart appearance
     */
    private void customizePieChart(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineStroke(new BasicStroke(1.0f));
        plot.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        plot.setNoDataMessage("No data available");
        
        // Set custom colors
        plot.setSectionPaint("TODO", new Color(52, 152, 219));
        plot.setSectionPaint("IN_PROGRESS", new Color(241, 196, 15));
        plot.setSectionPaint("REVIEW", new Color(155, 89, 182));
        plot.setSectionPaint("COMPLETED", new Color(46, 204, 113));
        plot.setSectionPaint("CANCELLED", new Color(231, 76, 60));
        
        plot.setSectionPaint("LOW", new Color(46, 204, 113));
        plot.setSectionPaint("MEDIUM", new Color(241, 196, 15));
        plot.setSectionPaint("HIGH", new Color(230, 126, 34));
        plot.setSectionPaint("URGENT", new Color(231, 76, 60));
    }
    
    /**
     * Customize bar chart appearance
     */
    private void customizeBarChart(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(52, 152, 219));
        renderer.setSeriesPaint(1, new Color(46, 204, 113));
        renderer.setSeriesPaint(2, new Color(241, 196, 15));
        renderer.setSeriesPaint(3, new Color(155, 89, 182));
        renderer.setSeriesPaint(4, new Color(231, 76, 60));
    }
    
    /**
     * Customize time series chart appearance
     */
    private void customizeTimeSeriesChart(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        chart.getXYPlot().setBackgroundPaint(Color.WHITE);
        chart.getXYPlot().setDomainGridlinePaint(Color.LIGHT_GRAY);
        chart.getXYPlot().setRangeGridlinePaint(Color.LIGHT_GRAY);
    }
}
