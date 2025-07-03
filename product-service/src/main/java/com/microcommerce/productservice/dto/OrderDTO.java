package com.microcommerce.productservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderDTO {
    private Long id;
    private Long customerId;
    private List<OrderItemDTO> orderItems;
}
