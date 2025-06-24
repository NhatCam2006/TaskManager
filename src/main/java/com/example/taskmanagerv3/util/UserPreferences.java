package com.example.taskmanagerv3.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.prefs.Preferences;

/**
 * Utility class for managing user preferences including remember me functionality
 */
public class UserPreferences {
    private static final Logger logger = LoggerFactory.getLogger(UserPreferences.class);
    private static final String PREF_NODE = "com.example.taskmanagerv3";
    private static final String REMEMBER_ME_KEY = "rememberMe";
    private static final String SAVED_USERNAME_KEY = "savedUsername";
    private static final String SAVED_PASSWORD_KEY = "savedPassword";
    private static final String ENCRYPTION_KEY_FILE = "app.key";

    private static final Preferences prefs = Preferences.userNodeForPackage(UserPreferences.class);
    private static SecretKey encryptionKey;

    static {
        initializeEncryptionKey();
    }

    /**
     * Initialize or load encryption key for password storage
     */
    private static void initializeEncryptionKey() {
        try {
            Path keyPath = Paths.get(System.getProperty("user.home"), ".taskmanager", ENCRYPTION_KEY_FILE);

            if (Files.exists(keyPath)) {
                // Load existing key
                byte[] keyBytes = Files.readAllBytes(keyPath);
                encryptionKey = new SecretKeySpec(keyBytes, "AES");
                logger.debug("Encryption key loaded successfully");
            } else {
                // Generate new key
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256);
                encryptionKey = keyGen.generateKey();

                // Save key to file
                Files.createDirectories(keyPath.getParent());
                Files.write(keyPath, encryptionKey.getEncoded());
                logger.debug("New encryption key generated and saved");
            }
        } catch (Exception e) {
            logger.error("Failed to initialize encryption key", e);
            // Fallback to a default key (not recommended for production)
            encryptionKey = new SecretKeySpec("MySecretKey12345".getBytes(), "AES");
        }
    }

    /**
     * Save remember me credentials
     */
    public static void saveRememberMeCredentials(String username, String password) {
        try {
            prefs.putBoolean(REMEMBER_ME_KEY, true);
            prefs.put(SAVED_USERNAME_KEY, username);

            // Encrypt password before storing
            String encryptedPassword = encrypt(password);
            prefs.put(SAVED_PASSWORD_KEY, encryptedPassword);

            prefs.flush();
            logger.info("Remember me credentials saved for user: {}", username);
        } catch (Exception e) {
            logger.error("Failed to save remember me credentials", e);
        }
    }

    /**
     * Get saved username if remember me is enabled
     */
    public static String getSavedUsername() {
        if (isRememberMeEnabled()) {
            return prefs.get(SAVED_USERNAME_KEY, "");
        }
        return "";
    }

    /**
     * Get saved password if remember me is enabled
     */
    public static String getSavedPassword() {
        if (isRememberMeEnabled()) {
            try {
                String encryptedPassword = prefs.get(SAVED_PASSWORD_KEY, "");
                if (!encryptedPassword.isEmpty()) {
                    return decrypt(encryptedPassword);
                }
            } catch (Exception e) {
                logger.error("Failed to decrypt saved password", e);
            }
        }
        return "";
    }

    /**
     * Check if remember me is enabled
     */
    public static boolean isRememberMeEnabled() {
        return prefs.getBoolean(REMEMBER_ME_KEY, false);
    }

    /**
     * Clear remember me credentials
     */
    public static void clearRememberMeCredentials() {
        prefs.remove(REMEMBER_ME_KEY);
        prefs.remove(SAVED_USERNAME_KEY);
        prefs.remove(SAVED_PASSWORD_KEY);
        try {
            prefs.flush();
            logger.info("Remember me credentials cleared");
        } catch (Exception e) {
            logger.error("Failed to clear remember me credentials", e);
        }
    }

    /**
     * Encrypt text using AES
     */
    private static String encrypt(String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * Decrypt text using AES
     */
    private static String decrypt(String encryptedText) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decryptedBytes);
    }

    /**
     * Save window position and size
     */
    public static void saveWindowBounds(double x, double y, double width, double height) {
        prefs.putDouble("windowX", x);
        prefs.putDouble("windowY", y);
        prefs.putDouble("windowWidth", width);
        prefs.putDouble("windowHeight", height);
        try {
            prefs.flush();
        } catch (Exception e) {
            logger.error("Failed to save window bounds", e);
        }
    }

    /**
     * Get saved window X position
     */
    public static double getWindowX() {
        return prefs.getDouble("windowX", -1);
    }

    /**
     * Get saved window Y position
     */
    public static double getWindowY() {
        return prefs.getDouble("windowY", -1);
    }

    /**
     * Get saved window width
     */
    public static double getWindowWidth() {
        return prefs.getDouble("windowWidth", 1200);
    }

    /**
     * Get saved window height
     */
    public static double getWindowHeight() {
        return prefs.getDouble("windowHeight", 800);
    }
}
