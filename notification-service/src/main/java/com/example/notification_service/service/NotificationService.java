package com.example.notification_service.service;

import com.example.notification_service.dto.NotificationRequest;

public interface NotificationService {
    void send(NotificationRequest request);
}
