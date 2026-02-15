# Scaling and Cost Optimization

Scaling is not only about performance. It is also about cost efficiency and predictable operations.

## Goals

- Scale safely with autoscaling
- Control infrastructure costs
- Set performance budgets

## Autoscaling

- Use horizontal autoscaling for stateless API nodes.
- Set CPU and memory requests and limits.
- Scale queues and workers independently.

## Database Cost Control

- Add indexes for hot queries.
- Use connection pooling to avoid idle costs.
- Archive cold data when possible.

## Caching Strategy

- Cache expensive queries and computed responses.
- Keep TTLs short for frequently changing data.
- Measure cache hit ratios.

## Performance Budgets

Define targets such as:

- p95 latency under 300ms
- Error rate below 0.1%
- CPU utilization under 70%

## Load Testing

- Test with realistic traffic patterns.
- Include cold start and surge scenarios.
- Validate autoscaling behavior.

## Cost Monitoring

- Tag infrastructure resources by service and environment.
- Review monthly cost reports and anomalies.
- Set alerts for sudden spikes.

## Tips

- Prefer smaller, more numerous nodes for resilience.
- Reduce over-provisioning by measuring actual usage.
- Use serverless for bursty workloads when appropriate.

---

[Previous: High-Scale Architecture](./27-high-scale-architecture.md) | [Back to Index](./README.md)
