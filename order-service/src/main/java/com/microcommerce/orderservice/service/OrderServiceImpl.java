package com.microcommerce.orderservice.service;

import com.microcommerce.orderservice.model.Order;
import com.microcommerce.orderservice.model.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.microcommerce.orderservice.model.ProductDTO;
import com.microcommerce.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
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

        String credentials = "admin:password";
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        String authHeader = "Basic " + encodedCredentials;

        // Validate customer existence
        try {
            webClientBuilder.build().get()
                    .uri(customerServiceUrl + "/{customerId}", orderRequest.getCustomerId())
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // block() for simplicity in this demo
        } catch (WebClientResponseException.NotFound e) {
            throw new IllegalArgumentException("Customer with id " + orderRequest.getCustomerId() + " not found.");
        } catch (WebClientResponseException e) {
            log.error("Error validating customer: {}, status code: {}", e.getResponseBodyAsString(), e.getStatusCode());
            throw new IllegalStateException("Error during customer validation: " + e.getMessage());
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
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
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
            } catch (WebClientResponseException e) {
                log.error("Error validating product: {}, status code: {}", e.getResponseBodyAsString(), e.getStatusCode());
                throw new IllegalStateException("Error during product validation: " + e.getMessage());
            }
        }

        newOrder.setOrderItems(newOrderItems);

        return orderRepository.save(newOrder);
    }
}
