package com.example.taskmanagerv3.controller;

import com.example.taskmanagerv3.model.User;
import com.example.taskmanagerv3.model.UserRole;
import com.example.taskmanagerv3.service.UserService;
import com.example.taskmanagerv3.util.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for User Management Interface
 */
public class UserManagementController {
    private static final Logger logger = LoggerFactory.getLogger(UserManagementController.class);

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> idColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> fullNameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> departmentColumn;
    @FXML private TableColumn<User, String> statusColumn;
    @FXML private TableColumn<User, String> lastLoginColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<UserRole> roleFilterComboBox;
    @FXML private CheckBox activeOnlyCheckBox;
    @FXML private Button refreshButton;
    @FXML private Button createUserButton;
    @FXML private Button editUserButton;
    @FXML private Button toggleStatusButton;
    @FXML private Button closeButton;

    @FXML private Label totalUsersLabel;
    @FXML private Label selectedUserLabel;

    private UserService userService;
    private SessionManager sessionManager;
    private ObservableList<User> allUsers;
    private ObservableList<User> filteredUsers;

    @FXML
    private void initialize() {
        userService = new UserService();
        sessionManager = SessionManager.getInstance();

        allUsers = FXCollections.observableArrayList();
        filteredUsers = FXCollections.observableArrayList();

        setupTable();
        setupFilters();
        setupButtons();
        loadData();

        logger.info("User Management interface initialized");
    }

    /**
     * Setup table columns and properties
     */
    private void setupTable() {
        // Setup columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        departmentColumn.setCellValueFactory(new PropertyValueFactory<>("department"));

        // Custom cell factories for better display
        roleColumn.setCellValueFactory(cellData -> {
            UserRole role = cellData.getValue().getRole();
            return new javafx.beans.property.SimpleStringProperty(role.getDisplayName());
        });

        statusColumn.setCellValueFactory(cellData -> {
            boolean isActive = cellData.getValue().isActive();
            return new javafx.beans.property.SimpleStringProperty(isActive ? "Active" : "Inactive");
        });

        lastLoginColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getLastLogin() != null) {
                String formattedDate = cellData.getValue().getLastLogin()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                return new javafx.beans.property.SimpleStringProperty(formattedDate);
            }
            return new javafx.beans.property.SimpleStringProperty("Never");
        });

        // Custom cell factory for status column with colors
        statusColumn.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("Active".equals(status)) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Set table data
        usersTable.setItems(filteredUsers);

        // Selection listener
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            updateSelectedUserInfo(newSelection);
            updateButtonStates();
        });

        // Double-click to edit
        usersTable.setRowFactory(tv -> {
            TableRow<User> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    editSelectedUser();
                }
            });
            return row;
        });
    }

    /**
     * Setup filters and search
     */
    private void setupFilters() {
        // Role filter
        roleFilterComboBox.getItems().add(null); // "All" option
        roleFilterComboBox.getItems().addAll(UserRole.values());
        roleFilterComboBox.setValue(null);

        // Custom cell factory for role filter
        roleFilterComboBox.setCellFactory(listView -> new ListCell<UserRole>() {
            @Override
            protected void updateItem(UserRole role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setText("All Roles");
                } else {
                    setText(role.getDisplayName());
                }
            }
        });

        roleFilterComboBox.setButtonCell(new ListCell<UserRole>() {
            @Override
            protected void updateItem(UserRole role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setText("All Roles");
                } else {
                    setText(role.getDisplayName());
                }
            }
        });

        // Active only checkbox
        activeOnlyCheckBox.setSelected(true);

        // Filter listeners
        searchField.textProperty().addListener((obs, oldText, newText) -> applyFilters());
        roleFilterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        activeOnlyCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> applyFilters());
    }

    /**
     * Setup button actions and states
     */
    private void setupButtons() {
        refreshButton.setOnAction(e -> loadData());
        createUserButton.setOnAction(e -> createNewUser());
        editUserButton.setOnAction(e -> editSelectedUser());
        toggleStatusButton.setOnAction(e -> toggleUserStatus());
        closeButton.setOnAction(e -> closeWindow());

        // Initial button states
        updateButtonStates();
    }

    /**
     * Load data from database
     */
    private void loadData() {
        new Thread(() -> {
            try {
                List<User> users = userService.getAllActiveUsers();

                Platform.runLater(() -> {
                    allUsers.clear();
                    allUsers.addAll(users);
                    applyFilters();
                    updateUserCount();
                });

            } catch (Exception e) {
                logger.error("Error loading user data", e);
                Platform.runLater(() -> {
                    showAlert("Error", "Failed to load user data: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Apply search and filters
     */
    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase().trim();
        UserRole roleFilter = roleFilterComboBox.getValue();
        boolean activeOnly = activeOnlyCheckBox.isSelected();

        List<User> filtered = allUsers.stream()
            .filter(user -> {
                // Active filter
                return !activeOnly || user.isActive();
            })
            .filter(user -> {
                // Search filter
                if (!searchText.isEmpty()) {
                    return user.getUsername().toLowerCase().contains(searchText) ||
                           user.getFullName().toLowerCase().contains(searchText) ||
                           user.getEmail().toLowerCase().contains(searchText) ||
                           (user.getDepartment() != null && user.getDepartment().toLowerCase().contains(searchText));
                }
                return true;
            })
            .filter(user -> {
                // Role filter
                return roleFilter == null || user.getRole() == roleFilter;
            })
            .collect(Collectors.toList());

        filteredUsers.clear();
        filteredUsers.addAll(filtered);
        updateUserCount();
    }

    /**
     * Update user count label
     */
    private void updateUserCount() {
        totalUsersLabel.setText(String.format("Total: %d users", filteredUsers.size()));
    }

    /**
     * Update selected user info
     */
    private void updateSelectedUserInfo(User user) {
        if (user != null) {
            selectedUserLabel.setText(String.format("Selected: %s (%s)",
                user.getFullName(), user.getUsername()));
        } else {
            selectedUserLabel.setText("No user selected");
        }
    }

    /**
     * Update button states based on selection
     */
    private void updateButtonStates() {
        User selectedUser = usersTable.getSelectionModel().getSelectedItem();
        boolean hasSelection = selectedUser != null;
        boolean canModify = hasSelection && selectedUser.getUserId() != sessionManager.getCurrentUserId();

        editUserButton.setDisable(!hasSelection);
        toggleStatusButton.setDisable(!canModify);

        if (hasSelection && selectedUser.getUserId() == sessionManager.getCurrentUserId()) {
            toggleStatusButton.setText("Cannot modify self");
        } else if (hasSelection) {
            toggleStatusButton.setText(selectedUser.isActive() ? "Deactivate User" : "Activate User");
        } else {
            toggleStatusButton.setText("Toggle Status");
        }
    }

    /**
     * Create new user
     */
    @FXML
    private void createNewUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/taskmanagerv3/create-user-dialog.fxml"));
            Parent dialogContent = loader.load();

            CreateUserController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Create New User");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(createUserButton.getScene().getWindow());
            dialogStage.setScene(new Scene(dialogContent, 500, 600));
            dialogStage.setResizable(false);
            dialogStage.centerOnScreen();

            dialogStage.showAndWait();

            if (controller.isUserCreated()) {
                loadData();
                showAlert("Success", "User created successfully!");
            }

        } catch (IOException e) {
            logger.error("Failed to open create user dialog", e);
            showAlert("Error", "Failed to open create user dialog: " + e.getMessage());
        }
    }

    /**
     * Edit selected user
     */
    @FXML
    private void editSelectedUser() {
        User selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert("No Selection", "Please select a user to edit");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/taskmanagerv3/edit-user-dialog.fxml"));
            VBox dialogContent = loader.load();

            EditUserController controller = loader.getController();
            controller.setUser(selectedUser);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit User - " + selectedUser.getUsername());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(editUserButton.getScene().getWindow());
            dialogStage.setScene(new Scene(dialogContent, 500, 650));
            dialogStage.setResizable(false);
            dialogStage.centerOnScreen();

            dialogStage.showAndWait();

            if (controller.isUserUpdated()) {
                loadData();
                showAlert("Success", "User updated successfully!");
            }

        } catch (IOException e) {
            logger.error("Failed to open edit user dialog", e);
            showAlert("Error", "Failed to open edit user dialog: " + e.getMessage());
        }
    }

    /**
     * Toggle user status (activate/deactivate)
     */
    @FXML
    private void toggleUserStatus() {
        User selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert("No Selection", "Please select a user to modify");
            return;
        }

        if (selectedUser.getUserId() == sessionManager.getCurrentUserId()) {
            showAlert("Cannot Modify", "You cannot modify your own account status");
            return;
        }

        String action = selectedUser.isActive() ? "deactivate" : "activate";
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Action");
        confirmAlert.setHeaderText("Are you sure you want to " + action + " this user?");
        confirmAlert.setContentText("User: " + selectedUser.getFullName() + " (" + selectedUser.getUsername() + ")");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            selectedUser.setActive(!selectedUser.isActive());

            if (userService.updateUser(selectedUser)) {
                loadData();
                showAlert("Success", "User status updated successfully!");
            } else {
                selectedUser.setActive(!selectedUser.isActive()); // Revert change
                showAlert("Error", "Failed to update user status. Please try again.");
            }
        }
    }

    /**
     * Close window
     */
    @FXML
    private void closeWindow() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
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
}
