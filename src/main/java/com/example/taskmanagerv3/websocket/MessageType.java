package com.example.taskmanagerv3.websocket;

/**
 * Message types for WebSocket communication
 */
public enum MessageType {
    // Connection messages
    USER_CONNECT,
    CONNECTION_ACK,
    USER_DISCONNECT,
    
    // Chat messages
    CHAT_MESSAGE,
    FILE_MESSAGE,
    CHAT_WITH_FILES,
    
    // Typing indicators
    TYPING_START,
    TYPING_STOP,
    
    // Status messages
    USER_ONLINE,
    USER_OFFLINE,
    MESSAGE_READ,
    
    // System messages
    HEARTBEAT,
    ERROR
}
