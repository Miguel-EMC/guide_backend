# Microservices Example (Spring Boot 4)

This example demonstrates a small microservices system with an API gateway and three services.

## Services
- `gateway` (Spring Cloud Gateway)
- `orders-service`
- `billing-service`
- `inventory-service`

## Run Locally (Docker Compose)
```bash
docker compose up --build
```

## Example Requests
```bash
curl http://localhost:8080/orders
curl http://localhost:8080/orders/1
curl http://localhost:8080/inventory/sku-123
curl -X POST http://localhost:8080/billing/charge -H "Content-Type: application/json" -d '{"orderId":1,"amount":25.0}'
```

## Ports
- Gateway: `8080`
- Orders: `8081`
- Billing: `8082`
- Inventory: `8083`

## Notes
- This is a minimal skeleton intended for learning and extension.
- Replace the in-memory logic with real persistence when needed.
