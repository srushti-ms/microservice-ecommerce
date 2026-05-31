package com.example.order_service.service;

import com.example.order_service.dto.OrderRequestDto;
import com.example.order_service.dto.OrderResponseDto;
import java.util.List;

public interface OrderService {
    OrderResponseDto placeOrder(OrderRequestDto orderRequest);
    OrderResponseDto getOrderById(Long id);
    List<OrderResponseDto> getOrdersByUserId(Long userId);
    List<OrderResponseDto> getAllOrders();
    OrderResponseDto cancelOrder(Long id);
}
