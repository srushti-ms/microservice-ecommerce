package com.example.notification_service.service;

import com.example.notification_service.dto.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    @Override
    public void send(NotificationRequest request) {
        System.out.println("========================================");
        System.out.println("NOTIFICATION SERVICE - Sending notification");
        System.out.println("Order ID : " + request.getOrderId());
        System.out.println("User ID  : " + request.getUserId());
        System.out.println("Message  : " + request.getMessage());
        System.out.println("Notification sent successfully (simulated)");
        System.out.println("========================================");
    }
}
