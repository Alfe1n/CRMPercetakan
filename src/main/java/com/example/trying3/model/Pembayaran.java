package com.example.trying3.model;

import java.time.LocalDateTime;

public class Pembayaran {
    private int idPembayaran;
    private int idPesanan;
    private String nomorPesanan;
    private String namaPelanggan;
    private int idMetode;
    private String namaMetode;
    private double jumlah;
    private String statusPembayaran;
    private String buktiPembayaranPath;
    private LocalDateTime tanggalPembayaran;
    private LocalDateTime tanggalVerifikasi;
    private Integer verifiedBy;
    private String catatan;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Additional fields for display
    private String jenisLayanan;
    private int jumlahItem;

    // Constructors
    public Pembayaran() {
    }

    public Pembayaran(int idPembayaran, int idPesanan, String nomorPesanan, String namaPelanggan,
                      int idMetode, String namaMetode, double jumlah, String statusPembayaran,
                      LocalDateTime tanggalPembayaran) {
        this.idPembayaran = idPembayaran;
        this.idPesanan = idPesanan;
        this.nomorPesanan = nomorPesanan;
        this.namaPelanggan = namaPelanggan;
        this.idMetode = idMetode;
        this.namaMetode = namaMetode;
        this.jumlah = jumlah;
        this.statusPembayaran = statusPembayaran;
        this.tanggalPembayaran = tanggalPembayaran;
    }

    // Getters and Setters
    public int getIdPembayaran() {
        return idPembayaran;
    }

    public void setIdPembayaran(int idPembayaran) {
        this.idPembayaran = idPembayaran;
    }

    public int getIdPesanan() {
        return idPesanan;
    }

    public void setIdPesanan(int idPesanan) {
        this.idPesanan = idPesanan;
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

    public int getIdMetode() {
        return idMetode;
    }

    public void setIdMetode(int idMetode) {
        this.idMetode = idMetode;
    }

    public String getNamaMetode() {
        return namaMetode;
    }

    public void setNamaMetode(String namaMetode) {
        this.namaMetode = namaMetode;
    }

    public double getJumlah() {
        return jumlah;
    }

    public void setJumlah(double jumlah) {
        this.jumlah = jumlah;
    }

    public String getStatusPembayaran() {
        return statusPembayaran;
    }

    public void setStatusPembayaran(String statusPembayaran) {
        this.statusPembayaran = statusPembayaran;
    }

    public String getBuktiPembayaranPath() {
        return buktiPembayaranPath;
    }

    public void setBuktiPembayaranPath(String buktiPembayaranPath) {
        this.buktiPembayaranPath = buktiPembayaranPath;
    }

    public LocalDateTime getTanggalPembayaran() {
        return tanggalPembayaran;
    }

    public void setTanggalPembayaran(LocalDateTime tanggalPembayaran) {
        this.tanggalPembayaran = tanggalPembayaran;
    }

    public LocalDateTime getTanggalVerifikasi() {
        return tanggalVerifikasi;
    }

    public void setTanggalVerifikasi(LocalDateTime tanggalVerifikasi) {
        this.tanggalVerifikasi = tanggalVerifikasi;
    }

    public Integer getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(Integer verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public String getCatatan() {
        return catatan;
    }

    public void setCatatan(String catatan) {
        this.catatan = catatan;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getJenisLayanan() {
        return jenisLayanan;
    }

    public void setJenisLayanan(String jenisLayanan) {
        this.jenisLayanan = jenisLayanan;
    }

    public int getJumlahItem() {
        return jumlahItem;
    }

    public void setJumlahItem(int jumlahItem) {
        this.jumlahItem = jumlahItem;
    }

    @Override
    public String toString() {
        return "Pembayaran{" +
                "idPembayaran=" + idPembayaran +
                ", nomorPesanan='" + nomorPesanan + '\'' +
                ", namaPelanggan='" + namaPelanggan + '\'' +
                ", jumlah=" + jumlah +
                ", statusPembayaran='" + statusPembayaran + '\'' +
                '}';
    }
}
