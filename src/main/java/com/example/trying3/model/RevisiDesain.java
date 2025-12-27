package com.example.trying3.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Model untuk tabel revisi_desain
 * Menyimpan history setiap revisi desain
 */
public class RevisiDesain {

    private int idRevisi;
    private int idDesain;
    private int revisiKe;
    private String filePath;
    private String catatanRevisi;
    private int direvisiOleh;
    private String namaDesigner; // Untuk display (dari JOIN)
    private LocalDateTime tanggalRevisi;

    // Constructors
    public RevisiDesain() {}

    public RevisiDesain(int idDesain, int revisiKe, String filePath, String catatanRevisi, int direvisiOleh) {
        this.idDesain = idDesain;
        this.revisiKe = revisiKe;
        this.filePath = filePath;
        this.catatanRevisi = catatanRevisi;
        this.direvisiOleh = direvisiOleh;
        this.tanggalRevisi = LocalDateTime.now();
    }

    // Getters and Setters
    public int getIdRevisi() {
        return idRevisi;
    }

    public void setIdRevisi(int idRevisi) {
        this.idRevisi = idRevisi;
    }

    public int getIdDesain() {
        return idDesain;
    }

    public void setIdDesain(int idDesain) {
        this.idDesain = idDesain;
    }

    public int getRevisiKe() {
        return revisiKe;
    }

    public void setRevisiKe(int revisiKe) {
        this.revisiKe = revisiKe;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getCatatanRevisi() {
        return catatanRevisi;
    }

    public void setCatatanRevisi(String catatanRevisi) {
        this.catatanRevisi = catatanRevisi;
    }

    public int getDirevisiOleh() {
        return direvisiOleh;
    }

    public void setDirevisiOleh(int direvisiOleh) {
        this.direvisiOleh = direvisiOleh;
    }

    public String getNamaDesigner() {
        return namaDesigner;
    }

    public void setNamaDesigner(String namaDesigner) {
        this.namaDesigner = namaDesigner;
    }

    public LocalDateTime getTanggalRevisi() {
        return tanggalRevisi;
    }

    public void setTanggalRevisi(LocalDateTime tanggalRevisi) {
        this.tanggalRevisi = tanggalRevisi;
    }

    // Helper Methods
    public String getFormattedTanggal() {
        if (tanggalRevisi == null) return "-";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        return tanggalRevisi.format(formatter);
    }

    public String getFileName() {
        if (filePath == null || filePath.isEmpty()) return "-";
        // Ambil nama file dari path
        int lastSeparator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return lastSeparator >= 0 ? filePath.substring(lastSeparator + 1) : filePath;
    }

    @Override
    public String toString() {
        return "RevisiDesain{" +
                "idRevisi=" + idRevisi +
                ", idDesain=" + idDesain +
                ", revisiKe=" + revisiKe +
                ", filePath='" + filePath + '\'' +
                ", tanggalRevisi=" + tanggalRevisi +
                '}';
    }
}