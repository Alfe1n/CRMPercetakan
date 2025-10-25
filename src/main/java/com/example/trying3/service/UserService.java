package com.example.trying3.service;

import com.example.trying3.dao.UserDAO;
import com.example.trying3.model.User;
import com.example.trying3.util.PasswordUtil;

import java.util.List;

public class UserService {
    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    /**
     * Create new user (dengan password hashing)
     */
    public User createUser(User user) {
        try {
            // Hash password sebelum disimpan
            String plainPassword = user.getPasswordHash(); // Sementara ambil dari passwordHash
            String hashedPassword = PasswordUtil.hashPassword(plainPassword);
            user.setPasswordHash(hashedPassword);

            // Simpan ke database menggunakan method yang sudah ada
            boolean success = userDAO.insertUser(user);

            if (success) {
                System.out.println("✅ User berhasil dibuat: " + user.getUsername());
                return user;
            } else {
                System.err.println("❌ Gagal membuat user");
                return null;
            }
        } catch (Exception e) {
            System.err.println("❌ Error saat membuat user: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Gagal membuat user: " + e.getMessage());
        }
    }

    /**
     * Get all users
     */
    public List<User> getAllUsers() {
        return userDAO.getAllUsers();
    }

    /**
     * Get user by username
     */
    public User getUserByUsername(String username) {
        return userDAO.getUserByUsername(username);
    }

    /**
     * Update user
     */
    public boolean updateUser(User user) {
        return userDAO.updateUser(user);
    }

    /**
     * Update password
     */
    public boolean updatePassword(int userId, String newPassword) {
        String hashedPassword = PasswordUtil.hashPassword(newPassword);
        return userDAO.updatePassword(userId, hashedPassword);
    }

    /**
     * Get total users count
     */
    public int getTotalUsers() {
        return userDAO.getTotalUsers();
    }
}