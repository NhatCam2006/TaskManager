package com.example.taskmanagerv3.util;

import com.example.taskmanagerv3.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quick utility to delete all chat history without prompts
 * Use with caution!
 */
public class QuickChatCleaner {
    private static final Logger logger = LoggerFactory.getLogger(QuickChatCleaner.class);
    
    /**
     * Delete all chat history immediately
     * WARNING: No confirmation prompts!
     */
    public static void deleteAllChatHistoryNow() {
        logger.warn("=== QUICK CHAT CLEANER STARTED ===");
        
        try {
            ChatService chatService = new ChatService();
            
            // Get current stats
            ChatService.ChatStatistics beforeStats = chatService.getChatStatistics();
            logger.info("Before deletion: {} messages, {} files, {} total size", 
                       beforeStats.totalMessages, beforeStats.totalFiles, beforeStats.getFormattedFileSize());
            
            // Delete everything
            boolean success = chatService.deleteAllChatHistory();
            
            if (success) {
                // Verify deletion
                ChatService.ChatStatistics afterStats = chatService.getChatStatistics();
                logger.info("After deletion: {} messages, {} files, {} total size", 
                           afterStats.totalMessages, afterStats.totalFiles, afterStats.getFormattedFileSize());
                
                if (afterStats.totalMessages == 0 && afterStats.totalFiles == 0) {
                    logger.info("✅ ALL CHAT HISTORY DELETED SUCCESSFULLY");
                } else {
                    logger.warn("⚠️ Some data may still remain");
                }
            } else {
                logger.error("❌ FAILED TO DELETE CHAT HISTORY");
            }
            
        } catch (Exception e) {
            logger.error("Error during quick chat cleanup", e);
        }
        
        logger.warn("=== QUICK CHAT CLEANER FINISHED ===");
    }
    
    /**
     * Main method for standalone execution
     */
    public static void main(String[] args) {
        System.out.println("🗑️ Quick Chat Cleaner - Deleting all chat history...");
        deleteAllChatHistoryNow();
        System.out.println("✅ Done!");
    }
}
