package com.example.taskmanagerv3.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple WebSocket-like server for real-time chat communication
 * Redesigned from scratch for better reliability and performance
 */
public class WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketServer.class);
    private static final int PORT = 9876;
    private static WebSocketServer instance;

    private ServerSocket serverSocket;
    private boolean running = false;
    private ExecutorService executorService;
    private CopyOnWriteArrayList<ClientConnection> connections;
    private ConcurrentHashMap<Integer, ClientConnection> userConnections; // userId -> connection

    private WebSocketServer() {
        connections = new CopyOnWriteArrayList<>();
        userConnections = new ConcurrentHashMap<>();
        executorService = Executors.newCachedThreadPool();
    }

    public static synchronized WebSocketServer getInstance() {
        if (instance == null) {
            instance = new WebSocketServer();
        }
        return instance;
    }

    /**
     * Start the WebSocket server
     */
    public synchronized boolean start() {
        if (running) {
            logger.warn("WebSocket server is already running");
            return true;
        }

        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            logger.info("WebSocket server started on port {}", PORT);

            // Accept connections in background thread
            executorService.execute(this::acceptConnections);
            return true;

        } catch (IOException e) {
            logger.error("Failed to start WebSocket server: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Stop the WebSocket server
     */
    public synchronized void stop() {
        if (!running) {
            return;
        }

        running = false;
        logger.info("Stopping WebSocket server...");

        // Close all client connections
        for (ClientConnection connection : connections) {
            connection.close();
        }
        connections.clear();
        userConnections.clear();

        // Close server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.error("Error closing server socket: {}", e.getMessage());
        }

        // Shutdown executor
        executorService.shutdown();
        logger.info("WebSocket server stopped");
    }

    /**
     * Accept incoming connections
     */
    private void acceptConnections() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                logger.info("New client connected from: {}", clientSocket.getInetAddress().getHostAddress());

                ClientConnection connection = new ClientConnection(clientSocket);
                connections.add(connection);
                executorService.execute(connection);

            } catch (IOException e) {
                if (running) {
                    logger.error("Error accepting client connection: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Send message to specific user
     */
    public void sendToUser(int userId, ChatMessage message) {
        ClientConnection connection = userConnections.get(userId);
        if (connection != null && connection.isConnected()) {
            connection.sendMessage(message);
            logger.debug("Message sent to user {}: {}", userId, message.getType());
        } else {
            logger.warn("User {} not connected or connection lost", userId);
        }
    }

    /**
     * Broadcast message to all connected users
     */
    public void broadcast(ChatMessage message) {
        int sentCount = 0;
        for (ClientConnection connection : connections) {
            if (connection.isConnected()) {
                connection.sendMessage(message);
                sentCount++;
            }
        }
        logger.debug("Message broadcasted to {} users: {}", sentCount, message.getType());
    }

    /**
     * Send message to all admins
     */
    public void sendToAdmins(ChatMessage message) {
        int sentCount = 0;
        for (ClientConnection connection : connections) {
            if (connection.isConnected() && connection.isAdmin()) {
                connection.sendMessage(message);
                sentCount++;
            }
        }
        logger.debug("Message sent to {} admins: {}", sentCount, message.getType());
    }

    /**
     * Get number of connected users
     */
    public int getConnectedUsersCount() {
        return userConnections.size();
    }

    /**
     * Check if user is online
     */
    public boolean isUserOnline(int userId) {
        ClientConnection connection = userConnections.get(userId);
        return connection != null && connection.isConnected();
    }

    /**
     * Check if server is running
     */
    public boolean isRunning() {
        return running && serverSocket != null && !serverSocket.isClosed();
    }

    /**
     * Remove disconnected connection
     */
    void removeConnection(ClientConnection connection) {
        connections.remove(connection);
        if (connection.getUserId() != null) {
            userConnections.remove(connection.getUserId());
            logger.info("User {} disconnected", connection.getUserId());
        }
    }

    /**
     * Register user connection
     */
    void registerUser(ClientConnection connection, int userId, String username, boolean isAdmin) {
        connection.setUserInfo(userId, username, isAdmin);
        userConnections.put(userId, connection);
        logger.info("User {} ({}) registered as {}", username, userId, isAdmin ? "admin" : "user");
    }
}
