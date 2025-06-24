package com.example.taskmanagerv3.model.gemini;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request model for Gemini API
 */
public class GeminiRequest {
    
    @JsonProperty("contents")
    private List<Content> contents;
    
    @JsonProperty("generationConfig")
    private GenerationConfig generationConfig;
    
    @JsonProperty("safetySettings")
    private List<SafetySetting> safetySettings;
    
    public GeminiRequest() {}
    
    public GeminiRequest(List<Content> contents, GenerationConfig generationConfig) {
        this.contents = contents;
        this.generationConfig = generationConfig;
    }
    
    // Getters and Setters
    public List<Content> getContents() {
        return contents;
    }
    
    public void setContents(List<Content> contents) {
        this.contents = contents;
    }
    
    public GenerationConfig getGenerationConfig() {
        return generationConfig;
    }
    
    public void setGenerationConfig(GenerationConfig generationConfig) {
        this.generationConfig = generationConfig;
    }
    
    public List<SafetySetting> getSafetySettings() {
        return safetySettings;
    }
    
    public void setSafetySettings(List<SafetySetting> safetySettings) {
        this.safetySettings = safetySettings;
    }
    
    /**
     * Content part of the request
     */
    public static class Content {
        @JsonProperty("parts")
        private List<Part> parts;
        
        @JsonProperty("role")
        private String role;
        
        public Content() {}
        
        public Content(List<Part> parts) {
            this.parts = parts;
        }
        
        public Content(List<Part> parts, String role) {
            this.parts = parts;
            this.role = role;
        }
        
        public List<Part> getParts() {
            return parts;
        }
        
        public void setParts(List<Part> parts) {
            this.parts = parts;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
    }
    
    /**
     * Part of the content
     */
    public static class Part {
        @JsonProperty("text")
        private String text;
        
        public Part() {}
        
        public Part(String text) {
            this.text = text;
        }
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
    }
    
    /**
     * Generation configuration
     */
    public static class GenerationConfig {
        @JsonProperty("temperature")
        private Double temperature;
        
        @JsonProperty("topK")
        private Integer topK;
        
        @JsonProperty("topP")
        private Double topP;
        
        @JsonProperty("maxOutputTokens")
        private Integer maxOutputTokens;
        
        @JsonProperty("stopSequences")
        private List<String> stopSequences;
        
        public GenerationConfig() {}
        
        public GenerationConfig(Double temperature, Integer maxOutputTokens) {
            this.temperature = temperature;
            this.maxOutputTokens = maxOutputTokens;
        }
        
        // Getters and Setters
        public Double getTemperature() {
            return temperature;
        }
        
        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }
        
        public Integer getTopK() {
            return topK;
        }
        
        public void setTopK(Integer topK) {
            this.topK = topK;
        }
        
        public Double getTopP() {
            return topP;
        }
        
        public void setTopP(Double topP) {
            this.topP = topP;
        }
        
        public Integer getMaxOutputTokens() {
            return maxOutputTokens;
        }
        
        public void setMaxOutputTokens(Integer maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
        }
        
        public List<String> getStopSequences() {
            return stopSequences;
        }
        
        public void setStopSequences(List<String> stopSequences) {
            this.stopSequences = stopSequences;
        }
    }
    
    /**
     * Safety setting for content filtering
     */
    public static class SafetySetting {
        @JsonProperty("category")
        private String category;
        
        @JsonProperty("threshold")
        private String threshold;
        
        public SafetySetting() {}
        
        public SafetySetting(String category, String threshold) {
            this.category = category;
            this.threshold = threshold;
        }
        
        public String getCategory() {
            return category;
        }
        
        public void setCategory(String category) {
            this.category = category;
        }
        
        public String getThreshold() {
            return threshold;
        }
        
        public void setThreshold(String threshold) {
            this.threshold = threshold;
        }
    }
}
