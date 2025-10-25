package com.example.trying3.model;

import java.time.LocalDateTime;

public class User {
    // Primary Key
    private int idUser;

    // Credentials
    private String username;
    private String passwordHash;

    // Personal Info
    private String namaLengkap;
    private String email;

    // Role & Status
    private int idRole;
    private boolean isActive;

    // Security & Tracking
    private LocalDateTime lastLogin;
    private int failedLoginAttempts;
    private LocalDateTime lockedUntil;

    // Audit Trail
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor
    public User() {}

    public User(int idUser, String username, String namaLengkap, String email,
                int idRole, boolean isActive) {
        this.idUser = idUser;
        this.username = username;
        this.namaLengkap = namaLengkap;
        this.email = email;
        this.idRole = idRole;
        this.isActive = isActive;
    }

    // Getters and Setters
    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getNamaLengkap() { return namaLengkap; }
    public void setNamaLengkap(String namaLengkap) { this.namaLengkap = namaLengkap; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getIdRole() { return idRole; }
    public void setIdRole(int idRole) { this.idRole = idRole; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Business Methods
    public boolean isLocked() {
        if (lockedUntil == null) return false;
        return LocalDateTime.now().isBefore(lockedUntil);
    }

    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
        // Lock account setelah 5 failed attempts (30 menit)
        if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(30);
        }
    }

    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + idUser +
                ", username='" + username + '\'' +
                ", namaLengkap='" + namaLengkap + '\'' +
                ", email='" + email + '\'' +
                ", role=" + idRole +
                ", active=" + isActive +
                '}';
    }
}