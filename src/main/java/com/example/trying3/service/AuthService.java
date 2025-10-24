package com.example.trying3.service;

import com.example.trying3.dao.UserDAO;
import com.example.trying3.model.User;
import com.example.trying3.util.PasswordUtil;
import com.example.trying3.util.SessionManager;

import java.time.LocalDateTime;

/**
 * Service untuk handle authentication & authorization
 */
public class AuthService {
    private final UserDAO userDAO;
    private final SessionManager sessionManager;

    public AuthService() {
        this.userDAO = new UserDAO();
        this.sessionManager = SessionManager.getInstance();
    }

    /**
     * Login user
     * @return User object jika berhasil, null jika gagal
     */
    public User login(String username, String password) {
        // Validasi input
        if (username == null || username.trim().isEmpty()) {
            System.out.println("❌ Username kosong");
            return null;
        }

        if (password == null || password.trim().isEmpty()) {
            System.out.println("❌ Password kosong");
            return null;
        }

        // Cari user di database
        User user = userDAO.getUserByUsername(username);

        if (user == null) {
            System.out.println("❌ User tidak ditemukan: " + username);
            return null;
        }

        // Check apakah akun aktif
        if (!user.isActive()) {
            System.out.println("❌ Akun tidak aktif: " + username);
            return null;
        }

        // Check apakah akun terkunci
        if (user.isLocked()) {
            System.out.println("❌ Akun terkunci: " + username);
            return null;
        }

        // Verify password
        boolean passwordMatch = PasswordUtil.verifyPassword(password, user.getPasswordHash());

        if (!passwordMatch) {
            System.out.println("❌ Password salah untuk user: " + username);

            // Increment failed attempts
            userDAO.incrementFailedAttempts(user.getIdUser());
            user.incrementFailedAttempts();

            // Lock account jika sudah 5x gagal
            if (user.getFailedLoginAttempts() >= 5) {
                userDAO.lockAccount(user.getIdUser(), user.getLockedUntil());
                System.out.println("🔒 Akun dikunci sampai: " + user.getLockedUntil());
            }

            return null;
        }

        // Login berhasil
        System.out.println("✅ Login berhasil: " + username);

        // Update last login di database
        userDAO.updateLastLogin(user.getIdUser());
        user.resetFailedAttempts();
        user.setLastLogin(LocalDateTime.now());

        // Set session
        sessionManager.setCurrentUser(user);

        return user;
    }

    /**
     * Logout user
     */
    public void logout() {
        sessionManager.clearSession();
    }

    /**
     * Check apakah user sedang login
     */
    public boolean isLoggedIn() {
        return sessionManager.isLoggedIn();
    }

    /**
     * Get current logged in user
     */
    public User getCurrentUser() {
        return sessionManager.getCurrentUser();
    }

    /**
     * Change password
     */
    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        User user = userDAO.getUserByUsername(
                sessionManager.getCurrentUser().getUsername()
        );

        if (user == null) return false;

        // Verify old password
        if (!PasswordUtil.verifyPassword(oldPassword, user.getPasswordHash())) {
            System.out.println("❌ Password lama salah");
            return false;
        }

        // Hash new password
        String newHash = PasswordUtil.hashPassword(newPassword);

        // Update di database
        boolean updated = userDAO.updatePassword(userId, newHash);

        if (updated) {
            System.out.println("✅ Password berhasil diubah");
        }

        return updated;
    }
}
