package com.example;

import io.javalin.Javalin;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class App {
    private static DatabaseManager dbManager;

    public static void main(String[] args) {
        try {
            dbManager = new DatabaseManager();
            
            Javalin app = Javalin.create().start(8080);
            
            app.before(ctx -> {
                ctx.header("Access-Control-Allow-Origin", "*");
                ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
            });

            app.get("/", ctx -> {
                ctx.json(Map.of("message", "Java API with MariaDB", "status", "running"));
            });

            app.get("/users", ctx -> {
                try {
                    List<User> users = dbManager.getUsersList();
                    ctx.json(users);
                } catch (SQLException e) {
                    ctx.status(500).json(Map.of("error", "Database error: " + e.getMessage()));
                }
            });

            app.get("/users/{id}", ctx -> {
                try {
                    int id = Integer.parseInt(ctx.pathParam("id"));
                    User user = dbManager.getUserById(id);
                    if (user != null) {
                        ctx.json(user);
                    } else {
                        ctx.status(404).json(Map.of("error", "User not found"));
                    }
                } catch (NumberFormatException e) {
                    ctx.status(400).json(Map.of("error", "Invalid user ID"));
                } catch (SQLException e) {
                    ctx.status(500).json(Map.of("error", "Database error: " + e.getMessage()));
                }
            });

            app.post("/users", ctx -> {
                try {
                    String name = ctx.formParam("name");
                    String email = ctx.formParam("email");
                    
                    if (name == null || email == null) {
                        ctx.status(400).json(Map.of("error", "Name and email are required"));
                        return;
                    }
                    
                    User user = dbManager.createUser(name, email);
                    if (user != null) {
                        ctx.status(201).json(user);
                    } else {
                        ctx.status(500).json(Map.of("error", "Failed to create user"));
                    }
                } catch (SQLException e) {
                    ctx.status(500).json(Map.of("error", "Database error: " + e.getMessage()));
                }
            });

            app.get("/health", ctx -> {
                ctx.json(Map.of("status", "healthy", "database", "connected"));
            });

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                app.stop();
                if (dbManager != null) {
                    try {
                        dbManager.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }));

        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
