package com.example.taskmanagerv3.websocket;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WebSocket chat message for real-time communication
 */
public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String messageId;
    private MessageType type;
    private Object data;
    private LocalDateTime timestamp;
    private Integer senderId;
    private String senderName;
    private Integer receiverId;

    public ChatMessage() {
        this.messageId = generateMessageId();
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(MessageType type, Object data) {
        this();
        this.type = type;
        this.data = data;
    }

    public ChatMessage(MessageType type, Object data, Integer receiverId) {
        this(type, data);
        this.receiverId = receiverId;
    }

    private String generateMessageId() {
        return "msg_" + UUID.randomUUID().toString().substring(0, 8);
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getSenderId() {
        return senderId;
    }

    public void setSenderId(Integer senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public Integer getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Integer receiverId) {
        this.receiverId = receiverId;
    }

    @Override
    public String toString() {
        return String.format("ChatMessage{id='%s', type=%s, senderId=%d, receiverId=%d, timestamp=%s}",
                messageId, type, senderId, receiverId, timestamp);
    }
}
