package com.example.trying3.service;

import com.example.trying3.config.DatabaseConnection;
import com.example.trying3.util.SessionManager;

import java.sql.*;

/**
 * Service untuk operasi pembayaran.
 * Menangani verifikasi pembayaran dan statistik pembayaran.
 */
public class PembayaranService {

    private static final int STATUS_PENDING = 1;
    private static final int STATUS_MENUNGGU_PEMBAYARAN = 2;
    private static final int STATUS_SEDANG_DIKERJAKAN = 3;

    public boolean verifikasiPembayaran(int idPesanan, double nominal, String namaMetode) {
        if (idPesanan <= 0) {
            System.err.println("❌ Error: idPesanan harus lebih dari 0");
            return false;
        }
        if (nominal <= 0) {
            System.err.println("❌ Error: nominal harus lebih dari 0");
            return false;
        }
        if (namaMetode == null || namaMetode.trim().isEmpty()) {
            System.err.println("❌ Error: namaMetode tidak boleh kosong");
            return false;
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int idMetode = getMetodeIdByName(conn, namaMetode);
            int idAdmin = getCurrentAdminId();

            insertPembayaran(conn, idPesanan, idMetode, nominal, idAdmin);
            updateStatusPesanan(conn, idPesanan, STATUS_SEDANG_DIKERJAKAN);

            conn.commit();
            return true;

        } catch (Exception e) {
            rollbackQuietly(conn);
            System.err.println("❌ Error verifikasiPembayaran: " + e.getMessage());
            e.printStackTrace();
            return false;

        } finally {
            closeConnectionQuietly(conn);
        }
    }

    public boolean tolakPembayaran(int idPesanan) {
        if (idPesanan <= 0) {
            System.err.println("❌ Error: idPesanan harus lebih dari 0");
            return false;
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            updateStatusPesanan(conn, idPesanan, STATUS_PENDING);

            conn.commit();
            return true;

        } catch (Exception e) {
            rollbackQuietly(conn);
            System.err.println("❌ Error tolakPembayaran: " + e.getMessage());
            e.printStackTrace();
            return false;

        } finally {
            closeConnectionQuietly(conn);
        }
    }

    public int getMenungguVerifikasiCount() {
        return executeCountQuery(
                "SELECT COUNT(*) as total FROM pesanan WHERE id_status = " + STATUS_MENUNGGU_PEMBAYARAN);
    }

    public int getTerverifikasiCount() {
        return executeCountQuery(
                "SELECT COUNT(*) as total FROM pembayaran WHERE status_pembayaran = 'verified'");
    }

    public double getTotalPendapatan() {
        String sql = "SELECT COALESCE(SUM(jumlah), 0) as total FROM pembayaran WHERE status_pembayaran = 'verified'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (Exception e) {
            System.err.println("❌ Error getTotalPendapatan: " + e.getMessage());
        }
        return 0;
    }

    public int getMetodeIdByName(Connection conn, String namaMetode) throws SQLException {
        String sql = "SELECT id_metode FROM metode_pembayaran WHERE nama_metode = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaMetode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt("id_metode");
            }
        }
        return 1; // Default
    }

    public String[] getMetodePembayaranList() {
        String sql = "SELECT nama_metode FROM metode_pembayaran WHERE is_active = 1";
        java.util.List<String> list = new java.util.ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(rs.getString("nama_metode"));
            }

        } catch (Exception e) {
            System.err.println("❌ Error getMetodePembayaranList: " + e.getMessage());
        }

        return list.toArray(new String[0]);
    }

    private void insertPembayaran(Connection conn, int idPesanan, int idMetode,
                                  double nominal, int idAdmin) throws SQLException {
        String sql = """
                INSERT INTO pembayaran
                (id_pesanan, id_metode, jumlah, tanggal_pembayaran, status_pembayaran, verified_by, tanggal_verifikasi)
                VALUES (?, ?, ?, NOW(), 'verified', ?, NOW())
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPesanan);
            ps.setInt(2, idMetode);
            ps.setDouble(3, nominal);
            ps.setInt(4, idAdmin);
            ps.executeUpdate();
        }
    }

    private void updateStatusPesanan(Connection conn, int idPesanan, int idStatus) throws SQLException {
        String sql = "UPDATE pesanan SET id_status = ?, updated_at = NOW() WHERE id_pesanan = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idStatus);
            ps.setInt(2, idPesanan);
            ps.executeUpdate();
        }
    }

    private int getCurrentAdminId() {
        try {
            return SessionManager.getInstance().getCurrentUser().getIdUser();
        } catch (Exception e) {
            return 1; // Default fallback
        }
    }

    private int executeCountQuery(String sql) {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next())
                return rs.getInt("total");
        } catch (Exception e) {
            System.err.println("❌ Error executeCountQuery: " + e.getMessage());
        }
        return 0;
    }

    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
            }
        }
    }

    private void closeConnectionQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
