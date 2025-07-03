package com.microcommerce.orderservice.service;

import com.microcommerce.orderservice.model.Order;
import com.microcommerce.orderservice.model.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.microcommerce.orderservice.model.ProductDTO;
import com.microcommerce.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${customer.service.url}")
    private String customerServiceUrl;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    @Override
    @Transactional
    public Order createOrder(Order orderRequest) {
        log.info("Attempting to create an order...");
        // Validate customer existence
        try {
            webClientBuilder.build().get()
                .uri(customerServiceUrl + "/{customerId}", orderRequest.getCustomerId())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // block() for simplicity in this demo
        } catch (WebClientResponseException.NotFound e) {
            throw new IllegalArgumentException("Customer with id " + orderRequest.getCustomerId() + " not found.");
        }

        // Validate product existence for each order item
        Order newOrder = new Order();
        newOrder.setCustomerId(orderRequest.getCustomerId());
        newOrder.setOrderDate(LocalDate.now());
        newOrder.setStatus("CREATED");

        List<OrderItem> newOrderItems = new ArrayList<>();
        for (OrderItem requestedItem : orderRequest.getOrderItems()) {
            try {
                ProductDTO product = webClientBuilder.build().get()
                    .uri(productServiceUrl + "/{productId}", requestedItem.getProductId())
                    .retrieve()
                    .bodyToMono(ProductDTO.class)
                    .block();

                if (product != null) {
                    OrderItem newItem = new OrderItem();
                    newItem.setProductId(requestedItem.getProductId());
                    newItem.setQuantity(requestedItem.getQuantity());
                    newItem.setPrice(product.getPrice());
                    newItem.setOrder(newOrder);
                    newOrderItems.add(newItem);
                } else {
                    throw new IllegalArgumentException("Product with id " + requestedItem.getProductId() + " not found or returned null.");
                }
            } catch (WebClientResponseException.NotFound e) {
                throw new IllegalArgumentException("Product with id " + requestedItem.getProductId() + " not found.");
            }
        }

        newOrder.setOrderItems(newOrderItems);

        return orderRepository.save(newOrder);
    }
}
