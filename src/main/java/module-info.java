module com.example.trying3 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.sql;

    opens com.example.trying3 to javafx.fxml;
    exports com.example.trying3;
    opens com.example.trying3.controller.auth to javafx.fxml;
    exports com.example.trying3.controller.auth;
    exports com.example.trying3.controller.admin;
    opens com.example.trying3.controller.admin to javafx.fxml;
    exports com.example.trying3.config;
    opens com.example.trying3.config to javafx.fxml;
    exports com.example.trying3.util;
    opens com.example.trying3.util to javafx.fxml;
    exports com.example.trying3.model;
    opens com.example.trying3.model to javafx.fxml;
    exports com.example.trying3.dao;
    opens com.example.trying3.dao to javafx.fxml;
}