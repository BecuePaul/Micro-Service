package com.microcommerce.productservice.listener;

import com.microcommerce.productservice.config.RabbitMQConfig;
import com.microcommerce.productservice.dto.OrderDTO;
import com.microcommerce.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);
    private final ProductService productService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleOrderCreated(OrderDTO orderDTO) {
        try {
            log.info("Received order: {}", orderDTO.getId());

            orderDTO.getOrderItems().forEach(item -> 
                productService.updateStock(item.getProductId(), item.getQuantity()));

            log.info("Stock updated for orderId: {}", orderDTO.getId());
        } catch (Exception e) {
            log.error("Error processing order from RabbitMQ", e);
        }
    }
}
