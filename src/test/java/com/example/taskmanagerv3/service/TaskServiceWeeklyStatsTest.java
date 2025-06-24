package com.example.taskmanagerv3.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for TaskService weekly statistics functionality
 */
public class TaskServiceWeeklyStatsTest {
    
    private TaskService taskService;
    
    @BeforeEach
    void setUp() {
        taskService = new TaskService();
    }
    
    @Test
    @DisplayName("Test getWeeklyTaskCreationStats returns array of correct size")
    void testGetWeeklyTaskCreationStatsArraySize() {
        // Test with a sample user ID
        int testUserId = 1;
        
        // Get weekly stats
        int[] weeklyStats = taskService.getWeeklyTaskCreationStats(testUserId);
        
        // Verify array size is 7 (Monday to Sunday)
        assertNotNull(weeklyStats, "Weekly stats array should not be null");
        assertEquals(7, weeklyStats.length, "Weekly stats array should have 7 elements");
    }
    
    @Test
    @DisplayName("Test getCurrentWeekTaskStats returns array of correct size")
    void testGetCurrentWeekTaskStatsArraySize() {
        // Test with a sample user ID
        int testUserId = 1;
        
        // Get current week stats
        int[] weeklyStats = taskService.getCurrentWeekTaskStats(testUserId);
        
        // Verify array size is 7 (Monday to Sunday)
        assertNotNull(weeklyStats, "Current week stats array should not be null");
        assertEquals(7, weeklyStats.length, "Current week stats array should have 7 elements");
    }
    
    @Test
    @DisplayName("Test weekly stats contain non-negative values")
    void testWeeklyStatsNonNegativeValues() {
        // Test with a sample user ID
        int testUserId = 1;
        
        // Get weekly stats
        int[] weeklyStats = taskService.getWeeklyTaskCreationStats(testUserId);
        
        // Verify all values are non-negative
        for (int i = 0; i < weeklyStats.length; i++) {
            assertTrue(weeklyStats[i] >= 0, 
                "Weekly stats value at index " + i + " should be non-negative, but was: " + weeklyStats[i]);
        }
    }
    
    @Test
    @DisplayName("Test current week stats contain non-negative values")
    void testCurrentWeekStatsNonNegativeValues() {
        // Test with a sample user ID
        int testUserId = 1;
        
        // Get current week stats
        int[] weeklyStats = taskService.getCurrentWeekTaskStats(testUserId);
        
        // Verify all values are non-negative
        for (int i = 0; i < weeklyStats.length; i++) {
            assertTrue(weeklyStats[i] >= 0, 
                "Current week stats value at index " + i + " should be non-negative, but was: " + weeklyStats[i]);
        }
    }
    
    @Test
    @DisplayName("Test weekly stats with invalid user ID")
    void testWeeklyStatsWithInvalidUserId() {
        // Test with invalid user ID
        int invalidUserId = -1;
        
        // Get weekly stats
        int[] weeklyStats = taskService.getWeeklyTaskCreationStats(invalidUserId);
        
        // Should return array of zeros
        assertNotNull(weeklyStats, "Weekly stats array should not be null even for invalid user");
        assertEquals(7, weeklyStats.length, "Weekly stats array should have 7 elements");
        
        // All values should be 0 for invalid user
        for (int i = 0; i < weeklyStats.length; i++) {
            assertEquals(0, weeklyStats[i], 
                "Weekly stats value at index " + i + " should be 0 for invalid user");
        }
    }
    
    @Test
    @DisplayName("Test current week stats with invalid user ID")
    void testCurrentWeekStatsWithInvalidUserId() {
        // Test with invalid user ID
        int invalidUserId = -1;
        
        // Get current week stats
        int[] weeklyStats = taskService.getCurrentWeekTaskStats(invalidUserId);
        
        // Should return array of zeros
        assertNotNull(weeklyStats, "Current week stats array should not be null even for invalid user");
        assertEquals(7, weeklyStats.length, "Current week stats array should have 7 elements");
        
        // All values should be 0 for invalid user
        for (int i = 0; i < weeklyStats.length; i++) {
            assertEquals(0, weeklyStats[i], 
                "Current week stats value at index " + i + " should be 0 for invalid user");
        }
    }
}
