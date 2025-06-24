package com.example.taskmanagerv3.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration class for Google Gemini API
 */
public class GeminiConfig {
    private static final Logger logger = LoggerFactory.getLogger(GeminiConfig.class);

    private static final String CONFIG_FILE = "gemini.properties";
    private static final String DEFAULT_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent";
    private static final String DEFAULT_MODEL = "gemini-2.0-flash-exp";
    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final int DEFAULT_MAX_TOKENS = 1000;
    private static final int DEFAULT_TIMEOUT = 30000; // 30 seconds

    private static Properties properties;
    private static boolean initialized = false;

    static {
        loadConfiguration();
    }

    /**
     * Load configuration from properties file
     */
    private static void loadConfiguration() {
        properties = new Properties();

        try (InputStream input = GeminiConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                properties.load(input);
                logger.info("Gemini configuration loaded from {}", CONFIG_FILE);
            } else {
                logger.warn("Configuration file {} not found, using default values", CONFIG_FILE);
            }
        } catch (IOException e) {
            logger.error("Error loading Gemini configuration", e);
        }

        initialized = true;
    }

    /**
     * Get Gemini API key
     */
    public static String getApiKey() {
        String apiKey = properties.getProperty("gemini.api.key");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            // Try environment variable as fallback
            apiKey = System.getenv("GEMINI_API_KEY");
        }
        return apiKey;
    }

    /**
     * Get API URL
     */
    public static String getApiUrl() {
        return properties.getProperty("gemini.api.url", DEFAULT_API_URL);
    }

    /**
     * Get model name
     */
    public static String getModel() {
        return properties.getProperty("gemini.model", DEFAULT_MODEL);
    }

    /**
     * Get temperature for response generation
     */
    public static double getTemperature() {
        String temp = properties.getProperty("gemini.temperature");
        if (temp != null) {
            try {
                return Double.parseDouble(temp);
            } catch (NumberFormatException e) {
                logger.warn("Invalid temperature value: {}, using default", temp);
            }
        }
        return DEFAULT_TEMPERATURE;
    }

    /**
     * Get maximum tokens for response
     */
    public static int getMaxTokens() {
        String maxTokens = properties.getProperty("gemini.max.tokens");
        if (maxTokens != null) {
            try {
                return Integer.parseInt(maxTokens);
            } catch (NumberFormatException e) {
                logger.warn("Invalid max tokens value: {}, using default", maxTokens);
            }
        }
        return DEFAULT_MAX_TOKENS;
    }

    /**
     * Get request timeout in milliseconds
     */
    public static int getTimeout() {
        String timeout = properties.getProperty("gemini.timeout");
        if (timeout != null) {
            try {
                return Integer.parseInt(timeout);
            } catch (NumberFormatException e) {
                logger.warn("Invalid timeout value: {}, using default", timeout);
            }
        }
        return DEFAULT_TIMEOUT;
    }

    /**
     * Check if API is properly configured
     */
    public static boolean isConfigured() {
        String apiKey = getApiKey();
        boolean configured = apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equals("YOUR_GEMINI_API_KEY_HERE");

        if (!configured) {
            logger.warn("Gemini API not configured. API key: {}",
                apiKey == null ? "NULL" :
                apiKey.equals("YOUR_GEMINI_API_KEY_HERE") ? "DEFAULT_PLACEHOLDER" :
                "SET_BUT_EMPTY");
        } else {
            logger.info("Gemini API configured successfully");
        }

        return configured;
    }

    /**
     * Get system prompt for task management context
     */
    public static String getSystemPrompt() {
        return properties.getProperty("gemini.system.prompt",
            "You are an AI assistant for a task management system called WorkFlow Manager. " +
            "You help users manage their tasks, provide productivity insights, and offer suggestions " +
            "for better workflow management. Be helpful, concise, and professional. " +
            "Focus on task management, productivity tips, and workflow optimization.");
    }

    /**
     * Check if conversation history should be maintained
     */
    public static boolean isConversationHistoryEnabled() {
        String enabled = properties.getProperty("gemini.conversation.history.enabled", "true");
        return Boolean.parseBoolean(enabled);
    }

    /**
     * Get maximum conversation history length
     */
    public static int getMaxConversationHistory() {
        String maxHistory = properties.getProperty("gemini.conversation.history.max", "10");
        try {
            return Integer.parseInt(maxHistory);
        } catch (NumberFormatException e) {
            logger.warn("Invalid max conversation history value: {}, using default", maxHistory);
            return 10;
        }
    }
}
