package com.example.trying3.dao;

import com.example.trying3.config.DatabaseConnection;
import com.example.trying3.util.SessionManager;

import java.sql.*;

public class PesananDAO {
    public boolean createPesanan(String namaPelanggan, String noTelepon, String email, String jenisLayanan, int jumlah, double totalHarga, String spesifikasi) {
        Connection conn = null;
        PreparedStatement psPelanggan = null;
        PreparedStatement psPesanan = null;
        PreparedStatement psDetail = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // 1. HANDLE PELANGGAN
            int idPelanggan = -1;

            // Cek apakah pelanggan dengan no_telepon ini sudah ada
            String checkPelangganSql = "SELECT id_pelanggan FROM pelanggan WHERE no_telepon = ?";
            psPelanggan = conn.prepareStatement(checkPelangganSql);
            psPelanggan.setString(1, noTelepon);
            rs = psPelanggan.executeQuery();

            if (rs.next()) {
                // Pelanggan lama
                idPelanggan = rs.getInt("id_pelanggan");
            } else {
                // Pelanggan baru, insert dulu
                String insertPelangganSql = "INSERT INTO pelanggan (nama, no_telepon, email) VALUES (?, ?, ?)";
                psPelanggan = conn.prepareStatement(insertPelangganSql, Statement.RETURN_GENERATED_KEYS);
                psPelanggan.setString(1, namaPelanggan);
                psPelanggan.setString(2, noTelepon);
                psPelanggan.setString(3, email);
                psPelanggan.executeUpdate();
                rs = psPelanggan.getGeneratedKeys();
                if (rs.next()) {
                    idPelanggan = rs.getInt(1);
                }
            }

            // 2. HANDLE PESANAN (HEAD)
            // Ambil ID Admin dari Session
            int idAdmin = SessionManager.getInstance().getCurrentUserId();
            if (idAdmin == -1) {
                throw new SQLException("User tidak terautentikasi.");
            }

            // Insert ke tabel pesanan
            // Note: nomor_pesanan digenerate otomatis oleh Trigger MySQL
            String insertPesananSql = "INSERT INTO pesanan (id_pelanggan, id_user_admin, id_status, total_biaya, catatan) VALUES (?, ?, 1, ?, ?)";
            psPesanan = conn.prepareStatement(insertPesananSql, Statement.RETURN_GENERATED_KEYS);
            psPesanan.setInt(1, idPelanggan);
            psPesanan.setInt(2, idAdmin);
            psPesanan.setDouble(3, totalHarga);
            psPesanan.setString(4, spesifikasi); // Simpan spesifikasi di catatan

            int affectedRows = psPesanan.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Membuat pesanan gagal, tidak ada baris yang terpengaruh.");
            }

            int idPesanan = -1;
            rs = psPesanan.getGeneratedKeys();
            if (rs.next()) {
                idPesanan = rs.getInt(1);
            }

            // 3. HANDLE DETAIL PESANAN & JENIS LAYANAN
            // Cari ID Layanan berdasarkan nama (dari ComboBox)
            int idJenisLayanan = getLayananIdByName(conn, jenisLayanan);
            String insertDetailSql = "INSERT INTO detail_pesanan (id_pesanan, id_layanan, jumlah, harga_satuan, subtotal, spesifikasi) VALUES (?, ?, ?, ?, ?, ?)";
            psDetail = conn.prepareStatement(insertDetailSql);
            double hargaSatuan = totalHarga / jumlah; // Asumsi harga satuan dihitung dari total / jumlah
            psDetail.setInt(1, idPesanan);
            psDetail.setInt(2, idJenisLayanan);
            psDetail.setInt(3, jumlah);
            psDetail.setDouble(4, hargaSatuan);
            psDetail.setDouble(5, totalHarga);
            psDetail.setString(6, spesifikasi);
            psDetail.executeUpdate();

            conn.commit(); // Commit transaction
            System.out.println("✅ Transaksi Pesanan Berhasil!");
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    System.err.println("Rolling back transaction...");
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            // Tutup resource
            try { if (rs != null) rs.close(); } catch (Exception e) {};
            try { if (psPelanggan != null) psPelanggan.close(); } catch (Exception e) {};
            try { if (psPesanan != null) psPesanan.close(); } catch (Exception e) {};
            try { if (psDetail != null) psDetail.close(); } catch (Exception e) {};
            try { if (conn != null) conn.setAutoCommit(true); conn.close(); } catch (Exception e) {};
        }

    }

    private int getLayananIdByName(Connection conn, String namaLayanan) throws SQLException {
        String sql = "SELECT id_layanan FROM jenis_layanan WHERE nama_layanan = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaLayanan);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id_layanan");
        }
        // Jika tidak ditemukan, return ID default atau throw error
        // Untuk contoh ini kita return 1 (pastikan di database ada data dummy id 1)
        return 1;
    }
}
