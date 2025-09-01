package com.example;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;


import java.sql.SQLException;
import java.util.Properties;
import java.time.Duration;
import java.util.Collections;


public class KafkaConsumerService implements Runnable {
    private final KafkaConsumer<String, String> consumer;
    private static final String TOPIC_NAME = "names-topic";
    private static final String GROUP_ID = "names-consumer-group-v2";
    private final DatabaseManager dbManager;
    private volatile boolean running = true;

    public KafkaConsumerService(DatabaseManager dbManager) {
        this.dbManager = dbManager;

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv("KAFKA_BOOTSTRAP_SERVERS"));
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
            this.consumer.subscribe(Collections.singletonList(TOPIC_NAME));

            while (running) {
                System.out.println("Consuming messages...");
                ConsumerRecords<String, String> records = this.consumer.poll(Duration.ofMillis(1000));
                System.out.println("Number of records: " + records.count());
                for (ConsumerRecord<String, String> record : records) {
                    try {
                        System.out.println("Consumed message: " + record.value());
                        this.dbManager.insertMessage(record.value());
                    } catch (SQLException e) {  
                        System.err.println("Failed to insert message: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to consume messages: " + e.getMessage());
        } finally {
            this.consumer.close();
        }
    }

    public void stop() {
        this.running = false;
    }
}
