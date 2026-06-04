package com.example.notification_service.dto;

public record OrderCreatedEvent(Long orderId,
                                Long userId,
                                Integer itemCount) {
}
