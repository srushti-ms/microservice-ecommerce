package com.example.order_service.client;

import com.example.order_service.exception.BadRequestException;
import com.example.order_service.exception.ExternalServiceException;
import com.example.order_service.exception.ResourceNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
public class ExternalServiceClient {

    private final RestClient userClient;
    private final RestClient productClient;

    public ExternalServiceClient(
            @Value("${user.service.url}") String userServiceUrl,
            @Value("${product.service.url}") String productServiceUrl) {
        this.userClient = RestClient.builder().baseUrl(userServiceUrl).build();
        this.productClient = RestClient.builder().baseUrl(productServiceUrl).build();
    }

    public UserResponse getUser(Long userId) {
        try {
            return userClient.get()
                    .uri("/{id}", userId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        if (res.getStatusCode().value() == 404) {
                            throw new ResourceNotFoundException("User not found with id: " + userId);
                        }
                        throw new BadRequestException("Invalid request to User Service");
                    })
                    .body(UserResponse.class);
        } catch (ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException("Failed to communicate with User Service: " + e.getMessage());
        }
    }

    public ProductResponse getProduct(Long productId) {
        try {
            return productClient.get()
                    .uri("/{id}", productId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        if (res.getStatusCode().value() == 404) {
                            throw new ResourceNotFoundException("Product not found with id: " + productId);
                        }
                        throw new BadRequestException("Invalid request to Product Service");
                    })
                    .body(ProductResponse.class);
        } catch (ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException("Failed to communicate with Product Service: " + e.getMessage());
        }
    }

    public void deductProductQuantity(Long productId, Integer quantity) {
        ProductResponse product = getProduct(productId);
        int updatedQuantity = product.getQuantity() - quantity;

        if (updatedQuantity < 0) {
            throw new BadRequestException("Insufficient stock for product: " + product.getName());
        }

        ProductUpdateRequest updateRequest = ProductUpdateRequest.builder()
                .name(product.getName())
                .price(product.getPrice())
                .quantity(updatedQuantity)
                .build();

        try {
            productClient.put()
                    .uri("/{id}", productId)
                    .body(updateRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new BadRequestException("Failed to update stock for product id: " + productId);
                    })
                    .toBodilessEntity();
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException("Failed to update product stock: " + e.getMessage());
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserResponse {
        private Long id;
        private String name;
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductResponse {
        private Long id;
        private String name;
        private BigDecimal price;
        private Integer quantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductUpdateRequest {
        private String name;
        private BigDecimal price;
        private Integer quantity;
    }
}
