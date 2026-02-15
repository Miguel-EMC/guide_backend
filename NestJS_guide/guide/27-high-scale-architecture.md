# High-Scale Architecture

Scaling a NestJS system often requires more than vertical scaling. This chapter focuses on architectural patterns for high traffic and high reliability.

## Goals

- Decompose the system safely
- Improve read scalability
- Reduce coupling between services

## Read Scaling

- Use read replicas for heavy read workloads.
- Add caches at the service and edge layers.
- Introduce search engines for complex queries.

## Write Scaling

- Partition data by tenant or region.
- Use queues to offload slow writes.
- Apply idempotency for retried requests.

## Event-Driven Patterns

Use events to decouple services.

```
API -> Write DB -> Publish Event -> Consumers
```

## CQRS (Command Query Responsibility Segregation)

Separate read models from write models when you need independent scaling or specialized query performance.

## Outbox Pattern

If you publish events, consider the outbox pattern to guarantee delivery.

```
Write transaction
  -> update domain data
  -> write outbox record
Async worker reads outbox and publishes
```

## Service Boundaries

- Keep each service focused on a single domain.
- Avoid shared databases across services.
- Define stable APIs and event contracts.

## Tips

- Measure before you split services.
- Start with a modular monolith and extract services when necessary.
- Always define ownership of data and events.

---

[Previous: Security Hardening](./26-security-hardening.md) | [Back to Index](./README.md) | [Next: Scaling and Cost Optimization ->](./28-scaling-cost-optimization.md)
