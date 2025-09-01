package com.example;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class KafkaConsumerService implements Runnable {
    private static final String TOPIC_NAME = "names-topic";
    private static final String GROUP_ID = "names-consumer-group";
    private final KafkaConsumer<String, String> consumer;
    private final DatabaseManager dbManager;
    private volatile boolean running = true;

    public KafkaConsumerService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, 
                  System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);

        this.consumer = new KafkaConsumer<>(props);
    }

    @Override
    public void run() {
        try {
            consumer.subscribe(Collections.singletonList(TOPIC_NAME));
            
            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                
                for (ConsumerRecord<String, String> record : records) {
                    System.out.println("Received message: " + record.value() + 
                                     " from topic " + record.topic() + 
                                     " partition " + record.partition() + 
                                     " offset " + record.offset());
                    
                    try {
                        dbManager.insertMessage(record.value());
                    } catch (SQLException e) {
                        System.err.println("Failed to insert message to database: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Consumer error: " + e.getMessage());
        } finally {
            consumer.close();
        }
    }

    public void stop() {
        running = false;
    }
}