package com.example.orders.service;

import com.example.orders.web.dto.OrderDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    public List<OrderDto> findAll() {
        return List.of(
            new OrderDto(1L, "PAID"),
            new OrderDto(2L, "SHIPPED")
        );
    }

    public OrderDto findById(Long id) {
        return new OrderDto(id, "PAID");
    }
}
