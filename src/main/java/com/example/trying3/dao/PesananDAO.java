package com.example.trying3.dao;

import com.example.trying3.config.DatabaseConnection;
import com.example.trying3.model.Pesanan;
import com.example.trying3.util.SessionManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PesananDAO {
    public boolean createPesanan(String namaPelanggan, String noTelepon, String email, String jenisLayanan, int jumlah, double totalHarga, String spesifikasi) {
        Connection conn = null;
        PreparedStatement psPelanggan = null;
        PreparedStatement psPesanan = null;
        PreparedStatement psDetail = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

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

            // HANDLE PESANAN (HEAD)
            // Ambil ID Admin dari Session
            int idAdmin = SessionManager.getInstance().getCurrentUserId();
            if (idAdmin == -1) {
                throw new SQLException("User tidak terautentikasi.");
            }

            // Insert ke tabel pesanan
            String insertPesananSql = "INSERT INTO pesanan (id_pelanggan, id_user_admin, id_status, total_biaya, catatan) VALUES (?, ?, 1, ?, ?)";
            psPesanan = conn.prepareStatement(insertPesananSql, Statement.RETURN_GENERATED_KEYS);
            psPesanan.setInt(1, idPelanggan);
            psPesanan.setInt(2, idAdmin);
            psPesanan.setDouble(3, totalHarga);
            psPesanan.setString(4, spesifikasi);

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
            double hargaSatuan = totalHarga / jumlah;
            psDetail.setInt(1, idPesanan);
            psDetail.setInt(2, idJenisLayanan);
            psDetail.setInt(3, jumlah);
            psDetail.setDouble(4, hargaSatuan);
            psDetail.setDouble(5, totalHarga);
            psDetail.setString(6, spesifikasi);
            psDetail.executeUpdate();

            conn.commit();
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
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            try { if (psPelanggan != null) psPelanggan.close(); } catch (Exception ignored) {}
            try { if (psPesanan != null) psPesanan.close(); } catch (Exception ignored) {}
            try { if (psDetail != null) psDetail.close(); } catch (Exception ignored) {}
            try { if (conn != null) conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {}
        }
    }

    // Get all pesanan
    public List<Pesanan> getAllPesanan() {
        List<Pesanan> list = new ArrayList<>();
        String sql = """
            SELECT 
                p.id_pesanan,
                p.nomor_pesanan,
                pel.nama AS nama_pelanggan,
                pel.no_telepon,
                pel.email,
                jl.nama_layanan AS jenis_layanan,
                dp.jumlah,
                p.total_biaya AS total_harga,
                dp.spesifikasi,
                sp.nama_status AS status,
                p.created_at AS tanggal_pesanan,
                p.updated_at
            FROM pesanan p
            JOIN pelanggan pel ON p.id_pelanggan = pel.id_pelanggan
            JOIN status_pesanan sp ON p.id_status = sp.id_status
            LEFT JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan
            LEFT JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan
            ORDER BY p.created_at DESC
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Pesanan pesanan = mapResultSetToPesanan(rs);
                list.add(pesanan);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Get pesanan by status
    public List<Pesanan> getPesananByStatus(String statusName) {
        List<Pesanan> list = new ArrayList<>();
        String sql = """
            SELECT 
                p.id_pesanan,
                p.nomor_pesanan,
                pel.nama AS nama_pelanggan,
                pel.no_telepon,
                pel.email,
                jl.nama_layanan AS jenis_layanan,
                dp.jumlah,
                p.total_biaya AS total_harga,
                dp.spesifikasi,
                sp.nama_status AS status,
                p.created_at AS tanggal_pesanan,
                p.updated_at
            FROM pesanan p
            JOIN pelanggan pel ON p.id_pelanggan = pel.id_pelanggan
            JOIN status_pesanan sp ON p.id_status = sp.id_status
            LEFT JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan
            LEFT JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan
            WHERE sp.nama_status = ?
            ORDER BY p.created_at DESC
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statusName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Pesanan pesanan = mapResultSetToPesanan(rs);
                    list.add(pesanan);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Get pesanan for design team
    public List<Pesanan> getPesananForDesignTeam() {
        List<Pesanan> list = new ArrayList<>();
        String sql = """
        SELECT 
            p.id_pesanan,
            p.nomor_pesanan,
            pel.nama AS nama_pelanggan,
            pel.no_telepon,
            pel.email,
            jl.nama_layanan AS jenis_layanan,
            dp.jumlah,
            p.total_biaya AS total_harga,
            dp.spesifikasi,
            sp.nama_status AS status,
            tanggal_pesanan,
            p.updated_at,
            p.catatan
        FROM pesanan p
        JOIN pelanggan pel ON p.id_pelanggan = pel.id_pelanggan
        JOIN status_pesanan sp ON p.id_status = sp.id_status
        LEFT JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan
        LEFT JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan
        -- PERBAIKAN DI SINI: Tambahkan 'Pembayaran Verified'
        WHERE sp.nama_status IN ('Pembayaran Verified', 'Menunggu Desain', 'Desain Direvisi', 'Desain Disetujui')
        ORDER BY 
            CASE sp.nama_status
                WHEN 'Desain Direvisi' THEN 1
                WHEN 'Pembayaran Verified' THEN 2  -- Prioritas tinggi
                WHEN 'Menunggu Desain' THEN 3
                WHEN 'Desain Disetujui' THEN 4
            END,
            p.tanggal_pesanan ASC
        """;

        // ... sisa kode catch/try ...

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Pesanan pesanan = mapResultSetToPesanan(rs);
                list.add(pesanan);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int countByStatus(String statusName) {
        String sql = """
            SELECT COUNT(*) as total 
            FROM pesanan p
            JOIN status_pesanan sp ON p.id_status = sp.id_status
            WHERE sp.nama_status = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statusName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean updateStatus(int idPesanan, String newStatusName) {
        String sql = """
            UPDATE pesanan 
            SET id_status = (SELECT id_status FROM status_pesanan WHERE nama_status = ?),
                updated_at = NOW()
            WHERE id_pesanan = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatusName);
            ps.setInt(2, idPesanan);
            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateDesignStatus(int idPesanan, String action) {
        String newStatus;
        switch (action.toLowerCase()) {
            case "approve":
                newStatus = "Desain Disetujui";
                break;
            case "revisi":
                newStatus = "Desain Direvisi";
                break;
            case "to_production":
                newStatus = "Antrian Produksi";
                break;
            default:
                return false;
        }
        return updateStatus(idPesanan, newStatus);
    }

    public Pesanan getPesananById(int idPesanan) {
        String sql = """
            SELECT 
                p.id_pesanan,
                p.nomor_pesanan,
                pel.nama AS nama_pelanggan,
                pel.no_telepon,
                pel.email,
                jl.nama_layanan AS jenis_layanan,
                dp.jumlah,
                p.total_biaya AS total_harga,
                dp.spesifikasi,
                sp.nama_status AS status,
                p.tanggal_pesanan,
                p.updated_at,
                p.catatan
            FROM pesanan p
            JOIN pelanggan pel ON p.id_pelanggan = pel.id_pelanggan
            JOIN status_pesanan sp ON p.id_status = sp.id_status
            LEFT JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan
            LEFT JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan
            WHERE p.id_pesanan = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPesanan);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPesanan(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Helper Methods
    private Pesanan mapResultSetToPesanan(ResultSet rs) throws SQLException {
        Pesanan pesanan = new Pesanan();
        pesanan.setIdPesanan(rs.getInt("id_pesanan"));
        pesanan.setNomorPesanan(rs.getString("nomor_pesanan"));
        pesanan.setNamaPelanggan(rs.getString("nama_pelanggan"));
        pesanan.setNoTelepon(rs.getString("no_telepon"));
        pesanan.setEmail(rs.getString("email"));
        pesanan.setJenisLayanan(rs.getString("jenis_layanan"));
        pesanan.setJumlah(rs.getInt("jumlah"));
        pesanan.setTotalHarga(rs.getDouble("total_harga"));
        pesanan.setSpesifikasi(rs.getString("spesifikasi"));
        pesanan.setStatus(rs.getString("status"));

        // Catatan dari pesanan
        String catatan = rs.getString("catatan");
        if (catatan != null && !catatan.isEmpty()) {
            String existingSpec = pesanan.getSpesifikasi();
            if (existingSpec != null && !existingSpec.isEmpty()) {
                pesanan.setCatatan(catatan);
            } else {
                pesanan.setSpesifikasi(catatan);
            }
        }

        Timestamp tanggalTs = rs.getTimestamp("tanggal_pesanan");
        if (tanggalTs != null) {
            pesanan.setTanggalPesanan(tanggalTs.toLocalDateTime());
        }

        Timestamp updatedTs = rs.getTimestamp("updated_at");
        if (updatedTs != null) {
            pesanan.setUpdatedAt(updatedTs.toLocalDateTime());
        }

        return pesanan;
    }

    private int getLayananIdByName(Connection conn, String namaLayanan) throws SQLException {
        String sql = "SELECT id_layanan FROM jenis_layanan WHERE nama_layanan = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaLayanan);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id_layanan");
        }
        return 1;
    }
}