package com.example.trying3.dao;

import com.example.trying3.config.DatabaseConnection;
import com.example.trying3.model.DesainInfo;

import java.sql.*;

/**
 * Data Access Object untuk operasi Desain.
 * Menangani penyimpanan, update, dan approval desain dengan fitur revisi.
 */
public class DesainDAO {

    private static final int STATUS_DALAM_PENGERJAAN = 2;
    private static final int STATUS_PERLU_REVISI = 4;
    private static final int STATUS_DISETUJUI = 5;
    private static final int STATUS_PESANAN_ANTRIAN_PRODUKSI = 7;

    /**
     * Menyimpan file desain. Insert jika belum ada, update jika sudah ada.
     */
    public boolean simpanDesain(int idPesanan, String filePath, int idDesigner) {
        if (idPesanan <= 0) {
            System.err.println("❌ Error: idPesanan harus lebih dari 0");
            return false;
        }
        if (filePath == null || filePath.trim().isEmpty()) {
            System.err.println("❌ Error: filePath tidak boleh kosong");
            return false;
        }
        if (idDesigner <= 0) {
            System.err.println("❌ Error: idDesigner harus lebih dari 0");
            return false;
        }

        boolean exists = checkDesainExists(idPesanan);

        String sql = exists
                ? "UPDATE desain SET file_desain_path = ?, id_designer = ?, updated_at = NOW() WHERE id_pesanan = ?"
                : "INSERT INTO desain (file_desain_path, id_designer, id_pesanan, id_status_desain) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, filePath);
            ps.setInt(2, idDesigner);
            ps.setInt(3, idPesanan);

            if (!exists) {
                ps.setInt(4, STATUS_DALAM_PENGERJAAN);
            }

            boolean success = ps.executeUpdate() > 0;
            if (success) {
                System.out.println("✅ Desain berhasil disimpan untuk pesanan: " + idPesanan);
            }
            return success;

        } catch (SQLException e) {
            System.err.println("❌ Error menyimpan desain: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Menyimpan file desain dengan dukungan revisi.
     * File lama disimpan ke revisi_desain jika status = "Perlu Revisi".
     */
    public boolean simpanDesainDenganRevisi(int idPesanan, String newFilePath, int idDesigner) {
        if (idPesanan <= 0 || newFilePath == null || newFilePath.trim().isEmpty() || idDesigner <= 0) {
            System.err.println("❌ Error: Parameter tidak valid");
            return false;
        }
        Connection conn = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            DesainExisting existing = getDesainExisting(conn, idPesanan);

            if (existing == null) {
                insertNewDesain(conn, idPesanan, newFilePath, idDesigner);
            } else {
                processDesainUpdate(conn, existing, newFilePath, idDesigner);
            }

            conn.commit();
            System.out.println("✅ COMMIT berhasil!");
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Error menyimpan desain dengan revisi: " + e.getMessage());
            e.printStackTrace();
            rollbackQuietly(conn);
            return false;

        } finally {
            closeConnectionQuietly(conn);
        }
    }

    /**
     * Menyetujui desain dan update status pesanan ke "Antrian Produksi".
     */
    public boolean approveDesain(int idPesanan) {
        if (idPesanan <= 0) {
            System.err.println("❌ Error: idPesanan harus lebih dari 0");
            return false;
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            String sqlPesanan = "UPDATE pesanan SET id_status = ? WHERE id_pesanan = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlPesanan)) {
                ps.setInt(1, STATUS_PESANAN_ANTRIAN_PRODUKSI);
                ps.setInt(2, idPesanan);
                ps.executeUpdate();
            }

            String sqlDesain = "UPDATE desain SET id_status_desain = ?, tanggal_disetujui = NOW() WHERE id_pesanan = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlDesain)) {
                ps.setInt(1, STATUS_DISETUJUI);
                ps.setInt(2, idPesanan);
                ps.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Error menyetujui desain: " + e.getMessage());
            rollbackQuietly(conn);
            e.printStackTrace();
            return false;

        } finally {
            closeConnectionQuietly(conn);
        }
    }

    public DesainInfo getDesainInfo(int idPesanan) {
        if (idPesanan <= 0) {
            return null;
        }

        String sql = """
                SELECT
                    d.id_desain,
                    d.file_desain_path,
                    d.revisi_ke,
                    d.id_status_desain,
                    sd.nama_status as status_desain,
                    d.tanggal_dibuat,
                    d.tanggal_disetujui,
                    u.nama_lengkap as nama_designer
                FROM desain d
                JOIN status_desain sd ON d.id_status_desain = sd.id_status_desain
                JOIN user u ON d.id_designer = u.id_user
                WHERE d.id_pesanan = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idPesanan);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDesainInfo(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Error mengambil info desain: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    private boolean checkDesainExists(int idPesanan) {
        String checkSql = "SELECT id_desain FROM desain WHERE id_pesanan = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setInt(1, idPesanan);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private DesainExisting getDesainExisting(Connection conn, int idPesanan) throws SQLException {
        String checkSql = """
                SELECT id_desain, file_desain_path, revisi_ke, id_status_desain
                FROM desain
                WHERE id_pesanan = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setInt(1, idPesanan);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new DesainExisting(
                            rs.getInt("id_desain"),
                            rs.getString("file_desain_path"),
                            rs.getInt("revisi_ke"),
                            rs.getInt("id_status_desain"));
                }
            }
        }
        return null;
    }

    private void insertNewDesain(Connection conn, int idPesanan, String filePath, int idDesigner) throws SQLException {
        String insertSql = """
                INSERT INTO desain
                (id_pesanan, id_designer, id_status_desain, file_desain_path, revisi_ke)
                VALUES (?, ?, ?, ?, 1)
                """;

        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setInt(1, idPesanan);
            ps.setInt(2, idDesigner);
            ps.setInt(3, STATUS_DALAM_PENGERJAAN);
            ps.setString(4, filePath);
            int rows = ps.executeUpdate();
        }
    }

    private void processDesainUpdate(Connection conn, DesainExisting existing, String newFilePath, int idDesigner)
            throws SQLException {

        int currentRevisiKe = existing.revisiKe;

        boolean isRevisi = (existing.idStatusDesain == STATUS_PERLU_REVISI) ||
                (existing.oldFilePath != null && !existing.oldFilePath.isEmpty());

        if (isRevisi && existing.oldFilePath != null && !existing.oldFilePath.isEmpty()) {
            saveToRevisiHistory(conn, existing.idDesain, currentRevisiKe, existing.oldFilePath, idDesigner);
            currentRevisiKe++;
        }

        updateDesain(conn, existing.idDesain, newFilePath, idDesigner, currentRevisiKe);
    }

    private void saveToRevisiHistory(Connection conn, int idDesain, int revisiKe, String oldFilePath, int idDesigner)
            throws SQLException {

        String insertRevisiSql = """
                INSERT INTO revisi_desain
                (id_desain, revisi_ke, file_path, catatan_revisi, direvisi_oleh, tanggal_revisi)
                VALUES (?, ?, ?, ?, ?, NOW())
                """;

        try (PreparedStatement ps = conn.prepareStatement(insertRevisiSql)) {
            ps.setInt(1, idDesain);
            ps.setInt(2, revisiKe);
            ps.setString(3, oldFilePath);
            ps.setString(4, "Revisi dari file sebelumnya");
            ps.setInt(5, idDesigner);
            int rows = ps.executeUpdate();
        }
    }

    private void updateDesain(Connection conn, int idDesain, String newFilePath, int idDesigner, int revisiKe)
            throws SQLException {

        String updateSql = """
                UPDATE desain
                SET file_desain_path = ?,
                    id_designer = ?,
                    revisi_ke = ?,
                    id_status_desain = ?,
                    updated_at = NOW()
                WHERE id_desain = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, newFilePath);
            ps.setInt(2, idDesigner);
            ps.setInt(3, revisiKe);
            ps.setInt(4, STATUS_DALAM_PENGERJAAN);
            ps.setInt(5, idDesain);
            int rows = ps.executeUpdate();
            System.out.println("🔍 DEBUG: UPDATE desain rows affected: " + rows);
        }

        System.out.println("✅ Desain berhasil diupdate. Revisi ke-" + revisiKe);
    }

    private DesainInfo mapResultSetToDesainInfo(ResultSet rs) throws SQLException {
        DesainInfo info = new DesainInfo();
        info.setIdDesain(rs.getInt("id_desain"));
        info.setFilePath(rs.getString("file_desain_path"));
        info.setRevisiKe(rs.getInt("revisi_ke"));
        info.setIdStatusDesain(rs.getInt("id_status_desain"));
        info.setStatusDesain(rs.getString("status_desain"));
        info.setNamaDesigner(rs.getString("nama_designer"));

        Timestamp tanggalDibuat = rs.getTimestamp("tanggal_dibuat");
        if (tanggalDibuat != null) {
            info.setTanggalDibuat(tanggalDibuat.toLocalDateTime());
        }

        Timestamp tanggalDisetujui = rs.getTimestamp("tanggal_disetujui");
        if (tanggalDisetujui != null) {
            info.setTanggalDisetujui(tanggalDisetujui.toLocalDateTime());
        }

        return info;
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

    private record DesainExisting(int idDesain, String oldFilePath, int revisiKe, int idStatusDesain) {
    }
}
