package com.example.trying3.controller.management;

import com.example.trying3.dao.PesananDAO;
import com.example.trying3.dao.PelangganDAO; // Pastikan DAO ini ada
import com.example.trying3.model.Pesanan;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class EksporDataController {

    private final PesananDAO pesananDAO = new PesananDAO();
    private final PelangganDAO pelangganDAO = new PelangganDAO();

    @FXML
    private void handleEksporSemua() {
        List<Pesanan> data = pesananDAO.getAllPesananForExport();
        generateExcel("Semua_Pesanan", new String[]{"ID", "Pelanggan", "Status", "Total"}, data, "PESANAN");
    }

    @FXML
    private void handleEksporPelanggan() {
        List<Map<String, String>> data = pelangganDAO.getAllPelangganForExport();
        generateExcel("Data_Pelanggan", new String[]{"Nama", "Email", "Telepon", "Alamat"}, data, "PELANGGAN");
    }

    @FXML
    private void handleLaporanKeuangan() {
        // Ambil semua pesanan untuk laporan keuangan
        List<Pesanan> data = pesananDAO.getAllPesananForExport();
        generateExcel("Laporan_Keuangan", new String[]{"ID", "Tanggal", "Pelanggan", "Total Pendapatan"}, data, "KEUANGAN");
    }

    @FXML
    private void handleLaporanProduksi() {
        List<Pesanan> data = pesananDAO.getAllPesananForExport();
        generateExcel("Laporan_Produksi", new String[]{"ID", "Pelanggan", "Status Produksi"}, data, "PRODUKSI");
    }

    private void generateExcel(String fileName, String[] headers, Object dataList, String type) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(fileName + "_" + LocalDate.now() + ".xlsx");
        File file = fileChooser.showSaveDialog(null);

        if (file == null) return;

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Laporan");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            // Create Header
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Fill Data
            int rowIdx = 1;
            if (type.equals("PELANGGAN")) {
                List<Map<String, String>> list = (List<Map<String, String>>) dataList;
                for (Map<String, String> p : list) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(p.get("Nama"));
                    row.createCell(1).setCellValue(p.get("Email"));
                    row.createCell(2).setCellValue(p.get("Telepon"));
                    row.createCell(3).setCellValue(p.get("Alamat"));
                }
            } else {
                List<Pesanan> list = (List<Pesanan>) dataList;
                for (Pesanan p : list) {
                    Row row = sheet.createRow(rowIdx++);
                    if (type.equals("PESANAN")) {
                        row.createCell(0).setCellValue(p.getIdPesanan());
                        row.createCell(1).setCellValue(p.getNamaPelanggan());
                        row.createCell(2).setCellValue(p.getStatus());
                        row.createCell(3).setCellValue(p.getTotalBiaya());
                    } else if (type.equals("KEUANGAN")) {
                        row.createCell(0).setCellValue(p.getIdPesanan());
                        row.createCell(1).setCellValue(p.getFormattedDate());
                        row.createCell(2).setCellValue(p.getNamaPelanggan());
                        row.createCell(3).setCellValue(p.getTotalBiaya());
                    } else if (type.equals("PRODUKSI")) {
                        row.createCell(0).setCellValue(p.getIdPesanan());
                        row.createCell(1).setCellValue(p.getNamaPelanggan());
                        row.createCell(2).setCellValue(p.getStatus());
                    }
                }
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            try (FileOutputStream out = new FileOutputStream(file)) {
                workbook.write(out);
                showSuccessAlert("Berhasil", "Data berhasil disimpan di " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Gagal", "Terjadi kesalahan: " + e.getMessage());
        }
    }

    private void showSuccessAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}