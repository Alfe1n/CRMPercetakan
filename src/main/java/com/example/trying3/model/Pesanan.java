package com.example.trying3.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Model untuk Pesanan/Order.
 * Menyimpan informasi pesanan dari pelanggan termasuk detail layanan dan status.
 */
public class Pesanan {
    private int idPesanan;
    private String namaPelanggan;
    private String nomorPesanan;
    private String noTelepon;
    private String email;
    private String jenisLayanan;
    private int jumlah;
    private double totalBiaya;
    private String spesifikasi;
    private String catatan;
    private String status;
    private LocalDateTime tanggalPesanan;
    private LocalDateTime updatedAt;
    private String fileDesainPath;

    public Pesanan() {
    }

    public Pesanan(int idPesanan, String namaPelanggan, String noTelepon, String email,
                   String jenisLayanan, int jumlah, double totalBiaya, String spesifikasi,
                   String status, LocalDateTime tanggalPesanan) {
        this.idPesanan = idPesanan;
        this.namaPelanggan = namaPelanggan;
        this.noTelepon = noTelepon;
        this.email = email;
        this.jenisLayanan = jenisLayanan;
        this.jumlah = jumlah;
        this.totalBiaya = totalBiaya;
        this.spesifikasi = spesifikasi;
        this.status = status;
        this.tanggalPesanan = tanggalPesanan;
    }

    public int getIdPesanan() { return idPesanan; }
    public void setIdPesanan(int idPesanan) { this.idPesanan = idPesanan; }

    public String getNomorPesanan() { return nomorPesanan; }
    public void setNomorPesanan(String nomorPesanan) { this.nomorPesanan = nomorPesanan; }

    public String getDisplayId() {
        if (nomorPesanan != null && !nomorPesanan.isEmpty()) {
            return nomorPesanan;
        }
        return String.format("ORD-%03d", idPesanan);
    }

    public String getNamaPelanggan() { return namaPelanggan; }
    public void setNamaPelanggan(String namaPelanggan) { this.namaPelanggan = namaPelanggan; }

    public String getNoTelepon() { return noTelepon; }
    public void setNoTelepon(String noTelepon) { this.noTelepon = noTelepon; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getJenisLayanan() { return jenisLayanan; }
    public void setJenisLayanan(String jenisLayanan) { this.jenisLayanan = jenisLayanan; }

    public int getJumlah() { return jumlah; }
    public void setJumlah(int jumlah) { this.jumlah = jumlah; }

    public double getTotalBiaya() { return totalBiaya; }
    public void setTotalBiaya(double totalBiaya) { this.totalBiaya = totalBiaya; }

    public String getSpesifikasi() { return spesifikasi; }
    public void setSpesifikasi(String spesifikasi) { this.spesifikasi = spesifikasi; }

    public String getCatatan() { return catatan; }
    public void setCatatan(String catatan) { this.catatan = catatan; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getTanggalPesanan() { return tanggalPesanan; }
    public void setTanggalPesanan(LocalDateTime tanggalPesanan) { this.tanggalPesanan = tanggalPesanan; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getFileDesainPath() { return fileDesainPath; }
    public void setFileDesainPath(String fileDesainPath) { this.fileDesainPath = fileDesainPath; }

    public String getFormattedDate() {
        if (tanggalPesanan == null) return "-";
        return tanggalPesanan.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    public String getFormattedDateTime() {
        if (tanggalPesanan == null) return "-";
        return tanggalPesanan.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
    }

    public String getFormattedJumlah() {
        return jumlah + " pcs";
    }

    @Override
    public String toString() {
        return "Pesanan{" +
                "idPesanan=" + idPesanan +
                ", namaPelanggan='" + namaPelanggan + '\'' +
                ", status='" + status + '\'' +
                ", totalBiaya=" + totalBiaya +
                '}';
    }
}