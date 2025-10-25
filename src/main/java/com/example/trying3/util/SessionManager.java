package com.example.trying3.util;

import com.example.trying3.model.User;
import java.time.LocalDateTime;

/**
 * Singleton class untuk manage session user yang login
 */
public class SessionManager {
    private static SessionManager instance;
    private User currentUser;
    private LocalDateTime loginTime;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.loginTime = LocalDateTime.now();
        System.out.println("✅ User logged in: " + user.getUsername() +
                " (Role: " + user.getIdRole() + ")");
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public void clearSession() {
        if (currentUser != null) {
            System.out.println("🔴 User logged out: " + currentUser.getUsername());
        }
        this.currentUser = null;
        this.loginTime = null;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public long getSessionDurationMinutes() {
        if (loginTime == null) return 0;
        return java.time.Duration.between(loginTime, LocalDateTime.now()).toMinutes();
    }

    // Helper methods
    public int getCurrentUserId() {
        return currentUser != null ? currentUser.getIdUser() : -1;
    }

    public String getCurrentUsername() {
        return currentUser != null ? currentUser.getUsername() : "Guest";
    }

    public String getCurrentUserFullName() {
        return currentUser != null ? currentUser.getNamaLengkap() : "Guest";
    }

    public int getCurrentUserRole() {
        return currentUser != null ? currentUser.getIdRole() : -1;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.getIdRole() == 1;
    }

    public boolean isDesigner() {
        return currentUser != null && currentUser.getIdRole() == 2;
    }

    public boolean isOperator() {
        return currentUser != null && currentUser.getIdRole() == 3;
    }

    public boolean isManager() {
        return currentUser != null && currentUser.getIdRole() == 4;
    }
}
