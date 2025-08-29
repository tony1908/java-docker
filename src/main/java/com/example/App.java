package com.example;

import java.sql.SQLException;
//import redis.clients.jedis.UnifiedJedis;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        //UnifiedJedis jedis = new UnifiedJedis("redis://redis:6379");
        //jedis.set("name", "John Doe");
        //System.out.println(jedis.get("name"));
        //jedis.close();

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
