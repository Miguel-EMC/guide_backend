package com.example.billing.web;

import com.example.billing.service.BillingService;
import com.example.billing.web.dto.ChargeRequest;
import com.example.billing.web.dto.ChargeResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/charge")
    public ChargeResponse charge(@RequestBody ChargeRequest request) {
        return billingService.charge(request);
    }
}
