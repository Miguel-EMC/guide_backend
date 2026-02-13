package com.example.billing.web.dto;

public record ChargeRequest(Long orderId, double amount) {
}
