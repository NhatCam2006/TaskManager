package com.example.taskmanagerv3.util;

import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to convert Markdown report to Word document
 */
public class WordDocumentGenerator {
    private static final Logger logger = LoggerFactory.getLogger(WordDocumentGenerator.class);
    
    private XWPFDocument document;
    private int currentHeadingLevel = 0;
    
    /**
     * Convert Markdown file to Word document
     */
    public boolean convertMarkdownToWord(String markdownFilePath, String outputWordPath) {
        try {
            // Read markdown content
            List<String> lines = Files.readAllLines(Paths.get(markdownFilePath));
            
            // Create new Word document
            document = new XWPFDocument();
            
            // Set up document properties
            setupDocumentProperties();
            
            // Process each line
            for (String line : lines) {
                processLine(line);
            }
            
            // Save document
            try (FileOutputStream out = new FileOutputStream(outputWordPath)) {
                document.write(out);
            }
            
            document.close();
            logger.info("Successfully converted Markdown to Word: {}", outputWordPath);
            return true;
            
        } catch (Exception e) {
            logger.error("Error converting Markdown to Word", e);
            return false;
        }
    }
    
    /**
     * Set up document properties
     */
    private void setupDocumentProperties() {
        // Set document title
        document.getProperties().getCoreProperties().setTitle("Báo Cáo Phân Tích Hệ Thống TaskManager V3");
        document.getProperties().getCoreProperties().setCreator("TaskManager V3 System");
        document.getProperties().getCoreProperties().setDescription("Báo cáo phân tích toàn diện hệ thống quản lý công việc");
    }
    
    /**
     * Process a single line of markdown
     */
    private void processLine(String line) {
        if (line.trim().isEmpty()) {
            // Add empty paragraph for spacing
            document.createParagraph();
            return;
        }
        
        // Check for headers
        if (line.startsWith("#")) {
            processHeader(line);
        }
        // Check for code blocks
        else if (line.startsWith("```")) {
            processCodeBlock(line);
        }
        // Check for lists
        else if (line.trim().startsWith("-") || line.trim().startsWith("*")) {
            processList(line);
        }
        // Check for bold text
        else if (line.contains("**")) {
            processBoldText(line);
        }
        // Regular paragraph
        else {
            processRegularText(line);
        }
    }
    
    /**
     * Process header lines
     */
    private void processHeader(String line) {
        // Count # symbols to determine header level
        int level = 0;
        for (char c : line.toCharArray()) {
            if (c == '#') level++;
            else break;
        }
        
        String headerText = line.substring(level).trim();
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(headerText);
        run.setBold(true);
        
        // Set font size based on header level
        switch (level) {
            case 1:
                run.setFontSize(20);
                paragraph.setStyle("Heading1");
                break;
            case 2:
                run.setFontSize(18);
                paragraph.setStyle("Heading2");
                break;
            case 3:
                run.setFontSize(16);
                paragraph.setStyle("Heading3");
                break;
            case 4:
                run.setFontSize(14);
                paragraph.setStyle("Heading4");
                break;
            default:
                run.setFontSize(12);
                break;
        }
        
        currentHeadingLevel = level;
    }
    
    /**
     * Process code blocks
     */
    private void processCodeBlock(String line) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        
        if (line.length() > 3) {
            // Code block with language specification
            String code = line.substring(3).trim();
            run.setText(code);
        } else {
            // Empty code block marker
            run.setText("");
        }
        
        // Style as code
        run.setFontFamily("Courier New");
        run.setFontSize(10);
        paragraph.setStyle("Code");
    }
    
    /**
     * Process list items
     */
    private void processList(String line) {
        String listText = line.trim().substring(1).trim(); // Remove - or *
        
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText("• " + listText);
        
        // Set indentation for nested lists
        int indentLevel = (line.length() - line.trim().length()) / 2;
        paragraph.setIndentationLeft(indentLevel * 360); // 360 twips = 0.25 inch
    }
    
    /**
     * Process text with bold formatting
     */
    private void processBoldText(String line) {
        XWPFParagraph paragraph = document.createParagraph();
        
        // Pattern to match **bold** text
        Pattern boldPattern = Pattern.compile("\\*\\*(.*?)\\*\\*");
        Matcher matcher = boldPattern.matcher(line);
        
        int lastEnd = 0;
        while (matcher.find()) {
            // Add text before bold
            if (matcher.start() > lastEnd) {
                XWPFRun normalRun = paragraph.createRun();
                normalRun.setText(line.substring(lastEnd, matcher.start()));
            }
            
            // Add bold text
            XWPFRun boldRun = paragraph.createRun();
            boldRun.setText(matcher.group(1));
            boldRun.setBold(true);
            
            lastEnd = matcher.end();
        }
        
        // Add remaining text
        if (lastEnd < line.length()) {
            XWPFRun normalRun = paragraph.createRun();
            normalRun.setText(line.substring(lastEnd));
        }
    }
    
    /**
     * Process regular text
     */
    private void processRegularText(String line) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(line);
    }
    
    /**
     * Create Word document from markdown content directly
     */
    public boolean createWordDocument(String content, String outputPath) {
        try {
            document = new XWPFDocument();
            setupDocumentProperties();
            
            String[] lines = content.split("\n");
            for (String line : lines) {
                processLine(line);
            }
            
            try (FileOutputStream out = new FileOutputStream(outputPath)) {
                document.write(out);
            }
            
            document.close();
            logger.info("Word document created successfully: {}", outputPath);
            return true;
            
        } catch (Exception e) {
            logger.error("Error creating Word document", e);
            return false;
        }
    }
    
    /**
     * Add table of contents (simplified version)
     */
    private void addTableOfContents() {
        XWPFParagraph tocParagraph = document.createParagraph();
        XWPFRun tocRun = tocParagraph.createRun();
        tocRun.setText("MỤC LỤC");
        tocRun.setBold(true);
        tocRun.setFontSize(16);
        
        // Add some spacing
        document.createParagraph();
    }
    
    /**
     * Add page break
     */
    private void addPageBreak() {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.addBreak(BreakType.PAGE);
    }
    
    /**
     * Set document margins and formatting
     */
    private void setDocumentFormatting() {
        // This would require more complex POI operations
        // For now, we'll use default formatting
    }
}
