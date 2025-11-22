package com.example.trying3.model;

import java.time.LocalDateTime;

public class Pesanan {
    private int idPesanan;
    private String namaPelanggan;
    private String noTelepon;
    private String email;
    private String jenisLayanan;
    private int jumlah;
    private double totalHarga;
    private String spesifikasi;
    private String status;
    private LocalDateTime tanggalPesanan;
    private LocalDateTime updatedAt;

    // Constructors
    public Pesanan() {
    }

    public Pesanan(int idPesanan, String namaPelanggan, String noTelepon, String email, 
                   String jenisLayanan, int jumlah, double totalHarga, String spesifikasi,
                   String status, LocalDateTime tanggalPesanan) {
        this.idPesanan = idPesanan;
        this.namaPelanggan = namaPelanggan;
        this.noTelepon = noTelepon;
        this.email = email;
        this.jenisLayanan = jenisLayanan;
        this.jumlah = jumlah;
        this.totalHarga = totalHarga;
        this.spesifikasi = spesifikasi;
        this.status = status;
        this.tanggalPesanan = tanggalPesanan;
    }

    // Getters and Setters
    public int getIdPesanan() {
        return idPesanan;
    }

    public void setIdPesanan(int idPesanan) {
        this.idPesanan = idPesanan;
    }

    public String getNamaPelanggan() {
        return namaPelanggan;
    }

    public void setNamaPelanggan(String namaPelanggan) {
        this.namaPelanggan = namaPelanggan;
    }

    public String getNoTelepon() {
        return noTelepon;
    }

    public void setNoTelepon(String noTelepon) {
        this.noTelepon = noTelepon;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getJenisLayanan() {
        return jenisLayanan;
    }

    public void setJenisLayanan(String jenisLayanan) {
        this.jenisLayanan = jenisLayanan;
    }

    public int getJumlah() {
        return jumlah;
    }

    public void setJumlah(int jumlah) {
        this.jumlah = jumlah;
    }

    public double getTotalHarga() {
        return totalHarga;
    }

    public void setTotalHarga(double totalHarga) {
        this.totalHarga = totalHarga;
    }

    public String getSpesifikasi() {
        return spesifikasi;
    }

    public void setSpesifikasi(String spesifikasi) {
        this.spesifikasi = spesifikasi;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getTanggalPesanan() {
        return tanggalPesanan;
    }

    public void setTanggalPesanan(LocalDateTime tanggalPesanan) {
        this.tanggalPesanan = tanggalPesanan;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Pesanan{" +
                "idPesanan=" + idPesanan +
                ", namaPelanggan='" + namaPelanggan + '\'' +
                ", noTelepon='" + noTelepon + '\'' +
                ", jenisLayanan='" + jenisLayanan + '\'' +
                ", status='" + status + '\'' +
                ", totalHarga=" + totalHarga +
                '}';
    }
}
