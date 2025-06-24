package com.example.taskmanagerv3.service;

import com.example.taskmanagerv3.config.GeminiConfig;
import com.example.taskmanagerv3.model.gemini.GeminiRequest;
import com.example.taskmanagerv3.model.gemini.GeminiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service for communicating with Google Gemini API
 */
public class GeminiApiService {
    private static final Logger logger = LoggerFactory.getLogger(GeminiApiService.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final List<ConversationMessage> conversationHistory;

    public GeminiApiService() {
        this.objectMapper = new ObjectMapper();
        this.conversationHistory = new ArrayList<>();

        // Configure HTTP client with timeouts
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(GeminiConfig.getTimeout()))
            .build();
    }

    /**
     * Generate response from Gemini API
     */
    public String generateResponse(String userMessage, String contextData) throws IOException {
        logger.info("Generating response for message: {}", userMessage);

        if (!GeminiConfig.isConfigured()) {
            logger.warn("Gemini API is not configured. API key: {}", GeminiConfig.getApiKey() != null ? "SET" : "NOT SET");
            throw new IllegalStateException("Gemini API is not properly configured. Please set your API key.");
        }

        logger.info("Gemini API is configured. Using API URL: {}", GeminiConfig.getApiUrl());

        try {
            // Build the request
            GeminiRequest request = buildRequest(userMessage, contextData);
            logger.debug("Built Gemini request with {} contents", request.getContents().size());

            // Send request to Gemini API
            GeminiResponse response = sendRequest(request);

            // Handle response
            if (response.hasError()) {
                logger.error("Gemini API error: {}", response.getError().getMessage());
                throw new IOException("Gemini API error: " + response.getError().getMessage());
            }

            String generatedText = response.getGeneratedText();
            if (generatedText == null || generatedText.trim().isEmpty()) {
                logger.warn("Empty response from Gemini API");
                throw new IOException("Empty response from Gemini API");
            }

            logger.info("Successfully generated response of length: {}", generatedText.length());

            // Update conversation history
            if (GeminiConfig.isConversationHistoryEnabled()) {
                updateConversationHistory(userMessage, generatedText);
            }

            return generatedText.trim();

        } catch (Exception e) {
            logger.error("Error generating response from Gemini API: {}", e.getMessage(), e);
            throw new IOException("Failed to generate response: " + e.getMessage(), e);
        }
    }

    /**
     * Build Gemini API request
     */
    private GeminiRequest buildRequest(String userMessage, String contextData) {
        List<GeminiRequest.Content> contents = new ArrayList<>();

        // Combine system prompt with user message for simpler format
        String fullMessage = buildSystemPrompt(contextData) + "\n\nUser: " + userMessage;

        // Add conversation history if enabled
        if (GeminiConfig.isConversationHistoryEnabled()) {
            for (ConversationMessage msg : conversationHistory) {
                contents.add(new GeminiRequest.Content(
                    Arrays.asList(new GeminiRequest.Part(msg.getMessage())),
                    msg.getRole()
                ));
            }
        }

        // Add current user message with system context
        contents.add(new GeminiRequest.Content(
            Arrays.asList(new GeminiRequest.Part(fullMessage)),
            "user"
        ));

        // Configure generation parameters
        GeminiRequest.GenerationConfig config = new GeminiRequest.GenerationConfig(
            GeminiConfig.getTemperature(),
            GeminiConfig.getMaxTokens()
        );

        GeminiRequest request = new GeminiRequest(contents, config);

        return request;
    }

    /**
     * Build system prompt with context data
     */
    private String buildSystemPrompt(String contextData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(GeminiConfig.getSystemPrompt());

        if (contextData != null && !contextData.trim().isEmpty()) {
            prompt.append("\n\nCurrent user context:\n").append(contextData);
        }

        prompt.append("\n\nPlease provide helpful, accurate, and concise responses related to task management and productivity.");

        return prompt.toString();
    }

    /**
     * Send request to Gemini API
     */
    private GeminiResponse sendRequest(GeminiRequest request) throws IOException {
        try {
            String apiUrl = GeminiConfig.getApiUrl() + "?key=" + GeminiConfig.getApiKey();

            // Convert request to JSON
            String requestJson = objectMapper.writeValueAsString(request);
            logger.debug("Sending request to Gemini API: {}", requestJson);

            // Build HTTP request
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMillis(GeminiConfig.getTimeout()))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

            // Send request
            logger.info("Sending HTTP request to Gemini API...");
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            String responseBody = response.body();
            logger.info("Received HTTP response with status: {}", response.statusCode());
            logger.debug("Response body: {}", responseBody);

            if (response.statusCode() != 200) {
                logger.error("HTTP error {}: {}", response.statusCode(), responseBody);
                throw new IOException("HTTP error " + response.statusCode() + ": " + responseBody);
            }

            return objectMapper.readValue(responseBody, GeminiResponse.class);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }

    /**
     * Update conversation history
     */
    private void updateConversationHistory(String userMessage, String assistantResponse) {
        conversationHistory.add(new ConversationMessage("user", userMessage));
        conversationHistory.add(new ConversationMessage("model", assistantResponse));

        // Limit conversation history size
        int maxHistory = GeminiConfig.getMaxConversationHistory() * 2; // *2 for user+assistant pairs
        while (conversationHistory.size() > maxHistory) {
            conversationHistory.remove(0);
        }
    }

    /**
     * Clear conversation history
     */
    public void clearConversationHistory() {
        conversationHistory.clear();
        logger.debug("Conversation history cleared");
    }

    /**
     * Check if API is available
     */
    public boolean isApiAvailable() {
        return GeminiConfig.isConfigured();
    }

    /**
     * Test API connection with a simple request
     */
    public String testApiConnection() {
        try {
            logger.info("Testing Gemini API connection...");
            String response = generateResponse("Hello, can you respond with 'API test successful'?", "");
            logger.info("API test successful. Response: {}", response);
            return response;
        } catch (Exception e) {
            logger.error("API test failed: {}", e.getMessage(), e);
            return "API test failed: " + e.getMessage();
        }
    }

    /**
     * Close HTTP client resources
     */
    public void close() {
        // Java 11+ HttpClient doesn't need explicit closing
        logger.debug("HTTP client resources cleaned up");
    }

    /**
     * Conversation message for history tracking
     */
    private static class ConversationMessage {
        private final String role;
        private final String message;

        public ConversationMessage(String role, String message) {
            this.role = role;
            this.message = message;
        }

        public String getRole() {
            return role;
        }

        public String getMessage() {
            return message;
        }
    }
}
