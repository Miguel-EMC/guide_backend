package com.example.billing.service;

import com.example.billing.web.dto.ChargeRequest;
import com.example.billing.web.dto.ChargeResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BillingService {

    public ChargeResponse charge(ChargeRequest request) {
        return new ChargeResponse("APPROVED", UUID.randomUUID().toString());
    }
}
