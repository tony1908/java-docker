package com.example;

import java.sql.SQLException;
import io.javalin.Javalin;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        DatabaseManager dbManager = null;
        KafkaConsumerService consumerService = null;
        KafkaProducerService producerService = null;

        try {
            dbManager = new DatabaseManager();
            consumerService = new KafkaConsumerService(dbManager);
            producerService = new KafkaProducerService();

            Thread consumerThread = new Thread(consumerService);
            consumerThread.start();

            ObjectMapper objectMapper = new ObjectMapper();

            final DatabaseManager finalDbManager = dbManager;
            final KafkaConsumerService finalConsumerService = consumerService;
            final KafkaProducerService finalProducerService = producerService;

            Javalin app = Javalin.create().start(8080);
            
            app.get("/", ctx -> ctx.result("Hello World"));
            
            app.post("/message", ctx -> {
                try {
                    String requestBody = ctx.body();
                    @SuppressWarnings("unchecked")
                    Map<String, String> requestMap = objectMapper.readValue(requestBody, Map.class);
                    String name = requestMap.get("name");

                    finalProducerService.sendMessage(name);
                    ctx.status(200).result("Message sent successfully");
                } catch (Exception e) {
                    ctx.status(500).result(e.getMessage());
                }
            });
            
            app.get("/last-message", ctx -> {
                try {
                    String lastMessage = finalDbManager.getLastMessage();
                    ctx.json(Map.of("lastMessage", lastMessage));
                } catch (Exception e) {
                    ctx.status(500).result(e.getMessage());
                }
            });

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    finalDbManager.close();
                    finalConsumerService.stop();
                    finalProducerService.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));

        } catch (Exception e) {
            e.printStackTrace();
        }
        

    }
}
