package com.example.order_service.service;

import com.example.order_service.dto.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(OrderCreatedEvent event) {
        try {
            kafkaTemplate.send("order-created", event).get();
            System.out.println("MESSAGE SENT");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
