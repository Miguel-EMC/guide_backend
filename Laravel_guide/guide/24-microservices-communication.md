# 24 - Microservices Communication

Microservices communicate through synchronous APIs and asynchronous events. Choosing the right approach is key for reliability and performance.

## Goals

- Choose the right communication style
- Handle failures and retries
- Keep contracts stable and versioned

## 1. Synchronous HTTP (REST)

Use REST for queries that require immediate responses.

Best practices:

- Set timeouts on every request
- Use retries with exponential backoff
- Implement circuit breakers for unstable services
- Return correlation IDs for tracing

## 2. gRPC (Optional)

Use gRPC for low‑latency internal services and strong contracts.

- Protobuf schemas define contracts
- Efficient binary transport

## 3. Asynchronous Events

Use queues or event streams for decoupled workflows.

- Publish `UserRegistered` and let other services react
- Accept eventual consistency
- Use outbox pattern for reliable delivery

## 4. Idempotency

Design endpoints and event handlers to be safe on retries.

- Accept an `Idempotency-Key` header
- Store processed keys with TTL

## 5. Versioning and Contracts

- Version APIs when you make breaking changes
- Version events (e.g., `order.created.v2`)
- Maintain a deprecation policy

## 6. Observability Across Services

- Propagate `X-Request-ID`
- Use distributed tracing
- Centralize logs and metrics

## Tips

- Prefer async messaging for side effects.
- Keep payloads small and schema‑validated.
- Avoid shared databases across services.

---

[Previous: Lumen and Octane](./23-lumen-and-octane.md) | [Back to Index](./README.md) | [Next: Caching Strategies ->](./25-caching-strategies.md)
