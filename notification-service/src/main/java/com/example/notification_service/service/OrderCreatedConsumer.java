package com.example.notification_service.service;

import com.example.notification_service.controller.NotificationController;
import com.example.notification_service.dto.NotificationRequest;
import com.example.notification_service.dto.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderCreatedConsumer {

    private final NotificationServiceImpl notificationServiceImpl;

    public OrderCreatedConsumer(NotificationServiceImpl notificationServiceImpl){
        this.notificationServiceImpl = notificationServiceImpl;
    }

    @KafkaListener(
            topics = "order-created",
            groupId = "notification-group"
    )
    public void consume(OrderCreatedEvent event) {

        NotificationRequest notification = new NotificationRequest();
        notification.setOrderId(event.orderId());
        notification.setUserId(event.userId());
        notification.setMessage("ORDER CREATED");
        notificationServiceImpl.send(notification);

        log.info(
                "Order created. OrderId={}, UserId={}, ItemCount={}",
                event.orderId(),
                event.userId(),
                event.itemCount()
        );

    }
}
