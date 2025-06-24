package com.example.taskmanagerv3.websocket;

/**
 * Interface for listening to WebSocket messages
 */
public interface WebSocketMessageListener {
    /**
     * Called when a message is received from the WebSocket
     */
    void onMessageReceived(ChatMessage message);
    
    /**
     * Called when connection is established
     */
    default void onConnected() {}
    
    /**
     * Called when connection is lost
     */
    default void onDisconnected() {}
    
    /**
     * Called when connection error occurs
     */
    default void onError(Exception error) {}
}
