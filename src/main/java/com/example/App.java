package com.example;

import java.sql.SQLException;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        DatabaseManager dbManager = null;
        try {
            dbManager = new DatabaseManager();

            dbManager.insertUser("John Doe", "john.doe@example.com");
            dbManager.insertUser("Tony Stark", "tony.stark@example.com");
            dbManager.getAllUsers();

            while (true) {
                Thread.sleep(10000);
            }
            
            
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (dbManager != null) {
                try {
                    dbManager.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
