module com.gestion {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.j;
    requires kernel;
    requires layout;
    requires io;
    requires java.mail;
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires java.desktop;
    requires org.json;

    opens com.gestion to javafx.fxml;
    opens com.gestion.controller to javafx.fxml;
    opens com.gestion.entity to javafx.base;
    opens com.gestion.util to javafx.fxml;
    
    exports com.gestion;
    exports com.gestion.controller;
    exports com.gestion.entity;
    exports com.gestion.util;
}
