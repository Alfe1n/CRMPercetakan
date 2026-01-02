package com.example.trying3.util;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility class untuk handle password hashing dengan BCrypt
 */
public class PasswordUtil {

    /**
     * Hash password menggunakan BCrypt
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));
    }

    /**
     * Verify password dengan hash yang tersimpan di database
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword){
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            System.err.println("Error verifying password: " + e.getMessage());
            return false;
        }
    }

    /**
     * Generate random password (untuk reset password)
     */
    public static String generateRandomPassword(){
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 12; i++){
            int index = (int) (Math.random() * chars.length());;
            password.append(chars.charAt(index));
        }
        return password.toString();
    }
}
