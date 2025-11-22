package com.example.trying3.config;

import com.example.trying3.util.AlertUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/crm_percetakan";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Koneksi database berhasil!");
            return conn;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver database tidak ditemukan!", e);
        } catch (SQLException e) {
            throw e;
        }
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()){
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
