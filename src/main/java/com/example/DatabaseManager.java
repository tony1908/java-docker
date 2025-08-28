package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DB_HOST = "mariadb";
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "myapp";
    private static final String DB_USER = "appuser";
    private static final String DB_PASSWORD = "apppassword";

    private static final String JDBC_URL = String.format("jdbc:mariadb://%s:%s/%s", DB_HOST, DB_PORT, DB_NAME);

    private Connection connection;

    public DatabaseManager() throws SQLException {
        connect();
        initDatabase();
    }

    public void connect() throws SQLException {
        connection = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
    }

    private void initDatabase() throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS users (" +
             "    id INT AUTO_INCREMENT PRIMARY KEY," +
             "    name VARCHAR(100) NOT NULL," +
             "    email VARCHAR(100) NOT NULL UNIQUE," +
             "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
             ")";
            
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createTableSQL);
            }
    }

    public void insertUser(String name, String email) throws SQLException {
        String insertSQL = "INSERT INTO users (name, email) VALUES (?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(insertSQL)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            int rowsAffected = stmt.executeUpdate();
            System.out.println(rowsAffected + " row(s) inserted.");
        }
    }

    public void getAllUsers() throws SQLException {
        String selectSQL = "SELECT * FROM users";
        
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(selectSQL);
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String email = rs.getString("email");
                System.out.println("ID: " + id + ", Name: " + name + ", Email: " + email);
            }
        }
    }

    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }
}
