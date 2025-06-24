package com.example.taskmanagerv3.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database configuration and connection management
 */
public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    
    // Database connection parameters
    private static final String SERVER = "localhost";
    private static final String PORT = "1433";
    private static final String DATABASE = "WorkFlowManagerDB";
    private static final String USERNAME = "sa";
    private static final String PASSWORD = "123"; // Change this to your SQL Server password
    
    private static final String CONNECTION_URL = String.format(
        "jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=false;trustServerCertificate=true",
        SERVER, PORT, DATABASE
    );
    
    private static Connection connection;
    
    /**
     * Get database connection
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                connection = DriverManager.getConnection(CONNECTION_URL, USERNAME, PASSWORD);
                logger.info("Database connection established successfully");
            } catch (ClassNotFoundException e) {
                logger.error("SQL Server JDBC Driver not found", e);
                throw new SQLException("SQL Server JDBC Driver not found", e);
            } catch (SQLException e) {
                logger.error("Failed to connect to database", e);
                throw e;
            }
        }
        return connection;
    }
    
    /**
     * Close database connection
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Database connection closed");
            } catch (SQLException e) {
                logger.error("Error closing database connection", e);
            }
        }
    }
    
    /**
     * Test database connection
     */
    public static boolean testConnection() {
        try (Connection testConn = getConnection()) {
            return testConn != null && !testConn.isClosed();
        } catch (SQLException e) {
            logger.error("Database connection test failed", e);
            return false;
        }
    }
    
    /**
     * Initialize database schema
     */
    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Create Users table
            String createUsersTable = """
                IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Users' AND xtype='U')
                CREATE TABLE Users (
                    user_id INT IDENTITY(1,1) PRIMARY KEY,
                    username NVARCHAR(50) UNIQUE NOT NULL,
                    email NVARCHAR(100) UNIQUE NOT NULL,
                    password_hash NVARCHAR(255) NOT NULL,
                    full_name NVARCHAR(100) NOT NULL,
                    role NVARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'USER')),
                    is_active BIT DEFAULT 1,
                    created_at DATETIME2 DEFAULT GETDATE(),
                    last_login DATETIME2,
                    profile_picture NVARCHAR(255),
                    department NVARCHAR(100),
                    phone_number NVARCHAR(20)
                )
                """;
            
            // Create Tasks table
            String createTasksTable = """
                IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Tasks' AND xtype='U')
                CREATE TABLE Tasks (
                    task_id INT IDENTITY(1,1) PRIMARY KEY,
                    title NVARCHAR(200) NOT NULL,
                    description NTEXT,
                    status NVARCHAR(20) NOT NULL CHECK (status IN ('TODO', 'IN_PROGRESS', 'REVIEW', 'COMPLETED', 'CANCELLED')),
                    priority NVARCHAR(20) NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
                    assigned_user_id INT NOT NULL,
                    created_by_user_id INT NOT NULL,
                    created_at DATETIME2 DEFAULT GETDATE(),
                    updated_at DATETIME2 DEFAULT GETDATE(),
                    due_date DATETIME2,
                    completed_at DATETIME2,
                    estimated_hours INT DEFAULT 0,
                    actual_hours INT DEFAULT 0,
                    progress_percentage DECIMAL(5,2) DEFAULT 0.00,
                    comments NTEXT,
                    attachments NTEXT,
                    project_id INT,
                    FOREIGN KEY (assigned_user_id) REFERENCES Users(user_id),
                    FOREIGN KEY (created_by_user_id) REFERENCES Users(user_id)
                )
                """;
            
            // Execute table creation
            stmt.execute(createUsersTable);
            stmt.execute(createTasksTable);
            
            logger.info("Database schema initialized successfully");
            
        } catch (SQLException e) {
            logger.error("Failed to initialize database schema", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    /**
     * Create default admin user if not exists
     */
    public static void createDefaultAdmin() {
        String checkAdminQuery = "SELECT COUNT(*) FROM Users WHERE role = 'ADMIN'";
        String insertAdminQuery = """
            INSERT INTO Users (username, email, password_hash, full_name, role, department)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = getConnection();
             var checkStmt = conn.prepareStatement(checkAdminQuery)) {
            
            var rs = checkStmt.executeQuery();
            rs.next();
            int adminCount = rs.getInt(1);
            
            if (adminCount == 0) {
                try (var insertStmt = conn.prepareStatement(insertAdminQuery)) {
                    // Default admin credentials (password: admin123)
                    String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw("admin123", org.mindrot.jbcrypt.BCrypt.gensalt());
                    
                    insertStmt.setString(1, "admin");
                    insertStmt.setString(2, "admin@workflowmanager.com");
                    insertStmt.setString(3, hashedPassword);
                    insertStmt.setString(4, "System Administrator");
                    insertStmt.setString(5, "ADMIN");
                    insertStmt.setString(6, "IT Department");
                    
                    insertStmt.executeUpdate();
                    logger.info("Default admin user created successfully");
                }
            }
            
        } catch (SQLException e) {
            logger.error("Failed to create default admin user", e);
        }
    }
}
