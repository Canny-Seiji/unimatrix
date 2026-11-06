package com.unimatrix.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Update your local MySQL URL, username, and password if necessary
    private static final String URL = "jdbc:mysql://localhost:3306/unimatrix_db";
    private static final String USER = "root"; // Change to your MySQL username
    private static final String PASSWORD = "admin123"; // Change to your MySQL password

    public static Connection getConnection() throws SQLException {
        String url = System.getProperty("unimatrix.db.url", System.getenv().getOrDefault("UNIMATRIX_DB_URL", URL));
        java.util.Properties properties = new java.util.Properties();
        properties.setProperty("user", System.getProperty("unimatrix.db.user", System.getenv().getOrDefault("UNIMATRIX_DB_USER", USER)));
        properties.setProperty("password", System.getProperty("unimatrix.db.password", System.getenv().getOrDefault("UNIMATRIX_DB_PASSWORD", PASSWORD)));
        properties.setProperty("connectTimeout", "5000");
        properties.setProperty("socketTimeout", "15000");
        return DriverManager.getConnection(url, properties);
    }

    // Quick main method to test the connection directly
    public static void main(String[] args) throws SQLException {
        try (Connection connection = getConnection()) {
            System.out.println("Successfully connected to the UniMatrix MySQL database!");
        }
    }
}
