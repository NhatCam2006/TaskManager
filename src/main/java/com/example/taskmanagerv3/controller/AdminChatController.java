package com.example.taskmanagerv3.controller;

import com.example.taskmanagerv3.model.ChatFile;
import com.example.taskmanagerv3.model.ChatMessage;
import com.example.taskmanagerv3.model.User;
import com.example.taskmanagerv3.model.UserRole;
import com.example.taskmanagerv3.service.ChatService;
import com.example.taskmanagerv3.service.UserService;
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
import javafx.scene.layout.Priority;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for admin chat functionality
 */
public class AdminChatController implements WebSocketMessageListener {
    private static final Logger logger = LoggerFactory.getLogger(AdminChatController.class);

    // FXML Components - Header
    @FXML private Label onlineUsersLabel;
    @FXML private Button refreshButton;
    @FXML private Button settingsButton;

    // FXML Components - User List
    @FXML private TextField searchUsersField;
    @FXML private VBox usersListContainer;
    @FXML private Label totalMessagesLabel;
    @FXML private Label unreadMessagesLabel;

    // FXML Components - Chat Area
    @FXML private HBox chatHeader;
    @FXML private Label selectedUserAvatar;
    @FXML private Label selectedUserName;
    @FXML private Label selectedUserStatus;
    @FXML private Button userInfoButton;
    @FXML private Button clearChatButton;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private VBox messagesContainer;
    @FXML private VBox defaultMessageContainer;
    @FXML private HBox typingIndicator;
    @FXML private Label typingLabel;

    // FXML Components - Message Input
    @FXML private VBox messageInputArea;
    @FXML private HBox attachmentsPreview;
    @FXML private HBox attachmentsContainer;
    @FXML private TextArea messageTextArea;
    @FXML private Button attachFileButton;
    @FXML private Button sendButton;
    @FXML private Button clearAttachmentsButton;
    @FXML private Button quickReply1;
    @FXML private Button quickReply2;
    @FXML private Button quickReply3;

    // FXML Components - Status
    @FXML private Label connectionStatusLabel;
    @FXML private Label lastActivityLabel;

    // Services and utilities
    private ChatService chatService;
    private UserService userService;

    private SessionManager sessionManager;
    private User currentAdmin;
    private User selectedUser;
    private List<User> allUsers;
    private List<File> selectedFiles;
    private Map<Integer, Integer> unreadCounts; // userId -> unread count
    private boolean isTyping = false;
    private WebSocketClient webSocketClient;

    @FXML
    private void initialize() {
        chatService = new ChatService();
        userService = new UserService();
        webSocketClient = WebSocketClient.getInstance();

        sessionManager = SessionManager.getInstance();
        selectedFiles = new ArrayList<>();
        allUsers = new ArrayList<>();
        unreadCounts = new HashMap<>();

        setupUI();
        loadCurrentAdmin();

        loadAllUsers();
        startPeriodicRefresh();
        connectToWebSocket();
    }

    /**
     * Setup UI components and event handlers
     */
    private void setupUI() {
        // Setup search functionality
        searchUsersField.textProperty().addListener((obs, oldText, newText) -> filterUsers(newText));

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
        refreshButton.setOnAction(e -> refreshData());
        settingsButton.setOnAction(e -> showSettings());
        userInfoButton.setOnAction(e -> showUserInfo());
        clearChatButton.setOnAction(e -> clearChat());

        // Setup quick replies
        quickReply1.setOnAction(e -> sendQuickReply("✅ Your issue has been resolved. If you need further assistance, please let me know."));
        quickReply2.setOnAction(e -> sendQuickReply("🔍 Could you please provide more details about your issue? This will help me assist you better."));
        quickReply3.setOnAction(e -> sendQuickReply("⏳ I'm currently working on your request. I'll get back to you shortly with an update."));

        // Auto-scroll to bottom when new messages are added
        messagesContainer.heightProperty().addListener((obs, oldHeight, newHeight) -> {
            Platform.runLater(() -> {
                messagesScrollPane.setVvalue(1.0);
            });
        });
    }

    /**
     * Load current admin from session
     */
    private void loadCurrentAdmin() {
        try {
            currentAdmin = sessionManager.getCurrentUser();
            if (currentAdmin == null || currentAdmin.getRole() != UserRole.ADMIN) {
                logger.error("Current user is not an admin");
                showAlert("Error", "Access denied. Admin privileges required.");
                return;
            }
            logger.info("Admin chat initialized for: {}", currentAdmin.getUsername());
        } catch (Exception e) {
            logger.error("Error loading current admin", e);
        }
    }



    /**
     * Load all users for chat
     */
    private void loadAllUsers() {
        try {
            allUsers = userService.getAllActiveUsers();
            // Remove current admin from list
            allUsers.removeIf(user -> user.getUserId() == currentAdmin.getUserId());

            Platform.runLater(() -> {
                updateUsersDisplay();
                updateStats();
            });

            logger.info("Loaded {} users for admin chat", allUsers.size());
        } catch (Exception e) {
            logger.error("Error loading users", e);
        }
    }

    /**
     * Update users display in left panel
     */
    private void updateUsersDisplay() {
        usersListContainer.getChildren().clear();

        for (User user : allUsers) {
            VBox userCard = createUserCard(user);
            usersListContainer.getChildren().add(userCard);
        }

        // Update online users count
        onlineUsersLabel.setText(allUsers.size() + " users available");
    }

    /**
     * Create user card for left panel
     */
    private VBox createUserCard(User user) {
        VBox userCard = new VBox(5);
        userCard.setPadding(new Insets(10));
        userCard.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e9ecef; -fx-border-radius: 8; -fx-border-width: 1; -fx-cursor: hand;");

        // User info
        HBox userInfo = new HBox(10);
        userInfo.setAlignment(Pos.CENTER_LEFT);

        Label avatar = new Label("👤");
        avatar.setStyle("-fx-font-size: 16px;");

        VBox userDetails = new VBox(2);
        Label nameLabel = new Label(user.getFullName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        Label usernameLabel = new Label("@" + user.getUsername());
        usernameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");
        userDetails.getChildren().addAll(nameLabel, usernameLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Unread count badge
        Label unreadBadge = new Label();
        int unreadCount = unreadCounts.getOrDefault(user.getUserId(), 0);
        if (unreadCount > 0) {
            unreadBadge.setText(String.valueOf(unreadCount));
            unreadBadge.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 2 6; -fx-font-size: 10px;");
            unreadBadge.setVisible(true);
        } else {
            unreadBadge.setVisible(false);
        }

        userInfo.getChildren().addAll(avatar, userDetails, spacer, unreadBadge);

        // Last message preview
        Label lastMessageLabel = new Label("Click to start conversation");
        lastMessageLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d; -fx-font-style: italic;");

        userCard.getChildren().addAll(userInfo, lastMessageLabel);

        // Click handler
        userCard.setOnMouseClicked(e -> selectUser(user));

        // Hover effect
        userCard.setOnMouseEntered(e -> userCard.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-border-color: #007bff; -fx-border-radius: 8; -fx-border-width: 1; -fx-cursor: hand;"));
        userCard.setOnMouseExited(e -> {
            if (selectedUser == null || selectedUser.getUserId() != user.getUserId()) {
                userCard.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e9ecef; -fx-border-radius: 8; -fx-border-width: 1; -fx-cursor: hand;");
            }
        });

        return userCard;
    }

    /**
     * Select user to chat with
     */
    private void selectUser(User user) {
        selectedUser = user;

        // Update UI
        Platform.runLater(() -> {
            // Show chat header and input area
            chatHeader.setVisible(true);
            chatHeader.setManaged(true);
            messageInputArea.setVisible(true);
            messageInputArea.setManaged(true);

            // Hide default message
            defaultMessageContainer.setVisible(false);
            defaultMessageContainer.setManaged(false);

            // Update selected user info
            selectedUserName.setText(user.getFullName());
            selectedUserStatus.setText("@" + user.getUsername() + " • " + user.getRole());

            // Update user card selection
            updateUsersDisplay();

            // Load chat history
            loadChatHistory();

            // Mark messages as read
            chatService.markMessagesAsRead(user.getUserId(), currentAdmin.getUserId());
            unreadCounts.put(user.getUserId(), 0);
        });

        logger.info("Selected user for chat: {}", user.getUsername());
    }

    /**
     * Load chat history with selected user
     */
    private void loadChatHistory() {
        if (selectedUser == null) return;

        try {
            List<ChatMessage> messages = chatService.getChatMessages(currentAdmin.getUserId(), selectedUser.getUserId(), 50);
            Platform.runLater(() -> {
                messagesContainer.getChildren().clear();

                // Add messages in chronological order (oldest first)
                for (ChatMessage message : messages) {
                    addMessageToUI(message);
                }

                // Auto-scroll to bottom after loading messages
                Platform.runLater(() -> {
                    messagesScrollPane.setVvalue(1.0);
                });
            });

        } catch (Exception e) {
            logger.error("Error loading chat history", e);
        }
    }

    /**
     * Add message to UI (Zalo-style compact design)
     */
    private void addMessageToUI(ChatMessage message) {
        try {
            // Determine if message is from admin (current user)
            boolean isFromAdmin = message.getSenderId() == currentAdmin.getUserId();

            // Main container for the message row
            HBox messageRow = new HBox();
            messageRow.setPadding(new Insets(2, 10, 2, 10));
            messageRow.setAlignment(isFromAdmin ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            // Message bubble container
            VBox messageBubble = new VBox(2);
            messageBubble.setMaxWidth(250);

            // Message content
            Label messageLabel = new Label(message.getMessage());
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(240);

            // Zalo-style bubble design
            String bubbleStyle;
            if (isFromAdmin) {
                bubbleStyle = "-fx-background-color: #0084ff; -fx-text-fill: white; " +
                             "-fx-padding: 8 12; -fx-background-radius: 18 18 4 18; -fx-font-size: 13px;";
                messageBubble.setAlignment(Pos.CENTER_RIGHT);
            } else {
                bubbleStyle = "-fx-background-color: #f1f3f4; -fx-text-fill: #050505; " +
                             "-fx-padding: 8 12; -fx-background-radius: 18 18 18 4; -fx-font-size: 13px;";
                messageBubble.setAlignment(Pos.CENTER_LEFT);
            }
            messageLabel.setStyle(bubbleStyle);

            // Time label (very compact)
            Label timeLabel = new Label(message.getTimeOnly());
            timeLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #8e8e93; -fx-padding: 1 0 0 0;");

            if (isFromAdmin) {
                timeLabel.setAlignment(Pos.CENTER_RIGHT);
            } else {
                timeLabel.setAlignment(Pos.CENTER_LEFT);
            }

            // Add to bubble
            messageBubble.getChildren().addAll(messageLabel, timeLabel);

            // Add file attachments if any (compact style)
            if (message.isHasAttachments() && message.getAttachments() != null) {
                for (ChatFile file : message.getAttachments()) {
                    VBox fileBox = createCompactFileAttachmentUI(file, isFromAdmin);
                    messageBubble.getChildren().add(fileBox);
                }
            }

            messageRow.getChildren().add(messageBubble);
            messagesContainer.getChildren().add(messageRow);

            // Auto-scroll to bottom after adding message
            Platform.runLater(() -> {
                messagesScrollPane.setVvalue(1.0);
            });

        } catch (Exception e) {
            logger.error("Error adding message to UI", e);
        }
    }

    /**
     * Create compact file attachment UI (Zalo-style)
     */
    private VBox createCompactFileAttachmentUI(ChatFile file, boolean isFromAdmin) {
        VBox container = new VBox(3);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setMaxWidth(250);

        // Check if this is an image file
        if (file.isImage()) {
            VBox imageBox = createCompactImagePreview(file, isFromAdmin);
            if (imageBox != null) {
                return imageBox;
            }
            // If image preview fails, fall back to regular file display
        }

        // Regular file attachment UI
        HBox fileBox = new HBox(6);
        fileBox.setAlignment(Pos.CENTER_LEFT);
        fileBox.setMaxWidth(220);

        String bgColor = isFromAdmin ? "rgba(255,255,255,0.2)" : "rgba(0,0,0,0.05)";
        fileBox.setStyle("-fx-background-color: " + bgColor + "; -fx-padding: 6; -fx-background-radius: 12;");

        // File icon (smaller)
        String icon = file.isImage() ? "🖼️" : (file.isDocument() ? "📄" : "📎");
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 14px;");

        // File info (compact)
        VBox fileInfo = new VBox(1);
        Label nameLabel = new Label(file.getOriginalFileName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: " +
                          (isFromAdmin ? "white" : "#333"));
        nameLabel.setMaxWidth(140);

        Label sizeLabel = new Label(file.getFormattedFileSize());
        sizeLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: " +
                          (isFromAdmin ? "rgba(255,255,255,0.8)" : "#666"));
        fileInfo.getChildren().addAll(nameLabel, sizeLabel);

        // Download button (compact)
        Button downloadButton = new Button("⬇");
        downloadButton.setStyle("-fx-background-color: " + (isFromAdmin ? "rgba(255,255,255,0.3)" : "#e0e0e0") +
                               "; -fx-text-fill: " + (isFromAdmin ? "white" : "#333") +
                               "; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-size: 10px; -fx-padding: 3 6;");
        downloadButton.setOnAction(e -> downloadFile(file));

        fileBox.getChildren().addAll(iconLabel, fileInfo, downloadButton);
        container.getChildren().add(fileBox);
        return container;
    }

    /**
     * Create compact image preview for admin chat
     */
    private VBox createCompactImagePreview(ChatFile file, boolean isFromAdmin) {
        try {
            // Load image data
            byte[] imageData = chatService.getFileData(file.getFilePath());
            if (imageData == null) {
                logger.error("Could not load image data for: {}", file.getOriginalFileName());
                return null;
            }

            // Create image from byte array
            Image image = new Image(new java.io.ByteArrayInputStream(imageData));
            if (image.isError()) {
                logger.error("Error loading image: {}", file.getOriginalFileName());
                return null;
            }

            VBox imageContainer = new VBox(3);
            imageContainer.setAlignment(Pos.CENTER_LEFT);
            imageContainer.setMaxWidth(200);

            String bgColor = isFromAdmin ? "rgba(255,255,255,0.1)" : "rgba(0,0,0,0.03)";
            imageContainer.setStyle("-fx-background-color: " + bgColor + "; -fx-padding: 6; -fx-background-radius: 12;");

            // Image info header (compact)
            HBox headerBox = new HBox(4);
            headerBox.setAlignment(Pos.CENTER_LEFT);

            Label iconLabel = new Label("🖼️");
            iconLabel.setStyle("-fx-font-size: 12px;");

            Label nameLabel = new Label(file.getOriginalFileName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 10px; -fx-text-fill: " +
                              (isFromAdmin ? "white" : "#333"));
            nameLabel.setMaxWidth(120);

            Button downloadButton = new Button("⬇");
            downloadButton.setStyle("-fx-background-color: " + (isFromAdmin ? "rgba(255,255,255,0.3)" : "#e0e0e0") +
                                   "; -fx-text-fill: " + (isFromAdmin ? "white" : "#333") +
                                   "; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 9px; -fx-padding: 2 4;");
            downloadButton.setOnAction(e -> downloadFile(file));

            headerBox.getChildren().addAll(iconLabel, nameLabel, downloadButton);

            // Image preview (compact)
            ImageView imageView = new ImageView(image);

            // Calculate appropriate size (max 180px width for compact view)
            double maxWidth = 180;
            double maxHeight = 120;

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
            imageView.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-cursor: hand;");

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

            logger.info("Compact image preview created successfully for: {}", file.getOriginalFileName());
            return imageContainer;

        } catch (Exception e) {
            logger.error("Error creating compact image preview for: {}", file.getOriginalFileName(), e);
            return null;
        }
    }

    /**
     * Show full size image in a new window
     */
    private void showFullSizeImage(Image image, String fileName) {
        try {
            Stage imageStage = new Stage();
            imageStage.setTitle("Image Viewer - " + fileName);
            imageStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            ImageView fullImageView = new ImageView(image);
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
     * Create file attachment UI (legacy method for compatibility)
     */
    private VBox createFileAttachmentUI(ChatFile file) {
        return createCompactFileAttachmentUI(file, false);
    }

    /**
     * Send message to selected user
     */
    private void sendMessage() {
        if (selectedUser == null) {
            showAlert("Error", "Please select a user to send message to.");
            return;
        }

        String messageText = messageTextArea.getText().trim();
        if (messageText.isEmpty() && selectedFiles.isEmpty()) {
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
                currentAdmin.getUserId(),
                currentAdmin.getFullName(),
                selectedUser.getUserId(),
                actualMessageText
            );
            chatMessage.setSenderRole(currentAdmin.getRole());

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
                                currentAdmin.getUserId()
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

                // Add to UI
                Platform.runLater(() -> {
                    addMessageToUI(savedMessage);
                    messageTextArea.clear();
                    clearAttachments();
                });

                // Send via WebSocket - Only send notification with the actual message content
                // This prevents sending empty messages when only files are attached
                logger.info("Notifying user about new message via WebSocket with text: '{}'", actualMessageText);
                webSocketClient.sendChatMessage(actualMessageText, selectedUser.getUserId());

                logger.info("Admin message sent to user: {}", selectedUser.getUsername());
            }

        } catch (Exception e) {
            logger.error("Error sending message", e);
            showAlert("Error", "Failed to send message: " + e.getMessage());
        }
    }

    /**
     * Send quick reply
     */
    private void sendQuickReply(String message) {
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
                VBox filePreview = createAdminFilePreview(file);
                attachmentsContainer.getChildren().add(filePreview);
            }

            attachmentsPreview.setVisible(!selectedFiles.isEmpty());
            attachmentsPreview.setManaged(!selectedFiles.isEmpty());
        });
    }

    /**
     * Create file preview for admin attachment selection
     */
    private VBox createAdminFilePreview(File file) {
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
                    Image image = new Image(file.toURI().toString());
                    if (!image.isError()) {
                        ImageView imageView = new ImageView(image);
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
     * Filter users based on search text
     */
    private void filterUsers(String searchText) {
        // Implementation for filtering users
        updateUsersDisplay();
    }

    /**
     * Update statistics
     */
    private void updateStats() {
        // Calculate total messages and unread count
        final int totalMessages = 0; // TODO: Calculate actual total messages
        final int totalUnread = unreadCounts.values().stream().mapToInt(Integer::intValue).sum();

        Platform.runLater(() -> {
            totalMessagesLabel.setText(totalMessages + " messages");
            unreadMessagesLabel.setText(totalUnread + " unread");
        });
    }



    /**
     * Refresh data
     */
    private void refreshData() {
        loadAllUsers();
        if (selectedUser != null) {
            loadChatHistory();
        }
    }

    /**
     * Show settings dialog
     */
    private void showSettings() {
        showAlert("Settings", "Admin chat settings will be implemented here.");
    }

    /**
     * Show user info
     */
    private void showUserInfo() {
        if (selectedUser != null) {
            showAlert("User Info",
                "Name: " + selectedUser.getFullName() + "\n" +
                "Username: " + selectedUser.getUsername() + "\n" +
                "Role: " + selectedUser.getRole() + "\n" +
                "Email: " + selectedUser.getEmail());
        }
    }

    /**
     * Clear chat history
     */
    private void clearChat() {
        if (selectedUser != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Clear Chat");
            alert.setHeaderText("Clear chat history with " + selectedUser.getFullName() + "?");
            alert.setContentText("This action cannot be undone.");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    messagesContainer.getChildren().clear();
                    // TODO: Implement database chat clearing
                }
            });
        }
    }

    /**
     * Start periodic refresh
     */
    private void startPeriodicRefresh() {
        // TODO: Implement periodic refresh for new messages
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
            if (currentAdmin != null) {
                webSocketClient.addMessageListener(this);
                boolean connected = webSocketClient.connect(
                    currentAdmin.getUserId(),
                    currentAdmin.getUsername(),
                    true // isAdmin = true
                );

                Platform.runLater(() -> {
                    if (connected) {
                        connectionStatusLabel.setText("🟢 Connected");
                        connectionStatusLabel.setStyle("-fx-text-fill: #28a745;");
                    } else {
                        connectionStatusLabel.setText("🔴 Connection Failed");
                        connectionStatusLabel.setStyle("-fx-text-fill: #dc3545;");
                    }
                });
            }
        } catch (Exception e) {
            logger.error("Error connecting to WebSocket", e);
            Platform.runLater(() -> {
                connectionStatusLabel.setText("🔴 Connection Error");
                connectionStatusLabel.setStyle("-fx-text-fill: #dc3545;");
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
                        connectionStatusLabel.setText("🟢 Connected");
                        connectionStatusLabel.setStyle("-fx-text-fill: #28a745;");
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
            connectionStatusLabel.setText("🟢 Connected");
            connectionStatusLabel.setStyle("-fx-text-fill: #28a745;");
            lastActivityLabel.setText("Connected at: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        });
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> {
            connectionStatusLabel.setText("🔴 Disconnected");
            connectionStatusLabel.setStyle("-fx-text-fill: #dc3545;");
        });
    }

    @Override
    public void onError(Exception error) {
        Platform.runLater(() -> {
            connectionStatusLabel.setText("🔴 Connection Error");
            connectionStatusLabel.setStyle("-fx-text-fill: #dc3545;");
            logger.error("WebSocket connection error", error);
        });
    }

    /**
     * Handle incoming chat message from WebSocket
     */
    private void handleIncomingChatMessage(com.example.taskmanagerv3.websocket.ChatMessage wsMessage) {
        try {
            logger.info("=== ADMIN HANDLING INCOMING WEBSOCKET MESSAGE ===");
            logger.info("WebSocket Message - Sender ID: {}, Sender Name: {}, Receiver ID: {}, Data: '{}'",
                       wsMessage.getSenderId(), wsMessage.getSenderName(),
                       wsMessage.getReceiverId(), wsMessage.getData());
            logger.info("Current Admin ID: {}", currentAdmin.getUserId());

            // Check if this is our own message coming back (should not happen but extra safety)
            if (wsMessage.getSenderId() != null && wsMessage.getSenderId().equals(currentAdmin.getUserId())) {
                logger.warn("Admin received own message back from server - IGNORING to prevent duplicate");
                return;
            }

            // Additional check: if message is not intended for us, ignore it
            if (wsMessage.getReceiverId() != null && !wsMessage.getReceiverId().equals(currentAdmin.getUserId())) {
                logger.info("Message not intended for admin (Receiver ID: {} vs Admin ID: {}) - IGNORING",
                           wsMessage.getReceiverId(), currentAdmin.getUserId());
                return;
            }

            // Create ChatMessage from WebSocket message
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setSenderId(wsMessage.getSenderId());
            chatMessage.setSenderName(wsMessage.getSenderName());
            chatMessage.setReceiverId(currentAdmin.getUserId());
            chatMessage.setReceiverName(currentAdmin.getFullName());
            chatMessage.setMessage((String) wsMessage.getData());
            chatMessage.setTimestamp(wsMessage.getTimestamp());
            chatMessage.setSenderRole(UserRole.USER);

            logger.info("Created ChatMessage from WebSocket: Sender={} ({}), Receiver={} ({}), Message='{}'",
                       chatMessage.getSenderName(), chatMessage.getSenderId(),
                       chatMessage.getReceiverName(), chatMessage.getReceiverId(),
                       chatMessage.getMessage());

            // Check for duplicate messages before saving
            boolean isDuplicate = chatService.isDuplicateMessage(
                chatMessage.getSenderId(),
                chatMessage.getReceiverId(),
                chatMessage.getMessage(),
                chatMessage.getTimestamp()
            );

            if (isDuplicate) {
                logger.warn("Admin - Duplicate message detected - IGNORING to prevent database duplication");
                return;
            }

            // Save message to database
            ChatMessage savedMessage = chatService.saveChatMessage(chatMessage);
            if (savedMessage != null) {
                logger.info("Admin - Saved incoming message to database: ID={}", savedMessage.getMessageId());

                // Add to UI if this is the selected user
                if (selectedUser != null && selectedUser.getUserId() == wsMessage.getSenderId()) {
                    addMessageToUI(savedMessage);
                    // Mark as read
                    chatService.markMessagesAsRead(wsMessage.getSenderId(), currentAdmin.getUserId());
                } else {
                    // Update unread count
                    int currentUnread = unreadCounts.getOrDefault(wsMessage.getSenderId(), 0);
                    unreadCounts.put(wsMessage.getSenderId(), currentUnread + 1);
                    updateUsersDisplay();
                    updateStats();
                }
            }

        } catch (Exception e) {
            logger.error("Error handling incoming chat message", e);
        }
    }

    /**
     * Show typing indicator
     */
    private void showTypingIndicator(String senderName) {
        if (selectedUser != null) {
            typingLabel.setText(senderName + " is typing...");
            typingIndicator.setVisible(true);
            typingIndicator.setManaged(true);
        }
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
