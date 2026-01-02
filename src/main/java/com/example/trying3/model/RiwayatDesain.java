package com.example.trying3.model;

/**
 * Model untuk data Riwayat Desain.
 * Digunakan untuk menampilkan riwayat desain di UI dengan immutable fields.
 */
public class RiwayatDesain {

    private final String idDesain;
    private final String namaPelanggan;
    private final String email;
    private final String jenisLayanan;
    private final String designer;
    private final String status;
    private final String tanggalSelesai;
    private final String harga;
    private final double totalBiayaNumeric;

    public RiwayatDesain(String idDesain, String namaPelanggan, String email, String jenisLayanan,
                         String designer, String status, String tanggalSelesai,
                         String harga, double totalBiayaNumeric) {
        this.idDesain = idDesain != null ? idDesain : "-";
        this.namaPelanggan = namaPelanggan != null ? namaPelanggan : "-";
        this.email = email != null ? email : "-";
        this.jenisLayanan = jenisLayanan != null ? jenisLayanan : "-";
        this.designer = designer != null ? designer : "-";
        this.status = status != null ? status : "Unknown";
        this.tanggalSelesai = tanggalSelesai != null ? tanggalSelesai : "-";
        this.harga = harga != null ? harga : "Rp 0";
        this.totalBiayaNumeric = totalBiayaNumeric;
    }

    public String getIdDesain() {
        return idDesain;
    }

    public String getIdPesanan() {
        return idDesain;
    }

    public String getNamaPelanggan() {
        return namaPelanggan;
    }

    public String getEmail() {
        return email;
    }

    public String getJenisLayanan() {
        return jenisLayanan;
    }

    public String getDesigner() {
        return designer;
    }

    public String getStatus() {
        return status;
    }

    public String getTanggalSelesai() {
        return tanggalSelesai;
    }

    public String getHarga() {
        return harga;
    }

    public String getTotal() {
        return harga;
    }

    public double getTotalBiayaNumeric() {
        return totalBiayaNumeric;
    }

    public boolean isApproved() {
        String lowerStatus = status.toLowerCase();
        return lowerStatus.equals("selesai") || lowerStatus.equals("disetujui");
    }

    public boolean isRejected() {
        return status.toLowerCase().equals("ditolak");
    }

    public boolean needsRevision() {
        return status.toLowerCase().equals("revisi");
    }

    public boolean hasValidEmail() {
        return email != null && !email.equals("-") && !email.trim().isEmpty();
    }


    @Override
    public String toString() {
        return "RiwayatDesain{" +
                "idDesain='" + idDesain + '\'' +
                ", namaPelanggan='" + namaPelanggan + '\'' +
                ", designer='" + designer + '\'' +
                ", status='" + status + '\'' +
                ", totalBiaya=" + totalBiayaNumeric +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        RiwayatDesain that = (RiwayatDesain) obj;
        return idDesain.equals(that.idDesain);
    }

    @Override
    public int hashCode() {
        return idDesain.hashCode();
    }
}
