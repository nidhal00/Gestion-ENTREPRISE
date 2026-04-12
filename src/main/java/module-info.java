module com.gestion {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.j;

    opens com.gestion to javafx.fxml;
    opens com.gestion.controller to javafx.fxml;
    opens com.gestion.entity to javafx.base;
    opens com.gestion.util to javafx.fxml;
    
    exports com.gestion;
    exports com.gestion.controller;
    exports com.gestion.entity;
}
