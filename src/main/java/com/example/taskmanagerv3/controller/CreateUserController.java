package com.example.taskmanagerv3.controller;

import com.example.taskmanagerv3.model.User;
import com.example.taskmanagerv3.model.UserRole;
import com.example.taskmanagerv3.service.UserService;
import com.example.taskmanagerv3.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for Create User Dialog
 */
public class CreateUserController {
    private static final Logger logger = LoggerFactory.getLogger(CreateUserController.class);
    
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField fullNameField;
    @FXML private ComboBox<UserRole> roleComboBox;
    @FXML private TextField departmentField;
    @FXML private TextField phoneNumberField;
    @FXML private Button createButton;
    @FXML private Button cancelButton;
    
    @FXML private Label usernameValidationLabel;
    @FXML private Label emailValidationLabel;
    @FXML private Label passwordValidationLabel;
    
    private UserService userService;
    private SessionManager sessionManager;
    private boolean userCreated = false;
    
    @FXML
    private void initialize() {
        userService = new UserService();
        sessionManager = SessionManager.getInstance();
        
        setupUI();
        setupValidation();
        
        logger.info("Create User dialog initialized");
    }
    
    /**
     * Setup UI components
     */
    private void setupUI() {
        // Setup role combo box
        roleComboBox.getItems().addAll(UserRole.values());
        roleComboBox.setValue(UserRole.USER);
        
        // Setup button actions
        createButton.setOnAction(e -> handleCreateUser());
        cancelButton.setOnAction(e -> handleCancel());
        
        // Initial validation
        validateForm();
    }
    
    /**
     * Setup real-time validation
     */
    private void setupValidation() {
        // Username validation
        usernameField.textProperty().addListener((obs, oldText, newText) -> {
            validateUsername();
            validateForm();
        });
        
        // Email validation
        emailField.textProperty().addListener((obs, oldText, newText) -> {
            validateEmail();
            validateForm();
        });
        
        // Password validation
        passwordField.textProperty().addListener((obs, oldText, newText) -> {
            validatePassword();
            validateForm();
        });
        
        confirmPasswordField.textProperty().addListener((obs, oldText, newText) -> {
            validatePassword();
            validateForm();
        });
        
        // Other fields
        fullNameField.textProperty().addListener((obs, oldText, newText) -> validateForm());
        roleComboBox.valueProperty().addListener((obs, oldValue, newValue) -> validateForm());
    }
    
    /**
     * Validate username
     */
    private void validateUsername() {
        String username = usernameField.getText().trim();
        
        if (username.isEmpty()) {
            usernameValidationLabel.setText("Username is required");
            usernameValidationLabel.setStyle("-fx-text-fill: red;");
            usernameValidationLabel.setVisible(true);
            return;
        }
        
        if (username.length() < 3) {
            usernameValidationLabel.setText("Username must be at least 3 characters");
            usernameValidationLabel.setStyle("-fx-text-fill: red;");
            usernameValidationLabel.setVisible(true);
            return;
        }
        
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            usernameValidationLabel.setText("Username can only contain letters, numbers, and underscore");
            usernameValidationLabel.setStyle("-fx-text-fill: red;");
            usernameValidationLabel.setVisible(true);
            return;
        }
        
        // Check if username exists (in background)
        new Thread(() -> {
            if (userService.usernameExists(username)) {
                javafx.application.Platform.runLater(() -> {
                    usernameValidationLabel.setText("Username already exists");
                    usernameValidationLabel.setStyle("-fx-text-fill: red;");
                    usernameValidationLabel.setVisible(true);
                });
            } else {
                javafx.application.Platform.runLater(() -> {
                    usernameValidationLabel.setText("✓ Username available");
                    usernameValidationLabel.setStyle("-fx-text-fill: green;");
                    usernameValidationLabel.setVisible(true);
                });
            }
        }).start();
    }
    
    /**
     * Validate email
     */
    private void validateEmail() {
        String email = emailField.getText().trim();
        
        if (email.isEmpty()) {
            emailValidationLabel.setText("Email is required");
            emailValidationLabel.setStyle("-fx-text-fill: red;");
            emailValidationLabel.setVisible(true);
            return;
        }
        
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            emailValidationLabel.setText("Invalid email format");
            emailValidationLabel.setStyle("-fx-text-fill: red;");
            emailValidationLabel.setVisible(true);
            return;
        }
        
        // Check if email exists (in background)
        new Thread(() -> {
            if (userService.emailExists(email)) {
                javafx.application.Platform.runLater(() -> {
                    emailValidationLabel.setText("Email already exists");
                    emailValidationLabel.setStyle("-fx-text-fill: red;");
                    emailValidationLabel.setVisible(true);
                });
            } else {
                javafx.application.Platform.runLater(() -> {
                    emailValidationLabel.setText("✓ Email available");
                    emailValidationLabel.setStyle("-fx-text-fill: green;");
                    emailValidationLabel.setVisible(true);
                });
            }
        }).start();
    }
    
    /**
     * Validate password
     */
    private void validatePassword() {
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        if (password.isEmpty()) {
            passwordValidationLabel.setText("Password is required");
            passwordValidationLabel.setStyle("-fx-text-fill: red;");
            passwordValidationLabel.setVisible(true);
            return;
        }
        
        if (password.length() < 6) {
            passwordValidationLabel.setText("Password must be at least 6 characters");
            passwordValidationLabel.setStyle("-fx-text-fill: red;");
            passwordValidationLabel.setVisible(true);
            return;
        }
        
        if (!confirmPassword.isEmpty() && !password.equals(confirmPassword)) {
            passwordValidationLabel.setText("Passwords do not match");
            passwordValidationLabel.setStyle("-fx-text-fill: red;");
            passwordValidationLabel.setVisible(true);
            return;
        }
        
        if (!confirmPassword.isEmpty() && password.equals(confirmPassword)) {
            passwordValidationLabel.setText("✓ Passwords match");
            passwordValidationLabel.setStyle("-fx-text-fill: green;");
            passwordValidationLabel.setVisible(true);
            return;
        }
        
        passwordValidationLabel.setVisible(false);
    }
    
    /**
     * Validate entire form
     */
    private void validateForm() {
        boolean isValid = !usernameField.getText().trim().isEmpty() &&
                         !emailField.getText().trim().isEmpty() &&
                         !passwordField.getText().isEmpty() &&
                         !confirmPasswordField.getText().isEmpty() &&
                         passwordField.getText().equals(confirmPasswordField.getText()) &&
                         !fullNameField.getText().trim().isEmpty() &&
                         roleComboBox.getValue() != null &&
                         usernameField.getText().length() >= 3 &&
                         passwordField.getText().length() >= 6 &&
                         emailField.getText().matches("^[A-Za-z0-9+_.-]+@(.+)$");
        
        createButton.setDisable(!isValid);
    }
    
    /**
     * Handle create user
     */
    @FXML
    private void handleCreateUser() {
        try {
            // Final validation
            if (!validateFinalForm()) {
                return;
            }
            
            // Create user object
            User user = new User();
            user.setUsername(usernameField.getText().trim());
            user.setEmail(emailField.getText().trim());
            user.setFullName(fullNameField.getText().trim());
            user.setRole(roleComboBox.getValue());
            user.setDepartment(departmentField.getText().trim());
            user.setPhoneNumber(phoneNumberField.getText().trim());
            
            String password = passwordField.getText();
            
            // Create user
            if (userService.createUser(user, password)) {
                userCreated = true;
                showAlert("Success", "User created successfully!");
                closeDialog();
            } else {
                showAlert("Error", "Failed to create user. Please try again.");
            }
            
        } catch (Exception e) {
            logger.error("Error creating user", e);
            showAlert("Error", "Failed to create user: " + e.getMessage());
        }
    }
    
    /**
     * Final form validation before submission
     */
    private boolean validateFinalForm() {
        // Check username availability
        if (userService.usernameExists(usernameField.getText().trim())) {
            showAlert("Validation Error", "Username already exists. Please choose a different username.");
            return false;
        }
        
        // Check email availability
        if (userService.emailExists(emailField.getText().trim())) {
            showAlert("Validation Error", "Email already exists. Please use a different email address.");
            return false;
        }
        
        return true;
    }
    
    /**
     * Handle cancel
     */
    @FXML
    private void handleCancel() {
        closeDialog();
    }
    
    /**
     * Close dialog
     */
    private void closeDialog() {
        Stage stage = (Stage) createButton.getScene().getWindow();
        stage.close();
    }
    
    /**
     * Show alert dialog
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Check if user was created
     */
    public boolean isUserCreated() {
        return userCreated;
    }
}
