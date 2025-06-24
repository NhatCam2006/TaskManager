package com.example.taskmanagerv3.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for exporting application logs to various formats
 */
public class LogExportService {
    private static final Logger logger = LoggerFactory.getLogger(LogExportService.class);
    
    private static final String LOG_FILE_PATH = "logs/workflow-manager.log";
    private static final Pattern LOG_PATTERN = Pattern.compile(
        "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}) \\[([^\\]]+)\\] (\\w+)\\s+([^\\s]+) - (.+)"
    );
    
    /**
     * Log entry model
     */
    public static class LogEntry {
        private final String timestamp;
        private final String thread;
        private final String level;
        private final String logger;
        private final String message;
        
        public LogEntry(String timestamp, String thread, String level, String logger, String message) {
            this.timestamp = timestamp;
            this.thread = thread;
            this.level = level;
            this.logger = logger;
            this.message = message;
        }
        
        // Getters
        public String getTimestamp() { return timestamp; }
        public String getThread() { return thread; }
        public String getLevel() { return level; }
        public String getLogger() { return logger; }
        public String getMessage() { return message; }
    }
    
    /**
     * Export logs to Excel format
     */
    public boolean exportLogsToExcel(File outputFile, LocalDateTime fromDate, LocalDateTime toDate) {
        try {
            List<LogEntry> logEntries = parseLogFile(fromDate, toDate);
            
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Application Logs");
                
                // Create header row
                Row headerRow = sheet.createRow(0);
                CellStyle headerStyle = createHeaderStyle(workbook);
                
                String[] headers = {"Timestamp", "Thread", "Level", "Logger", "Message"};
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }
                
                // Add data rows
                int rowNum = 1;
                for (LogEntry entry : logEntries) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(entry.getTimestamp());
                    row.createCell(1).setCellValue(entry.getThread());
                    row.createCell(2).setCellValue(entry.getLevel());
                    row.createCell(3).setCellValue(entry.getLogger());
                    row.createCell(4).setCellValue(entry.getMessage());
                }
                
                // Auto-size columns
                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }
                
                // Write to file
                try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
                    workbook.write(fileOut);
                }
            }
            
            logger.info("Logs exported to Excel: {}", outputFile.getAbsolutePath());
            return true;
            
        } catch (Exception e) {
            logger.error("Error exporting logs to Excel", e);
            return false;
        }
    }
    
    /**
     * Export logs to CSV format
     */
    public boolean exportLogsToCSV(File outputFile, LocalDateTime fromDate, LocalDateTime toDate) {
        try {
            List<LogEntry> logEntries = parseLogFile(fromDate, toDate);
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
                // Write header
                writer.println("Timestamp,Thread,Level,Logger,Message");
                
                // Write data
                for (LogEntry entry : logEntries) {
                    writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        escapeCSV(entry.getTimestamp()),
                        escapeCSV(entry.getThread()),
                        escapeCSV(entry.getLevel()),
                        escapeCSV(entry.getLogger()),
                        escapeCSV(entry.getMessage())
                    );
                }
            }
            
            logger.info("Logs exported to CSV: {}", outputFile.getAbsolutePath());
            return true;
            
        } catch (Exception e) {
            logger.error("Error exporting logs to CSV", e);
            return false;
        }
    }
    
    /**
     * Export logs to plain text format
     */
    public boolean exportLogsToText(File outputFile, LocalDateTime fromDate, LocalDateTime toDate) {
        try {
            List<LogEntry> logEntries = parseLogFile(fromDate, toDate);
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
                writer.println("Application Logs Export");
                writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                writer.println("Period: " + fromDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + 
                              " to " + toDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                writer.println("Total entries: " + logEntries.size());
                writer.println("=" + "=".repeat(80));
                writer.println();
                
                for (LogEntry entry : logEntries) {
                    writer.printf("%s [%s] %-5s %s - %s%n",
                        entry.getTimestamp(),
                        entry.getThread(),
                        entry.getLevel(),
                        entry.getLogger(),
                        entry.getMessage()
                    );
                }
            }
            
            logger.info("Logs exported to text: {}", outputFile.getAbsolutePath());
            return true;
            
        } catch (Exception e) {
            logger.error("Error exporting logs to text", e);
            return false;
        }
    }
    
    /**
     * Parse log file and extract entries within date range
     */
    private List<LogEntry> parseLogFile(LocalDateTime fromDate, LocalDateTime toDate) throws IOException {
        List<LogEntry> entries = new ArrayList<>();
        Path logPath = Paths.get(LOG_FILE_PATH);
        
        if (!Files.exists(logPath)) {
            logger.warn("Log file not found: {}", LOG_FILE_PATH);
            return entries;
        }
        
        try (BufferedReader reader = Files.newBufferedReader(logPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = LOG_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String timestamp = matcher.group(1);
                    String thread = matcher.group(2);
                    String level = matcher.group(3);
                    String loggerName = matcher.group(4);
                    String message = matcher.group(5);
                    
                    // Parse timestamp and check if within range
                    try {
                        LocalDateTime logTime = LocalDateTime.parse(timestamp, 
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
                        
                        if ((logTime.isEqual(fromDate) || logTime.isAfter(fromDate)) &&
                            (logTime.isEqual(toDate) || logTime.isBefore(toDate))) {
                            entries.add(new LogEntry(timestamp, thread, level, loggerName, message));
                        }
                    } catch (Exception e) {
                        // Skip entries with invalid timestamps
                        logger.debug("Skipping log entry with invalid timestamp: {}", timestamp);
                    }
                }
            }
        }
        
        return entries;
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
     * Escape CSV special characters
     */
    private String escapeCSV(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
    
    /**
     * Get log statistics
     */
    public LogStatistics getLogStatistics(LocalDateTime fromDate, LocalDateTime toDate) {
        try {
            List<LogEntry> entries = parseLogFile(fromDate, toDate);
            return new LogStatistics(entries);
        } catch (IOException e) {
            logger.error("Error getting log statistics", e);
            return new LogStatistics(new ArrayList<>());
        }
    }
    
    /**
     * Log statistics model
     */
    public static class LogStatistics {
        private final int totalEntries;
        private final int errorCount;
        private final int warnCount;
        private final int infoCount;
        private final int debugCount;
        
        public LogStatistics(List<LogEntry> entries) {
            this.totalEntries = entries.size();
            this.errorCount = (int) entries.stream().filter(e -> "ERROR".equals(e.getLevel())).count();
            this.warnCount = (int) entries.stream().filter(e -> "WARN".equals(e.getLevel())).count();
            this.infoCount = (int) entries.stream().filter(e -> "INFO".equals(e.getLevel())).count();
            this.debugCount = (int) entries.stream().filter(e -> "DEBUG".equals(e.getLevel())).count();
        }
        
        // Getters
        public int getTotalEntries() { return totalEntries; }
        public int getErrorCount() { return errorCount; }
        public int getWarnCount() { return warnCount; }
        public int getInfoCount() { return infoCount; }
        public int getDebugCount() { return debugCount; }
    }
}
