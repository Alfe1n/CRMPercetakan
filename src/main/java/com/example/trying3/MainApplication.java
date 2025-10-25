package com.example.trying3;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Memulai aplikasi dari halaman Login
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("fxml/auth/Login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1920, 1080); // Set ukuran scene di sini
        stage.setTitle("CRM Percetakan");
        stage.setScene(scene);
        stage.setResizable(false); // Agar ukuran window tidak bisa diubah
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
