package com.example.trying3.dao;

import com.example.trying3.config.DatabaseConnection;
import com.example.trying3.model.Pesanan;
import com.example.trying3.util.SessionManager;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class PesananDAO {

    // ==================================================================================
    // 1. FUNGSI UTAMA (CREATE PESANAN)
    // ==================================================================================
    public boolean createPesanan(String namaPelanggan, String noTelepon, String email, String jenisLayanan, int jumlah, double totalHarga, String spesifikasi) {
        Connection conn = null;
        PreparedStatement psPelanggan = null;
        PreparedStatement psPesanan = null;
        PreparedStatement psDetail = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // A. HANDLE PELANGGAN
            int idPelanggan = -1;
            String checkPelangganSql = "SELECT id_pelanggan FROM pelanggan WHERE no_telepon = ?";
            psPelanggan = conn.prepareStatement(checkPelangganSql);
            psPelanggan.setString(1, noTelepon);
            rs = psPelanggan.executeQuery();

            if (rs.next()) {
                idPelanggan = rs.getInt("id_pelanggan");
            } else {
                String insertPelangganSql = "INSERT INTO pelanggan (nama, no_telepon, email) VALUES (?, ?, ?)";
                psPelanggan = conn.prepareStatement(insertPelangganSql, Statement.RETURN_GENERATED_KEYS);
                psPelanggan.setString(1, namaPelanggan);
                psPelanggan.setString(2, noTelepon);
                psPelanggan.setString(3, email);
                psPelanggan.executeUpdate();
                rs = psPelanggan.getGeneratedKeys();
                if (rs.next()) idPelanggan = rs.getInt(1);
            }

            // B. HANDLE PESANAN
            int idAdmin = SessionManager.getInstance().getCurrentUserId();
            // Fallback jika session kosong (untuk testing)
            if (idAdmin == -1) idAdmin = 1;

            String insertPesananSql = "INSERT INTO pesanan (id_pelanggan, id_user_admin, id_status, total_biaya, catatan) VALUES (?, ?, 1, ?, ?)";
            psPesanan = conn.prepareStatement(insertPesananSql, Statement.RETURN_GENERATED_KEYS);
            psPesanan.setInt(1, idPelanggan);
            psPesanan.setInt(2, idAdmin);
            psPesanan.setDouble(3, totalHarga);
            psPesanan.setString(4, spesifikasi);

            if (psPesanan.executeUpdate() == 0) throw new SQLException("Gagal membuat pesanan.");

            int idPesanan = -1;
            rs = psPesanan.getGeneratedKeys();
            if (rs.next()) idPesanan = rs.getInt(1);

            // C. HANDLE DETAIL PESANAN
            int idJenisLayanan = getLayananIdByName(conn, jenisLayanan);
            String insertDetailSql = "INSERT INTO detail_pesanan (id_pesanan, id_layanan, jumlah, harga_satuan, subtotal, spesifikasi) VALUES (?, ?, ?, ?, ?, ?)";
            psDetail = conn.prepareStatement(insertDetailSql);
            double hargaSatuan = jumlah > 0 ? totalHarga / jumlah : 0;

            psDetail.setInt(1, idPesanan);
            psDetail.setInt(2, idJenisLayanan);
            psDetail.setInt(3, jumlah);
            psDetail.setDouble(4, hargaSatuan);
            psDetail.setDouble(5, totalHarga);
            psDetail.setString(6, spesifikasi);
            psDetail.executeUpdate();

            conn.commit();
            System.out.println("✅ Pesanan Berhasil Dibuat: ID " + idPesanan);
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            closeResources(rs, psPelanggan, psPesanan, psDetail, conn);
        }
    }

    // ==================================================================================
    // 2. INTEGRASI DESIGN & PRODUKSI (NEW)
    // ==================================================================================

    /**
     * Menyimpan file desain ke tabel 'desain'.
     * Jika belum ada record, insert. Jika sudah ada, update.
     */
    public boolean simpanDesain(int idPesanan, String filePath, int idDesigner) {
        // Cek apakah data desain sudah ada
        String checkSql = "SELECT id_desain FROM desain WHERE id_pesanan = ?";
        boolean exists = false;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setInt(1, idPesanan);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) exists = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        String sql;
        if (exists) {
            // Update record yang ada
            sql = "UPDATE desain SET file_desain_path = ?, id_designer = ?, updated_at = NOW() WHERE id_pesanan = ?";
        } else {
            // Insert baru (Status ID 2 = Dalam Pengerjaan)
            sql = "INSERT INTO desain (file_desain_path, id_designer, id_pesanan, id_status_desain) VALUES (?, ?, ?, 2)";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, filePath);
            ps.setInt(2, idDesigner);
            ps.setInt(3, idPesanan);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Menyetujui desain:
     * 1. Update tabel PESANAN -> Status 'Antrian Produksi' (ID 7).
     * 2. Update tabel DESAIN -> Status 'Disetujui' (ID 5).
     */
    public boolean approveDesain(int idPesanan) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Transaksi Atomik

            // Update Pesanan
            String sqlPesanan = "UPDATE pesanan SET id_status = 7 WHERE id_pesanan = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlPesanan)) {
                ps.setInt(1, idPesanan);
                ps.executeUpdate();
            }

            // Update Desain
            String sqlDesain = "UPDATE desain SET id_status_desain = 5, tanggal_disetujui = NOW() WHERE id_pesanan = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlDesain)) {
                ps.setInt(1, idPesanan);
                ps.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) {}
            e.printStackTrace();
            return false;
        } finally {
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (SQLException e) {}
        }
    }

    /**
     * Mengambil data untuk Dashboard Produksi.
     * Melakukan JOIN ke tabel 'desain' untuk mengambil path file.
     */
    public List<Pesanan> getPesananForProduction() {
        List<Pesanan> list = new ArrayList<>();
        String sql = """
            SELECT 
                p.id_pesanan, p.nomor_pesanan, p.tanggal_pesanan,
                pl.nama AS nama_pelanggan, pl.no_telepon, pl.email,
                dp.jumlah, dp.spesifikasi,
                jl.nama_layanan,
                p.total_biaya AS total_harga,
                sp.nama_status AS status,
                p.updated_at, p.catatan,
                d.file_desain_path
            FROM pesanan p
            JOIN pelanggan pl ON p.id_pelanggan = pl.id_pelanggan
            JOIN status_pesanan sp ON p.id_status = sp.id_status
            LEFT JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan
            LEFT JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan
            LEFT JOIN desain d ON p.id_pesanan = d.id_pesanan
            WHERE sp.nama_status IN ('Antrian Produksi', 'Sedang Diproduksi', 'Selesai')
            ORDER BY 
                CASE sp.nama_status
                    WHEN 'Sedang Diproduksi' THEN 1
                    WHEN 'Antrian Produksi' THEN 2
                    ELSE 3
                END,
                p.updated_at ASC
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToPesanan(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ==================================================================================
    // 3. GET DATA PESANAN (UMUM & DESIGN)
    // ==================================================================================

    public List<Pesanan> getPesananForDesignTeam() {
        List<Pesanan> list = new ArrayList<>();
        String sql = """
        SELECT 
            p.id_pesanan, p.nomor_pesanan, p.tanggal_pesanan,
            pl.nama AS nama_pelanggan, pl.no_telepon, pl.email,
            jl.nama_layanan,
            dp.jumlah, dp.spesifikasi,
            p.total_biaya AS total_harga,
            sp.nama_status AS status,
            p.updated_at, p.catatan,
            d.file_desain_path -- Optional: jika ingin lihat file yg sudah diupload
        FROM pesanan p
        JOIN pelanggan pl ON p.id_pelanggan = pl.id_pelanggan
        JOIN status_pesanan sp ON p.id_status = sp.id_status
        LEFT JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan
        LEFT JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan
        LEFT JOIN desain d ON p.id_pesanan = d.id_pesanan
        WHERE sp.nama_status IN ('Pembayaran Verified', 'Menunggu Desain', 'Desain Direvisi', 'Desain Disetujui')
        ORDER BY p.tanggal_pesanan ASC
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToPesanan(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Pesanan> getAllPesanan() {
        return executeQueryToList("""
            SELECT p.id_pesanan, p.nomor_pesanan, pl.nama AS nama_pelanggan, pl.no_telepon, pl.email,
                   jl.nama_layanan, dp.jumlah, p.total_biaya AS total_harga, dp.spesifikasi,
                   sp.nama_status AS status, p.tanggal_pesanan, p.updated_at, p.catatan
            FROM pesanan p
            JOIN pelanggan pel ON p.id_pelanggan = pel.id_pelanggan
            JOIN status_pesanan sp ON p.id_status = sp.id_status
            LEFT JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan
            LEFT JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan
            ORDER BY p.created_at DESC
        """);
    }

    public List<Pesanan> getPesananByStatus(String statusName) {
        String sql = """
            SELECT p.id_pesanan, p.nomor_pesanan, pl.nama AS nama_pelanggan, pl.no_telepon, pl.email,
                   jl.nama_layanan, dp.jumlah, p.total_biaya AS total_harga, dp.spesifikasi,
                   sp.nama_status AS status, p.tanggal_pesanan, p.updated_at, p.catatan
            FROM pesanan p
            JOIN pelanggan pel ON p.id_pelanggan = pel.id_pelanggan
            JOIN status_pesanan sp ON p.id_status = sp.id_status
            LEFT JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan
            LEFT JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan
            WHERE sp.nama_status = ?
            ORDER BY p.created_at DESC
        """;
        List<Pesanan> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statusName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToPesanan(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ==================================================================================
    // 4. UPDATE & UTILITIES
    // ==================================================================================

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
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateDesignStatus(int idPesanan, String action) {
        // Fungsi helper untuk status lain (misal: revisi)
        // Kalau Approve, sebaiknya pakai approveDesain() agar tabel desain ikut terupdate
        switch (action.toLowerCase()) {
            case "approve":
                return approveDesain(idPesanan);
            case "revisi":
                return updateStatus(idPesanan, "Desain Direvisi");
            default:
                return false;
        }
    }

    public int countByStatus(String statusName) {
        String sql = "SELECT COUNT(*) FROM pesanan p JOIN status_pesanan sp ON p.id_status = sp.id_status WHERE sp.nama_status = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statusName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // ==================================================================================
    // 5. MAPPERS & HELPERS
    // ==================================================================================

    private Pesanan mapResultSetToPesanan(ResultSet rs) throws SQLException {
        Pesanan pesanan = new Pesanan();
        pesanan.setIdPesanan(rs.getInt("id_pesanan"));
        pesanan.setNomorPesanan(rs.getString("nomor_pesanan"));
        pesanan.setNamaPelanggan(rs.getString("nama_pelanggan"));

        // Handle kolom yang mungkin null/tidak diselect di semua query
        try { pesanan.setNoTelepon(rs.getString("no_telepon")); } catch (SQLException e) {}
        try { pesanan.setEmail(rs.getString("email")); } catch (SQLException e) {}

        pesanan.setJenisLayanan(rs.getString("nama_layanan")); // Alias dari query
        pesanan.setJumlah(rs.getInt("jumlah"));
        pesanan.setTotalBiaya(rs.getDouble("total_harga"));
        pesanan.setSpesifikasi(rs.getString("spesifikasi"));
        pesanan.setStatus(rs.getString("status"));

        // Handle kolom file_desain_path dari tabel desain (jika ada di query)
        try {
            String filePath = rs.getString("file_desain_path");
            pesanan.setFileDesainPath(filePath != null ? filePath : null);
        } catch (SQLException e) {
            // Kolom ini tidak ada di query biasa, abaikan
        }

        try {
            String cat = rs.getString("catatan");
            pesanan.setCatatan(cat);
        } catch (SQLException e) {}

        Timestamp tanggalTs = rs.getTimestamp("tanggal_pesanan");
        if (tanggalTs != null) pesanan.setTanggalPesanan(tanggalTs.toLocalDateTime());

        Timestamp updatedTs = rs.getTimestamp("updated_at");
        if (updatedTs != null) pesanan.setUpdatedAt(updatedTs.toLocalDateTime());

        return pesanan;
    }

    private int getLayananIdByName(Connection conn, String namaLayanan) throws SQLException {
        String sql = "SELECT id_layanan FROM jenis_layanan WHERE nama_layanan = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaLayanan);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id_layanan");
        }
        return 1; // Default
    }

    private List<Pesanan> executeQueryToList(String sql) {
        List<Pesanan> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapResultSetToPesanan(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private void closeResources(AutoCloseable... resources) {
        for (AutoCloseable res : resources) {
            if (res != null) {
                try { res.close(); } catch (Exception ignored) {}
            }
        }
    }
    public boolean updateCatatan(int idPesanan, String catatanBaru) {
        String sql = "UPDATE pesanan SET catatan = ? WHERE id_pesanan = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, catatanBaru);
            ps.setInt(2, idPesanan);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
// ==================================================================================
    // BAGIAN LAPORAN & MANAJEMEN (FIXED SCHEMA BERDASARKAN KELOLAPESANANCONTROLLER)
    // ==================================================================================

    /**
     * Mengambil ringkasan statistik
     * FIX: Join ke tabel status_pesanan untuk filter 'Selesai'/'Pending'
     */
    public double[] getLaporanSummary(String periode) {
        double[] stats = {0, 0, 0, 0};

        // Query menggabungkan pesanan dengan status_pesanan untuk cek nama status
        String sql = "SELECT " +
                "COUNT(p.id_pesanan) as total_semua, " +
                "SUM(CASE WHEN sp.nama_status = 'Selesai' THEN 1 ELSE 0 END) as total_selesai, " +
                "SUM(CASE WHEN sp.nama_status NOT IN ('Selesai', 'Dibatalkan') THEN 1 ELSE 0 END) as total_tertunda, " +
                "SUM(CASE WHEN sp.nama_status != 'Dibatalkan' THEN p.total_biaya ELSE 0 END) as total_pendapatan " +
                "FROM pesanan p " +
                "JOIN status_pesanan sp ON p.id_status = sp.id_status " +
                "WHERE " + getPeriodeCondition(periode, "p.tanggal_pesanan");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                stats[0] = rs.getInt("total_semua");
                stats[1] = rs.getInt("total_selesai");
                stats[2] = rs.getInt("total_tertunda");
                stats[3] = rs.getDouble("total_pendapatan");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }

    /**
     * Mengambil performa layanan
     * FIX: Join 3 Tabel (pesanan -> detail_pesanan -> jenis_layanan)
     */
    public List<String[]> getAnalisaLayanan(String periode) {
        List<String[]> list = new ArrayList<>();

        // Kita harus join dari pesanan ke detail, lalu ke jenis layanan untuk dapat namanya
        String sql = "SELECT jl.nama_layanan, COUNT(p.id_pesanan) as jumlah, SUM(p.total_biaya) as total_uang " +
                "FROM pesanan p " +
                "JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan " +
                "JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan " +
                "WHERE " + getPeriodeCondition(periode, "p.tanggal_pesanan") + " " +
                "GROUP BY jl.nama_layanan " +
                "ORDER BY total_uang DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new String[]{
                        rs.getString("nama_layanan"),
                        String.valueOf(rs.getInt("jumlah")),
                        String.valueOf(rs.getDouble("total_uang"))
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Mengambil 5 aktivitas terbaru
     * FIX: Join Lengkap (Pelanggan, Status, Detail, Layanan) agar tidak ada error column not found
     */
    public List<Pesanan> getAktivitasTerbaru() {
        List<Pesanan> list = new ArrayList<>();

        // Query ini meniru logika loadPesananData di KelolaPesananController
        String sql = "SELECT p.*, " +
                "pel.nama as nama_pelanggan, pel.no_telepon, pel.email, " +
                "sp.nama_status, " +
                "jl.nama_layanan as jenis_layanan " +
                "FROM pesanan p " +
                "JOIN pelanggan pel ON p.id_pelanggan = pel.id_pelanggan " +
                "JOIN status_pesanan sp ON p.id_status = sp.id_status " +
                "LEFT JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan " +
                "LEFT JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan " +
                "ORDER BY p.id_pesanan DESC LIMIT 5";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Pesanan p = new Pesanan();
                // Mapping manual agar aman
                p.setIdPesanan(rs.getInt("id_pesanan"));
                p.setNamaPelanggan(rs.getString("nama_pelanggan")); // Dari tabel pelanggan
                p.setNoTelepon(rs.getString("no_telepon"));
                p.setEmail(rs.getString("email"));
                p.setStatus(rs.getString("nama_status")); // Dari tabel status_pesanan

                // Layanan mungkin null jika left join gagal, kita handle
                String layanan = rs.getString("jenis_layanan");
                p.setJenisLayanan(layanan != null ? layanan : "-");

                // Perhatikan: nama kolom di DB 'total_biaya', tapi di Model Pesanan 'totalHarga'
                p.setTotalBiaya(rs.getDouble("total_biaya"));

                Timestamp ts = rs.getTimestamp("tanggal_pesanan");
                if(ts != null) p.setTanggalPesanan(ts.toLocalDateTime());

                list.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Helper Filter SQL (Pastikan nama kolom tanggal benar: p.tanggal_pesanan)
    private String getPeriodeCondition(String periode, String colName) {
        if (periode == null) return "1=1";

        switch (periode) {
            case "Harian":
                return "DATE(" + colName + ") = CURDATE()";
            case "Mingguan":
                return colName + " >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)";
            case "Bulanan":
                return "MONTH(" + colName + ") = MONTH(CURDATE()) AND YEAR(" + colName + ") = YEAR(CURDATE())";
            case "Tahunan":
                return "YEAR(" + colName + ") = YEAR(CURDATE())";
            default:
                return "1=1";
        }
    }
    public List<Pesanan> getPesananForExport(String periode) {
        List<Pesanan> exportList = new ArrayList<>();

        String sql = "SELECT p.*, " +
                "pel.nama as nama_pelanggan_real, pel.no_telepon, pel.email, " +
                "sp.nama_status, " +
                "jl.nama_layanan " + // Ambil nama layanan dari tabel jenis_layanan
                "FROM pesanan p " +
                "JOIN pelanggan pel ON p.id_pelanggan = pel.id_pelanggan " +
                "JOIN status_pesanan sp ON p.id_status = sp.id_status " +
                "LEFT JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan " +
                "LEFT JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan " +
                "WHERE " + getPeriodeCondition(periode, "p.tanggal_pesanan") + " " +
                "ORDER BY p.tanggal_pesanan DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Pesanan p = new Pesanan();

                // 1. Mapping ID & Tanggal
                p.setIdPesanan(rs.getInt("id_pesanan"));
                Timestamp ts = rs.getTimestamp("tanggal_pesanan");
                if (ts != null) p.setTanggalPesanan(ts.toLocalDateTime());

                // 2. Mapping Data Pelanggan (Dari tabel Pelanggan)
                p.setNamaPelanggan(rs.getString("nama_pelanggan_real"));
                p.setNoTelepon(rs.getString("no_telepon"));
                p.setEmail(rs.getString("email"));

                // 3. Mapping Status (Dari tabel Status)
                p.setStatus(rs.getString("nama_status"));

                // 4. Mapping Layanan (Dari tabel Jenis Layanan)
                String layanan = rs.getString("nama_layanan");
                p.setJenisLayanan(layanan != null ? layanan : "-");

                // 5. Mapping Keuangan
                p.setTotalBiaya(rs.getDouble("total_biaya")); // Kolom di DB kamu 'total_biaya'

                // Tambahkan ke list
                exportList.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return exportList;
    }

    public int getTotalPesananCount() {
        String sql = "SELECT COUNT(*) FROM pesanan";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public double getTotalRevenue() {
        String sql = "SELECT SUM(total_biaya) FROM pesanan";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int getSelesaiCount() {
        String sql = "SELECT COUNT(*) FROM pesanan p JOIN status_pesanan s ON p.id_status = s.id_status WHERE s.nama_status = 'Selesai'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public Map<String, Integer> getServiceDistribution() {
        Map<String, Integer> dist = new LinkedHashMap<>();
        // Query ini menghubungkan pesanan -> detail -> jenis_layanan
        String sql = "SELECT jl.nama_layanan, COUNT(dp.id_layanan) " +
                "FROM pesanan p " +
                "JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan " +
                "JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan " +
                "GROUP BY jl.nama_layanan";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                dist.put(rs.getString(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            System.err.println("Error Service Distribution: " + e.getMessage());
        }
        return dist;
    }

    public Map<String, Integer> getStatusDistribution() {
        Map<String, Integer> map = new HashMap<>();
        // Gunakan LEFT JOIN untuk status
        String sql = "SELECT IFNULL(s.nama_status, 'Tanpa Status'), COUNT(p.id_pesanan) " +
                "FROM pesanan p " +
                "LEFT JOIN status_pesanan s ON p.id_status = s.id_status " +
                "GROUP BY s.nama_status";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                map.put(rs.getString(1), rs.getInt(2));
            }
            System.out.println("DEBUG Status Chart Data: " + map);

        } catch (SQLException e) {
            System.err.println("Error getStatusDistribution: " + e.getMessage());
        }
        return map;
    }

    public Map<String, Integer> getOrderTrend() {
        Map<String, Integer> trend = new LinkedHashMap<>();
        String sql = "SELECT DATE_FORMAT(tanggal_pesanan, '%d %b') as label_tgl, COUNT(*) " +
                "FROM pesanan " +
                "GROUP BY label_tgl, DATE(tanggal_pesanan) " +
                "ORDER BY DATE(tanggal_pesanan) ASC LIMIT 7";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                trend.put(rs.getString(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return trend;
    }

    public Map<String, Double> getRevenueDistribution() {
        Map<String, Double> dist = new HashMap<>();
        String sql = "SELECT jl.nama_layanan, SUM(p.total_biaya) " +
                "FROM pesanan p " +
                "JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan " +
                "JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan " +
                "GROUP BY jl.nama_layanan";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                dist.put(rs.getString(1), rs.getDouble(2));
            }
        } catch (SQLException e) {
            System.err.println("Error Revenue: " + e.getMessage());
        }
        return dist;
    }

    public List<Pesanan> getAllPesananForExport() {
        List<Pesanan> list = new ArrayList<>();
        // QUERY DIPERBAIKI: JOIN ke pelanggan dan status_pesanan
        String sql = "SELECT p.id_pesanan, pl.nama_pelanggan, " +
                "COALESCE(jl.nama_layanan, 'N/A') as nama_layanan, " +
                "p.total_biaya, s.nama_status, p.tanggal_pesanan " +
                "FROM pesanan p " +
                "JOIN pelanggan pl ON p.id_pelanggan = pl.id_pelanggan " +
                "JOIN status_pesanan s ON p.id_status = s.id_status " + // Ambil teks status
                "LEFT JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan " +
                "LEFT JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan " +
                "ORDER BY p.tanggal_pesanan DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Pesanan p = new Pesanan();
                p.setIdPesanan(rs.getInt("id_pesanan"));
                p.setNamaPelanggan(rs.getString("nama_pelanggan"));
                p.setJenisLayanan(rs.getString("nama_layanan"));
                p.setTotalBiaya(rs.getDouble("total_biaya"));
                p.setStatus(rs.getString("nama_status")); // Mengambil teks 'Baru Dibuat', 'Selesai', dll

                Timestamp ts = rs.getTimestamp("tanggal_pesanan");
                if (ts != null) p.setTanggalPesanan(ts.toLocalDateTime());

                list.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Error Export Data: " + e.getMessage());
        }
        return list;
    }

    // Tambahkan/Update metode ini di PesananDAO.java
    public List<Pesanan> getLaporanProduksi() {
        List<Pesanan> list = new ArrayList<>();
        // Query khusus produksi untuk melihat status pengerjaan
        String sql = "SELECT p.id_pesanan, pl.nama_pelanggan, s.nama_status, p.total_biaya " +
                "FROM pesanan p " +
                "JOIN pelanggan pl ON p.id_pelanggan = pl.id_pelanggan " +
                "JOIN status_pesanan s ON p.id_status = s.id_status " +
                "WHERE s.nama_status != 'Selesai'"; // Contoh: hanya yang sedang diproses

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Pesanan p = new Pesanan();
                p.setIdPesanan(rs.getInt("id_pesanan"));
                p.setNamaPelanggan(rs.getString("nama_pelanggan"));
                p.setJenisLayanan(rs.getString("nama_status"));
                p.setTotalBiaya(rs.getDouble("total_biaya"));

                list.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Error Export Data: " + e.getMessage());
        }

        return list;
    }

    // ==================================================================================
// TAMBAHKAN CODE INI KE FILE: PesananDAO.java
// Letakkan di bagian "INTEGRASI DESIGN & PRODUKSI"
// ==================================================================================

// Import tambahan di bagian atas file:
// import com.example.trying3.model.RevisiDesain;
// import com.example.trying3.dao.RevisiDesainDAO;

    /**
     * Menyimpan file desain dengan dukungan revisi.
     * Jika desain sudah ada DAN status = "Perlu Revisi" (id_status_desain = 4),
     * maka file lama disimpan ke tabel revisi_desain sebagai history.
     *
     * @param idPesanan ID pesanan
     * @param newFilePath Path file desain baru
     * @param idDesigner ID designer yang upload
     * @return true jika berhasil
     */
    // ==================================================================================
// GANTI METHOD simpanDesainDenganRevisi() DI PesananDAO.java DENGAN INI
// Versi dengan DEBUG LOG untuk troubleshooting
// ==================================================================================

    public boolean simpanDesainDenganRevisi(int idPesanan, String newFilePath, int idDesigner) {
        Connection conn = null;

        System.out.println("========================================");
        System.out.println("🔍 DEBUG: simpanDesainDenganRevisi() dipanggil");
        System.out.println("🔍 idPesanan: " + idPesanan);
        System.out.println("🔍 newFilePath: " + newFilePath);
        System.out.println("🔍 idDesigner: " + idDesigner);
        System.out.println("========================================");

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Cek apakah desain sudah ada dan ambil data existing
            String checkSql = """
            SELECT id_desain, file_desain_path, revisi_ke, id_status_desain 
            FROM desain 
            WHERE id_pesanan = ?
            """;

            int idDesain = -1;
            String oldFilePath = null;
            int currentRevisiKe = 1;
            int currentStatusDesain = 0;

            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setInt(1, idPesanan);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        idDesain = rs.getInt("id_desain");
                        oldFilePath = rs.getString("file_desain_path");
                        currentRevisiKe = rs.getInt("revisi_ke");
                        currentStatusDesain = rs.getInt("id_status_desain");
                    }
                }
            }

            System.out.println("🔍 DEBUG: Data desain existing:");
            System.out.println("   - idDesain: " + idDesain);
            System.out.println("   - oldFilePath: " + oldFilePath);
            System.out.println("   - currentRevisiKe: " + currentRevisiKe);
            System.out.println("   - currentStatusDesain: " + currentStatusDesain);

            if (idDesain == -1) {
                // Desain belum ada, insert baru (revisi_ke = 1)
                System.out.println("🔍 DEBUG: Desain belum ada, INSERT baru...");

                String insertSql = """
                INSERT INTO desain 
                (id_pesanan, id_designer, id_status_desain, file_desain_path, revisi_ke) 
                VALUES (?, ?, 2, ?, 1)
                """;

                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setInt(1, idPesanan);
                    ps.setInt(2, idDesigner);
                    ps.setString(3, newFilePath);
                    int rows = ps.executeUpdate();
                    System.out.println("🔍 DEBUG: INSERT rows affected: " + rows);
                }

                System.out.println("✅ Desain baru berhasil dibuat untuk pesanan: " + idPesanan);

            } else {
                // Desain sudah ada
                System.out.println("🔍 DEBUG: Desain sudah ada, cek apakah revisi...");

                // Cek apakah ini adalah revisi (status = 4 "Perlu Revisi")
                // ATAU file lama ada (berarti ini update/revisi)
                boolean isRevisi = (currentStatusDesain == 4) ||
                        (oldFilePath != null && !oldFilePath.isEmpty());

                System.out.println("🔍 DEBUG: isRevisi = " + isRevisi);
                System.out.println("   - currentStatusDesain == 4? " + (currentStatusDesain == 4));
                System.out.println("   - oldFilePath ada? " + (oldFilePath != null && !oldFilePath.isEmpty()));

                if (isRevisi && oldFilePath != null && !oldFilePath.isEmpty()) {
                    System.out.println("🔍 DEBUG: Menyimpan file lama ke revisi_desain...");

                    // 2. Simpan file lama ke tabel revisi_desain sebagai history
                    String insertRevisiSql = """
                    INSERT INTO revisi_desain 
                    (id_desain, revisi_ke, file_path, catatan_revisi, direvisi_oleh, tanggal_revisi) 
                    VALUES (?, ?, ?, ?, ?, NOW())
                    """;

                    try (PreparedStatement ps = conn.prepareStatement(insertRevisiSql)) {
                        ps.setInt(1, idDesain);
                        ps.setInt(2, currentRevisiKe);
                        ps.setString(3, oldFilePath);
                        ps.setString(4, "Revisi dari file sebelumnya");
                        ps.setInt(5, idDesigner);

                        int rows = ps.executeUpdate();
                        System.out.println("🔍 DEBUG: INSERT revisi_desain rows affected: " + rows);
                    }

                    System.out.println("✅ File lama disimpan ke history revisi: " + oldFilePath);

                    // Increment revisi_ke untuk file baru
                    currentRevisiKe++;
                    System.out.println("🔍 DEBUG: revisi_ke setelah increment: " + currentRevisiKe);
                } else {
                    System.out.println("⚠️ DEBUG: TIDAK menyimpan ke revisi_desain karena kondisi tidak terpenuhi!");
                }

                // 3. Update tabel desain dengan file baru
                System.out.println("🔍 DEBUG: Update tabel desain dengan file baru...");

                String updateSql = """
                UPDATE desain 
                SET file_desain_path = ?, 
                    id_designer = ?, 
                    revisi_ke = ?,
                    id_status_desain = 2,
                    updated_at = NOW() 
                WHERE id_desain = ?
                """;

                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, newFilePath);
                    ps.setInt(2, idDesigner);
                    ps.setInt(3, currentRevisiKe);
                    ps.setInt(4, idDesain);
                    int rows = ps.executeUpdate();
                    System.out.println("🔍 DEBUG: UPDATE desain rows affected: " + rows);
                }

                System.out.println("✅ Desain berhasil diupdate. Revisi ke-" + currentRevisiKe);
            }

            conn.commit();
            System.out.println("✅ COMMIT berhasil!");
            System.out.println("========================================");
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Error menyimpan desain dengan revisi: " + e.getMessage());
            e.printStackTrace();

            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return false;

        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    /**
     * Mengambil data desain lengkap untuk pesanan tertentu
     * Termasuk info revisi_ke
     */
    public DesainInfo getDesainInfo(int idPesanan) {
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
            }

        } catch (SQLException e) {
            System.err.println("❌ Error mengambil info desain: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // ==================================================================================
// INNER CLASS untuk menyimpan info desain (atau buat file terpisah)
// ==================================================================================
    public static class DesainInfo {
        private int idDesain;
        private String filePath;
        private int revisiKe;
        private int idStatusDesain;
        private String statusDesain;
        private String namaDesigner;
        private LocalDateTime tanggalDibuat;
        private LocalDateTime tanggalDisetujui;

        // Getters and Setters
        public int getIdDesain() { return idDesain; }
        public void setIdDesain(int idDesain) { this.idDesain = idDesain; }

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }

        public int getRevisiKe() { return revisiKe; }
        public void setRevisiKe(int revisiKe) { this.revisiKe = revisiKe; }

        public int getIdStatusDesain() { return idStatusDesain; }
        public void setIdStatusDesain(int idStatusDesain) { this.idStatusDesain = idStatusDesain; }

        public String getStatusDesain() { return statusDesain; }
        public void setStatusDesain(String statusDesain) { this.statusDesain = statusDesain; }

        public String getNamaDesigner() { return namaDesigner; }
        public void setNamaDesigner(String namaDesigner) { this.namaDesigner = namaDesigner; }

        public LocalDateTime getTanggalDibuat() { return tanggalDibuat; }
        public void setTanggalDibuat(LocalDateTime tanggalDibuat) { this.tanggalDibuat = tanggalDibuat; }

        public LocalDateTime getTanggalDisetujui() { return tanggalDisetujui; }
        public void setTanggalDisetujui(LocalDateTime tanggalDisetujui) { this.tanggalDisetujui = tanggalDisetujui; }

        public boolean isPerluRevisi() {
            return idStatusDesain == 4; // Status "Perlu Revisi"
        }
    }
}