package com.example.trying3.dao;

import com.example.trying3.config.DatabaseConnection;

import java.sql.*;

/**
 * Data Access Object untuk operasi Produksi.
 * Menangani kendala produksi dan update status produksi.
 */
public class ProduksiDAO {

    private static final int STATUS_PESANAN_SELESAI = 9;

    public boolean simpanKendalaProduksi(int idPesanan, String deskripsi, int idUser) {
        if (idPesanan <= 0) {
            System.err.println("❌ Error: idPesanan harus lebih dari 0");
            return false;
        }
        if (deskripsi == null || deskripsi.trim().isEmpty()) {
            System.err.println("❌ Error: deskripsi tidak boleh kosong");
            return false;
        }
        if (idUser <= 0) {
            System.err.println("❌ Error: idUser harus lebih dari 0");
            return false;
        }

        Connection conn = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int idProduksi = getOrCreateProduksi(conn, idPesanan, idUser);
            insertKendala(conn, idProduksi, deskripsi, idUser);
            updatePesananCatatan(conn, idPesanan, deskripsi);

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Error menyimpan kendala produksi: " + e.getMessage());
            e.printStackTrace();
            rollbackQuietly(conn);
            return false;

        } finally {
            closeConnectionQuietly(conn);
        }
    }

    /**
     * Menyelesaikan produksi dan resolve semua kendala terkait.
     */
    public boolean selesaikanProduksi(int idPesanan) {
        if (idPesanan <= 0) {
            System.err.println("❌ Error: idPesanan harus lebih dari 0");
            return false;
        }

        Connection conn = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            updateStatusPesananSelesai(conn, idPesanan);
            updateProduksiSelesai(conn, idPesanan);

            int resolved = resolveAllKendala(conn, idPesanan);
            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Error menyelesaikan produksi: " + e.getMessage());
            e.printStackTrace();
            rollbackQuietly(conn);
            return false;

        } finally {
            closeConnectionQuietly(conn);
        }
    }

    /**
     * Resolve kendala spesifik dengan solusi.
     */
    public boolean resolveKendala(int idKendala, String solusi) {
        if (idKendala <= 0) {
            System.err.println("❌ Error: idKendala harus lebih dari 0");
            return false;
        }

        String sql = """
                UPDATE kendala_produksi
                SET status = 'resolved',
                    solusi = ?,
                    tanggal_selesai = NOW()
                WHERE id_kendala = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, solusi != null ? solusi : "");
            ps.setInt(2, idKendala);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error resolve kendala: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    private int getOrCreateProduksi(Connection conn, int idPesanan, int idUser) throws SQLException {
        int idProduksi = findProduksiByPesanan(conn, idPesanan);

        if (idProduksi == -1) {
            idProduksi = createProduksi(conn, idPesanan, idUser);
        } else {
            updateProduksiStatus(conn, idProduksi, "terkendala");
        }

        return idProduksi;
    }

    private int findProduksiByPesanan(Connection conn, int idPesanan) throws SQLException {
        String findProduksiSql = "SELECT id_produksi FROM produksi WHERE id_pesanan = ?";
        try (PreparedStatement ps = conn.prepareStatement(findProduksiSql)) {
            ps.setInt(1, idPesanan);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_produksi");
                }
            }
        }
        return -1;
    }

    private int createProduksi(Connection conn, int idPesanan, int idUser) throws SQLException {
        String insertProduksiSql = """
                INSERT INTO produksi (id_pesanan, id_operator, status_produksi)
                VALUES (?, ?, 'terkendala')
                """;
        try (PreparedStatement ps = conn.prepareStatement(insertProduksiSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idPesanan);
            ps.setInt(2, idUser);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    private void updateProduksiStatus(Connection conn, int idProduksi, String status) throws SQLException {
        String updateProduksiSql = "UPDATE produksi SET status_produksi = ? WHERE id_produksi = ?";
        try (PreparedStatement ps = conn.prepareStatement(updateProduksiSql)) {
            ps.setString(1, status);
            ps.setInt(2, idProduksi);
            ps.executeUpdate();
        }
    }

    private void insertKendala(Connection conn, int idProduksi, String deskripsi, int idUser) throws SQLException {
        String insertKendalaSql = """
                INSERT INTO kendala_produksi
                (id_produksi, deskripsi, status, dilaporkan_oleh, tanggal_lapor)
                VALUES (?, ?, 'open', ?, NOW())
                """;
        try (PreparedStatement ps = conn.prepareStatement(insertKendalaSql)) {
            ps.setInt(1, idProduksi);
            ps.setString(2, deskripsi);
            ps.setInt(3, idUser);
            ps.executeUpdate();
        }
    }

    private void updatePesananCatatan(Connection conn, int idPesanan, String deskripsi) throws SQLException {
        String updateCatatanSql = "UPDATE pesanan SET catatan = CONCAT(IFNULL(catatan, ''), '\nKENDALA: ', ?) WHERE id_pesanan = ?";
        try (PreparedStatement ps = conn.prepareStatement(updateCatatanSql)) {
            ps.setString(1, deskripsi);
            ps.setInt(2, idPesanan);
            ps.executeUpdate();
        }
    }

    private void updateStatusPesananSelesai(Connection conn, int idPesanan) throws SQLException {
        String updatePesananSql = "UPDATE pesanan SET id_status = ? WHERE id_pesanan = ?";
        try (PreparedStatement ps = conn.prepareStatement(updatePesananSql)) {
            ps.setInt(1, STATUS_PESANAN_SELESAI);
            ps.setInt(2, idPesanan);
            ps.executeUpdate();
        }
    }

    private void updateProduksiSelesai(Connection conn, int idPesanan) throws SQLException {
        String updateProduksiSql = """
                UPDATE produksi
                SET status_produksi = 'selesai',
                    tanggal_selesai = NOW(),
                    progres_persen = 100
                WHERE id_pesanan = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(updateProduksiSql)) {
            ps.setInt(1, idPesanan);
            ps.executeUpdate();
        }
    }

    private int resolveAllKendala(Connection conn, int idPesanan) throws SQLException {
        String resolveKendalaSql = """
                UPDATE kendala_produksi kp
                JOIN produksi pr ON kp.id_produksi = pr.id_produksi
                SET kp.status = 'resolved',
                    kp.tanggal_selesai = NOW()
                WHERE pr.id_pesanan = ?
                AND kp.status IN ('open', 'in_progress')
                """;
        try (PreparedStatement ps = conn.prepareStatement(resolveKendalaSql)) {
            ps.setInt(1, idPesanan);
            return ps.executeUpdate();
        }
    }

    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void closeConnectionQuietly(Connection conn) {
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
