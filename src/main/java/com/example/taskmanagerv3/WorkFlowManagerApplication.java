package com.example.taskmanagerv3;

import com.example.taskmanagerv3.config.DatabaseConfig;
import com.example.taskmanagerv3.websocket.WebSocketServer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class WorkFlowManagerApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(WorkFlowManagerApplication.class);

    @Override
    public void start(Stage primaryStage) throws IOException {
        try {
            // Initialize database
            initializeDatabase();

            // Load login screen
            FXMLLoader fxmlLoader = new FXMLLoader(WorkFlowManagerApplication.class.getResource("login-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());

            // Set up primary stage
            primaryStage.setTitle("WorkFlow Manager - Login");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.centerOnScreen();

            // Handle application close
            primaryStage.setOnCloseRequest(event -> {
                DatabaseConfig.closeConnection();
                Platform.exit();
            });

            primaryStage.show();
            logger.info("WorkFlow Manager application started successfully");

        } catch (Exception e) {
            logger.error("Failed to start application", e);
            showErrorAlert("Application Error", "Failed to start WorkFlow Manager: " + e.getMessage());
            Platform.exit();
        }
    }

    /**
     * Initialize database connection and schema
     */
    private void initializeDatabase() {
        try {
            // Test database connection
            if (!DatabaseConfig.testConnection()) {
                throw new RuntimeException("Cannot connect to database");
            }

            // Initialize database schema
            DatabaseConfig.initializeDatabase();

            // Start WebSocket server
            startWebSocketServer();

            // Create default admin user
            DatabaseConfig.createDefaultAdmin();



            logger.info("Database initialized successfully");

        } catch (Exception e) {
            logger.error("Database initialization failed", e);
            showErrorAlert("Database Error",
                "Failed to initialize database. Please check your SQL Server connection.\n\n" +
                "Error: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Start WebSocket server for chat functionality
     */
    private void startWebSocketServer() {
        try {
            WebSocketServer server = WebSocketServer.getInstance();
            boolean started = server.start();
            if (started) {
                logger.info("WebSocket server started successfully on port 9876");
            } else {
                logger.error("Failed to start WebSocket server");
            }
        } catch (Exception e) {
            logger.error("Error starting WebSocket server", e);
            // Don't exit application if WebSocket server fails to start
        }
    }

    /**
     * Show error alert dialog
     */
    private void showErrorAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static void main(String[] args) {
        // Disable Log4j2 warning messages
        System.setProperty("log4j2.loggerContextFactory", "org.apache.logging.log4j.simple.SimpleLoggerContextFactory");
        System.setProperty("org.apache.logging.log4j.simplelog.StatusLogger.level", "OFF");

        launch();
    }
}
