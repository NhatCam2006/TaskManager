package com.example.taskmanagerv3.model.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response model for Gemini API
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiResponse {

    @JsonProperty("candidates")
    private List<Candidate> candidates;

    @JsonProperty("usageMetadata")
    private UsageMetadata usageMetadata;

    @JsonProperty("error")
    private ErrorInfo error;

    public GeminiResponse() {}

    // Getters and Setters
    public List<Candidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<Candidate> candidates) {
        this.candidates = candidates;
    }

    public UsageMetadata getUsageMetadata() {
        return usageMetadata;
    }

    public void setUsageMetadata(UsageMetadata usageMetadata) {
        this.usageMetadata = usageMetadata;
    }

    public ErrorInfo getError() {
        return error;
    }

    public void setError(ErrorInfo error) {
        this.error = error;
    }

    /**
     * Get the text content from the first candidate
     */
    public String getGeneratedText() {
        if (candidates != null && !candidates.isEmpty()) {
            Candidate candidate = candidates.get(0);
            if (candidate.getContent() != null &&
                candidate.getContent().getParts() != null &&
                !candidate.getContent().getParts().isEmpty()) {
                return candidate.getContent().getParts().get(0).getText();
            }
        }
        return null;
    }

    /**
     * Check if response has error
     */
    public boolean hasError() {
        return error != null;
    }

    /**
     * Candidate response
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Candidate {
        @JsonProperty("content")
        private Content content;

        @JsonProperty("finishReason")
        private String finishReason;

        @JsonProperty("index")
        private Integer index;

        @JsonProperty("safetyRatings")
        private List<SafetyRating> safetyRatings;

        @JsonProperty("avgLogprobs")
        private Double avgLogprobs;

        public Candidate() {}

        public Content getContent() {
            return content;
        }

        public void setContent(Content content) {
            this.content = content;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public List<SafetyRating> getSafetyRatings() {
            return safetyRatings;
        }

        public void setSafetyRatings(List<SafetyRating> safetyRatings) {
            this.safetyRatings = safetyRatings;
        }

        public Double getAvgLogprobs() {
            return avgLogprobs;
        }

        public void setAvgLogprobs(Double avgLogprobs) {
            this.avgLogprobs = avgLogprobs;
        }
    }

    /**
     * Content in the response
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Content {
        @JsonProperty("parts")
        private List<Part> parts;

        @JsonProperty("role")
        private String role;

        public Content() {}

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
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Part {
        @JsonProperty("text")
        private String text;

        public Part() {}

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    /**
     * Safety rating information
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SafetyRating {
        @JsonProperty("category")
        private String category;

        @JsonProperty("probability")
        private String probability;

        public SafetyRating() {}

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getProbability() {
            return probability;
        }

        public void setProbability(String probability) {
            this.probability = probability;
        }
    }

    /**
     * Usage metadata
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UsageMetadata {
        @JsonProperty("promptTokenCount")
        private Integer promptTokenCount;

        @JsonProperty("candidatesTokenCount")
        private Integer candidatesTokenCount;

        @JsonProperty("totalTokenCount")
        private Integer totalTokenCount;

        @JsonProperty("promptTokensDetails")
        private Object promptTokensDetails; // Can be complex object, using Object for flexibility

        public UsageMetadata() {}

        public Integer getPromptTokenCount() {
            return promptTokenCount;
        }

        public void setPromptTokenCount(Integer promptTokenCount) {
            this.promptTokenCount = promptTokenCount;
        }

        public Integer getCandidatesTokenCount() {
            return candidatesTokenCount;
        }

        public void setCandidatesTokenCount(Integer candidatesTokenCount) {
            this.candidatesTokenCount = candidatesTokenCount;
        }

        public Integer getTotalTokenCount() {
            return totalTokenCount;
        }

        public void setTotalTokenCount(Integer totalTokenCount) {
            this.totalTokenCount = totalTokenCount;
        }

        public Object getPromptTokensDetails() {
            return promptTokensDetails;
        }

        public void setPromptTokensDetails(Object promptTokensDetails) {
            this.promptTokensDetails = promptTokensDetails;
        }
    }

    /**
     * Error information
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorInfo {
        @JsonProperty("code")
        private Integer code;

        @JsonProperty("message")
        private String message;

        @JsonProperty("status")
        private String status;

        public ErrorInfo() {}

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
