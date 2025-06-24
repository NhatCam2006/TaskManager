package com.example.taskmanagerv3.service;

import com.example.taskmanagerv3.config.DatabaseConfig;
import com.example.taskmanagerv3.model.Task;
import com.example.taskmanagerv3.model.TaskPriority;
import com.example.taskmanagerv3.model.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service class for task-related operations
 */
public class TaskService {
    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    /**
     * Create new task
     */
    public boolean createTask(Task task) {
        String query = """
            INSERT INTO Tasks (title, description, status, priority, assigned_user_id,
                             created_by_user_id, due_date, estimated_hours, comments, project_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, task.getTitle());
            stmt.setString(2, task.getDescription());
            stmt.setString(3, task.getStatus().name());
            stmt.setString(4, task.getPriority().name());
            stmt.setInt(5, task.getAssignedUserId());
            stmt.setInt(6, task.getCreatedByUserId());

            if (task.getDueDate() != null) {
                stmt.setTimestamp(7, Timestamp.valueOf(task.getDueDate()));
            } else {
                stmt.setNull(7, Types.TIMESTAMP);
            }

            stmt.setInt(8, task.getEstimatedHours());
            stmt.setString(9, task.getComments());
            stmt.setInt(10, task.getProjectId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    task.setTaskId(generatedKeys.getInt(1));
                }
                logger.info("Task created successfully: {}", task.getTitle());
                return true;
            }

        } catch (SQLException e) {
            logger.error("Error creating task: {}", task.getTitle(), e);
        }

        return false;
    }

    /**
     * Get task by ID
     */
    public Optional<Task> getTaskById(int taskId) {
        String query = """
            SELECT task_id, title, description, status, priority, assigned_user_id,
                   created_by_user_id, created_at, updated_at, due_date, completed_at,
                   estimated_hours, actual_hours, progress_percentage, comments,
                   attachments, project_id
            FROM Tasks
            WHERE task_id = ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, taskId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToTask(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting task by ID: {}", taskId, e);
        }

        return Optional.empty();
    }

    /**
     * Get all tasks
     */
    public List<Task> getAllTasks() {
        List<Task> tasks = new ArrayList<>();
        String query = """
            SELECT task_id, title, description, status, priority, assigned_user_id,
                   created_by_user_id, created_at, updated_at, due_date, completed_at,
                   estimated_hours, actual_hours, progress_percentage, comments,
                   attachments, project_id
            FROM Tasks
            ORDER BY created_at DESC
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                tasks.add(mapResultSetToTask(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting all tasks", e);
        }

        return tasks;
    }

    /**
     * Get tasks by assigned user ID
     */
    public List<Task> getTasksByUserId(int userId) {
        List<Task> tasks = new ArrayList<>();
        String query = """
            SELECT task_id, title, description, status, priority, assigned_user_id,
                   created_by_user_id, created_at, updated_at, due_date, completed_at,
                   estimated_hours, actual_hours, progress_percentage, comments,
                   attachments, project_id
            FROM Tasks
            WHERE assigned_user_id = ?
            ORDER BY created_at DESC
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                tasks.add(mapResultSetToTask(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting tasks by user ID: {}", userId, e);
        }

        return tasks;
    }

    /**
     * Update task
     */
    public boolean updateTask(Task task) {
        String query = """
            UPDATE Tasks
            SET title = ?, description = ?, status = ?, priority = ?,
                due_date = ?, estimated_hours = ?, actual_hours = ?,
                progress_percentage = ?, comments = ?, updated_at = GETDATE()
            WHERE task_id = ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, task.getTitle());
            stmt.setString(2, task.getDescription());
            stmt.setString(3, task.getStatus().name());
            stmt.setString(4, task.getPriority().name());

            if (task.getDueDate() != null) {
                stmt.setTimestamp(5, Timestamp.valueOf(task.getDueDate()));
            } else {
                stmt.setNull(5, Types.TIMESTAMP);
            }

            stmt.setInt(6, task.getEstimatedHours());
            stmt.setInt(7, task.getActualHours());
            stmt.setDouble(8, task.getProgressPercentage());
            stmt.setString(9, task.getComments());
            stmt.setInt(10, task.getTaskId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("Task updated successfully: {}", task.getTitle());
                return true;
            }

        } catch (SQLException e) {
            logger.error("Error updating task: {}", task.getTitle(), e);
        }

        return false;
    }

    /**
     * Delete task
     */
    public boolean deleteTask(int taskId) {
        String query = "DELETE FROM Tasks WHERE task_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, taskId);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("Task deleted successfully: {}", taskId);
                return true;
            }

        } catch (SQLException e) {
            logger.error("Error deleting task: {}", taskId, e);
        }

        return false;
    }

    /**
     * Get task statistics
     */
    public TaskStatistics getTaskStatistics() {
        String query = """
            SELECT
                COUNT(*) as total_tasks,
                SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_tasks,
                SUM(CASE WHEN status IN ('TODO', 'IN_PROGRESS', 'REVIEW') THEN 1 ELSE 0 END) as pending_tasks,
                SUM(CASE WHEN due_date < GETDATE() AND status != 'COMPLETED' THEN 1 ELSE 0 END) as overdue_tasks
            FROM Tasks
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return new TaskStatistics(
                    rs.getInt("total_tasks"),
                    rs.getInt("completed_tasks"),
                    rs.getInt("pending_tasks"),
                    rs.getInt("overdue_tasks")
                );
            }

        } catch (SQLException e) {
            logger.error("Error getting task statistics", e);
        }

        return new TaskStatistics(0, 0, 0, 0);
    }

    /**
     * Get weekly task creation statistics for a specific user
     * Returns array of 7 integers representing task counts for each day of the week (Monday to Sunday)
     */
    public int[] getWeeklyTaskCreationStats(int userId) {
        int[] weeklyStats = new int[7]; // Monday to Sunday

        String query = """
            SELECT
                DATEPART(WEEKDAY, created_at) as day_of_week,
                COUNT(*) as task_count
            FROM Tasks
            WHERE assigned_user_id = ?
                AND created_at >= DATEADD(DAY, -7, GETDATE())
                AND created_at <= GETDATE()
            GROUP BY DATEPART(WEEKDAY, created_at)
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int dayOfWeek = rs.getInt("day_of_week");
                int taskCount = rs.getInt("task_count");

                // SQL Server: Sunday=1, Monday=2, ..., Saturday=7
                // Convert to array index: Monday=0, Tuesday=1, ..., Sunday=6
                int arrayIndex;
                if (dayOfWeek == 1) { // Sunday
                    arrayIndex = 6;
                } else { // Monday to Saturday
                    arrayIndex = dayOfWeek - 2;
                }

                if (arrayIndex >= 0 && arrayIndex < 7) {
                    weeklyStats[arrayIndex] = taskCount;
                }
            }

            logger.info("Weekly task creation stats for user {}: {}", userId, java.util.Arrays.toString(weeklyStats));

        } catch (SQLException e) {
            logger.error("Error getting weekly task creation statistics for user: {}", userId, e);
        }

        return weeklyStats;
    }

    /**
     * Get weekly task assignment statistics for current week
     * Returns array of 7 integers representing task counts for each day of the current week
     */
    public int[] getCurrentWeekTaskStats(int userId) {
        int[] weeklyStats = new int[7]; // Monday to Sunday

        String query = """
            SELECT
                DATEPART(WEEKDAY, created_at) as day_of_week,
                COUNT(*) as task_count
            FROM Tasks
            WHERE assigned_user_id = ?
                AND created_at >= DATEADD(DAY, -(DATEPART(WEEKDAY, GETDATE()) + 5) % 7, GETDATE())
                AND created_at < DATEADD(DAY, 7 - (DATEPART(WEEKDAY, GETDATE()) + 5) % 7, GETDATE())
            GROUP BY DATEPART(WEEKDAY, created_at)
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int dayOfWeek = rs.getInt("day_of_week");
                int taskCount = rs.getInt("task_count");

                // Convert SQL Server day of week to array index
                int arrayIndex;
                if (dayOfWeek == 1) { // Sunday
                    arrayIndex = 6;
                } else { // Monday to Saturday
                    arrayIndex = dayOfWeek - 2;
                }

                if (arrayIndex >= 0 && arrayIndex < 7) {
                    weeklyStats[arrayIndex] = taskCount;
                }
            }

            logger.info("Current week task stats for user {}: {}", userId, java.util.Arrays.toString(weeklyStats));

        } catch (SQLException e) {
            logger.error("Error getting current week task statistics for user: {}", userId, e);
        }

        return weeklyStats;
    }

    /**
     * Map ResultSet to Task object
     */
    private Task mapResultSetToTask(ResultSet rs) throws SQLException {
        Task task = new Task();
        task.setTaskId(rs.getInt("task_id"));
        task.setTitle(rs.getString("title"));
        task.setDescription(rs.getString("description"));
        task.setStatus(TaskStatus.valueOf(rs.getString("status")));
        task.setPriority(TaskPriority.valueOf(rs.getString("priority")));
        task.setAssignedUserId(rs.getInt("assigned_user_id"));
        task.setCreatedByUserId(rs.getInt("created_by_user_id"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            task.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            task.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        Timestamp dueDate = rs.getTimestamp("due_date");
        if (dueDate != null) {
            task.setDueDate(dueDate.toLocalDateTime());
        }

        Timestamp completedAt = rs.getTimestamp("completed_at");
        if (completedAt != null) {
            task.setCompletedAt(completedAt.toLocalDateTime());
        }

        task.setEstimatedHours(rs.getInt("estimated_hours"));
        task.setActualHours(rs.getInt("actual_hours"));
        task.setProgressPercentage(rs.getDouble("progress_percentage"));
        task.setComments(rs.getString("comments"));
        task.setAttachments(rs.getString("attachments"));
        task.setProjectId(rs.getInt("project_id"));

        return task;
    }

    /**
     * Inner class for task statistics
     */
    public static class TaskStatistics {
        private final int totalTasks;
        private final int completedTasks;
        private final int pendingTasks;
        private final int overdueTasks;

        public TaskStatistics(int totalTasks, int completedTasks, int pendingTasks, int overdueTasks) {
            this.totalTasks = totalTasks;
            this.completedTasks = completedTasks;
            this.pendingTasks = pendingTasks;
            this.overdueTasks = overdueTasks;
        }

        public int getTotalTasks() { return totalTasks; }
        public int getCompletedTasks() { return completedTasks; }
        public int getPendingTasks() { return pendingTasks; }
        public int getOverdueTasks() { return overdueTasks; }
    }
}
