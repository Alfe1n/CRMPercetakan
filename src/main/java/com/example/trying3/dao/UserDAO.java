package com.example.trying3.dao;

import com.example.trying3.config.DatabaseConnection;
import com.example.trying3.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object untuk operasi CRUD User.
 * Menangani autentikasi, manajemen user, dan security features.
 */
public class UserDAO {

    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM user WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error getUserByUsername: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public boolean updateLastLogin(int userId) {
        String sql = "UPDATE user SET last_login = NOW(), " +
                "failed_login_attempts = 0, locked_until = NULL " +
                "WHERE id_user = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updateLastLogin: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public boolean incrementFailedAttempts(int userId) {
        String sql = "UPDATE user SET failed_login_attempts = failed_login_attempts + 1 " +
                "WHERE id_user = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error incrementFailedAttempts: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public boolean lockAccount(int userId, LocalDateTime until) {
        String sql = "UPDATE user SET locked_until = ? WHERE id_user = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, Timestamp.valueOf(until));
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error lockAccount: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();
        String sql = "SELECT id_user, username, nama_lengkap, email, id_role, " +
                "is_active, last_login, failed_login_attempts, locked_until, " +
                "created_at, updated_at FROM user ORDER BY nama_lengkap ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                userList.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error saat mengambil data user!");
            e.printStackTrace();
        }

        return userList;
    }

    public boolean insertUser(User user) {
        String sql = "INSERT INTO user (username, password_hash, email, nama_lengkap, " +
                "id_role, is_active) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getNamaLengkap());
            pstmt.setInt(5, user.getIdRole());
            pstmt.setBoolean(6, user.isActive());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    user.setIdUser(generatedKeys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error insertUser: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public boolean updateUser(User user) {
        String sql = "UPDATE user SET username = ?, email = ?, nama_lengkap = ?, " +
                "id_role = ?, is_active = ? WHERE id_user = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getNamaLengkap());
            pstmt.setInt(4, user.getIdRole());
            pstmt.setBoolean(5, user.isActive());
            pstmt.setInt(6, user.getIdUser());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error updateUser: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public boolean updatePassword(int userId, String newPasswordHash) {
        String sql = "UPDATE user SET password_hash = ? WHERE id_user = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newPasswordHash);
            pstmt.setInt(2, userId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updatePassword: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public int getTotalUsers() {
        String sql = "SELECT COUNT(*) as total FROM user";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("total");
            }

        } catch (SQLException e) {
            System.err.println("Error getTotalUsers: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setIdUser(rs.getInt("id_user"));
        user.setUsername(rs.getString("username"));

        try {
            user.setPasswordHash(rs.getString("password_hash"));
        } catch (SQLException e) {
        }

        user.setNamaLengkap(rs.getString("nama_lengkap"));
        user.setEmail(rs.getString("email"));
        user.setIdRole(rs.getInt("id_role"));
        user.setActive(rs.getBoolean("is_active"));

        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null) {
            user.setLastLogin(lastLogin.toLocalDateTime());
        }

        user.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));

        Timestamp lockedUntil = rs.getTimestamp("locked_until");
        if (lockedUntil != null) {
            user.setLockedUntil(lockedUntil.toLocalDateTime());
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return user;
    }
}