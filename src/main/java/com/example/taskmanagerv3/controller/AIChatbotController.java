package com.example.taskmanagerv3.controller;

import com.example.taskmanagerv3.service.AIChatbotService;
import com.example.taskmanagerv3.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller for AI Chatbot interface
 */
public class AIChatbotController {
    private static final Logger logger = LoggerFactory.getLogger(AIChatbotController.class);

    @FXML private VBox chatContainer;
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextArea messageInput;
    @FXML private Button sendButton;
    @FXML private Button clearButton;
    @FXML private Label statusLabel;
    @FXML private Button closeButton;

    private AIChatbotService chatbotService;
    private SessionManager sessionManager;

    @FXML
    private void initialize() {
        chatbotService = new AIChatbotService();
        sessionManager = SessionManager.getInstance();

        setupUI();
        showWelcomeMessage();

        logger.info("AI Chatbot interface initialized");
    }

    /**
     * Setup UI components
     */
    private void setupUI() {
        // Setup message input
        messageInput.setPromptText("Type your message here... (e.g., 'How am I doing?', 'Show my tasks', 'Give me suggestions')");
        messageInput.setWrapText(true);
        messageInput.setPrefRowCount(2);

        // Setup buttons
        sendButton.setOnAction(e -> sendMessage());
        clearButton.setOnAction(e -> clearChat());
        closeButton.setOnAction(e -> closeWindow());

        // Enable send on Enter (Ctrl+Enter for new line)
        messageInput.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER") && !event.isControlDown()) {
                event.consume();
                sendMessage();
            }
        });

        // Auto-scroll to bottom
        chatContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
        });

        // Update status with API availability
        updateStatusLabel();
    }

    /**
     * Update status label with API information
     */
    private void updateStatusLabel() {
        if (chatbotService.isGeminiApiAvailable()) {
            statusLabel.setText("✅ Connected to Gemini 2.0 Flash API");
            statusLabel.setStyle("-fx-text-fill: #27ae60;");
        } else {
            statusLabel.setText("⚠️ Using fallback responses (API not configured)");
            statusLabel.setStyle("-fx-text-fill: #f39c12;");
        }
    }

    /**
     * Show welcome message
     */
    private void showWelcomeMessage() {
        String apiStatus = chatbotService.getApiStatus();
        String welcomeMessage = "Hello! I'm your AI assistant for WorkFlow Manager. " +
                              "I can help you with task management, provide insights about your productivity, " +
                              "and offer suggestions to improve your workflow.\n\n" +
                              "Current Status: " + apiStatus + "\n\n" +
                              "Try asking me:\n" +
                              "• 'How am I doing?' - Get your current status\n" +
                              "• 'What tasks are due today?' - Check today's deadlines\n" +
                              "• 'Give me some suggestions' - Get productivity tips\n" +
                              "• 'Show my overdue tasks' - See what needs attention\n\n" +
                              "What would you like to know?";

        addBotMessage(welcomeMessage);
    }

    /**
     * Send user message
     */
    @FXML
    private void sendMessage() {
        String userMessage = messageInput.getText().trim();

        if (userMessage.isEmpty()) {
            return;
        }

        // Add user message to chat
        addUserMessage(userMessage);

        // Clear input
        messageInput.clear();

        // Update status
        statusLabel.setText("AI is thinking...");
        statusLabel.setStyle("-fx-text-fill: #3498db;");

        // Process message in background thread
        new Thread(() -> {
            try {
                int currentUserId = sessionManager.getCurrentUserId();
                String response = chatbotService.processMessage(userMessage, currentUserId);

                Platform.runLater(() -> {
                    addBotMessage(response);
                    updateStatusLabel();
                });

            } catch (Exception e) {
                logger.error("Error processing AI message", e);
                Platform.runLater(() -> {
                    addBotMessage("I apologize, but I'm having trouble processing your request right now. Please try again later.");
                    updateStatusLabel();
                });
            }
        }).start();
    }

    /**
     * Add user message to chat
     */
    private void addUserMessage(String message) {
        VBox messageBox = createMessageBox(message, true);
        chatContainer.getChildren().add(messageBox);

        // Auto-scroll to bottom
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    /**
     * Add bot message to chat
     */
    private void addBotMessage(String message) {
        VBox messageBox = createMessageBox(message, false);
        chatContainer.getChildren().add(messageBox);

        // Auto-scroll to bottom
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    /**
     * Create message box
     */
    private VBox createMessageBox(String message, boolean isUser) {
        VBox messageBox = new VBox(5);
        messageBox.setPadding(new Insets(10));
        messageBox.setMaxWidth(400);

        // Create header with sender and timestamp
        HBox header = new HBox(10);
        Label senderLabel = new Label(isUser ? "You" : "🤖 AI Assistant");
        senderLabel.setFont(Font.font(null, FontWeight.BOLD, 12));

        Label timestampLabel = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        timestampLabel.setFont(Font.font(10));
        timestampLabel.setStyle("-fx-text-fill: #666666;");

        header.getChildren().addAll(senderLabel, timestampLabel);

        // Create message content
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(380);
        messageLabel.setFont(Font.font(13));

        // Style message box
        if (isUser) {
            messageBox.setAlignment(Pos.CENTER_RIGHT);
            messageBox.setStyle("-fx-background-color: #3498db; -fx-background-radius: 15; -fx-padding: 10;");
            senderLabel.setStyle("-fx-text-fill: white;");
            messageLabel.setStyle("-fx-text-fill: white;");
            timestampLabel.setStyle("-fx-text-fill: #E8F4FD;");
        } else {
            messageBox.setAlignment(Pos.CENTER_LEFT);
            messageBox.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 15; -fx-padding: 10;");
            senderLabel.setStyle("-fx-text-fill: #2c3e50;");
            messageLabel.setStyle("-fx-text-fill: #2c3e50;");
        }

        messageBox.getChildren().addAll(header, messageLabel);

        // Add margin
        VBox.setMargin(messageBox, new Insets(5, 10, 5, 10));

        return messageBox;
    }

    /**
     * Clear chat history
     */
    @FXML
    private void clearChat() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear Chat");
        alert.setHeaderText("Clear Chat History");
        alert.setContentText("Are you sure you want to clear the chat history?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                chatContainer.getChildren().clear();
                chatbotService.clearConversationHistory();
                showWelcomeMessage();
                logger.info("Chat history cleared");
            }
        });
    }

    /**
     * Close chatbot window
     */
    @FXML
    private void closeWindow() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Show quick suggestions
     */
    @FXML
    private void showQuickSuggestions() {
        String[] suggestions = {
            "How am I doing?",
            "What tasks are due today?",
            "Give me some suggestions",
            "Show my overdue tasks",
            "What's my completion rate?",
            "Help me prioritize my tasks",
            "TEST_API_CONNECTION" // Hidden test option
        };

        ChoiceDialog<String> dialog = new ChoiceDialog<>(suggestions[0], suggestions);
        dialog.setTitle("Quick Suggestions");
        dialog.setHeaderText("Choose a quick question:");
        dialog.setContentText("Select a suggestion:");

        dialog.showAndWait().ifPresent(suggestion -> {
            if (suggestion.equals("TEST_API_CONNECTION")) {
                testApiConnection();
            } else {
                messageInput.setText(suggestion);
                sendMessage();
            }
        });
    }

    /**
     * Test API connection directly
     */
    private void testApiConnection() {
        addBotMessage("🔧 Testing Gemini API connection...");

        new Thread(() -> {
            try {
                AIChatbotService service = new AIChatbotService();
                String result = service.testApiDirectly();

                Platform.runLater(() -> {
                    addBotMessage("🔧 API Test Result:\n" + result);
                });

            } catch (Exception e) {
                logger.error("Error testing API", e);
                Platform.runLater(() -> {
                    addBotMessage("🔧 API Test Failed: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Show help information
     */
    @FXML
    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("AI Assistant Help");
        alert.setHeaderText("How to use the AI Assistant");
        alert.setContentText("""
            The AI Assistant can help you with:

            📊 Status & Analytics:
            • "How am I doing?" - Get your productivity overview
            • "What's my completion rate?" - See your task completion statistics

            📅 Task Management:
            • "What tasks are due today?" - Check today's deadlines
            • "Show my overdue tasks" - See tasks that need attention
            • "What tasks do I have?" - List your current tasks

            💡 Suggestions & Tips:
            • "Give me some suggestions" - Get personalized productivity tips
            • "Help me prioritize" - Get advice on task prioritization
            • "How can I be more productive?" - Get productivity recommendations

            🤖 General:
            • Just type naturally! The AI understands conversational language
            • Ask follow-up questions for more details
            • Use the Quick Suggestions button for common queries

            Tips:
            • Be specific in your questions for better responses
            • The AI learns from your task data to provide personalized insights
            • You can ask about time management, productivity tips, and workflow optimization
            """);

        alert.getDialogPane().setPrefWidth(500);
        alert.showAndWait();
    }
}
