package com.example.trying3.dao;

import com.example.trying3.config.DatabaseConnection;
import com.example.trying3.model.DesainInfo;
import com.example.trying3.model.Pesanan;
import com.example.trying3.util.SessionManager;

import java.sql.*;
import java.util.*;

/**
 * Data Access Object untuk operasi CRUD Pesanan.
 * Mendelegasikan operasi spesifik ke DesainDAO, ProduksiDAO, dan LaporanDAO.
 */
public class PesananDAO {

    private final DesainDAO desainDAO;
    private final ProduksiDAO produksiDAO;
    private final LaporanDAO laporanDAO;

    public PesananDAO() {
        this.desainDAO = new DesainDAO();
        this.produksiDAO = new ProduksiDAO();
        this.laporanDAO = new LaporanDAO();
    }

    /**
     * Membuat pesanan baru dengan data pelanggan dan detail pesanan.
     */
    public boolean createPesanan(String namaPelanggan, String noTelepon, String email, String alamat,
                                 String jenisLayanan, int jumlah, double totalHarga,
                                 String spesifikasi) {
        if (namaPelanggan == null || namaPelanggan.trim().isEmpty()) {
            System.err.println("❌ Error: namaPelanggan tidak boleh kosong");
            return false;
        }
        if (noTelepon == null || noTelepon.trim().isEmpty()) {
            System.err.println("❌ Error: noTelepon tidak boleh kosong");
            return false;
        }

        Connection conn = null;
        PreparedStatement psPelanggan = null;
        PreparedStatement psPesanan = null;
        PreparedStatement psDetail = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int idPelanggan = getOrCreatePelanggan(conn, namaPelanggan, noTelepon, email, alamat);
            if (idPelanggan == -1) {
                throw new SQLException("Gagal membuat/mengambil data pelanggan.");
            }

            int idAdmin = SessionManager.getInstance().getCurrentUserId();
            if (idAdmin == -1) idAdmin = 1;

            int idPesanan = createPesananRecord(conn, idPelanggan, idAdmin, totalHarga, spesifikasi);
            if (idPesanan == -1) {
                throw new SQLException("Gagal membuat pesanan.");
            }

            createDetailPesanan(conn, idPesanan, jenisLayanan, jumlah, totalHarga, spesifikasi);

            conn.commit();
            return true;

        } catch (SQLException e) {
            rollbackQuietly(conn);
            System.err.println("❌ Error createPesanan: " + e.getMessage());
            e.printStackTrace();
            return false;

        } finally {
            closeResources(rs, psPelanggan, psPesanan, psDetail, conn);
        }
    }

    public List<Pesanan> getAllPesanan() {
        return executeQueryToList("""
            SELECT p.id_pesanan, p.nomor_pesanan, pl.nama AS nama_pelanggan, pl.no_telepon, pl.email, pl.alamat,
                   jl.nama_layanan, dp.jumlah, p.total_biaya AS total_harga, dp.spesifikasi,
                   sp.nama_status AS status, p.tanggal_pesanan, p.updated_at, p.catatan
            FROM pesanan p
            JOIN pelanggan pl ON p.id_pelanggan = pl.id_pelanggan
            JOIN status_pesanan sp ON p.id_status = sp.id_status
            LEFT JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan
            LEFT JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan
            ORDER BY p.created_at DESC
            """);
    }

    public List<Pesanan> getPesananByStatus(String statusName) {
        if (statusName == null || statusName.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String sql = """
            SELECT p.id_pesanan, p.nomor_pesanan, pl.nama AS nama_pelanggan, pl.no_telepon, pl.email,
                   jl.nama_layanan, dp.jumlah, p.total_biaya AS total_harga, dp.spesifikasi,
                   sp.nama_status AS status, p.tanggal_pesanan, p.updated_at, p.catatan
            FROM pesanan p
            JOIN pelanggan pl ON p.id_pelanggan = pl.id_pelanggan
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
            System.err.println("❌ Error getPesananByStatus: " + e.getMessage());
        }
        return list;
    }

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
                d.file_desain_path
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
            System.err.println("❌ Error getPesananForDesignTeam: " + e.getMessage());
        }
        return list;
    }

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
            System.err.println("❌ Error getPesananForProduction: " + e.getMessage());
        }
        return list;
    }

    public boolean updateStatus(int idPesanan, String newStatusName) {
        if (idPesanan <= 0 || newStatusName == null) {
            return false;
        }

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
            System.err.println("❌ Error updateStatus: " + e.getMessage());
            return false;
        }
    }

    public void updateCatatan(int idPesanan, String catatanBaru) {
        if (idPesanan <= 0) {
            return;
        }

        String sql = "UPDATE pesanan SET catatan = ? WHERE id_pesanan = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, catatanBaru);
            ps.setInt(2, idPesanan);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Error updateCatatan: " + e.getMessage());
        }
    }

    public boolean updateDesignStatus(int idPesanan, String action) {
        if (action == null) return false;

        return switch (action.toLowerCase()) {
            case "approve" -> desainDAO.approveDesain(idPesanan);
            case "revisi" -> updateStatus(idPesanan, "Desain Direvisi");
            default -> false;
        };
    }

    public boolean simpanDesain(int idPesanan, String filePath, int idDesigner) {
        return desainDAO.simpanDesain(idPesanan, filePath, idDesigner);
    }

    public boolean simpanDesainDenganRevisi(int idPesanan, String newFilePath, int idDesigner) {
        return desainDAO.simpanDesainDenganRevisi(idPesanan, newFilePath, idDesigner);
    }

    public boolean approveDesain(int idPesanan) {
        return desainDAO.approveDesain(idPesanan);
    }

    public DesainInfo getDesainInfo(int idPesanan) {
        return desainDAO.getDesainInfo(idPesanan);
    }

    public boolean simpanKendalaProduksi(int idPesanan, String deskripsi, int idUser) {
        return produksiDAO.simpanKendalaProduksi(idPesanan, deskripsi, idUser);
    }

    public boolean selesaikanProduksi(int idPesanan) {
        return produksiDAO.selesaikanProduksi(idPesanan);
    }

    public boolean resolveKendala(int idKendala, String solusi) {
        return produksiDAO.resolveKendala(idKendala, solusi);
    }

    public double[] getLaporanSummary(String periode) {
        return laporanDAO.getLaporanSummary(periode);
    }

    /** @see LaporanDAO#getAnalisaLayanan(String) */
    public List<String[]> getAnalisaLayanan(String periode) {
        return laporanDAO.getAnalisaLayanan(periode);
    }

    public List<Pesanan> getAktivitasTerbaru() {
        return laporanDAO.getAktivitasTerbaru();
    }

    public int getTotalPesananCount() {
        return laporanDAO.getTotalPesananCount();
    }

    public double getTotalRevenue() {
        return laporanDAO.getTotalRevenue();
    }

    public int getSelesaiCount() {
        return laporanDAO.getSelesaiCount();
    }

    public int countByStatus(String statusName) {
        return laporanDAO.countByStatus(statusName);
    }

    public Map<String, Integer> getServiceDistribution() {
        return laporanDAO.getServiceDistribution();
    }

    public Map<String, Integer> getStatusDistribution() {
        return laporanDAO.getStatusDistribution();
    }

    public Map<String, Integer> getOrderTrend() {
        return laporanDAO.getOrderTrend();
    }

    public Map<String, Double> getRevenueDistribution() {
        return laporanDAO.getRevenueDistribution();
    }

    public List<Pesanan> getAllPesananForExport() {
        return laporanDAO.getAllPesananForExport();
    }

    public List<Pesanan> getPesananForExport(String periode) {
        return laporanDAO.getPesananForExport(periode);
    }

    private int getOrCreatePelanggan(Connection conn, String nama, String noTelepon, String email, String alamat) throws SQLException {
        String checkSql = "SELECT id_pelanggan FROM pelanggan WHERE no_telepon = ?";
        try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, noTelepon);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_pelanggan");
                }
            }
        }

        String insertSql = "INSERT INTO pelanggan (nama, no_telepon, email, alamat) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nama);
            ps.setString(2, noTelepon);
            ps.setString(3, email);
            ps.setString(4, alamat);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    private int createPesananRecord(Connection conn, int idPelanggan, int idAdmin,
                                    double totalHarga, String catatan) throws SQLException {
        String sql = "INSERT INTO pesanan (id_pelanggan, id_user_admin, id_status, total_biaya, catatan) VALUES (?, ?, 1, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idPelanggan);
            ps.setInt(2, idAdmin);
            ps.setDouble(3, totalHarga);
            ps.setString(4, catatan);

            if (ps.executeUpdate() == 0) return -1;

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    private void createDetailPesanan(Connection conn, int idPesanan, String jenisLayanan,
                                     int jumlah, double totalHarga, String spesifikasi) throws SQLException {
        int idJenisLayanan = getLayananIdByName(conn, jenisLayanan);
        double hargaSatuan = jumlah > 0 ? totalHarga / jumlah : 0;

        String sql = "INSERT INTO detail_pesanan (id_pesanan, id_layanan, jumlah, harga_satuan, subtotal, spesifikasi) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPesanan);
            ps.setInt(2, idJenisLayanan);
            ps.setInt(3, jumlah);
            ps.setDouble(4, hargaSatuan);
            ps.setDouble(5, totalHarga);
            ps.setString(6, spesifikasi);
            ps.executeUpdate();
        }
    }

    private int getLayananIdByName(Connection conn, String namaLayanan) throws SQLException {
        String sql = "SELECT id_layanan FROM jenis_layanan WHERE nama_layanan = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaLayanan);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id_layanan");
            }
        }
        return 1;
    }

    private List<Pesanan> executeQueryToList(String sql) {
        List<Pesanan> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapResultSetToPesanan(rs));
        } catch (SQLException e) {
            System.err.println("❌ Error executeQueryToList: " + e.getMessage());
        }
        return list;
    }

    private Pesanan mapResultSetToPesanan(ResultSet rs) throws SQLException {
        Pesanan pesanan = new Pesanan();
        pesanan.setIdPesanan(rs.getInt("id_pesanan"));

        try { pesanan.setNomorPesanan(rs.getString("nomor_pesanan")); } catch (SQLException ignored) {}
        pesanan.setNamaPelanggan(rs.getString("nama_pelanggan"));

        try { pesanan.setNoTelepon(rs.getString("no_telepon")); } catch (SQLException ignored) {}
        try { pesanan.setEmail(rs.getString("email")); } catch (SQLException ignored) {}
        try { pesanan.setAlamat(rs.getString("alamat")); } catch (SQLException ignored) {}
        try { pesanan.setJenisLayanan(rs.getString("nama_layanan")); } catch (SQLException ignored) {}
        try { pesanan.setJumlah(rs.getInt("jumlah")); } catch (SQLException ignored) {}
        try { pesanan.setTotalBiaya(rs.getDouble("total_harga")); } catch (SQLException ignored) {}
        try { pesanan.setSpesifikasi(rs.getString("spesifikasi")); } catch (SQLException ignored) {}
        pesanan.setStatus(rs.getString("status"));

        try {
            String filePath = rs.getString("file_desain_path");
            pesanan.setFileDesainPath(filePath);
        } catch (SQLException ignored) {}

        try { pesanan.setCatatan(rs.getString("catatan")); } catch (SQLException ignored) {}

        Timestamp tanggalTs = rs.getTimestamp("tanggal_pesanan");
        if (tanggalTs != null) pesanan.setTanggalPesanan(tanggalTs.toLocalDateTime());

        try {
            Timestamp updatedTs = rs.getTimestamp("updated_at");
            if (updatedTs != null) pesanan.setUpdatedAt(updatedTs.toLocalDateTime());
        } catch (SQLException ignored) {}

        return pesanan;
    }

    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try { conn.rollback(); } catch (SQLException ignored) {}
        }
    }

    private void closeResources(AutoCloseable... resources) {
        for (AutoCloseable res : resources) {
            if (res != null) {
                try { res.close(); } catch (Exception ignored) {}
            }
        }
    }
}
