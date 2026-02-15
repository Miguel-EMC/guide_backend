# 25 - High-Scale Architecture

This chapter covers patterns for high traffic, high reliability systems.

## Goals

- Reduce coupling between services
- Scale reads and writes independently
- Increase system resilience

## 1. Start with a Modular Monolith

Split by domain boundaries before you split by services.

- Clear modules for billing, users, content
- Internal APIs between modules
- Shared libraries only when necessary

## 2. Event-Driven Architecture

Use events to decouple services.

```
API -> DB write -> publish event -> consumers
```

Design events as public contracts and keep them versioned.

## 3. CQRS

Separate command and query responsibilities when read and write workloads differ.

- Commands update state
- Queries read optimized views
- Materialized views power fast reads

## 4. Outbox Pattern

Guarantee event delivery by writing to an outbox in the same transaction.

```
Transaction
  -> update domain data
  -> insert outbox record
Worker publishes outbox events
```

Example outbox record:

```sql
INSERT INTO outbox (event_type, payload, created_at)
VALUES ('post.created', '{"id": 1}', NOW());
```

## 5. Sharding and Partitioning

Partition data by tenant, region, or hash key.

- Keep shard keys stable
- Avoid cross-shard joins
- Prefer data locality for high traffic tenants

## 6. Read Replicas and Caches

- Use read replicas for heavy read traffic.
- Cache hot reads in Redis.
- Add request coalescing to avoid stampedes.

## 7. Service Boundaries

- Define clear ownership of data.
- Avoid shared databases across services.
- Document API and event contracts.

## Tips

- Start simple and evolve when metrics demand it.
- Favor idempotent handlers for at-least-once events.
- Treat contracts as public APIs.

---

[Previous: SRE and Operations](./24-sre-operations.md) | [Back to Index](./README.md) | [Next: Cost Optimization ->](./26-cost-optimization.md)
