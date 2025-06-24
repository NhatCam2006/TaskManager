package com.example.taskmanagerv3.model;

import java.time.LocalDateTime;

/**
 * Model for password reset requests
 */
public class PasswordResetRequest {
    private int requestId;
    private int userId;
    private String username;
    private String email;
    private String fullName;
    private String reason;
    private RequestStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
    private int processedBy;
    private String adminNotes;

    public enum RequestStatus {
        PENDING,
        APPROVED,
        REJECTED,
        COMPLETED
    }

    // Default constructor
    public PasswordResetRequest() {
        this.status = RequestStatus.PENDING;
        this.requestedAt = LocalDateTime.now();
    }

    // Constructor with required fields
    public PasswordResetRequest(int userId, String username, String email, String fullName, String reason) {
        this();
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.reason = reason;
    }

    // Getters and Setters
    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public int getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(int processedBy) {
        this.processedBy = processedBy;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    @Override
    public String toString() {
        return "PasswordResetRequest{" +
                "requestId=" + requestId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status +
                ", requestedAt=" + requestedAt +
                '}';
    }
}
