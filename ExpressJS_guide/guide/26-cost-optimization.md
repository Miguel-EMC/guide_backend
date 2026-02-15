# 26 - Cost Optimization

Scaling is also about cost efficiency and predictable spend.

## Goals

- Reduce idle capacity
- Keep performance within budgets
- Avoid runaway infrastructure costs

## 1. Autoscaling

- Scale API instances based on CPU and request rate.
- Scale workers based on queue depth.
- Prefer horizontal scaling for stateless services.

## 2. Capacity Planning

- Track peak traffic per hour and per day.
- Estimate resource usage per request.
- Reserve baseline capacity and autoscale the rest.

## 3. Cost per Request

Track cost per request as a leading indicator.

- Infra cost / number of requests
- Break down by service and environment

## 4. Database Cost Controls

- Use read replicas only when they deliver clear wins.
- Right-size storage and IOPS.
- Add query optimization before scaling hardware.

## 5. Cache Efficiency

- Cache only hot data with clear TTLs.
- Avoid large values that waste memory.
- Use cache hit ratio as a KPI.

## 6. Performance Budgets

Example budgets:

- p95 latency under 300ms
- Error rate under 0.1%
- CPU utilization under 70%

## 7. Cost Controls

- Tag resources by service and environment.
- Set budget alerts for anomalies.
- Review monthly cost reports.

## 8. Capacity Buffers

Keep a small buffer (for example 10-20%) above expected peak to avoid sudden saturation.

## Tips

- Right-size databases and cache nodes regularly.
- Use serverless for bursty workloads.
- Remove unused resources quickly.

---

[Previous: High-Scale Architecture](./25-high-scale-architecture.md) | [Back to Index](./README.md) | [Next: Datastores and Caching ->](./27-datastores-and-caching.md)
