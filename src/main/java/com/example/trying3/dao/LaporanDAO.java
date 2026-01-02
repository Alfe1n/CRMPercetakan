package com.example.trying3.dao;

import com.example.trying3.config.DatabaseConnection;
import com.example.trying3.model.Pesanan;

import java.sql.*;
import java.util.*;

/**
 * Data Access Object untuk operasi Laporan dan Statistik.
 * Menyediakan berbagai laporan berdasarkan periode dan jenis layanan.
 */
public class LaporanDAO {

    public double[] getLaporanSummary(String periode) {
        double[] stats = { 0, 0, 0, 0 };

        String sql = """
                SELECT
                    COUNT(p.id_pesanan) as total_semua,
                    SUM(CASE WHEN sp.nama_status = 'Selesai' THEN 1 ELSE 0 END) as total_selesai,
                    SUM(CASE WHEN sp.nama_status NOT IN ('Selesai', 'Dibatalkan') THEN 1 ELSE 0 END) as total_tertunda,
                    SUM(CASE WHEN sp.nama_status != 'Dibatalkan' THEN p.total_biaya ELSE 0 END) as total_pendapatan
                FROM pesanan p
                JOIN status_pesanan sp ON p.id_status = sp.id_status
                WHERE %s
                """.formatted(getPeriodeCondition(periode));

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
            System.err.println("❌ Error getLaporanSummary: " + e.getMessage());
            e.printStackTrace();
        }
        return stats;
    }

    public List<String[]> getAnalisaLayanan(String periode) {
        List<String[]> list = new ArrayList<>();

        String sql = """
                SELECT jl.nama_layanan, COUNT(p.id_pesanan) as jumlah, SUM(p.total_biaya) as total_uang
                FROM pesanan p
                JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan
                JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan
                WHERE %s
                GROUP BY jl.nama_layanan
                ORDER BY total_uang DESC
                """.formatted(getPeriodeCondition(periode));

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new String[] {
                        rs.getString("nama_layanan"),
                        String.valueOf(rs.getInt("jumlah")),
                        String.valueOf(rs.getDouble("total_uang"))
                });
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getAnalisaLayanan: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public List<Pesanan> getAktivitasTerbaru() {
        List<Pesanan> list = new ArrayList<>();

        String sql = """
                SELECT p.*,
                    pel.nama as nama_pelanggan, pel.no_telepon, pel.email,
                    sp.nama_status,
                    jl.nama_layanan as jenis_layanan
                FROM pesanan p
                JOIN pelanggan pel ON p.id_pelanggan = pel.id_pelanggan
                JOIN status_pesanan sp ON p.id_status = sp.id_status
                LEFT JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan
                LEFT JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan
                ORDER BY p.id_pesanan DESC LIMIT 5
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToPesanan(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getAktivitasTerbaru: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public int getTotalPesananCount() {
        return executeCountQuery("SELECT COUNT(*) FROM pesanan");
    }

    public double getTotalRevenue() {
        String sql = "SELECT SUM(total_biaya) FROM pesanan";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next())
                return rs.getDouble(1);
        } catch (SQLException e) {
            System.err.println("❌ Error getTotalRevenue: " + e.getMessage());
        }
        return 0;
    }

    public int getSelesaiCount() {
        return executeCountQuery("""
                SELECT COUNT(*) FROM pesanan p
                JOIN status_pesanan s ON p.id_status = s.id_status
                WHERE s.nama_status = 'Selesai'
                """);
    }

    public int countByStatus(String statusName) {
        if (statusName == null || statusName.trim().isEmpty()) {
            return 0;
        }

        String sql = """
                SELECT COUNT(*) FROM pesanan p
                JOIN status_pesanan sp ON p.id_status = sp.id_status
                WHERE sp.nama_status = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statusName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error countByStatus: " + e.getMessage());
        }
        return 0;
    }

    public Map<String, Integer> getServiceDistribution() {
        Map<String, Integer> dist = new LinkedHashMap<>();

        String sql = """
                SELECT jl.nama_layanan, COUNT(dp.id_layanan)
                FROM pesanan p
                JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan
                JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan
                GROUP BY jl.nama_layanan
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                dist.put(rs.getString(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getServiceDistribution: " + e.getMessage());
        }
        return dist;
    }

    public Map<String, Integer> getStatusDistribution() {
        Map<String, Integer> map = new HashMap<>();

        String sql = """
                SELECT IFNULL(s.nama_status, 'Tanpa Status'), COUNT(p.id_pesanan)
                FROM pesanan p
                LEFT JOIN status_pesanan s ON p.id_status = s.id_status
                GROUP BY s.nama_status
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                map.put(rs.getString(1), rs.getInt(2));
            }
            System.out.println("DEBUG Status Chart Data: " + map);

        } catch (SQLException e) {
            System.err.println("❌ Error getStatusDistribution: " + e.getMessage());
        }
        return map;
    }

    public Map<String, Integer> getOrderTrend() {
        Map<String, Integer> trend = new LinkedHashMap<>();

        String sql = """
                SELECT DATE_FORMAT(tanggal_pesanan, '%d %b') as label_tgl, COUNT(*)
                FROM pesanan
                GROUP BY label_tgl, DATE(tanggal_pesanan)
                ORDER BY DATE(tanggal_pesanan) ASC LIMIT 7
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                trend.put(rs.getString(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getOrderTrend: " + e.getMessage());
        }
        return trend;
    }

    public Map<String, Double> getRevenueDistribution() {
        Map<String, Double> dist = new HashMap<>();

        String sql = """
                SELECT jl.nama_layanan, SUM(p.total_biaya)
                FROM pesanan p
                JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan
                JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan
                GROUP BY jl.nama_layanan
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                dist.put(rs.getString(1), rs.getDouble(2));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getRevenueDistribution: " + e.getMessage());
        }
        return dist;
    }

    public List<Pesanan> getAllPesananForExport() {
        List<Pesanan> list = new ArrayList<>();

        String sql = """
                SELECT p.id_pesanan, pl.nama AS nama_pelanggan,
                    COALESCE(jl.nama_layanan, 'N/A') as nama_layanan,
                    p.total_biaya, s.nama_status, p.tanggal_pesanan
                FROM pesanan p
                JOIN pelanggan pl ON p.id_pelanggan = pl.id_pelanggan
                JOIN status_pesanan s ON p.id_status = s.id_status
                LEFT JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan
                LEFT JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan
                ORDER BY p.tanggal_pesanan DESC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Pesanan p = new Pesanan();
                p.setIdPesanan(rs.getInt("id_pesanan"));
                p.setNamaPelanggan(rs.getString("nama_pelanggan"));
                p.setJenisLayanan(rs.getString("nama_layanan"));
                p.setTotalBiaya(rs.getDouble("total_biaya"));
                p.setStatus(rs.getString("nama_status"));

                Timestamp ts = rs.getTimestamp("tanggal_pesanan");
                if (ts != null)
                    p.setTanggalPesanan(ts.toLocalDateTime());

                list.add(p);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getAllPesananForExport: " + e.getMessage());
        }
        return list;
    }

    public List<Pesanan> getPesananForExport(String periode) {
        List<Pesanan> exportList = new ArrayList<>();

        String sql = """
                SELECT p.*,
                    pel.nama as nama_pelanggan_real, pel.no_telepon, pel.email,
                    sp.nama_status,
                    jl.nama_layanan
                FROM pesanan p
                JOIN pelanggan pel ON p.id_pelanggan = pel.id_pelanggan
                JOIN status_pesanan sp ON p.id_status = sp.id_status
                LEFT JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan
                LEFT JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan
                WHERE %s
                ORDER BY p.tanggal_pesanan DESC
                """.formatted(getPeriodeCondition(periode));

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Pesanan p = new Pesanan();
                p.setIdPesanan(rs.getInt("id_pesanan"));
                p.setNamaPelanggan(rs.getString("nama_pelanggan_real"));
                p.setNoTelepon(rs.getString("no_telepon"));
                p.setEmail(rs.getString("email"));
                p.setStatus(rs.getString("nama_status"));

                String layanan = rs.getString("nama_layanan");
                p.setJenisLayanan(layanan != null ? layanan : "-");
                p.setTotalBiaya(rs.getDouble("total_biaya"));

                Timestamp ts = rs.getTimestamp("tanggal_pesanan");
                if (ts != null)
                    p.setTanggalPesanan(ts.toLocalDateTime());

                exportList.add(p);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getPesananForExport: " + e.getMessage());
        }

        return exportList;
    }

    private String getPeriodeCondition(String periode) {
        if (periode == null)
            return "1=1";

        return switch (periode) {
            case "Harian" -> "DATE(" + "p.tanggal_pesanan" + ") = CURDATE()";
            case "Mingguan" -> "p.tanggal_pesanan" + " >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)";
            case "Bulanan" -> "MONTH(" + "p.tanggal_pesanan" + ") = MONTH(CURDATE()) AND YEAR(" + "p.tanggal_pesanan" + ") = YEAR(CURDATE())";
            case "Tahunan" -> "YEAR(" + "p.tanggal_pesanan" + ") = YEAR(CURDATE())";
            default -> "1=1";
        };
    }

    private int executeCountQuery(String sql) {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("❌ Error executeCountQuery: " + e.getMessage());
        }
        return 0;
    }

    private Pesanan mapResultSetToPesanan(ResultSet rs) throws SQLException {
        Pesanan p = new Pesanan();
        p.setIdPesanan(rs.getInt("id_pesanan"));
        p.setNamaPelanggan(rs.getString("nama_pelanggan"));
        p.setNoTelepon(rs.getString("no_telepon"));
        p.setEmail(rs.getString("email"));
        p.setStatus(rs.getString("nama_status"));

        String layanan = rs.getString("jenis_layanan");
        p.setJenisLayanan(layanan != null ? layanan : "-");
        p.setTotalBiaya(rs.getDouble("total_biaya"));

        Timestamp ts = rs.getTimestamp("tanggal_pesanan");
        if (ts != null)
            p.setTanggalPesanan(ts.toLocalDateTime());

        return p;
    }
}
