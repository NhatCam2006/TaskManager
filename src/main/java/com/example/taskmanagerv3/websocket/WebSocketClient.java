package com.example.taskmanagerv3.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket client for connecting to chat server
 */
public class WebSocketClient {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketClient.class);
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9876;
    private static final int RECONNECT_DELAY_SECONDS = 5;
    private static final int HEARTBEAT_INTERVAL_SECONDS = 30;

    private static WebSocketClient instance;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean connected = false;
    private boolean shouldReconnect = true;

    private ExecutorService executorService;
    private ScheduledExecutorService scheduledExecutor;
    private CopyOnWriteArrayList<WebSocketMessageListener> listeners;

    // User information
    private Integer userId;
    private String username;
    private boolean isAdmin;

    private WebSocketClient() {
        listeners = new CopyOnWriteArrayList<>();
        executorService = Executors.newSingleThreadExecutor();
        scheduledExecutor = Executors.newScheduledThreadPool(2);
    }

    public static synchronized WebSocketClient getInstance() {
        if (instance == null) {
            instance = new WebSocketClient();
        }
        return instance;
    }

    /**
     * Connect to WebSocket server
     */
    public boolean connect(int userId, String username, boolean isAdmin) {
        this.userId = userId;
        this.username = username;
        this.isAdmin = isAdmin;
        this.shouldReconnect = true;

        return attemptConnection();
    }

    /**
     * Attempt to connect to server
     */
    private boolean attemptConnection() {
        try {
            if (connected) {
                logger.warn("Already connected to server");
                return true;
            }

            logger.info("Connecting to WebSocket server at {}:{}", SERVER_HOST, SERVER_PORT);
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;

            // Start message listener
            executorService.execute(this::listenForMessages);

            // Send connection message
            sendUserConnect();

            // Start heartbeat
            startHeartbeat();

            // Notify listeners
            notifyConnected();

            logger.info("Connected to WebSocket server successfully. User: {}", username);
            return true;

        } catch (IOException e) {
            logger.error("Failed to connect to server: {}", e.getMessage());
            notifyError(e);
            
            // Schedule reconnection if needed
            if (shouldReconnect) {
                scheduleReconnection();
            }
            return false;
        }
    }

    /**
     * Disconnect from server
     */
    public void disconnect() {
        shouldReconnect = false;
        closeConnection();
    }

    /**
     * Close connection
     */
    private void closeConnection() {
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

        // Stop heartbeat
        stopHeartbeat();

        // Notify listeners
        notifyDisconnected();

        logger.info("Disconnected from WebSocket server");
    }

    /**
     * Send user connection message
     */
    private void sendUserConnect() {
        UserConnectData connectData = new UserConnectData(userId, username, isAdmin);
        ChatMessage message = new ChatMessage(MessageType.USER_CONNECT, connectData);
        sendMessage(message);
    }

    /**
     * Send message to server
     */
    public void sendMessage(ChatMessage message) {
        if (!connected || out == null) {
            logger.warn("Cannot send message - not connected to server");
            return;
        }

        try {
            // Set sender info if not already set
            if (message.getSenderId() == null) {
                message.setSenderId(userId);
            }
            if (message.getSenderName() == null) {
                message.setSenderName(username);
            }

            out.writeObject(message);
            out.flush();
            logger.debug("Message sent: {}", message.getType());

        } catch (IOException e) {
            logger.error("Error sending message: {}", e.getMessage());
            handleConnectionLost();
        }
    }

    /**
     * Send chat message
     */
    public void sendChatMessage(String messageText, Integer receiverId) {
        ChatMessage message = new ChatMessage(MessageType.CHAT_MESSAGE, messageText, receiverId);
        sendMessage(message);
    }

    /**
     * Send typing notification
     */
    public void sendTypingNotification(Integer receiverId, boolean isTyping) {
        MessageType type = isTyping ? MessageType.TYPING_START : MessageType.TYPING_STOP;
        ChatMessage message = new ChatMessage(type, null, receiverId);
        sendMessage(message);
    }

    /**
     * Listen for incoming messages
     */
    private void listenForMessages() {
        try {
            while (connected) {
                try {
                    Object obj = in.readObject();
                    if (obj instanceof ChatMessage) {
                        ChatMessage message = (ChatMessage) obj;
                        notifyMessageReceived(message);
                    }
                } catch (ClassNotFoundException e) {
                    logger.error("Unknown object received: {}", e.getMessage());
                }
            }
        } catch (IOException e) {
            if (connected) {
                logger.error("Connection to server lost: {}", e.getMessage());
                handleConnectionLost();
            }
        }
    }

    /**
     * Handle connection lost
     */
    private void handleConnectionLost() {
        closeConnection();
        
        if (shouldReconnect) {
            scheduleReconnection();
        }
    }

    /**
     * Schedule reconnection attempt
     */
    private void scheduleReconnection() {
        logger.info("Scheduling reconnection in {} seconds", RECONNECT_DELAY_SECONDS);
        scheduledExecutor.schedule(() -> {
            if (shouldReconnect && !connected) {
                logger.info("Attempting to reconnect...");
                attemptConnection();
            }
        }, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Start heartbeat
     */
    private void startHeartbeat() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            if (connected) {
                ChatMessage heartbeat = new ChatMessage(MessageType.HEARTBEAT, "ping");
                sendMessage(heartbeat);
            }
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Stop heartbeat
     */
    private void stopHeartbeat() {
        // Heartbeat will stop automatically when scheduledExecutor is shutdown
    }

    // Listener management
    public void addMessageListener(WebSocketMessageListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeMessageListener(WebSocketMessageListener listener) {
        listeners.remove(listener);
    }

    private void notifyMessageReceived(ChatMessage message) {
        for (WebSocketMessageListener listener : listeners) {
            try {
                listener.onMessageReceived(message);
            } catch (Exception e) {
                logger.error("Error notifying listener: {}", e.getMessage());
            }
        }
    }

    private void notifyConnected() {
        for (WebSocketMessageListener listener : listeners) {
            try {
                listener.onConnected();
            } catch (Exception e) {
                logger.error("Error notifying connection: {}", e.getMessage());
            }
        }
    }

    private void notifyDisconnected() {
        for (WebSocketMessageListener listener : listeners) {
            try {
                listener.onDisconnected();
            } catch (Exception e) {
                logger.error("Error notifying disconnection: {}", e.getMessage());
            }
        }
    }

    private void notifyError(Exception error) {
        for (WebSocketMessageListener listener : listeners) {
            try {
                listener.onError(error);
            } catch (Exception e) {
                logger.error("Error notifying error: {}", e.getMessage());
            }
        }
    }

    // Getters
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public Integer getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        shouldReconnect = false;
        closeConnection();
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        if (scheduledExecutor != null && !scheduledExecutor.isShutdown()) {
            scheduledExecutor.shutdown();
        }
    }
}
