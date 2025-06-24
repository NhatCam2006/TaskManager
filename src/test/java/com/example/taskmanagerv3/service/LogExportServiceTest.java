package com.example.taskmanagerv3.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for LogExportService
 */
public class LogExportServiceTest {
    
    private LogExportService logExportService;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        logExportService = new LogExportService();
    }
    
    @Test
    void testExportLogsToExcel() {
        // Given
        File outputFile = tempDir.resolve("test_logs.xlsx").toFile();
        LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
        LocalDateTime toDate = LocalDateTime.now();
        
        // When
        boolean result = logExportService.exportLogsToExcel(outputFile, fromDate, toDate);
        
        // Then
        assertTrue(result, "Export should succeed even with no log file");
        assertTrue(outputFile.exists(), "Output file should be created");
        assertTrue(outputFile.length() > 0, "Output file should not be empty");
    }
    
    @Test
    void testExportLogsToCSV() {
        // Given
        File outputFile = tempDir.resolve("test_logs.csv").toFile();
        LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
        LocalDateTime toDate = LocalDateTime.now();
        
        // When
        boolean result = logExportService.exportLogsToCSV(outputFile, fromDate, toDate);
        
        // Then
        assertTrue(result, "Export should succeed even with no log file");
        assertTrue(outputFile.exists(), "Output file should be created");
        assertTrue(outputFile.length() > 0, "Output file should not be empty");
    }
    
    @Test
    void testExportLogsToText() {
        // Given
        File outputFile = tempDir.resolve("test_logs.txt").toFile();
        LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
        LocalDateTime toDate = LocalDateTime.now();
        
        // When
        boolean result = logExportService.exportLogsToText(outputFile, fromDate, toDate);
        
        // Then
        assertTrue(result, "Export should succeed even with no log file");
        assertTrue(outputFile.exists(), "Output file should be created");
        assertTrue(outputFile.length() > 0, "Output file should not be empty");
    }
    
    @Test
    void testGetLogStatistics() {
        // Given
        LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
        LocalDateTime toDate = LocalDateTime.now();
        
        // When
        LogExportService.LogStatistics stats = logExportService.getLogStatistics(fromDate, toDate);
        
        // Then
        assertNotNull(stats, "Statistics should not be null");
        assertTrue(stats.getTotalEntries() >= 0, "Total entries should be non-negative");
        assertTrue(stats.getErrorCount() >= 0, "Error count should be non-negative");
        assertTrue(stats.getWarnCount() >= 0, "Warn count should be non-negative");
        assertTrue(stats.getInfoCount() >= 0, "Info count should be non-negative");
        assertTrue(stats.getDebugCount() >= 0, "Debug count should be non-negative");
    }
}
