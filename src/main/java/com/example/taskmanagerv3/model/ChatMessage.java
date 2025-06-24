package com.example.taskmanagerv3.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Model class for chat messages
 */
public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int messageId;
    private int senderId;
    private String senderName;
    private UserRole senderRole;
    private int receiverId;
    private String receiverName;
    private String message;
    private LocalDateTime timestamp;
    private boolean isRead;
    private boolean hasAttachments;
    private List<ChatFile> attachments;
    
    // Constructors
    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
        this.isRead = false;
        this.hasAttachments = false;
        this.attachments = new ArrayList<>();
    }
    
    public ChatMessage(int senderId, String senderName, int receiverId, String message) {
        this();
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.message = message;
    }
    
    // Getters and Setters
    public int getMessageId() {
        return messageId;
    }
    
    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }
    
    public int getSenderId() {
        return senderId;
    }
    
    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public UserRole getSenderRole() {
        return senderRole;
    }
    
    public void setSenderRole(UserRole senderRole) {
        this.senderRole = senderRole;
    }
    
    public int getReceiverId() {
        return receiverId;
    }
    
    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }
    
    public String getReceiverName() {
        return receiverName;
    }
    
    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isRead() {
        return isRead;
    }
    
    public void setRead(boolean read) {
        isRead = read;
    }
    
    public boolean isHasAttachments() {
        return hasAttachments;
    }
    
    public void setHasAttachments(boolean hasAttachments) {
        this.hasAttachments = hasAttachments;
    }
    
    public List<ChatFile> getAttachments() {
        return attachments;
    }
    
    public void setAttachments(List<ChatFile> attachments) {
        this.attachments = attachments;
        this.hasAttachments = attachments != null && !attachments.isEmpty();
    }
    
    public void addAttachment(ChatFile file) {
        if (attachments == null) {
            attachments = new ArrayList<>();
        }
        attachments.add(file);
        this.hasAttachments = true;
    }
    
    // Utility methods
    public String getFormattedTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        return timestamp.format(formatter);
    }
    
    public String getTimeOnly() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return timestamp.format(formatter);
    }
    
    public boolean isFromCurrentUser(int currentUserId) {
        return senderId == currentUserId;
    }
    
    public boolean isFromAdmin() {
        return senderRole == UserRole.ADMIN;
    }
    
    public boolean isToAdmin(int adminUserId) {
        return receiverId == adminUserId;
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s (%s) -> %s: %s", 
                           getFormattedTimestamp(), senderName, senderRole, 
                           receiverName, message);
    }
}
