package com.example.trying3.service;

import com.example.trying3.dao.UserDAO;
import com.example.trying3.model.User;
import com.example.trying3.util.PasswordUtil;
import com.example.trying3.util.SessionManager;

import java.time.LocalDateTime;

/**
 * Service untuk authentication dan authorization.
 */
public class AuthService {
    private final UserDAO userDAO;
    private final SessionManager sessionManager;

    public AuthService() {
        this.userDAO = new UserDAO();
        this.sessionManager = SessionManager.getInstance();
    }

    public User login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        if (password == null || password.trim().isEmpty()) {
            return null;
        }

        User user = userDAO.getUserByUsername(username);

        if (user == null) {
            return null;
        }

        if (!user.isActive()) {
            return null;
        }

        if (user.isLocked()) {
            return null;
        }

        boolean passwordMatch = PasswordUtil.verifyPassword(password, user.getPasswordHash());

        if (!passwordMatch) {
            userDAO.incrementFailedAttempts(user.getIdUser());
            user.incrementFailedAttempts();

            if (user.getFailedLoginAttempts() >= 5) {
                userDAO.lockAccount(user.getIdUser(), user.getLockedUntil());
            }

            return null;
        }

        userDAO.updateLastLogin(user.getIdUser());
        user.resetFailedAttempts();
        user.setLastLogin(LocalDateTime.now());

        sessionManager.setCurrentUser(user);

        return user;
    }

    public void logout() {
        sessionManager.clearSession();
    }

    public boolean isLoggedIn() {
        return sessionManager.isLoggedIn();
    }

    public User getCurrentUser() {
        return sessionManager.getCurrentUser();
    }

    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        User user = userDAO.getUserByUsername(
                sessionManager.getCurrentUser().getUsername()
        );

        if (user == null) return false;

        if (!PasswordUtil.verifyPassword(oldPassword, user.getPasswordHash())) {
            System.out.println("❌ Password lama salah");
            return false;
        }

        String newHash = PasswordUtil.hashPassword(newPassword);

        boolean updated = userDAO.updatePassword(userId, newHash);

        if (updated) {
            System.out.println("✅ Password berhasil diubah");
        }

        return updated;
    }
}
