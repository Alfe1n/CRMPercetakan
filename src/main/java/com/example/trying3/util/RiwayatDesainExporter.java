package com.example.trying3.util;

import com.example.trying3.model.RiwayatDesain;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Utility untuk ekspor data Riwayat Desain ke Excel.
 */
public class RiwayatDesainExporter {

    private static final String[] HEADERS = {
            "No", "ID Desain", "Nama Pelanggan", "Email",
            "Jenis Layanan", "Desainer", "Tanggal Selesai",
            "Status", "Total Biaya"
    };

    private static final String SHEET_NAME = "Riwayat Desain";
    private static final String REPORT_TITLE = "LAPORAN RIWAYAT DESAIN";

    public static boolean exportWithDialog(ObservableList<RiwayatDesain> data, Stage stage) {
        if (data == null || data.isEmpty()) {
            AlertUtil.showWarning("Peringatan", "Tidak ada data untuk diekspor.");
            return false;
        }

        if (stage == null) {
            System.err.println("❌ Stage is null, cannot show file dialog");
            return false;
        }

        File file = showSaveDialog(stage);
        if (file == null) {
            return false;
        }

        return exportToExcel(data, file);
    }

    public static boolean exportToExcel(ObservableList<RiwayatDesain> data, File file) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        if (file == null) {
            return false;
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);

            // Create styles
            ExcelStyles styles = createStyles(workbook);

            // Create title
            createTitleSection(sheet, styles);

            // Create header
            createHeaderRow(sheet, styles, 3);

            // Create data rows
            double grandTotal = createDataRows(sheet, styles, data, 4);

            // Create total row
            createTotalRow(workbook, sheet, data.size() + 5, grandTotal);

            // Auto-size columns
            autoSizeColumns(sheet);

            // Save file
            saveWorkbook(workbook, file);

            AlertUtil.showInfo("Berhasil", "Data berhasil diekspor ke:\n" + file.getAbsolutePath());
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "Gagal mengekspor data: " + e.getMessage());
            return false;
        }
    }

    private static File showSaveDialog(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Simpan File Excel");
        fileChooser.setInitialFileName("Riwayat_Desain_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        return fileChooser.showSaveDialog(stage);
    }

    private static void createTitleSection(Sheet sheet, ExcelStyles styles) {
        // Title row
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(REPORT_TITLE);
        titleCell.setCellStyle(styles.titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

        // Subtitle row
        Row subtitleRow = sheet.createRow(1);
        Cell subtitleCell = subtitleRow.createCell(0);
        subtitleCell.setCellValue("Diekspor pada: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm",
                        new Locale("id", "ID"))));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 8));
    }

    private static void createHeaderRow(Sheet sheet, ExcelStyles styles, int rowIndex) {
        Row headerRow = sheet.createRow(rowIndex);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(styles.headerStyle);
        }
    }

    private static double createDataRows(Sheet sheet, ExcelStyles styles,
                                         ObservableList<RiwayatDesain> data, int startRow) {
        int rowNum = startRow;
        int no = 1;
        double grandTotal = 0;

        for (RiwayatDesain riwayat : data) {
            Row row = sheet.createRow(rowNum++);

            createCell(row, 0, no++, styles.dataStyle);
            createCell(row, 1, riwayat.getIdDesain(), styles.dataStyle);
            createCell(row, 2, riwayat.getNamaPelanggan(), styles.dataStyle);
            createCell(row, 3, riwayat.getEmail(), styles.dataStyle);
            createCell(row, 4, riwayat.getJenisLayanan(), styles.dataStyle);
            createCell(row, 5, riwayat.getDesigner(), styles.dataStyle);
            createCell(row, 6, riwayat.getTanggalSelesai(), styles.dataStyle);

            // Status with conditional formatting
            Cell cellStatus = row.createCell(7);
            cellStatus.setCellValue(riwayat.getStatus());
            cellStatus.setCellStyle(getStatusStyle(riwayat.getStatus(), styles));

            // Currency cell
            Cell cellBiaya = row.createCell(8);
            cellBiaya.setCellValue(riwayat.getTotalBiayaNumeric());
            cellBiaya.setCellStyle(styles.currencyStyle);

            grandTotal += riwayat.getTotalBiayaNumeric();
        }

        return grandTotal;
    }

    private static void createTotalRow(Workbook workbook, Sheet sheet, int rowIndex, double grandTotal) {
        Row totalRow = sheet.createRow(rowIndex);

        // Label
        Cell totalLabelCell = totalRow.createCell(7);
        totalLabelCell.setCellValue("GRAND TOTAL:");
        CellStyle totalLabelStyle = workbook.createCellStyle();
        Font totalFont = workbook.createFont();
        totalFont.setBold(true);
        totalLabelStyle.setFont(totalFont);
        totalLabelStyle.setAlignment(HorizontalAlignment.RIGHT);
        totalLabelCell.setCellStyle(totalLabelStyle);

        // Value
        Cell totalValueCell = totalRow.createCell(8);
        totalValueCell.setCellValue(grandTotal);
        CellStyle totalValueStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        totalValueStyle.setDataFormat(format.getFormat("\"Rp\" #,##0"));
        totalValueStyle.setFont(totalFont);
        totalValueStyle.setAlignment(HorizontalAlignment.RIGHT);
        totalValueCell.setCellStyle(totalValueStyle);
    }

    private static void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "-");
        cell.setCellStyle(style);
    }

    private static void createCell(Row row, int column, int value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static CellStyle getStatusStyle(String status, ExcelStyles styles) {
        if (status == null)
            return styles.statusLainnyaStyle;

        return switch (status.toLowerCase()) {
            case "selesai", "disetujui" -> styles.statusSelesaiStyle;
            case "ditolak" -> styles.statusDitolakStyle;
            default -> styles.statusLainnyaStyle;
        };
    }

    private static void autoSizeColumns(Sheet sheet) {
        for (int i = 0; i < HEADERS.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 500);
        }

        // Set minimum width for specific columns
        sheet.setColumnWidth(2, Math.max(sheet.getColumnWidth(2), 5000)); // Nama Pelanggan
        sheet.setColumnWidth(3, Math.max(sheet.getColumnWidth(3), 6000)); // Email
        sheet.setColumnWidth(4, Math.max(sheet.getColumnWidth(4), 5000)); // Jenis Layanan
        sheet.setColumnWidth(8, Math.max(sheet.getColumnWidth(8), 4000)); // Total Biaya
    }

    private static void saveWorkbook(Workbook workbook, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            workbook.write(fos);
        }
    }

    private static ExcelStyles createStyles(Workbook workbook) {
        ExcelStyles styles = new ExcelStyles();

        // Header style
        styles.headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        styles.headerStyle.setFont(headerFont);
        styles.headerStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        styles.headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.headerStyle.setAlignment(HorizontalAlignment.CENTER);
        styles.headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        styles.headerStyle.setBorderBottom(BorderStyle.THIN);
        styles.headerStyle.setBorderTop(BorderStyle.THIN);
        styles.headerStyle.setBorderLeft(BorderStyle.THIN);
        styles.headerStyle.setBorderRight(BorderStyle.THIN);

        // Title style
        styles.titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        styles.titleStyle.setFont(titleFont);
        styles.titleStyle.setAlignment(HorizontalAlignment.CENTER);

        // Data style
        styles.dataStyle = workbook.createCellStyle();
        styles.dataStyle.setBorderBottom(BorderStyle.THIN);
        styles.dataStyle.setBorderTop(BorderStyle.THIN);
        styles.dataStyle.setBorderLeft(BorderStyle.THIN);
        styles.dataStyle.setBorderRight(BorderStyle.THIN);
        styles.dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // Currency style
        styles.currencyStyle = workbook.createCellStyle();
        styles.currencyStyle.cloneStyleFrom(styles.dataStyle);
        DataFormat format = workbook.createDataFormat();
        styles.currencyStyle.setDataFormat(format.getFormat("\"Rp\" #,##0"));
        styles.currencyStyle.setAlignment(HorizontalAlignment.RIGHT);

        // Status styles
        styles.statusSelesaiStyle = createStatusStyle(workbook, styles.dataStyle, IndexedColors.LIGHT_GREEN);
        styles.statusDitolakStyle = createStatusStyle(workbook, styles.dataStyle, IndexedColors.CORAL);
        styles.statusLainnyaStyle = createStatusStyle(workbook, styles.dataStyle, IndexedColors.LIGHT_YELLOW);

        return styles;
    }

    private static CellStyle createStatusStyle(Workbook workbook, CellStyle baseStyle, IndexedColors bgColor) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(baseStyle);
        style.setFillForegroundColor(bgColor.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private static class ExcelStyles {
        CellStyle headerStyle;
        CellStyle titleStyle;
        CellStyle dataStyle;
        CellStyle currencyStyle;
        CellStyle statusSelesaiStyle;
        CellStyle statusDitolakStyle;
        CellStyle statusLainnyaStyle;
    }
}
