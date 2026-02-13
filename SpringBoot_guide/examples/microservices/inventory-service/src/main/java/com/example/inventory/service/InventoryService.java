package com.example.inventory.service;

import com.example.inventory.web.dto.InventoryDto;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {

    public InventoryDto check(String sku) {
        return new InventoryDto(sku, 42);
    }
}
