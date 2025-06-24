package com.example.taskmanagerv3.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;

/**
 * Represents a client connection to the WebSocket server
 */
public class ClientConnection implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientConnection.class);

    private final Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean connected = true;
    
    // User information
    private Integer userId;
    private String username;
    private boolean isAdmin = false;
    private LocalDateTime connectedAt;

    public ClientConnection(Socket socket) {
        this.socket = socket;
        this.connectedAt = LocalDateTime.now();
        
        try {
            // Create streams
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            logger.debug("Client connection streams created successfully");
        } catch (IOException e) {
            logger.error("Failed to create client streams: {}", e.getMessage());
            close();
        }
    }

    @Override
    public void run() {
        try {
            while (connected && !socket.isClosed()) {
                try {
                    Object obj = in.readObject();
                    if (obj instanceof ChatMessage) {
                        handleMessage((ChatMessage) obj);
                    } else {
                        logger.warn("Received unknown object type: {}", obj.getClass().getSimpleName());
                    }
                } catch (ClassNotFoundException e) {
                    logger.error("Failed to deserialize message: {}", e.getMessage());
                }
            }
        } catch (IOException e) {
            if (connected) {
                logger.info("Client connection lost: {}", e.getMessage());
            }
        } finally {
            close();
        }
    }

    /**
     * Handle incoming message from client
     */
    private void handleMessage(ChatMessage message) {
        try {
            logger.debug("Received message type: {} from user: {}", message.getType(), userId);
            
            switch (message.getType()) {
                case USER_CONNECT:
                    handleUserConnect(message);
                    break;
                case CHAT_MESSAGE:
                    handleChatMessage(message);
                    break;
                case TYPING_START:
                    handleTypingStart(message);
                    break;
                case TYPING_STOP:
                    handleTypingStop(message);
                    break;
                case HEARTBEAT:
                    handleHeartbeat(message);
                    break;
                default:
                    logger.warn("Unknown message type: {}", message.getType());
            }
        } catch (Exception e) {
            logger.error("Error handling message: {}", e.getMessage());
        }
    }

    /**
     * Handle user connection
     */
    private void handleUserConnect(ChatMessage message) {
        try {
            UserConnectData data = (UserConnectData) message.getData();
            WebSocketServer.getInstance().registerUser(this, data.getUserId(), data.getUsername(), data.isAdmin());
            
            // Send connection acknowledgment
            ChatMessage ack = new ChatMessage(MessageType.CONNECTION_ACK, "Connected successfully");
            sendMessage(ack);
            
            logger.info("User {} connected successfully", data.getUsername());
        } catch (Exception e) {
            logger.error("Error handling user connect: {}", e.getMessage());
        }
    }

    /**
     * Handle chat message
     */
    private void handleChatMessage(ChatMessage message) {
        try {
            // Set sender information
            message.setSenderId(userId);
            message.setSenderName(username);
            message.setTimestamp(LocalDateTime.now());
            
            // Forward to recipient
            if (message.getReceiverId() != null) {
                WebSocketServer.getInstance().sendToUser(message.getReceiverId(), message);
                logger.debug("Chat message forwarded from {} to {}", userId, message.getReceiverId());
            } else {
                // Broadcast to all users (if no specific recipient)
                WebSocketServer.getInstance().broadcast(message);
                logger.debug("Chat message broadcasted from {}", userId);
            }
        } catch (Exception e) {
            logger.error("Error handling chat message: {}", e.getMessage());
        }
    }

    /**
     * Handle typing start
     */
    private void handleTypingStart(ChatMessage message) {
        message.setSenderId(userId);
        message.setSenderName(username);
        
        if (message.getReceiverId() != null) {
            WebSocketServer.getInstance().sendToUser(message.getReceiverId(), message);
        }
    }

    /**
     * Handle typing stop
     */
    private void handleTypingStop(ChatMessage message) {
        message.setSenderId(userId);
        message.setSenderName(username);
        
        if (message.getReceiverId() != null) {
            WebSocketServer.getInstance().sendToUser(message.getReceiverId(), message);
        }
    }

    /**
     * Handle heartbeat
     */
    private void handleHeartbeat(ChatMessage message) {
        ChatMessage response = new ChatMessage(MessageType.HEARTBEAT, "pong");
        sendMessage(response);
    }

    /**
     * Send message to this client
     */
    public void sendMessage(ChatMessage message) {
        if (!connected || out == null) {
            return;
        }
        
        try {
            out.writeObject(message);
            out.flush();
            logger.debug("Message sent to user {}: {}", userId, message.getType());
        } catch (IOException e) {
            logger.error("Failed to send message to user {}: {}", userId, e.getMessage());
            close();
        }
    }

    /**
     * Close connection
     */
    public void close() {
        if (!connected) {
            return;
        }
        
        connected = false;
        
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            logger.error("Error closing connection: {}", e.getMessage());
        }
        
        // Remove from server
        WebSocketServer.getInstance().removeConnection(this);
        logger.debug("Client connection closed for user: {}", userId);
    }

    /**
     * Set user information
     */
    public void setUserInfo(int userId, String username, boolean isAdmin) {
        this.userId = userId;
        this.username = username;
        this.isAdmin = isAdmin;
    }

    // Getters
    public boolean isConnected() {
        return connected && !socket.isClosed();
    }

    public Integer getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public LocalDateTime getConnectedAt() {
        return connectedAt;
    }
}
