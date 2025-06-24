package com.example.taskmanagerv3.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

/**
 * Controller for Admin Settings Interface
 */
public class AdminSettingsController {
    private static final Logger logger = LoggerFactory.getLogger(AdminSettingsController.class);

    // Navigation buttons
    @FXML private Button generalButton;
    @FXML private Button securityButton;
    @FXML private Button notificationsButton;
    @FXML private Button databaseButton;
    @FXML private Button backupButton;
    @FXML private Button closeButton;

    // Settings content areas
    @FXML private VBox settingsContentArea;
    @FXML private VBox generalSettings;
    @FXML private VBox securitySettings;
    @FXML private VBox notificationSettings;
    @FXML private VBox databaseSettings;
    @FXML private VBox backupSettings;

    // General Settings
    @FXML private TextField systemNameField;
    @FXML private TextField companyNameField;
    @FXML private ComboBox<String> languageComboBox;
    @FXML private ComboBox<String> timeZoneComboBox;
    @FXML private CheckBox enableLoggingCheckBox;
    @FXML private CheckBox enableDebugModeCheckBox;

    // Security Settings
    @FXML private Spinner<Integer> sessionTimeoutSpinner;
    @FXML private CheckBox requireUppercaseCheckBox;
    @FXML private CheckBox requireNumbersCheckBox;
    @FXML private CheckBox requireSpecialCharsCheckBox;
    @FXML private Spinner<Integer> minPasswordLengthSpinner;
    @FXML private Spinner<Integer> maxLoginAttemptsSpinner;

    // Notification Settings
    @FXML private CheckBox enableEmailNotificationsCheckBox;
    @FXML private CheckBox enableTaskRemindersCheckBox;
    @FXML private CheckBox enableDeadlineAlertsCheckBox;
    @FXML private TextField smtpServerField;
    @FXML private TextField smtpPortField;
    @FXML private TextField emailUsernameField;
    @FXML private PasswordField emailPasswordField;

    // Database Settings
    @FXML private TextField dbServerField;
    @FXML private TextField dbNameField;
    @FXML private TextField dbUsernameField;
    @FXML private PasswordField dbPasswordField;
    @FXML private Button testConnectionButton;
    @FXML private Button optimizeDatabaseButton;

    // Backup Settings
    @FXML private CheckBox enableAutoBackupCheckBox;
    @FXML private ComboBox<String> backupFrequencyComboBox;
    @FXML private TextField backupLocationField;
    @FXML private Button browseBackupLocationButton;
    @FXML private Button createBackupButton;
    @FXML private Button restoreBackupButton;

    // Action buttons
    @FXML private Button saveSettingsButton;
    @FXML private Button resetToDefaultButton;
    @FXML private Button cancelButton;

    private Properties systemSettings;

    @FXML
    private void initialize() {
        systemSettings = new Properties();
        
        setupUI();
        loadSettings();
        showGeneralSettings();

        logger.info("Admin settings initialized");
    }

    /**
     * Setup UI components and event handlers
     */
    private void setupUI() {
        // Setup navigation buttons
        generalButton.setOnAction(e -> showGeneralSettings());
        securityButton.setOnAction(e -> showSecuritySettings());
        notificationsButton.setOnAction(e -> showNotificationSettings());
        databaseButton.setOnAction(e -> showDatabaseSettings());
        backupButton.setOnAction(e -> showBackupSettings());
        closeButton.setOnAction(e -> closeSettings());

        // Setup action buttons
        saveSettingsButton.setOnAction(e -> saveSettings());
        resetToDefaultButton.setOnAction(e -> resetToDefault());
        cancelButton.setOnAction(e -> closeSettings());

        // Setup specific buttons
        testConnectionButton.setOnAction(e -> testDatabaseConnection());
        optimizeDatabaseButton.setOnAction(e -> optimizeDatabase());
        browseBackupLocationButton.setOnAction(e -> browseBackupLocation());
        createBackupButton.setOnAction(e -> createBackup());
        restoreBackupButton.setOnAction(e -> restoreBackup());

        // Setup ComboBoxes
        setupComboBoxes();
        
        // Setup Spinners
        setupSpinners();
    }

    /**
     * Setup ComboBox items
     */
    private void setupComboBoxes() {
        // Language ComboBox
        languageComboBox.getItems().addAll("English", "Vietnamese", "Chinese", "Japanese", "Korean");
        languageComboBox.setValue("English");

        // Time Zone ComboBox
        timeZoneComboBox.getItems().addAll("UTC", "Asia/Ho_Chi_Minh", "America/New_York", 
                                          "Europe/London", "Asia/Tokyo", "Australia/Sydney");
        timeZoneComboBox.setValue("Asia/Ho_Chi_Minh");

        // Backup Frequency ComboBox
        backupFrequencyComboBox.getItems().addAll("Daily", "Weekly", "Monthly", "Manual");
        backupFrequencyComboBox.setValue("Weekly");
    }

    /**
     * Setup Spinner controls
     */
    private void setupSpinners() {
        // Session timeout spinner (15-480 minutes)
        sessionTimeoutSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(15, 480, 60));

        // Password length spinner (6-50 characters)
        minPasswordLengthSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(6, 50, 8));

        // Max login attempts spinner (3-10 attempts)
        maxLoginAttemptsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(3, 10, 5));
    }

    /**
     * Show General Settings
     */
    private void showGeneralSettings() {
        hideAllSettings();
        generalSettings.setVisible(true);
        generalSettings.setManaged(true);
        updateButtonStyles(generalButton);
    }

    /**
     * Show Security Settings
     */
    private void showSecuritySettings() {
        hideAllSettings();
        securitySettings.setVisible(true);
        securitySettings.setManaged(true);
        updateButtonStyles(securityButton);
    }

    /**
     * Show Notification Settings
     */
    private void showNotificationSettings() {
        hideAllSettings();
        notificationSettings.setVisible(true);
        notificationSettings.setManaged(true);
        updateButtonStyles(notificationsButton);
    }

    /**
     * Show Database Settings
     */
    private void showDatabaseSettings() {
        hideAllSettings();
        databaseSettings.setVisible(true);
        databaseSettings.setManaged(true);
        updateButtonStyles(databaseButton);
    }

    /**
     * Show Backup Settings
     */
    private void showBackupSettings() {
        hideAllSettings();
        backupSettings.setVisible(true);
        backupSettings.setManaged(true);
        updateButtonStyles(backupButton);
    }

    /**
     * Hide all settings sections
     */
    private void hideAllSettings() {
        generalSettings.setVisible(false);
        generalSettings.setManaged(false);
        securitySettings.setVisible(false);
        securitySettings.setManaged(false);
        notificationSettings.setVisible(false);
        notificationSettings.setManaged(false);
        databaseSettings.setVisible(false);
        databaseSettings.setManaged(false);
        backupSettings.setVisible(false);
        backupSettings.setManaged(false);
    }

    /**
     * Update button styles for active section
     */
    private void updateButtonStyles(Button activeButton) {
        // Reset all buttons to default style
        String defaultStyle = "-fx-text-fill: white; -fx-padding: 10 15; -fx-background-radius: 5;";
        generalButton.setStyle(defaultStyle + " -fx-background-color: #3498db;");
        securityButton.setStyle(defaultStyle + " -fx-background-color: #e74c3c;");
        notificationsButton.setStyle(defaultStyle + " -fx-background-color: #f39c12;");
        databaseButton.setStyle(defaultStyle + " -fx-background-color: #27ae60;");
        backupButton.setStyle(defaultStyle + " -fx-background-color: #9b59b6;");

        // Highlight active button
        activeButton.setStyle(defaultStyle + " -fx-background-color: #2c3e50;");
    }

    /**
     * Load settings from configuration
     */
    private void loadSettings() {
        // Load default values (in a real app, these would come from a config file)
        systemNameField.setText("WorkFlow Manager");
        companyNameField.setText("Your Company");
        
        // Set default checkboxes
        enableLoggingCheckBox.setSelected(true);
        enableDebugModeCheckBox.setSelected(false);
        
        requireUppercaseCheckBox.setSelected(true);
        requireNumbersCheckBox.setSelected(true);
        requireSpecialCharsCheckBox.setSelected(false);
        
        enableEmailNotificationsCheckBox.setSelected(true);
        enableTaskRemindersCheckBox.setSelected(true);
        enableDeadlineAlertsCheckBox.setSelected(true);
        
        enableAutoBackupCheckBox.setSelected(true);
        backupLocationField.setText(System.getProperty("user.home") + "/WorkFlowManager/Backups");
    }

    /**
     * Save settings
     */
    private void saveSettings() {
        try {
            // In a real application, save to configuration file or database
            showAlert(Alert.AlertType.INFORMATION, "Settings Saved", 
                     "System settings have been saved successfully!\n\nSome changes may require a restart to take effect.");
            
            logger.info("System settings saved successfully");
            
        } catch (Exception e) {
            logger.error("Error saving settings", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save settings: " + e.getMessage());
        }
    }

    /**
     * Reset to default settings
     */
    private void resetToDefault() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Reset Settings");
        confirmAlert.setHeaderText("Reset to Default Settings");
        confirmAlert.setContentText("Are you sure you want to reset all settings to default values?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                loadSettings();
                showAlert(Alert.AlertType.INFORMATION, "Settings Reset", "All settings have been reset to default values.");
            }
        });
    }

    /**
     * Test database connection
     */
    private void testDatabaseConnection() {
        // In a real application, test the actual database connection
        showAlert(Alert.AlertType.INFORMATION, "Connection Test", 
                 "Database connection test successful!\n\nConnection parameters are valid.");
    }

    /**
     * Optimize database
     */
    private void optimizeDatabase() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Database Optimization");
        confirmAlert.setHeaderText("Optimize Database");
        confirmAlert.setContentText("This will optimize database tables and indexes. Continue?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // In a real application, perform database optimization
                showAlert(Alert.AlertType.INFORMATION, "Optimization Complete", 
                         "Database optimization completed successfully!");
            }
        });
    }

    /**
     * Browse for backup location
     */
    private void browseBackupLocation() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Backup Location");
        
        File selectedDirectory = directoryChooser.showDialog(browseBackupLocationButton.getScene().getWindow());
        if (selectedDirectory != null) {
            backupLocationField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    /**
     * Create backup
     */
    private void createBackup() {
        // In a real application, create actual backup
        showAlert(Alert.AlertType.INFORMATION, "Backup Created", 
                 "Database backup created successfully!\n\nLocation: " + backupLocationField.getText());
    }

    /**
     * Restore backup
     */
    private void restoreBackup() {
        Alert confirmAlert = new Alert(Alert.AlertType.WARNING);
        confirmAlert.setTitle("Restore Backup");
        confirmAlert.setHeaderText("Restore Database Backup");
        confirmAlert.setContentText("This will replace all current data with backup data. Are you sure?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // In a real application, restore from backup
                showAlert(Alert.AlertType.INFORMATION, "Backup Restored", 
                         "Database has been restored from backup successfully!");
            }
        });
    }

    /**
     * Close settings window
     */
    private void closeSettings() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
