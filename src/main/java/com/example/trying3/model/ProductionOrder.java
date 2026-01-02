package com.example.trying3.model;

/**
 * Model untuk Production Order.
 * Digunakan di ProduksiController untuk menampilkan data produksi.
 * Immutable object dengan final fields.
 */
public class ProductionOrder {

    private final String id;
    private final String customerName;
    private final String date;
    private final String service;
    private final int quantity;
    private final String specs;
    private final String fileDesainPath;
    private final String status;
    private final String catatan;

    public ProductionOrder(String id, String customerName, String date, String service,
                           int quantity, String specs, String fileDesainPath,
                           String status, String catatan) {
        this.id = id != null ? id : "";
        this.customerName = customerName != null ? customerName : "Unknown";
        this.date = date != null ? date : "-";
        this.service = service != null ? service : "-";
        this.quantity = quantity;
        this.specs = specs != null ? specs : "";
        this.fileDesainPath = fileDesainPath != null ? fileDesainPath : "";
        this.status = status != null ? status : "Unknown";
        this.catatan = catatan != null ? catatan : "";
    }

    public String getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getDate() {
        return date;
    }

    public String getService() {
        return service;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getSpecs() {
        return specs;
    }

    public String getFileDesainPath() {
        return fileDesainPath;
    }

    public String getStatus() {
        return status;
    }

    public String getCatatan() {
        return catatan;
    }

    /**
     * Mengecek apakah file desain sudah ada.
     */
    public boolean hasDesignFile() {
        return fileDesainPath != null && !fileDesainPath.trim().isEmpty();
    }

    /**
     * Mengecek apakah status adalah "Antrian Produksi".
     */
    public boolean isInQueue() {
        return "Antrian Produksi".equalsIgnoreCase(status);
    }

    /**
     * Mengecek apakah sedang dalam produksi.
     */
    public boolean isInProgress() {
        return "Sedang Diproduksi".equalsIgnoreCase(status);
    }

    /**
     * Mengecek apakah sudah selesai.
     */
    public boolean isCompleted() {
        return "Siap Dikirim".equalsIgnoreCase(status) ||
                "Selesai".equalsIgnoreCase(status);
    }

    /**
     * Mengecek apakah ada catatan.
     */
    public boolean hasCatatan() {
        return catatan != null && !catatan.trim().isEmpty();
    }

    /**
     * Mendapatkan display ID dengan format "PO-XXX".
     */
    public String getDisplayId() {
        return "PO-" + id;
    }


    @Override
    public String toString() {
        return "ProductionOrder{" +
                "id='" + id + '\'' +
                ", customerName='" + customerName + '\'' +
                ", status='" + status + '\'' +
                ", service='" + service + '\'' +
                ", quantity=" + quantity +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        ProductionOrder that = (ProductionOrder) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
