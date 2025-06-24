package com.example.taskmanagerv3.util;

import com.example.taskmanagerv3.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * Utility class for cleaning chat data
 * WARNING: This will permanently delete all chat history!
 */
public class ChatDataCleaner {
    private static final Logger logger = LoggerFactory.getLogger(ChatDataCleaner.class);
    
    public static void main(String[] args) {
        logger.info("=== CHAT DATA CLEANER ===");
        logger.warn("This tool will PERMANENTLY DELETE all chat messages and files!");
        
        ChatService chatService = new ChatService();
        Scanner scanner = new Scanner(System.in);
        
        try {
            // Show current statistics
            System.out.println("\n=== CURRENT CHAT STATISTICS ===");
            ChatService.ChatStatistics stats = chatService.getChatStatistics();
            System.out.println(stats.toString());
            
            if (stats.totalMessages == 0 && stats.totalFiles == 0) {
                System.out.println("\n✅ No chat data found. Database is already clean.");
                return;
            }
            
            // Confirmation prompts
            System.out.println("\n⚠️  WARNING: This action cannot be undone!");
            System.out.println("All chat messages and files will be permanently deleted.");
            System.out.print("\nDo you want to continue? (type 'YES' to confirm): ");
            
            String confirmation1 = scanner.nextLine().trim();
            if (!"YES".equals(confirmation1)) {
                System.out.println("❌ Operation cancelled.");
                return;
            }
            
            System.out.print("\nAre you absolutely sure? (type 'DELETE ALL' to confirm): ");
            String confirmation2 = scanner.nextLine().trim();
            if (!"DELETE ALL".equals(confirmation2)) {
                System.out.println("❌ Operation cancelled.");
                return;
            }
            
            // Perform deletion
            System.out.println("\n🗑️  Deleting all chat data...");
            boolean success = chatService.deleteAllChatHistory();
            
            if (success) {
                System.out.println("✅ All chat data has been successfully deleted!");
                
                // Show final statistics
                System.out.println("\n=== FINAL STATISTICS ===");
                ChatService.ChatStatistics finalStats = chatService.getChatStatistics();
                System.out.println(finalStats.toString());
                
                if (finalStats.totalMessages == 0 && finalStats.totalFiles == 0) {
                    System.out.println("✅ Database cleanup completed successfully.");
                } else {
                    System.out.println("⚠️  Some data may still remain. Please check manually.");
                }
            } else {
                System.out.println("❌ Failed to delete chat data. Check logs for details.");
            }
            
        } catch (Exception e) {
            logger.error("Error during chat data cleanup", e);
            System.out.println("❌ Error occurred during cleanup: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }
    
    /**
     * Delete all chat data programmatically (for use in other classes)
     */
    public static boolean deleteAllChatDataSilently() {
        try {
            ChatService chatService = new ChatService();
            ChatService.ChatStatistics stats = chatService.getChatStatistics();
            
            logger.info("Deleting chat data: {} messages, {} files", 
                       stats.totalMessages, stats.totalFiles);
            
            boolean success = chatService.deleteAllChatHistory();
            
            if (success) {
                logger.info("All chat data deleted successfully");
            } else {
                logger.error("Failed to delete chat data");
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error during silent chat data deletion", e);
            return false;
        }
    }
    
    /**
     * Get current chat statistics
     */
    public static ChatService.ChatStatistics getCurrentStats() {
        try {
            ChatService chatService = new ChatService();
            return chatService.getChatStatistics();
        } catch (Exception e) {
            logger.error("Error getting chat statistics", e);
            return new ChatService.ChatStatistics();
        }
    }
}
