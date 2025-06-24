module com.example.taskmanagerv3 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.sql;
    requires java.net.http;
    requires java.prefs;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires jbcrypt;
    requires com.fasterxml.jackson.databind;

    requires com.microsoft.sqlserver.jdbc;
    requires com.gluonhq.charm.glisten;
    requires com.gluonhq.attach.util;
    requires com.gluonhq.attach.lifecycle;
    requires com.gluonhq.attach.display;

    // Export and Chart dependencies
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires java.desktop;
    requires org.jfree.jfreechart;

    // requires java.mail;
    // requires java.websocket;
    // requires java.activation;


    opens com.example.taskmanagerv3 to javafx.fxml;
    opens com.example.taskmanagerv3.controller to javafx.fxml;
    opens com.example.taskmanagerv3.model to com.fasterxml.jackson.databind;
    opens com.example.taskmanagerv3.model.gemini to com.fasterxml.jackson.databind;

    exports com.example.taskmanagerv3;
    exports com.example.taskmanagerv3.model;
    exports com.example.taskmanagerv3.controller;
    exports com.example.taskmanagerv3.service;
    exports com.example.taskmanagerv3.config;
    exports com.example.taskmanagerv3.util;
}