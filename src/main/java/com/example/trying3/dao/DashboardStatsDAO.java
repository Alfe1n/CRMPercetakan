package com.example.trying3.dao;

import com.example.trying3.config.DatabaseConnection;
import com.example.trying3.model.Pesanan;
import com.example.trying3.model.User;

import java.sql.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardStatsDAO {

    // Statistik Pesanan

    /**
     * Mengambil total semua pesanan
     */
    public int getTotalPesanan() {
        String sql = "SELECT COUNT(*) FROM pesanan";
        return executeCountQuery(sql);
    }

    /**
     * Mengambil jumlah pesanan dengan status 'Selesai'
     */
    public int getPesananSelesai() {
        String sql = """
            SELECT COUNT(*) FROM pesanan p
            JOIN status_pesanan sp ON p.id_status = sp.id_status
            WHERE sp.nama_status = 'Selesai'
        """;
        return executeCountQuery(sql);
    }

    /**
     * Menghitung persentase pesanan selesai dari total
     */
    public String getPersentasePesananSelesai() {
        int total = getTotalPesanan();
        int selesai = getPesananSelesai();
        if (total == 0) return "0%";
        int persen = (selesai * 100) / total;
        return persen + "% dari total";
    }

    /**
     * Mengambil jumlah pembayaran dengan status 'pending'
     */
    public int getPembayaranPending() {
        String sql = """
            SELECT COUNT(*) FROM pesanan p
            JOIN status_pesanan sp ON p.id_status = sp.id_status
            WHERE sp.nama_status = 'Menunggu Pembayaran'
        """;
        return executeCountQuery(sql);
    }

    /**
     * Mengambil total pendapatan dari pembayaran yang sudah verified
     */
    public double getTotalPendapatan() {
        String sql = "SELECT COALESCE(SUM(jumlah), 0) FROM pembayaran WHERE status_pembayaran = 'verified'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Format total pendapatan ke format Rupiah
     */
    public String getFormattedTotalPendapatan() {
        double total = getTotalPendapatan();
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        String formatted = formatter.format(total);
        // Hilangkan ,00 di belakang jika ada
        return formatted.replace(",00", "").replace("Rp", "Rp ");
    }

    // Dashboard Stats View

    /**
     * Model untuk menampung data dari view v_dashboard_stats
     */
    public static class DashboardStats {
        public int pesananHariIni;
        public int pesananMingguIni;
        public int pesananSelesai;
        public int pembayaranPending;
        public double pendapatanBulanIni;
        public int produksiBerjalan;
        public int kendalaAktif;
    }

    /**
     * Mengambil semua statistik sekaligus dari view v_dashboard_stats
     */
    public DashboardStats getAllStats() {
        String sql = "SELECT * FROM v_dashboard_stats";
        DashboardStats stats = new DashboardStats();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                stats.pesananHariIni = rs.getInt("pesanan_hari_ini");
                stats.pesananMingguIni = rs.getInt("pesanan_minggu_ini");
                stats.pesananSelesai = rs.getInt("pesanan_selesai");
                stats.pembayaranPending = rs.getInt("pembayaran_pending");
                stats.pendapatanBulanIni = rs.getDouble("pendapatan_bulan_ini");
                stats.produksiBerjalan = rs.getInt("produksi_berjalan");
                stats.kendalaAktif = rs.getInt("kendala_aktif");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }

    // Pesanan Terbaru

    /**
     * Mengambil pesanan terbaru dengan limit tertentu
     * Data sudah di-join dengan pelanggan, status, dan detail pesanan
     */
    public List<Pesanan> getPesananTerbaru(int limit) {
        List<Pesanan> list = new ArrayList<>();
        String sql = """
            SELECT 
                p.id_pesanan,
                p.nomor_pesanan,
                pl.nama AS nama_pelanggan,
                pl.no_telepon,
                pl.email,
                jl.nama_layanan,
                dp.jumlah,
                p.total_biaya AS total_harga,
                dp.spesifikasi,
                sp.nama_status AS status,
                p.tanggal_pesanan,
                p.updated_at,
                p.catatan
            FROM pesanan p
            JOIN pelanggan pl ON p.id_pelanggan = pl.id_pelanggan
            JOIN status_pesanan sp ON p.id_status = sp.id_status
            LEFT JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan
            LEFT JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan
            ORDER BY p.tanggal_pesanan DESC
            LIMIT ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, limit);
            
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


    // Aktivitas User Terbaru

    /**
     * Mengambil daftar user aktif dengan role-nya
     * Untuk ditampilkan di section "Aktivitas User"
     */
    public List<User> getActiveUsers() {
        List<User> list = new ArrayList<>();
        String sql = """
            SELECT 
                u.id_user,
                u.username,
                u.nama_lengkap,
                u.email,
                u.id_role,
                r.nama_role,
                u.is_active,
                u.last_login,
                u.created_at,
                u.updated_at
            FROM user u
            JOIN role r ON u.id_role = r.id_role
            WHERE u.is_active = 1
            ORDER BY u.last_login DESC
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                User user = new User();
                user.setIdUser(rs.getInt("id_user"));
                user.setUsername(rs.getString("username"));
                user.setNamaLengkap(rs.getString("nama_lengkap"));
                user.setEmail(rs.getString("email"));
                user.setIdRole(rs.getInt("id_role"));
                user.setActive(rs.getBoolean("is_active"));
                
                Timestamp lastLogin = rs.getTimestamp("last_login");
                if (lastLogin != null) {
                    user.setLastLogin(lastLogin.toLocalDateTime());
                }
                
                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    user.setCreatedAt(createdAt.toLocalDateTime());
                }
                
                list.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Mendapatkan nama role berdasarkan id_role
     */
    public String getRoleName(int idRole) {
        String sql = "SELECT nama_role FROM role WHERE id_role = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idRole);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("nama_role");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Unknown";
    }

    // Helper Methods

    /**
     * Helper untuk menjalankan query COUNT
     */
    private int executeCountQuery(String sql) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Mapping ResultSet ke object Pesanan
     */
    private Pesanan mapResultSetToPesanan(ResultSet rs) throws SQLException {
        Pesanan pesanan = new Pesanan();
        pesanan.setIdPesanan(rs.getInt("id_pesanan"));
        pesanan.setNomorPesanan(rs.getString("nomor_pesanan"));
        pesanan.setNamaPelanggan(rs.getString("nama_pelanggan"));
        
        try { pesanan.setNoTelepon(rs.getString("no_telepon")); } catch (SQLException ignored) {}
        try { pesanan.setEmail(rs.getString("email")); } catch (SQLException ignored) {}
        
        pesanan.setJenisLayanan(rs.getString("nama_layanan"));
        pesanan.setJumlah(rs.getInt("jumlah"));
        pesanan.setTotalHarga(rs.getDouble("total_harga"));
        
        try { pesanan.setSpesifikasi(rs.getString("spesifikasi")); } catch (SQLException ignored) {}
        
        pesanan.setStatus(rs.getString("status"));
        
        try { pesanan.setCatatan(rs.getString("catatan")); } catch (SQLException ignored) {}

        Timestamp tanggalTs = rs.getTimestamp("tanggal_pesanan");
        if (tanggalTs != null) {
            pesanan.setTanggalPesanan(tanggalTs.toLocalDateTime());
        }

        try {
            Timestamp updatedTs = rs.getTimestamp("updated_at");
            if (updatedTs != null) {
                pesanan.setUpdatedAt(updatedTs.toLocalDateTime());
            }
        } catch (SQLException ignored) {}

        return pesanan;
    }
}
