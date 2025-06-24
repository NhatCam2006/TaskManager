package com.example.taskmanagerv3.controller;

import com.example.taskmanagerv3.service.AdminAIChatbotService;
import com.example.taskmanagerv3.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for Admin AI Chatbot interface
 */
public class AdminAIChatbotController {
    private static final Logger logger = LoggerFactory.getLogger(AdminAIChatbotController.class);

    @FXML private VBox chatContainer;
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextArea messageInput;
    @FXML private Button sendButton;
    @FXML private Button clearButton;
    @FXML private Label statusLabel;
    @FXML private Button closeButton;

    private AdminAIChatbotService chatbotService;
    private SessionManager sessionManager;

    @FXML
    private void initialize() {
        chatbotService = new AdminAIChatbotService();
        sessionManager = SessionManager.getInstance();

        setupUI();
        showWelcomeMessage();

        logger.info("Admin AI Chatbot interface initialized");
    }

    /**
     * Setup UI components
     */
    private void setupUI() {
        // Setup message input
        messageInput.setPromptText("Type your admin command... (e.g., 'Create task Fix login bug for John with high priority', 'Show team workload', 'Assign urgent tasks')");
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
        String welcomeMessage = """
            # 🤖 Welcome to Admin AI Assistant!

            Hello **Admin**! I'm your **AI assistant** for WorkFlow Manager. I can help you with:

            - **📋 Task Creation & Assignment** - Create and assign tasks quickly
            - **👥 Team Management** - Monitor team workload and status
            - **📊 Analytics & Reports** - Get insights on team productivity
            - **⚡ Quick Commands** - Use natural language for fast operations

            ## 🔗 Current Status:
            """ + apiStatus + """

            ## 💬 Try asking me:
            - `"Create task 'Fix login bug' for john with high priority"` - Quick task creation 📋
            - `"Show team status"` - View team overview 👥
            - `"Assign 'Update docs' to sarah due tomorrow"` - Smart assignment ⏰
            - `"Help"` - See all available commands 💡

            **Ready to help you manage your team efficiently!** 🚀
            """;

        addBotMessage(welcomeMessage);
    }

    /**
     * Send message to AI
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
                logger.error("Error processing admin AI message", e);
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
        VBox messageBox = new VBox();
        messageBox.setAlignment(Pos.CENTER_RIGHT);
        messageBox.setPadding(new Insets(5, 0, 5, 50));

        Label userLabel = new Label("👤 Admin");
        userLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
        userLabel.setAlignment(Pos.CENTER_RIGHT);

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(400);
        messageLabel.setStyle("""
            -fx-background-color: #3498db; 
            -fx-text-fill: white; 
            -fx-padding: 10; 
            -fx-background-radius: 15; 
            -fx-font-size: 14;
            """);

        messageBox.getChildren().addAll(userLabel, messageLabel);
        chatContainer.getChildren().add(messageBox);
    }

    /**
     * Add bot message to chat with markdown support
     */
    private void addBotMessage(String message) {
        VBox messageBox = new VBox();
        messageBox.setAlignment(Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(5, 50, 5, 0));

        Label botLabel = new Label("🤖 Admin AI Assistant");
        botLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");

        // Create a styled text area for markdown-like content
        TextFlow textFlow = createStyledTextFlow(message);
        textFlow.setMaxWidth(500);
        textFlow.setStyle("""
            -fx-background-color: #ecf0f1; 
            -fx-padding: 15; 
            -fx-background-radius: 15;
            -fx-border-color: #bdc3c7;
            -fx-border-radius: 15;
            """);

        messageBox.getChildren().addAll(botLabel, textFlow);
        chatContainer.getChildren().add(messageBox);
    }

    /**
     * Create styled text flow with basic markdown support
     */
    private TextFlow createStyledTextFlow(String message) {
        TextFlow textFlow = new TextFlow();
        
        // Simple markdown parsing for bold text
        String[] parts = message.split("\\*\\*");
        boolean isBold = false;
        
        for (String part : parts) {
            if (!part.isEmpty()) {
                Text text = new Text(part);
                text.setFont(Font.font("System", isBold ? FontWeight.BOLD : FontWeight.NORMAL, 14));
                textFlow.getChildren().add(text);
            }
            isBold = !isBold;
        }
        
        return textFlow;
    }

    /**
     * Clear chat
     */
    @FXML
    private void clearChat() {
        chatContainer.getChildren().clear();
        chatbotService.clearConversationHistory();
        showWelcomeMessage();
        logger.info("Admin chat cleared");
    }

    /**
     * Close window
     */
    @FXML
    private void closeWindow() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
        logger.info("Admin AI chatbot window closed");
    }

    /**
     * Show quick task creation dialog
     */
    @FXML
    private void showQuickTaskCreation() {
        String[] suggestions = {
            "Create task 'Fix login bug' for john with high priority",
            "Create task 'Update documentation' for sarah with medium priority",
            "Create task 'Review code' for mike with low priority due tomorrow",
            "Assign 'Database optimization' to alex with urgent priority"
        };

        ChoiceDialog<String> dialog = new ChoiceDialog<>(suggestions[0], suggestions);
        dialog.setTitle("Quick Task Creation");
        dialog.setHeaderText("Choose a task creation template:");
        dialog.setContentText("Select a template:");

        dialog.showAndWait().ifPresent(suggestion -> {
            messageInput.setText(suggestion);
            sendMessage();
        });
    }

    /**
     * Show quick assignment suggestions
     */
    @FXML
    private void showQuickAssignment() {
        String[] suggestions = {
            "Show team status",
            "Who has the most tasks?",
            "Show overdue tasks",
            "List all pending tasks"
        };

        ChoiceDialog<String> dialog = new ChoiceDialog<>(suggestions[0], suggestions);
        dialog.setTitle("Quick Assignment Commands");
        dialog.setHeaderText("Choose a team management command:");
        dialog.setContentText("Select a command:");

        dialog.showAndWait().ifPresent(suggestion -> {
            messageInput.setText(suggestion);
            sendMessage();
        });
    }

    /**
     * Show team status
     */
    @FXML
    private void showTeamStatus() {
        messageInput.setText("Show team status");
        sendMessage();
    }

    /**
     * Show help information
     */
    @FXML
    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Admin AI Assistant Help");
        alert.setHeaderText("How to use the Admin AI Assistant");
        alert.setContentText("""
            The Admin AI Assistant can help you with:

            📋 Task Management:
            • "Create task 'Title' for username with priority priority"
            • "Assign 'Task' to username due date"

            👥 Team Management:
            • "Show team status" - View all team members
            • "Team overview" - Get productivity summary

            ⚡ Quick Commands:
            • Use natural language for task creation
            • Mention usernames directly
            • Specify priorities (low, medium, high, urgent)
            • Use dates like 'today', 'tomorrow', 'friday'

            💡 Tips:
            • Be specific with task titles
            • Use quotes around task names
            • Check team status regularly
            """);

        alert.showAndWait();
    }
}
