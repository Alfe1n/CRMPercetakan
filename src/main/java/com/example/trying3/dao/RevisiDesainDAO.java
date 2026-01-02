package com.example.trying3.dao;

import com.example.trying3.config.DatabaseConnection;
import com.example.trying3.model.RevisiDesain;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object untuk revisi desain.
 * Mengelola operasi CRUD untuk history revisi desain.
 */
public class RevisiDesainDAO {

    public boolean simpanRevisi(RevisiDesain revisi) {
        String sql = """
            INSERT INTO revisi_desain 
            (id_desain, revisi_ke, file_path, catatan_revisi, direvisi_oleh, tanggal_revisi) 
            VALUES (?, ?, ?, ?, ?, NOW())
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, revisi.getIdDesain());
            ps.setInt(2, revisi.getRevisiKe());
            ps.setString(3, revisi.getFilePath());
            ps.setString(4, revisi.getCatatanRevisi());
            ps.setInt(5, revisi.getDirevisiOleh());

            int affected = ps.executeUpdate();

            if (affected > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    revisi.setIdRevisi(rs.getInt(1));
                }
                System.out.println("✅ Revisi desain berhasil disimpan: ID " + revisi.getIdRevisi());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error menyimpan revisi desain: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public List<RevisiDesain> getRevisiByDesainId(int idDesain) {
        List<RevisiDesain> list = new ArrayList<>();

        String sql = """
            SELECT 
                rd.id_revisi,
                rd.id_desain,
                rd.revisi_ke,
                rd.file_path,
                rd.catatan_revisi,
                rd.direvisi_oleh,
                rd.tanggal_revisi,
                u.nama_lengkap as nama_designer
            FROM revisi_desain rd
            JOIN user u ON rd.direvisi_oleh = u.id_user
            WHERE rd.id_desain = ?
            ORDER BY rd.revisi_ke DESC
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idDesain);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RevisiDesain revisi = mapResultSet(rs);
                    list.add(revisi);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Error mengambil history revisi: " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }
    public List<RevisiDesain> getRevisiByPesananId(int idPesanan) {
        List<RevisiDesain> list = new ArrayList<>();

        String sql = """
            SELECT 
                rd.id_revisi,
                rd.id_desain,
                rd.revisi_ke,
                rd.file_path,
                rd.catatan_revisi,
                rd.direvisi_oleh,
                rd.tanggal_revisi,
                u.nama_lengkap as nama_designer
            FROM revisi_desain rd
            JOIN desain d ON rd.id_desain = d.id_desain
            JOIN user u ON rd.direvisi_oleh = u.id_user
            WHERE d.id_pesanan = ?
            ORDER BY rd.revisi_ke DESC
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idPesanan);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RevisiDesain revisi = mapResultSet(rs);
                    list.add(revisi);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Error mengambil history revisi by pesanan: " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    public int countRevisiByDesainId(int idDesain) {
        String sql = "SELECT COUNT(*) FROM revisi_desain WHERE id_desain = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idDesain);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Error menghitung revisi: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    public RevisiDesain getLatestRevisi(int idDesain) {
        String sql = """
            SELECT 
                rd.id_revisi,
                rd.id_desain,
                rd.revisi_ke,
                rd.file_path,
                rd.catatan_revisi,
                rd.direvisi_oleh,
                rd.tanggal_revisi,
                u.nama_lengkap as nama_designer
            FROM revisi_desain rd
            JOIN user u ON rd.direvisi_oleh = u.id_user
            WHERE rd.id_desain = ?
            ORDER BY rd.revisi_ke DESC
            LIMIT 1
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idDesain);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Error mengambil revisi terakhir: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    private RevisiDesain mapResultSet(ResultSet rs) throws SQLException {
        RevisiDesain revisi = new RevisiDesain();
        revisi.setIdRevisi(rs.getInt("id_revisi"));
        revisi.setIdDesain(rs.getInt("id_desain"));
        revisi.setRevisiKe(rs.getInt("revisi_ke"));
        revisi.setFilePath(rs.getString("file_path"));
        revisi.setCatatanRevisi(rs.getString("catatan_revisi"));
        revisi.setDirevisiOleh(rs.getInt("direvisi_oleh"));

        try {
            revisi.setNamaDesigner(rs.getString("nama_designer"));
        } catch (SQLException e) {
        }

        Timestamp ts = rs.getTimestamp("tanggal_revisi");
        if (ts != null) {
            revisi.setTanggalRevisi(ts.toLocalDateTime());
        }

        return revisi;
    }
}