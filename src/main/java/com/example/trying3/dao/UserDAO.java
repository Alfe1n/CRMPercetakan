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

    public String canDeleteUser(int userId) {
        try (Connection conn = DatabaseConnection.getConnection()) {

            // Cek relasi dengan tabel pesanan (sebagai admin yang input)
            String sqlPesanan = "SELECT COUNT(*) as total FROM pesanan WHERE id_user_admin = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlPesanan)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt("total") > 0) {
                    return "User masih memiliki " + rs.getInt("total") + " pesanan yang terkait. " +
                            "Hapus atau pindahkan pesanan terlebih dahulu.";
                }
            }

            // Cek relasi dengan tabel produksi (sebagai operator)
            String sqlProduksi = "SELECT COUNT(*) as total FROM produksi WHERE id_operator = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlProduksi)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt("total") > 0) {
                    return "User masih memiliki " + rs.getInt("total") + " data produksi yang terkait.";
                }
            }

            // Cek relasi dengan tabel desain (sebagai desainer)
            String sqlDesain = "SELECT COUNT(*) as total FROM desain WHERE id_designer = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlDesain)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt("total") > 0) {
                    return "User masih memiliki " + rs.getInt("total") + " data desain yang terkait.";
                }
            }

            // Cek relasi dengan tabel pembayaran (sebagai verifikator)
            String sqlPembayaran = "SELECT COUNT(*) as total FROM pembayaran WHERE verified_by = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlPembayaran)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt("total") > 0) {
                    return "User masih memiliki " + rs.getInt("total") + " verifikasi pembayaran yang terkait.";
                }
            }

            // Cek relasi dengan tabel laporan
            String sqlLaporan = "SELECT COUNT(*) as total FROM laporan WHERE generated_by = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlLaporan)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt("total") > 0) {
                    return "User masih memiliki " + rs.getInt("total") + " laporan yang terkait.";
                }
            }

            // Cek relasi dengan tabel kendala_produksi (sebagai pelapor)
            String sqlKendala = "SELECT COUNT(*) as total FROM kendala_produksi WHERE dilaporkan_oleh = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlKendala)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt("total") > 0) {
                    return "User masih memiliki " + rs.getInt("total") + " laporan kendala yang terkait.";
                }
            }

            // Cek relasi dengan tabel revisi_desain
            String sqlRevisi = "SELECT COUNT(*) as total FROM revisi_desain WHERE direvisi_oleh = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlRevisi)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt("total") > 0) {
                    return "User masih memiliki " + rs.getInt("total") + " revisi desain yang terkait.";
                }
            }

            // Semua cek berhasil, user bisa dihapus
            return null;

        } catch (SQLException e) {
            System.err.println("Error canDeleteUser: " + e.getMessage());
            e.printStackTrace();
            return "Error saat mengecek relasi user: " + e.getMessage();
        }
    }

    public boolean deleteUser(int userId) {
        String sqlDeleteLog = "DELETE FROM log_aktivitas WHERE id_user = ?";
        String sqlDeleteSyncQueue = "DELETE FROM sync_queue WHERE created_by = ?";
        String sqlDeleteUser = "DELETE FROM user WHERE id_user = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Hapus log aktivitas user (jika ada)
            try (PreparedStatement pstmt = conn.prepareStatement(sqlDeleteLog)) {
                pstmt.setInt(1, userId);
                int deleted = pstmt.executeUpdate();
                System.out.println("Log aktivitas dihapus: " + deleted + " record");
            }

            // Hapus sync queue user (jika ada)
            try (PreparedStatement pstmt = conn.prepareStatement(sqlDeleteSyncQueue)) {
                pstmt.setInt(1, userId);
                int deleted = pstmt.executeUpdate();
                System.out.println("Sync queue dihapus: " + deleted + " record");
            }

            // Hapus user
            try (PreparedStatement pstmt = conn.prepareStatement(sqlDeleteUser)) {
                pstmt.setInt(1, userId);
                int affected = pstmt.executeUpdate();

                if (affected > 0) {
                    conn.commit();
                    System.out.println("✅ User dengan ID " + userId + " berhasil dihapus");
                    return true;
                } else {
                    conn.rollback();
                    System.out.println("❌ User dengan ID " + userId + " tidak ditemukan");
                    return false;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error deleteUser: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public User getUserById(int userId) {
        String sql = "SELECT * FROM user WHERE id_user = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error getUserById: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}