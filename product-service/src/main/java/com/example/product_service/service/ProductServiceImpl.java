package com.example.product_service.service;

import com.example.product_service.dto.ProductRequestDto;
import com.example.product_service.dto.ProductResponseDto;
import com.example.product_service.exception.ResourceNotFoundException;
import com.example.product_service.model.Product;
import com.example.product_service.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private static final String PRODUCTS_CACHE = "products";

    private final ProductRepository productRepository;

    @Autowired
    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @CacheEvict(value = PRODUCTS_CACHE, allEntries = true)
    public ProductResponseDto createProduct(ProductRequestDto requestDto) {
        Product product = Product.builder()
                .name(requestDto.getName())
                .price(requestDto.getPrice())
                .quantity(requestDto.getQuantity())
                .build();
        return toResponse(productRepository.save(product));
    }

    @Override
    @Cacheable(value = PRODUCTS_CACHE, key = "#id")
    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(Long id) {
        Product product = findProduct(id);
        return toResponse(product);
    }

    @Override
    @Cacheable(value = PRODUCTS_CACHE, key = "'all'")
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Caching(
            put = @CachePut(value = PRODUCTS_CACHE, key = "#id"),
            evict = @CacheEvict(value = PRODUCTS_CACHE, key = "'all'")
    )
    public ProductResponseDto updateProduct(Long id, ProductRequestDto requestDto) {
        Product product = findProduct(id);
        product.setName(requestDto.getName());
        product.setPrice(requestDto.getPrice());
        product.setQuantity(requestDto.getQuantity());
        return toResponse(productRepository.save(product));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = PRODUCTS_CACHE, key = "#id"),
            @CacheEvict(value = PRODUCTS_CACHE, key = "'all'")
    })
    public void deleteProduct(Long id) {
        Product product = findProduct(id);
        productRepository.delete(product);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private ProductResponseDto toResponse(Product product) {
        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .build();
    }
}
