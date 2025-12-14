package com.example.trying3.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class Notifikasi {
    public static final String TIPE_REVISI_DESAIN = "REVISI_DESAIN";
    public static final String TIPE_KENDALA_PRODUKSI = "KENDALA_PRODUKSI";
    public static final String TIPE_SIAP_DIKIRIM = "SIAP_DIKIRIM";

    private int id;
    private String tipe;
    private String judul;
    private String pesan;
    private String nomorPesanan;
    private String namaPelanggan;
    private int idPesanan;
    private int idReference;
    private String sumberDivisi;
    private LocalDateTime tanggalDibuat;
    private boolean sudahDibaca;

    // Constructors
    public Notifikasi() {
    }

    public Notifikasi(String tipe, String judul, String pesan, String nomorPesanan,
                      String namaPelanggan, int idPesanan, LocalDateTime tanggalDibuat) {
        this.tipe = tipe;
        this.judul = judul;
        this.pesan = pesan;
        this.nomorPesanan = nomorPesanan;
        this.namaPelanggan = namaPelanggan;
        this.idPesanan = idPesanan;
        this.tanggalDibuat = tanggalDibuat;
        this.sudahDibaca = false;
        setSumberDivisiByTipe();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTipe() {
        return tipe;
    }

    public void setTipe(String tipe) {
        this.tipe = tipe;
        setSumberDivisiByTipe();
    }

    public String getJudul() {
        return judul;
    }

    public void setJudul(String judul) {
        this.judul = judul;
    }

    public String getPesan() {
        return pesan;
    }

    public void setPesan(String pesan) {
        this.pesan = pesan;
    }

    public String getNomorPesanan() {
        return nomorPesanan;
    }

    public void setNomorPesanan(String nomorPesanan) {
        this.nomorPesanan = nomorPesanan;
    }

    public String getNamaPelanggan() {
        return namaPelanggan;
    }

    public void setNamaPelanggan(String namaPelanggan) {
        this.namaPelanggan = namaPelanggan;
    }

    public int getIdPesanan() {
        return idPesanan;
    }

    public void setIdPesanan(int idPesanan) {
        this.idPesanan = idPesanan;
    }

    public int getIdReference() {
        return idReference;
    }

    public void setIdReference(int idReference) {
        this.idReference = idReference;
    }

    public String getSumberDivisi() {
        return sumberDivisi;
    }

    public void setSumberDivisi(String sumberDivisi) {
        this.sumberDivisi = sumberDivisi;
    }

    public LocalDateTime getTanggalDibuat() {
        return tanggalDibuat;
    }

    public void setTanggalDibuat(LocalDateTime tanggalDibuat) {
        this.tanggalDibuat = tanggalDibuat;
    }

    public boolean isSudahDibaca() {
        return sudahDibaca;
    }

    public void setSudahDibaca(boolean sudahDibaca) {
        this.sudahDibaca = sudahDibaca;
    }

    // Helper Methods
    private void setSumberDivisiByTipe() {
        if (tipe == null) return;

        switch (tipe) {
            case TIPE_REVISI_DESAIN:
                this.sumberDivisi = "Tim Desain";
                break;
            case TIPE_KENDALA_PRODUKSI:
            case TIPE_SIAP_DIKIRIM:
                this.sumberDivisi = "Tim Produksi";
                break;
            default:
                this.sumberDivisi = "Sistem";
        }
    }

    /**
     * Mendapatkan icon berdasarkan tipe notifikasi
     */
    public String getIcon() {
        if (tipe == null) return "🔔";

        return switch (tipe) {
            case TIPE_REVISI_DESAIN -> "🎨";
            case TIPE_KENDALA_PRODUKSI -> "⚠";
            case TIPE_SIAP_DIKIRIM -> "📦";
            default -> "🔔";
        };
    }

    /**
     * Mendapatkan warna badge berdasarkan tipe
     */
    public String getBadgeColor() {
        if (tipe == null) return "#7f8c8d";

        return switch (tipe) {
            case TIPE_REVISI_DESAIN -> "#9b59b6";
            case TIPE_KENDALA_PRODUKSI -> "#e74c3c";
            case TIPE_SIAP_DIKIRIM -> "#27ae60";
            default -> "#7f8c8d";
        };
    }

    /**
     * Mendapatkan label tipe yang user-friendly
     */
    public String getTipeLabel() {
        if (tipe == null) return "Notifikasi";

        return switch (tipe) {
            case TIPE_REVISI_DESAIN -> "Revisi Desain";
            case TIPE_KENDALA_PRODUKSI -> "Kendala Produksi";
            case TIPE_SIAP_DIKIRIM -> "Siap Dikirim";
            default -> "Notifikasi";
        };
    }

    /**
     * Format waktu relatif
     */
    public String getWaktuRelatif() {
        if (tanggalDibuat == null) return "-";

        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(tanggalDibuat, now);
        long hours = ChronoUnit.HOURS.between(tanggalDibuat, now);
        long days = ChronoUnit.DAYS.between(tanggalDibuat, now);

        if (minutes < 1) {
            return "Baru saja";
        } else if (minutes < 60) {
            return minutes + " menit yang lalu";
        } else if (hours < 24) {
            return hours + " jam yang lalu";
        } else if (days < 7) {
            return days + " hari yang lalu";
        } else {
            return tanggalDibuat.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        }
    }

    /**
     * Format tanggal lengkap
     */
    public String getFormattedDateTime() {
        if (tanggalDibuat == null) return "-";
        return tanggalDibuat.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
    }

    @Override
    public String toString() {
        return "Notifikasi{" +
                "id=" + id +
                ", tipe='" + tipe + '\'' +
                ", judul='" + judul + '\'' +
                ", nomorPesanan='" + nomorPesanan + '\'' +
                ", tanggalDibuat=" + tanggalDibuat +
                '}';
    }
}
