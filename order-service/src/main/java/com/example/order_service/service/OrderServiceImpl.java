package com.example.order_service.service;

import com.example.order_service.client.ExternalServiceClient;
import com.example.order_service.dto.OrderCreatedEvent;
import com.example.order_service.dto.OrderItemRequestDto;
import com.example.order_service.dto.OrderItemResponseDto;
import com.example.order_service.dto.OrderRequestDto;
import com.example.order_service.dto.OrderResponseDto;
import com.example.order_service.exception.BadRequestException;
import com.example.order_service.exception.ResourceNotFoundException;
import com.example.order_service.model.Order;
import com.example.order_service.model.OrderItem;
import com.example.order_service.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ExternalServiceClient externalServiceClient;
    private final OrderEventProducer producer;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository, ExternalServiceClient externalServiceClient, OrderEventProducer producer) {
        this.orderRepository = orderRepository;
        this.externalServiceClient = externalServiceClient;
        this.producer = producer;
    }

    @Override
    public OrderResponseDto placeOrder(OrderRequestDto orderRequest) {
        externalServiceClient.getUser(orderRequest.getUserId());

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequestDto itemRequest : orderRequest.getItems()) {
            ExternalServiceClient.ProductResponse product =
                    externalServiceClient.getProduct(itemRequest.getProductId());

            if (product.getQuantity() < itemRequest.getQuantity()) {
                throw new BadRequestException(
                        "Insufficient stock for product: " + product.getName()
                                + ". Available: " + product.getQuantity()
                                + ", Requested: " + itemRequest.getQuantity());
            }

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);

            orderItems.add(OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .quantity(itemRequest.getQuantity())
                    .price(product.getPrice())
                    .build());
        }

        Order order = Order.builder()
                .userId(orderRequest.getUserId())
                .totalAmount(totalAmount)
                .status("COMPLETED")
                .build();

        orderItems.forEach(item -> item.setOrder(order));
        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        for (OrderItemRequestDto itemRequest : orderRequest.getItems()) {
            externalServiceClient.deductProductQuantity(itemRequest.getProductId(), itemRequest.getQuantity());
        }

        producer.publish(new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getUserId(),
                savedOrder.getItems().size()
        ));


        return toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long id) {
        return toResponse(findOrder(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponseDto cancelOrder(Long id) {
        Order order = findOrder(id);

        if ("CANCELLED".equals(order.getStatus())) {
            throw new BadRequestException("Order is already cancelled.");
        }

        order.setStatus("CANCELLED");
        return toResponse(orderRepository.save(order));
    }

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    private OrderResponseDto toResponse(Order order) {
        List<OrderItemResponseDto> items = order.getItems().stream()
                .map(item -> OrderItemResponseDto.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderResponseDto.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .items(items)
                .build();
    }
}
