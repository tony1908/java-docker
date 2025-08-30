package com.example;

import java.sql.SQLException;
//import redis.clients.jedis.UnifiedJedis;
import io.javalin.Javalin;

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

            //dbManager.insertUser("John Doe", "john.doe@example.com");
            //dbManager.insertUser("Tony Stark", "tony.stark@example.com");
            //dbManager.getAllUsers();

            Javalin app = Javalin.create().start(8080);

            app.get("/", ctx -> ctx.result("Hello World"));
            
            
        } catch (SQLException e) {
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
