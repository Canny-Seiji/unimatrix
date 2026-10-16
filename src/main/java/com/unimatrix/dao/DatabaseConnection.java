package com.unimatrix.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Update your local MySQL URL, username, and password if necessary
    private static final String URL = "jdbc:mysql://localhost:3306/unimatrix_db";
    private static final String USER = "root"; // Change to your MySQL username
    private static final String PASSWORD = "admin123"; // Change to your MySQL password

    public static Connection getConnection() {
        try {
            Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Successfully connected to the UniMatrix MySQL database!");
            return connection;
        } catch (SQLException e) {
            System.err.println("Database connection failed!");
            e.printStackTrace();
            return null;
        }
    }

    // Quick main method to test the connection directly
    public static void main(String[] args) {
        getConnection();
    }
}
