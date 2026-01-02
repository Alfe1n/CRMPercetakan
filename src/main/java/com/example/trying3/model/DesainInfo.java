package com.example.trying3.model;

import java.time.LocalDateTime;

/**
 * Model untuk informasi desain lengkap.
 * Digunakan untuk transfer data antara DAO dan Controller.
 */
public class DesainInfo {

    private int idDesain;
    private String filePath;
    private int revisiKe;
    private int idStatusDesain;
    private String statusDesain;
    private String namaDesigner;
    private LocalDateTime tanggalDibuat;
    private LocalDateTime tanggalDisetujui;

    public static final int STATUS_DALAM_PENGERJAAN = 2;
    public static final int STATUS_MENUNGGU_APPROVAL = 3;
    public static final int STATUS_PERLU_REVISI = 4;
    public static final int STATUS_DISETUJUI = 5;
    public static final int STATUS_DITOLAK = 6;

    public DesainInfo() {
    }

    public DesainInfo(int idDesain, String filePath, int revisiKe, int idStatusDesain,
                      String statusDesain, String namaDesigner,
                      LocalDateTime tanggalDibuat, LocalDateTime tanggalDisetujui) {
        this.idDesain = idDesain;
        this.filePath = filePath;
        this.revisiKe = revisiKe;
        this.idStatusDesain = idStatusDesain;
        this.statusDesain = statusDesain;
        this.namaDesigner = namaDesigner;
        this.tanggalDibuat = tanggalDibuat;
        this.tanggalDisetujui = tanggalDisetujui;
    }


    public int getIdDesain() {
        return idDesain;
    }

    public void setIdDesain(int idDesain) {
        this.idDesain = idDesain;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getRevisiKe() {
        return revisiKe;
    }

    public void setRevisiKe(int revisiKe) {
        this.revisiKe = revisiKe;
    }

    public int getIdStatusDesain() {
        return idStatusDesain;
    }

    public void setIdStatusDesain(int idStatusDesain) {
        this.idStatusDesain = idStatusDesain;
    }

    public String getStatusDesain() {
        return statusDesain;
    }

    public void setStatusDesain(String statusDesain) {
        this.statusDesain = statusDesain;
    }

    public String getNamaDesigner() {
        return namaDesigner;
    }

    public void setNamaDesigner(String namaDesigner) {
        this.namaDesigner = namaDesigner;
    }

    public LocalDateTime getTanggalDibuat() {
        return tanggalDibuat;
    }

    public void setTanggalDibuat(LocalDateTime tanggalDibuat) {
        this.tanggalDibuat = tanggalDibuat;
    }

    public LocalDateTime getTanggalDisetujui() {
        return tanggalDisetujui;
    }

    public void setTanggalDisetujui(LocalDateTime tanggalDisetujui) {
        this.tanggalDisetujui = tanggalDisetujui;
    }

    /**
     * Mengecek apakah desain perlu revisi.
     */
    public boolean isPerluRevisi() {
        return idStatusDesain == STATUS_PERLU_REVISI;
    }

    /**
     * Mengecek apakah desain sudah disetujui.
     */
    public boolean isDisetujui() {
        return idStatusDesain == STATUS_DISETUJUI;
    }

    /**
     * Mengecek apakah desain sedang dalam pengerjaan.
     */
    public boolean isDalamPengerjaan() {
        return idStatusDesain == STATUS_DALAM_PENGERJAAN;
    }

    /**
     * Mengecek apakah desain menunggu approval.
     */
    public boolean isMenungguApproval() {
        return idStatusDesain == STATUS_MENUNGGU_APPROVAL;
    }

    /**
     * Mengecek apakah file desain sudah ada.
     */
    public boolean hasFile() {
        return filePath != null && !filePath.trim().isEmpty();
    }


    @Override
    public String toString() {
        return "DesainInfo{" +
                "idDesain=" + idDesain +
                ", filePath='" + filePath + '\'' +
                ", revisiKe=" + revisiKe +
                ", statusDesain='" + statusDesain + '\'' +
                ", namaDesigner='" + namaDesigner + '\'' +
                '}';
    }
}
