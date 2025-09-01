package com.example;

import java.sql.SQLException;
import io.javalin.Javalin;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        DatabaseManager dbManager = null;
        KafkaProducerService kafkaProducer = null;
        KafkaConsumerService kafkaConsumer = null;
        
        try {
            dbManager = new DatabaseManager();
            kafkaProducer = new KafkaProducerService();
            kafkaConsumer = new KafkaConsumerService(dbManager);
            
            Thread consumerThread = new Thread(kafkaConsumer);
            consumerThread.start();

            ObjectMapper objectMapper = new ObjectMapper();
            final DatabaseManager finalDbManager = dbManager;
            final KafkaProducerService finalKafkaProducer = kafkaProducer;
            final KafkaConsumerService finalKafkaConsumer = kafkaConsumer;

            Javalin app = Javalin.create().start(8080);

            app.get("/", ctx -> ctx.result("Hello World"));
            
            app.post("/send-name", ctx -> {
                try {
                    String requestBody = ctx.body();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> jsonMap = objectMapper.readValue(requestBody, Map.class);
                    String name = (String) jsonMap.get("name");
                    
                    if (name == null || name.trim().isEmpty()) {
                        ctx.status(400).result("Name is required");
                        return;
                    }
                    
                    finalKafkaProducer.sendMessage(name);
                    ctx.status(200).result("Message sent to Kafka successfully");
                    
                } catch (Exception e) {
                    ctx.status(500).result("Error processing request: " + e.getMessage());
                }
            });
            
            app.get("/last-message", ctx -> {
                try {
                    String lastMessage = finalDbManager.getLastMessage();
                    if (lastMessage != null) {
                        ctx.json(Map.of("lastMessage", lastMessage));
                    } else {
                        ctx.json(Map.of("lastMessage", "No messages found"));
                    }
                } catch (SQLException e) {
                    ctx.status(500).result("Error retrieving last message: " + e.getMessage());
                }
            });
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down...");
                finalKafkaConsumer.stop();
                finalKafkaProducer.close();
                try {
                    finalDbManager.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }));
            
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // Resources will be closed by shutdown hook
        }
    }
}
