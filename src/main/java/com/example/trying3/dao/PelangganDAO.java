package com.example.trying3.dao;

import com.example.trying3.config.DatabaseConnection;
import java.sql.*;
import java.util.*;

/**
 * Data Access Object untuk Pelanggan.
 * Mengelola operasi CRUD dan ekspor data pelanggan.
 */
public class PelangganDAO {
    public List<Map<String, String>> getAllPelangganForExport() {
        List<Map<String, String>> list = new ArrayList<>();
        String sql = "SELECT nama, email, no_telepon, alamat FROM pelanggan";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, String> map = new HashMap<>();
                map.put("Nama", rs.getString("nama"));
                map.put("Email", rs.getString("email"));
                map.put("Telepon", rs.getString("no_telepon"));
                map.put("Alamat", rs.getString("alamat"));
                list.add(map);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}