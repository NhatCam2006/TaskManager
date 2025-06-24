package com.example.taskmanagerv3.controller;

import com.example.taskmanagerv3.model.ChatFile;
import com.example.taskmanagerv3.model.ChatMessage;
import com.example.taskmanagerv3.model.User;
import com.example.taskmanagerv3.service.ChatService;
import com.example.taskmanagerv3.websocket.WebSocketClient;
import com.example.taskmanagerv3.websocket.WebSocketMessageListener;
import com.example.taskmanagerv3.websocket.MessageType;
import com.example.taskmanagerv3.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for chat functionality
 */
public class ChatController implements WebSocketMessageListener {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    // FXML Components
    @FXML private Label connectionStatusLabel;
    @FXML private Label onlineStatusLabel;
    @FXML private Label lastSeenLabel;
    @FXML private Label typingLabel;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private VBox messagesContainer;
    @FXML private HBox typingIndicator;
    @FXML private HBox attachmentsPreview;
    @FXML private HBox attachmentsContainer;
    @FXML private TextArea messageTextArea;
    @FXML private Button sendButton;
    @FXML private Button attachFileButton;
    @FXML private Button clearAttachmentsButton;
    @FXML private Button refreshButton;
    @FXML private Button helpButton;
    @FXML private Button bugReportButton;
    @FXML private Button featureRequestButton;

    // Services and utilities
    private ChatService chatService;
    private WebSocketClient webSocketClient;

    private SessionManager sessionManager;
    private User currentUser;
    private List<User> adminUsers;
    private int currentAdminId = -1; // Default admin to chat with
    private List<File> selectedFiles;
    private boolean isTyping = false;

    @FXML
    private void initialize() {
        chatService = new ChatService();
        webSocketClient = WebSocketClient.getInstance();

        sessionManager = SessionManager.getInstance();
        selectedFiles = new ArrayList<>();

        // Fix database attachments (one-time fix)
        chatService.fixDatabaseAttachments();

        setupUI();
        loadCurrentUser();
        loadAdminUsers();
        connectToWebSocket();

        loadChatHistory();
    }

    /**
     * Setup UI components and event handlers
     */
    private void setupUI() {
        // Setup message input
        messageTextArea.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER:
                    if (event.isControlDown()) {
                        sendMessage();
                        event.consume();
                    }
                    break;
                case ESCAPE:
                    messageTextArea.clear();
                    event.consume();
                    break;
            }
        });



        // Setup buttons
        sendButton.setOnAction(e -> sendMessage());
        attachFileButton.setOnAction(e -> attachFile());
        clearAttachmentsButton.setOnAction(e -> clearAttachments());
        refreshButton.setOnAction(e -> refreshChat());

        // Setup quick action buttons
        helpButton.setOnAction(e -> sendQuickMessage("❓ I need help with the task manager."));
        bugReportButton.setOnAction(e -> sendQuickMessage("🐛 I found a bug that needs to be reported."));
        featureRequestButton.setOnAction(e -> sendQuickMessage("💡 I have a feature request to discuss."));

        // Auto-scroll to bottom
        messagesContainer.heightProperty().addListener((obs, oldHeight, newHeight) -> {
            Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
        });
    }

    /**
     * Load current user from session
     */
    private void loadCurrentUser() {
        try {
            currentUser = sessionManager.getCurrentUser();
            if (currentUser == null) {
                logger.error("No current user found in session");
                showAlert("Error", "User session not found. Please login again.");
                return;
            }
            logger.info("Chat initialized for user: {}", currentUser.getUsername());
        } catch (Exception e) {
            logger.error("Error loading current user", e);
        }
    }

    /**
     * Load admin users for chat
     */
    private void loadAdminUsers() {
        try {
            adminUsers = chatService.getAdminUsers();
            if (!adminUsers.isEmpty()) {
                currentAdminId = adminUsers.get(0).getUserId(); // Default to first admin
                logger.info("Found {} admin users, default admin: {}", adminUsers.size(), currentAdminId);
            } else {
                logger.warn("No admin users found");
                showAlert("Warning", "No admin users available for chat.");
            }
        } catch (Exception e) {
            logger.error("Error loading admin users", e);
        }
    }



    /**
     * Load chat history
     */
    private void loadChatHistory() {
        if (currentUser == null || currentAdminId == -1) {
            return;
        }

        try {
            logger.info("=== LOADING CHAT HISTORY ===");
            logger.info("Current user ID: {}, Admin ID: {}", currentUser.getUserId(), currentAdminId);

            List<ChatMessage> messages = chatService.getChatMessages(currentUser.getUserId(), currentAdminId, 50);

            logger.info("Retrieved {} messages from database", messages.size());

            Platform.runLater(() -> {
                messagesContainer.getChildren().clear();
                logger.info("Cleared messages container, adding {} messages to UI", messages.size());

                // Add welcome message if no history
                if (messages.isEmpty()) {
                    Label welcomeLabel = new Label("Welcome! You can chat with admin here. Send a message to get started.");
                    welcomeLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-style: italic; -fx-padding: 20; -fx-alignment: center;");
                    messagesContainer.getChildren().add(welcomeLabel);
                    logger.info("Added welcome message - no chat history found");
                } else {
                    for (int i = 0; i < messages.size(); i++) {
                        ChatMessage message = messages.get(i);
                        logger.info("Adding message {} of {}: ID={}, Sender={}, Text='{}'",
                                   i + 1, messages.size(), message.getMessageId(), message.getSenderId(),
                                   message.getMessage().length() > 50 ? message.getMessage().substring(0, 50) + "..." : message.getMessage());
                        addMessageToUI(message);
                    }
                }

                logger.info("Finished adding all messages to UI. Total UI children: {}", messagesContainer.getChildren().size());

                // Auto-scroll to bottom to show latest messages
                Platform.runLater(() -> {
                    messagesScrollPane.setVvalue(1.0);
                });

                // Mark messages as read
                chatService.markMessagesAsRead(currentAdminId, currentUser.getUserId());
            });

        } catch (Exception e) {
            logger.error("Error loading chat history", e);
        }
    }

    /**
     * Send message
     */
    private void sendMessage() {
        logger.info("=== SEND MESSAGE CALLED ===");
        String messageText = messageTextArea.getText().trim();
        logger.info("Message text: '{}'", messageText);
        logger.info("Selected files count: {}", selectedFiles.size());
        logger.info("Current user: {}", currentUser != null ? currentUser.getUsername() : "null");
        logger.info("Current admin ID: {}", currentAdminId);

        if (messageText.isEmpty() && selectedFiles.isEmpty()) {
            logger.warn("Message text is empty and no files selected - returning");
            return;
        }

        if (currentUser == null) {
            logger.error("Current user is null");
            showAlert("Error", "User not logged in.");
            return;
        }

        if (currentAdminId == -1 || currentAdminId == 0) {
            logger.error("Invalid admin ID: {}", currentAdminId);
            showAlert("Error", "Cannot send message. No admin available. Admin ID: " + currentAdminId);
            return;
        }

        try {
            // Determine the actual message content
            String actualMessageText = messageText;

            // If only files are being sent without text, create a descriptive message
            if (messageText.isEmpty() && !selectedFiles.isEmpty()) {
                if (selectedFiles.size() == 1) {
                    actualMessageText = "📎 Sent a file: " + selectedFiles.get(0).getName();
                } else {
                    actualMessageText = "📎 Sent " + selectedFiles.size() + " files";
                }
                logger.info("Generated message text for file-only message: '{}'", actualMessageText);
            }

            // Create chat message
            ChatMessage chatMessage = new ChatMessage(
                currentUser.getUserId(),
                currentUser.getFullName(),
                currentAdminId,
                actualMessageText
            );
            chatMessage.setSenderRole(currentUser.getRole());

            // Set receiver name (find admin name)
            String adminName = "Admin";
            if (!adminUsers.isEmpty()) {
                adminName = adminUsers.get(0).getFullName();
                if (adminName == null || adminName.trim().isEmpty()) {
                    adminName = adminUsers.get(0).getUsername();
                }
            }
            chatMessage.setReceiverName(adminName);

            logger.info("Creating message: Sender={} ({}), Receiver={} ({}), Text='{}'",
                       currentUser.getFullName(), currentUser.getUserId(),
                       adminName, currentAdminId, actualMessageText);

            // Save to database
            ChatMessage savedMessage = chatService.saveChatMessage(chatMessage);
            if (savedMessage != null) {
                // Handle file attachments
                if (!selectedFiles.isEmpty()) {
                    for (File file : selectedFiles) {
                        try {
                            byte[] fileData = Files.readAllBytes(file.toPath());
                            String fileType = Files.probeContentType(file.toPath());

                            ChatFile chatFile = chatService.saveChatFile(
                                savedMessage.getMessageId(),
                                file.getName(),
                                fileData,
                                fileType,
                                currentUser.getUserId()
                            );

                            if (chatFile != null) {
                                savedMessage.addAttachment(chatFile);
                            }
                        } catch (IOException e) {
                            logger.error("Error processing file: {}", file.getName(), e);
                        }
                    }
                }

                // Update hasAttachments flag in database if files were attached
                if (!selectedFiles.isEmpty()) {
                    savedMessage.setHasAttachments(true);
                    chatService.updateMessageAttachments(savedMessage.getMessageId(), true);
                    logger.info("Updated database: hasAttachments = true for message {} with {} files",
                               savedMessage.getMessageId(), selectedFiles.size());
                }

                logger.info("=== SENDING MESSAGE ===");
                logger.info("Message ID: {}, Sender: {} (ID: {}), Text: '{}'",
                           savedMessage.getMessageId(), savedMessage.getSenderName(),
                           savedMessage.getSenderId(), savedMessage.getMessage());

                // Add to UI immediately (since we won't receive our own message back)
                Platform.runLater(() -> {
                    logger.info("Adding message to UI locally - Message ID: {}", savedMessage.getMessageId());
                    addMessageToUI(savedMessage);
                    messageTextArea.clear();
                    clearAttachments();
                });

                // Send via WebSocket - Only send notification with the actual message content
                // This prevents sending empty messages when only files are attached
                logger.info("Notifying admin about new message via WebSocket with text: '{}'", actualMessageText);
                webSocketClient.sendChatMessage(actualMessageText, currentAdminId);

                logger.info("Message sent successfully");
            }

        } catch (Exception e) {
            logger.error("Error sending message", e);
            showAlert("Error", "Failed to send message: " + e.getMessage());
        }
    }

    /**
     * Send quick message
     */
    private void sendQuickMessage(String message) {
        messageTextArea.setText(message);
        sendMessage();
    }

    /**
     * Attach file
     */
    private void attachFile() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select File to Attach");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Files", "*.*"),
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx", "*.txt"),
                new FileChooser.ExtensionFilter("Archives", "*.zip", "*.rar", "*.7z")
            );

            Stage stage = (Stage) attachFileButton.getScene().getWindow();
            List<File> files = fileChooser.showOpenMultipleDialog(stage);

            if (files != null && !files.isEmpty()) {
                selectedFiles.addAll(files);
                updateAttachmentsPreview();
            }

        } catch (Exception e) {
            logger.error("Error attaching file", e);
            showAlert("Error", "Failed to attach file: " + e.getMessage());
        }
    }

    /**
     * Update attachments preview
     */
    private void updateAttachmentsPreview() {
        Platform.runLater(() -> {
            attachmentsContainer.getChildren().clear();

            for (File file : selectedFiles) {
                VBox filePreview = createFilePreview(file);
                attachmentsContainer.getChildren().add(filePreview);
            }

            attachmentsPreview.setVisible(!selectedFiles.isEmpty());
            attachmentsPreview.setManaged(!selectedFiles.isEmpty());
        });
    }

    /**
     * Create file preview for attachment selection
     */
    private VBox createFilePreview(File file) {
        VBox previewBox = new VBox(5);
        previewBox.setStyle("-fx-background-color: #e9ecef; -fx-padding: 8; -fx-background-radius: 5; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-border-radius: 5;");
        previewBox.setAlignment(Pos.CENTER);
        previewBox.setPrefWidth(120);
        previewBox.setMaxWidth(120);

        try {
            // Check if it's an image file
            String fileName = file.getName().toLowerCase();
            boolean isImage = fileName.endsWith(".png") || fileName.endsWith(".jpg") ||
                            fileName.endsWith(".jpeg") || fileName.endsWith(".gif") ||
                            fileName.endsWith(".bmp") || fileName.endsWith(".webp");

            if (isImage) {
                // Create image preview
                try {
                    javafx.scene.image.Image image = new javafx.scene.image.Image(file.toURI().toString());
                    if (!image.isError()) {
                        javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);
                        imageView.setFitWidth(80);
                        imageView.setFitHeight(60);
                        imageView.setPreserveRatio(true);
                        imageView.setSmooth(true);
                        imageView.setStyle("-fx-background-radius: 3; -fx-border-radius: 3;");

                        previewBox.getChildren().add(imageView);
                    } else {
                        // Fallback to icon if image loading fails
                        Label iconLabel = new Label("🖼️");
                        iconLabel.setStyle("-fx-font-size: 24px;");
                        previewBox.getChildren().add(iconLabel);
                    }
                } catch (Exception e) {
                    logger.warn("Could not create image preview for: {}", file.getName());
                    Label iconLabel = new Label("🖼️");
                    iconLabel.setStyle("-fx-font-size: 24px;");
                    previewBox.getChildren().add(iconLabel);
                }
            } else {
                // Non-image file - show appropriate icon
                String icon = "📄"; // Default document icon
                if (fileName.endsWith(".pdf")) icon = "📄";
                else if (fileName.endsWith(".zip") || fileName.endsWith(".rar") || fileName.endsWith(".7z")) icon = "📦";
                else if (fileName.endsWith(".txt")) icon = "📝";
                else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) icon = "📘";
                else if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) icon = "📊";
                else if (fileName.endsWith(".ppt") || fileName.endsWith(".pptx")) icon = "📈";
                else icon = "📎";

                Label iconLabel = new Label(icon);
                iconLabel.setStyle("-fx-font-size: 24px;");
                previewBox.getChildren().add(iconLabel);
            }

        } catch (Exception e) {
            logger.error("Error creating file preview", e);
            Label iconLabel = new Label("📎");
            iconLabel.setStyle("-fx-font-size: 24px;");
            previewBox.getChildren().add(iconLabel);
        }

        // File name (truncated if too long)
        String displayName = file.getName();
        if (displayName.length() > 15) {
            displayName = displayName.substring(0, 12) + "...";
        }

        Label nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #495057; -fx-text-alignment: center;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(110);

        // File size
        long fileSize = file.length();
        String sizeText;
        if (fileSize < 1024) {
            sizeText = fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            sizeText = String.format("%.1f KB", fileSize / 1024.0);
        } else {
            sizeText = String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }

        Label sizeLabel = new Label(sizeText);
        sizeLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #6c757d;");

        previewBox.getChildren().addAll(nameLabel, sizeLabel);

        // Remove button
        Button removeButton = new Button("×");
        removeButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 2 6 2 6; -fx-background-radius: 10;");
        removeButton.setOnAction(e -> {
            selectedFiles.remove(file);
            updateAttachmentsPreview();
        });

        // Position remove button at top-right
        HBox topBox = new HBox();
        topBox.setAlignment(Pos.TOP_RIGHT);
        topBox.getChildren().add(removeButton);

        VBox containerWithRemove = new VBox();
        containerWithRemove.getChildren().addAll(topBox, previewBox);

        return containerWithRemove;
    }

    /**
     * Clear attachments
     */
    private void clearAttachments() {
        selectedFiles.clear();
        Platform.runLater(() -> {
            attachmentsContainer.getChildren().clear();
            attachmentsPreview.setVisible(false);
            attachmentsPreview.setManaged(false);
        });
    }

    /**
     * Refresh chat
     */
    private void refreshChat() {
        loadChatHistory();
    }



    /**
     * Add message to UI
     */
    private void addMessageToUI(ChatMessage message) {
        try {
            VBox messageBox = new VBox(5);
            messageBox.setPadding(new Insets(10));

            // Determine if message is from current user
            boolean isFromCurrentUser = message.getSenderId() == currentUser.getUserId();

            // Debug logging
            logger.debug("Adding message - Sender ID: {}, Current User ID: {}, Is from current user: {}",
                        message.getSenderId(), currentUser.getUserId(), isFromCurrentUser);

            // Message header
            HBox headerBox = new HBox(10);
            headerBox.setAlignment(isFromCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            Label senderLabel = new Label(message.getSenderName());
            senderLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

            Label timeLabel = new Label(message.getTimeOnly());
            timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");

            if (isFromCurrentUser) {
                headerBox.getChildren().addAll(timeLabel, senderLabel);
                senderLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #007bff;");
            } else {
                headerBox.getChildren().addAll(senderLabel, timeLabel);
                senderLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #28a745;");
            }

            // Message content
            Label messageLabel = new Label(message.getMessage());
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(400);

            // Message bubble styling
            String bubbleStyle;
            if (isFromCurrentUser) {
                bubbleStyle = "-fx-background-color: #007bff; -fx-text-fill: white; -fx-background-radius: 15 15 5 15; -fx-padding: 10;";
                messageBox.setAlignment(Pos.CENTER_RIGHT);
            } else {
                bubbleStyle = "-fx-background-color: #e9ecef; -fx-text-fill: black; -fx-background-radius: 15 15 15 5; -fx-padding: 10;";
                messageBox.setAlignment(Pos.CENTER_LEFT);
            }
            messageLabel.setStyle(bubbleStyle);

            messageBox.getChildren().addAll(headerBox, messageLabel);

            // Add file attachments if any
            logger.info("Message has attachments: {}, attachments list: {}",
                       message.isHasAttachments(),
                       message.getAttachments() != null ? message.getAttachments().size() : "null");

            if (message.isHasAttachments() && message.getAttachments() != null) {
                logger.info("Adding {} file attachments to UI", message.getAttachments().size());
                for (int i = 0; i < message.getAttachments().size(); i++) {
                    ChatFile file = message.getAttachments().get(i);
                    logger.info("Processing attachment {}: {}", i + 1, file.getOriginalFileName());

                    VBox fileBox = createFileAttachmentUI(file);
                    if (fileBox != null) {
                        logger.info("Adding file box to message container");
                        messageBox.getChildren().add(fileBox);
                        logger.info("File box added successfully. MessageBox children count: {}", messageBox.getChildren().size());
                    } else {
                        logger.error("File box is null for file: {}", file.getOriginalFileName());
                    }
                }
                logger.info("Finished adding all file attachments. Total messageBox children: {}", messageBox.getChildren().size());
            } else {
                logger.info("No file attachments to display - hasAttachments: {}, attachments: {}",
                           message.isHasAttachments(), message.getAttachments());
            }

            messagesContainer.getChildren().add(messageBox);

            // Auto-scroll to bottom to show the new message
            Platform.runLater(() -> {
                messagesScrollPane.setVvalue(1.0);
            });

        } catch (Exception e) {
            logger.error("Error adding message to UI", e);
        }
    }

    /**
     * Create file attachment UI
     */
    private VBox createFileAttachmentUI(ChatFile file) {
        try {
            logger.info("=== CREATING FILE ATTACHMENT UI ===");
            logger.info("File: {}", file);
            logger.info("Original filename: {}", file.getOriginalFileName());
            logger.info("File type: {}", file.getFileType());
            logger.info("File size: {}", file.getFileSize());
            logger.info("File path: {}", file.getFilePath());

            VBox fileContainer = new VBox(5);
            fileContainer.setAlignment(Pos.CENTER_LEFT);

            // Check if this is an image file
            if (file.isImage()) {
                logger.info("Creating image preview for: {}", file.getOriginalFileName());
                VBox imageBox = createImagePreview(file);
                if (imageBox != null) {
                    return imageBox;
                }
                // If image preview fails, fall back to regular file display
            }

            // Regular file attachment UI
            HBox fileBox = new HBox(10);
            fileBox.setAlignment(Pos.CENTER_LEFT);
            fileBox.setStyle("-fx-background-color: #e3f2fd; -fx-padding: 10; -fx-background-radius: 8; -fx-border-color: #2196f3; -fx-border-width: 1; -fx-border-radius: 8;");
            fileBox.setPrefHeight(60);
            fileBox.setMinHeight(60);

            // File icon
            String icon;
            try {
                icon = file.isImage() ? "🖼️" : (file.isDocument() ? "📄" : "📎");
                logger.info("File icon: {}", icon);
            } catch (Exception e) {
                logger.error("Error determining file icon", e);
                icon = "📎";
            }

            Label iconLabel = new Label(icon);
            iconLabel.setStyle("-fx-font-size: 20px;");
            iconLabel.setPrefWidth(30);

            // File info
            VBox fileInfo = new VBox(2);

            String fileName = file.getOriginalFileName() != null ? file.getOriginalFileName() : "Unknown file";
            Label nameLabel = new Label(fileName);
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #1976d2;");

            String fileSize;
            try {
                fileSize = file.getFormattedFileSize();
                logger.info("Formatted file size: {}", fileSize);
            } catch (Exception e) {
                logger.error("Error formatting file size", e);
                fileSize = file.getFileSize() + " bytes";
            }

            Label sizeLabel = new Label(fileSize);
            sizeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

            fileInfo.getChildren().addAll(nameLabel, sizeLabel);

            // Download button
            Button downloadButton = new Button("Download");
            downloadButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 10px;");
            downloadButton.setPrefWidth(80);
            downloadButton.setOnAction(e -> {
                logger.info("Download button clicked for file: {}", file.getOriginalFileName());
                downloadFile(file);
            });

            fileBox.getChildren().addAll(iconLabel, fileInfo, downloadButton);
            fileContainer.getChildren().add(fileBox);

            logger.info("File attachment UI created successfully");
            return fileContainer;

        } catch (Exception e) {
            logger.error("ERROR creating file attachment UI", e);

            // Return a simple error box
            VBox errorContainer = new VBox();
            HBox errorBox = new HBox();
            errorBox.setStyle("-fx-background-color: #ffebee; -fx-padding: 10; -fx-background-radius: 5;");
            Label errorLabel = new Label("❌ Error displaying file: " + (file != null ? file.getOriginalFileName() : "null"));
            errorLabel.setStyle("-fx-text-fill: #d32f2f;");
            errorBox.getChildren().add(errorLabel);
            errorContainer.getChildren().add(errorBox);
            return errorContainer;
        }
    }

    /**
     * Create image preview UI
     */
    private VBox createImagePreview(ChatFile file) {
        try {
            logger.info("=== CREATING IMAGE PREVIEW ===");

            // Load image data
            byte[] imageData = chatService.getFileData(file.getFilePath());
            if (imageData == null) {
                logger.error("Could not load image data for: {}", file.getOriginalFileName());
                return null;
            }

            // Create image from byte array
            javafx.scene.image.Image image = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(imageData));
            if (image.isError()) {
                logger.error("Error loading image: {}", file.getOriginalFileName());
                return null;
            }

            VBox imageContainer = new VBox(5);
            imageContainer.setAlignment(Pos.CENTER_LEFT);
            imageContainer.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 10; -fx-background-radius: 8; -fx-border-color: #ddd; -fx-border-width: 1; -fx-border-radius: 8;");

            // Image info header
            HBox headerBox = new HBox(10);
            headerBox.setAlignment(Pos.CENTER_LEFT);

            Label iconLabel = new Label("🖼️");
            iconLabel.setStyle("-fx-font-size: 16px;");

            VBox fileInfo = new VBox(2);
            Label nameLabel = new Label(file.getOriginalFileName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #1976d2;");

            Label sizeLabel = new Label(file.getFormattedFileSize());
            sizeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

            fileInfo.getChildren().addAll(nameLabel, sizeLabel);

            Button downloadButton = new Button("Download");
            downloadButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 10px;");
            downloadButton.setPrefWidth(80);
            downloadButton.setOnAction(e -> downloadFile(file));

            headerBox.getChildren().addAll(iconLabel, fileInfo, downloadButton);

            // Image preview
            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);

            // Calculate appropriate size (max 300px width, maintain aspect ratio)
            double maxWidth = 300;
            double maxHeight = 200;

            double imageWidth = image.getWidth();
            double imageHeight = image.getHeight();

            if (imageWidth > maxWidth || imageHeight > maxHeight) {
                double widthRatio = maxWidth / imageWidth;
                double heightRatio = maxHeight / imageHeight;
                double ratio = Math.min(widthRatio, heightRatio);

                imageView.setFitWidth(imageWidth * ratio);
                imageView.setFitHeight(imageHeight * ratio);
            } else {
                imageView.setFitWidth(imageWidth);
                imageView.setFitHeight(imageHeight);
            }

            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imageView.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-cursor: hand;");

            // Click to view full size
            imageView.setOnMouseClicked(e -> {
                if (e.getClickCount() == 1) {
                    showFullSizeImage(image, file.getOriginalFileName());
                }
            });

            // Add hover effect
            imageView.setOnMouseEntered(e -> imageView.setOpacity(0.8));
            imageView.setOnMouseExited(e -> imageView.setOpacity(1.0));

            imageContainer.getChildren().addAll(headerBox, imageView);

            logger.info("Image preview created successfully for: {}", file.getOriginalFileName());
            return imageContainer;

        } catch (Exception e) {
            logger.error("Error creating image preview for: {}", file.getOriginalFileName(), e);
            return null;
        }
    }

    /**
     * Show full size image in a new window
     */
    private void showFullSizeImage(javafx.scene.image.Image image, String fileName) {
        try {
            Stage imageStage = new Stage();
            imageStage.setTitle("Image Viewer - " + fileName);
            imageStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            javafx.scene.image.ImageView fullImageView = new javafx.scene.image.ImageView(image);
            fullImageView.setPreserveRatio(true);
            fullImageView.setSmooth(true);

            // Fit to screen size but don't exceed original size
            double screenWidth = javafx.stage.Screen.getPrimary().getVisualBounds().getWidth() * 0.8;
            double screenHeight = javafx.stage.Screen.getPrimary().getVisualBounds().getHeight() * 0.8;

            double imageWidth = image.getWidth();
            double imageHeight = image.getHeight();

            if (imageWidth > screenWidth || imageHeight > screenHeight) {
                double widthRatio = screenWidth / imageWidth;
                double heightRatio = screenHeight / imageHeight;
                double ratio = Math.min(widthRatio, heightRatio);

                fullImageView.setFitWidth(imageWidth * ratio);
                fullImageView.setFitHeight(imageHeight * ratio);
            } else {
                fullImageView.setFitWidth(imageWidth);
                fullImageView.setFitHeight(imageHeight);
            }

            ScrollPane scrollPane = new ScrollPane(fullImageView);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setStyle("-fx-background-color: #000000;");

            javafx.scene.Scene scene = new javafx.scene.Scene(scrollPane);
            imageStage.setScene(scene);
            imageStage.show();

        } catch (Exception e) {
            logger.error("Error showing full size image", e);
        }
    }

    /**
     * Download file
     */
    private void downloadFile(ChatFile file) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save File");
            fileChooser.setInitialFileName(file.getOriginalFileName());

            Stage stage = (Stage) messagesScrollPane.getScene().getWindow();
            File saveFile = fileChooser.showSaveDialog(stage);

            if (saveFile != null) {
                byte[] fileData = chatService.getFileData(file.getFilePath());
                if (fileData != null) {
                    Files.write(saveFile.toPath(), fileData);
                    showAlert("Success", "File downloaded successfully!");
                } else {
                    showAlert("Error", "File not found or corrupted.");
                }
            }

        } catch (Exception e) {
            logger.error("Error downloading file", e);
            showAlert("Error", "Failed to download file: " + e.getMessage());
        }
    }

    /**
     * Show alert dialog
     */
    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Connect to WebSocket server
     */
    private void connectToWebSocket() {
        try {
            if (currentUser != null) {
                webSocketClient.addMessageListener(this);
                boolean connected = webSocketClient.connect(
                    currentUser.getUserId(),
                    currentUser.getUsername(),
                    false // isAdmin = false
                );

                Platform.runLater(() -> {
                    if (connected) {
                        connectionStatusLabel.setText("Connected");
                        onlineStatusLabel.setText("🟢 Online");
                    } else {
                        connectionStatusLabel.setText("Connection Failed");
                        onlineStatusLabel.setText("🔴 Offline");
                    }
                });
            }
        } catch (Exception e) {
            logger.error("Error connecting to WebSocket", e);
            Platform.runLater(() -> {
                connectionStatusLabel.setText("Connection Error");
                onlineStatusLabel.setText("🔴 Offline");
            });
        }
    }

    // WebSocketMessageListener implementation
    @Override
    public void onMessageReceived(com.example.taskmanagerv3.websocket.ChatMessage message) {
        Platform.runLater(() -> {
            try {
                switch (message.getType()) {
                    case CHAT_MESSAGE:
                        handleIncomingChatMessage(message);
                        break;
                    case TYPING_START:
                        showTypingIndicator(message.getSenderName());
                        break;
                    case TYPING_STOP:
                        hideTypingIndicator();
                        break;
                    case CONNECTION_ACK:
                        connectionStatusLabel.setText("Connected");
                        onlineStatusLabel.setText("🟢 Online");
                        break;
                }
            } catch (Exception e) {
                logger.error("Error handling WebSocket message", e);
            }
        });
    }

    @Override
    public void onConnected() {
        Platform.runLater(() -> {
            connectionStatusLabel.setText("Connected");
            onlineStatusLabel.setText("🟢 Online");
            lastSeenLabel.setText("Last seen: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        });
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> {
            connectionStatusLabel.setText("Disconnected");
            onlineStatusLabel.setText("🔴 Offline");
        });
    }

    @Override
    public void onError(Exception error) {
        Platform.runLater(() -> {
            connectionStatusLabel.setText("Connection Error");
            onlineStatusLabel.setText("🔴 Offline");
            logger.error("WebSocket connection error", error);
        });
    }

    /**
     * Handle incoming chat message from WebSocket
     */
    private void handleIncomingChatMessage(com.example.taskmanagerv3.websocket.ChatMessage wsMessage) {
        try {
            logger.info("=== HANDLING INCOMING WEBSOCKET MESSAGE ===");
            logger.info("WebSocket Message - Sender ID: {}, Sender Name: {}, Receiver ID: {}, Data: '{}'",
                       wsMessage.getSenderId(), wsMessage.getSenderName(),
                       wsMessage.getReceiverId(), wsMessage.getData());
            logger.info("Current User ID: {}", currentUser.getUserId());

            // Check if this is our own message coming back (should not happen but extra safety)
            if (wsMessage.getSenderId() != null && wsMessage.getSenderId().equals(currentUser.getUserId())) {
                logger.warn("Received our own message back from server - IGNORING to prevent duplicate");
                logger.warn("This should not happen - check WebSocket server logic");
                return;
            }

            // Additional check: if message is not intended for us, ignore it
            if (wsMessage.getReceiverId() != null && !wsMessage.getReceiverId().equals(currentUser.getUserId())) {
                logger.info("Message not intended for us (Receiver ID: {} vs Our ID: {}) - IGNORING",
                           wsMessage.getReceiverId(), currentUser.getUserId());
                return;
            }

            // Create ChatMessage from WebSocket message
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setSenderId(wsMessage.getSenderId());
            chatMessage.setSenderName(wsMessage.getSenderName());
            chatMessage.setReceiverId(currentUser.getUserId());
            chatMessage.setReceiverName(currentUser.getFullName());
            chatMessage.setMessage((String) wsMessage.getData());
            chatMessage.setTimestamp(wsMessage.getTimestamp());
            chatMessage.setSenderRole(com.example.taskmanagerv3.model.UserRole.ADMIN);

            logger.info("Created ChatMessage from WebSocket: Sender={} ({}), Receiver={} ({}), Message='{}'",
                       chatMessage.getSenderName(), chatMessage.getSenderId(),
                       chatMessage.getReceiverName(), chatMessage.getReceiverId(),
                       chatMessage.getMessage());

            // Check for duplicate messages before saving
            // This prevents saving the same message multiple times
            boolean isDuplicate = chatService.isDuplicateMessage(
                chatMessage.getSenderId(),
                chatMessage.getReceiverId(),
                chatMessage.getMessage(),
                chatMessage.getTimestamp()
            );

            if (isDuplicate) {
                logger.warn("Duplicate message detected - IGNORING to prevent database duplication");
                logger.warn("Message: '{}' from {} to {}",
                           chatMessage.getMessage(), chatMessage.getSenderId(), chatMessage.getReceiverId());
                return;
            }

            // Save message to database
            ChatMessage savedMessage = chatService.saveChatMessage(chatMessage);
            if (savedMessage != null) {
                logger.info("Saved incoming message to database: ID={}", savedMessage.getMessageId());

                // Add to UI
                addMessageToUI(savedMessage);

                // Mark as read
                chatService.markMessagesAsRead(wsMessage.getSenderId(), currentUser.getUserId());
            } else {
                logger.error("Failed to save incoming message to database");
                // Still add to UI even if database save fails
                addMessageToUI(chatMessage);
            }

        } catch (Exception e) {
            logger.error("Error handling incoming chat message", e);
        }
    }

    /**
     * Show typing indicator
     */
    private void showTypingIndicator(String senderName) {
        typingLabel.setText(senderName + " is typing...");
        typingIndicator.setVisible(true);
        typingIndicator.setManaged(true);
    }

    /**
     * Hide typing indicator
     */
    private void hideTypingIndicator() {
        typingIndicator.setVisible(false);
        typingIndicator.setManaged(false);
    }

    /**
     * Cleanup when controller is destroyed
     */
    public void cleanup() {
        if (webSocketClient != null) {
            webSocketClient.removeMessageListener(this);
        }
    }
}
