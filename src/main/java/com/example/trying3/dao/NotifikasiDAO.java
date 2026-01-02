package com.example.trying3.dao;

import com.example.trying3.config.DatabaseConnection;
import com.example.trying3.model.Notifikasi;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object untuk Notifikasi.
 * Mengelola notifikasi dari berbagai sumber: revisi desain, kendala produksi, dan siap dikirim.
 */
public class NotifikasiDAO {

    public List<Notifikasi> getAllNotifikasi() {

        List<Notifikasi> revisi = getRevisiDesainNotifikasi();
        List<Notifikasi> notifikasiList = new ArrayList<>(revisi);

        List<Notifikasi> kendala = getKendalaProduksiNotifikasi();
        notifikasiList.addAll(kendala);

        List<Notifikasi> siap = getSiapDikirimNotifikasi();
        notifikasiList.addAll(siap);

        notifikasiList.sort((n1, n2) -> {
            if (n1.getTanggalDibuat() == null) return 1;
            if (n2.getTanggalDibuat() == null) return -1;
            return n2.getTanggalDibuat().compareTo(n1.getTanggalDibuat());
        });

        return notifikasiList;
    }

    public List<Notifikasi> getRevisiDesainNotifikasi() {
        List<Notifikasi> list = new ArrayList<>();

        String query = """
            SELECT
                p.id_pesanan,
                p.nomor_pesanan,
                p.catatan,
                p.updated_at,
                p.total_biaya,
                pl.nama as nama_pelanggan,
                pl.no_telepon,
                sp.nama_status,
                jl.nama_layanan,
                dp.jumlah
            FROM pesanan p
            JOIN pelanggan pl ON p.id_pelanggan = pl.id_pelanggan
            JOIN status_pesanan sp ON p.id_status = sp.id_status
            LEFT JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan
            LEFT JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan
            WHERE p.id_status = 5
            ORDER BY p.updated_at DESC
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Notifikasi notif = new Notifikasi();
                notif.setId(rs.getInt("id_pesanan"));
                notif.setIdReference(rs.getInt("id_pesanan"));
                notif.setIdPesanan(rs.getInt("id_pesanan"));
                notif.setTipe(Notifikasi.TIPE_REVISI_DESAIN);
                notif.setNomorPesanan(rs.getString("nomor_pesanan"));
                notif.setNamaPelanggan(rs.getString("nama_pelanggan"));

                notif.setJudul("Desain memerlukan revisi");

                String catatan = rs.getString("catatan");
                String namaLayanan = rs.getString("nama_layanan");
                double totalBiaya = rs.getDouble("total_biaya");

                StringBuilder pesan = new StringBuilder();
                pesan.append(notif.getNomorPesanan()).append(" - ").append(notif.getNamaPelanggan());
                if (namaLayanan != null) {
                    pesan.append("\nLayanan: ").append(namaLayanan);
                }
                pesan.append("\nTotal: Rp ").append(String.format("%,.0f", totalBiaya));
                if (catatan != null && !catatan.isEmpty()) {
                    pesan.append("\nCatatan: ").append(catatan);
                }
                notif.setPesan(pesan.toString());

                Timestamp timestamp = rs.getTimestamp("updated_at");
                if (timestamp != null) {
                    notif.setTanggalDibuat(timestamp.toLocalDateTime());
                }

                list.add(notif);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error fetching revisi desain notifikasi: " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    public List<Notifikasi> getKendalaProduksiNotifikasi() {
        List<Notifikasi> list = new ArrayList<>();

        String query = """
            SELECT
                kp.id_kendala,
                kp.id_produksi,
                kp.deskripsi,
                kp.status,
                kp.tanggal_lapor,
                mk.nama_kendala,
                mk.kategori,
                pr.id_pesanan,
                p.nomor_pesanan,
                pl.nama as nama_pelanggan,
                u.nama_lengkap as operator_name
            FROM kendala_produksi kp
            JOIN produksi pr ON kp.id_produksi = pr.id_produksi
            JOIN pesanan p ON pr.id_pesanan = p.id_pesanan
            JOIN pelanggan pl ON p.id_pelanggan = pl.id_pelanggan
            JOIN user u ON kp.dilaporkan_oleh = u.id_user
            LEFT JOIN master_kendala mk ON kp.id_kendala_type = mk.id_kendala_type
            WHERE kp.status IN ('open', 'in_progress')
            ORDER BY kp.tanggal_lapor DESC
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Notifikasi notif = new Notifikasi();
                notif.setId(rs.getInt("id_kendala"));
                notif.setIdReference(rs.getInt("id_kendala"));
                notif.setIdPesanan(rs.getInt("id_pesanan"));
                notif.setTipe(Notifikasi.TIPE_KENDALA_PRODUKSI);
                notif.setNomorPesanan(rs.getString("nomor_pesanan"));
                notif.setNamaPelanggan(rs.getString("nama_pelanggan"));

                String namaKendala = rs.getString("nama_kendala");
                String kategori = rs.getString("kategori");
                String status = rs.getString("status");

                String judul = "Kendala Produksi";
                if (namaKendala != null) {
                    judul += ": " + namaKendala;
                }
                if ("in_progress".equals(status)) {
                    judul += " (Sedang Ditangani)";
                }
                notif.setJudul(judul);

                String deskripsi = rs.getString("deskripsi");
                String operatorName = rs.getString("operator_name");
                String pesan = notif.getNomorPesanan() + " - " + notif.getNamaPelanggan();
                if (deskripsi != null && !deskripsi.isEmpty()) {
                    pesan += "\nKendala: " + deskripsi;
                }
                if (kategori != null) {
                    pesan += "\nKategori: " + kategori;
                }
                pesan += "\nDilaporkan oleh: " + operatorName;
                notif.setPesan(pesan);

                Timestamp timestamp = rs.getTimestamp("tanggal_lapor");
                if (timestamp != null) {
                    notif.setTanggalDibuat(timestamp.toLocalDateTime());
                }

                list.add(notif);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error fetching kendala produksi notifikasi: " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    public List<Notifikasi> getSiapDikirimNotifikasi() {
        List<Notifikasi> list = new ArrayList<>();

        String query = """
            SELECT
                p.id_pesanan,
                p.nomor_pesanan,
                p.updated_at,
                p.total_biaya,
                pl.nama as nama_pelanggan,
                pl.no_telepon,
                pl.alamat,
                sp.nama_status,
                jl.nama_layanan
            FROM pesanan p
            JOIN pelanggan pl ON p.id_pelanggan = pl.id_pelanggan
            JOIN status_pesanan sp ON p.id_status = sp.id_status
            LEFT JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan
            LEFT JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan
            WHERE p.id_status = 10
            ORDER BY p.updated_at DESC
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Notifikasi notif = new Notifikasi();
                notif.setId(rs.getInt("id_pesanan"));
                notif.setIdReference(rs.getInt("id_pesanan"));
                notif.setIdPesanan(rs.getInt("id_pesanan"));
                notif.setTipe(Notifikasi.TIPE_SIAP_DIKIRIM);
                notif.setNomorPesanan(rs.getString("nomor_pesanan"));
                notif.setNamaPelanggan(rs.getString("nama_pelanggan"));

                notif.setJudul("Pesanan siap untuk dikirim");

                String namaLayanan = rs.getString("nama_layanan");
                String noTelepon = rs.getString("no_telepon");
                String alamat = rs.getString("alamat");
                double totalBiaya = rs.getDouble("total_biaya");

                StringBuilder pesan = new StringBuilder();
                pesan.append(notif.getNomorPesanan()).append(" - ").append(notif.getNamaPelanggan());
                if (namaLayanan != null) {
                    pesan.append("\nLayanan: ").append(namaLayanan);
                }
                pesan.append("\nTotal: Rp ").append(String.format("%,.0f", totalBiaya));
                if (noTelepon != null) {
                    pesan.append("\nTelepon: ").append(noTelepon);
                }
                if (alamat != null && !alamat.isEmpty()) {
                    pesan.append("\nAlamat: ").append(alamat);
                }
                notif.setPesan(pesan.toString());

                Timestamp timestamp = rs.getTimestamp("updated_at");
                if (timestamp != null) {
                    notif.setTanggalDibuat(timestamp.toLocalDateTime());
                }

                list.add(notif);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error fetching siap dikirim notifikasi: " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    public int getCountByTipe(String tipe) {
        return switch (tipe) {
            case Notifikasi.TIPE_REVISI_DESAIN -> getRevisiDesainCount();
            case Notifikasi.TIPE_KENDALA_PRODUKSI -> getKendalaProduksiCount();
            case Notifikasi.TIPE_SIAP_DIKIRIM -> getSiapDikirimCount();
            default -> 0;
        };
    }

    public int getRevisiDesainCount() {
        String query = "SELECT COUNT(*) FROM pesanan WHERE id_status = 5";
        return executeCountQuery(query);
    }

    public int getKendalaProduksiCount() {
        String query = "SELECT COUNT(*) FROM kendala_produksi WHERE status IN ('open', 'in_progress')";
        return executeCountQuery(query);
    }

    public int getSiapDikirimCount() {
        String query = "SELECT COUNT(*) FROM pesanan WHERE id_status = 10";
        return executeCountQuery(query);
    }

    public int getTotalNotifikasiCount() {
        return getRevisiDesainCount() + getKendalaProduksiCount() + getSiapDikirimCount();
    }

    private int executeCountQuery(String query) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error executing count query: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }
}