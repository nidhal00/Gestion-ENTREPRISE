package com.gestion.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataSource {
    private String url = "jdbc:mysql://localhost:3306/mindaudit_pidev?autoReconnect=true&useSSL=false";
    private String user = "root";
    private String password = "";
    private Connection connection;
    private static DataSource instance;

    private DataSource() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public static DataSource getInstance() {
        if (instance == null) {
            instance = new DataSource();
        } else {
            try {
                if (instance.getConnection().isClosed()) {
                    instance = new DataSource();
                }
            } catch (SQLException e) {
                instance = new DataSource();
            }
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}
