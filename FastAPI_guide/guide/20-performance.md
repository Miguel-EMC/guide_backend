# Performance and Profiling

This chapter focuses on practical ways to measure and improve FastAPI performance in production.

## Performance Goals

Define clear targets before optimizing:

- p95 latency under X ms
- Error rate under Y percent
- Throughput at Z RPS

## Measure First

- Add request timing middleware
- Track p95 and p99 latencies
- Capture slow query logs

## Async Pitfalls

Avoid blocking the event loop in async routes. Use threads for sync code.

```python
import asyncio


@app.get("/reports")
async def reports():
    result = await asyncio.to_thread(build_report_sync)
    return {"result": result}
```

## Database Efficiency

- Use connection pooling and proper pool sizes
- Avoid N+1 queries with eager loading
- Add indexes for common filters

## Caching Strategy

Cache read-heavy endpoints and expensive computations.

```python
from cachetools import TTLCache

cache = TTLCache(maxsize=1024, ttl=300)


@app.get("/config")
async def config():
    if "config" not in cache:
        cache["config"] = await load_config_from_db()
    return cache["config"]
```

## Payload Size

Reduce response sizes:

- Use pagination for list endpoints
- Exclude unused fields in response models
- Enable compression at the gateway

## Concurrency and Workers

Use multiple workers for CPU-bound workloads and to utilize multiple cores.

```bash
gunicorn app.main:app -k uvicorn.workers.UvicornWorker -w 4 -b 0.0.0.0:8000
```

## Profiling

When latency spikes, profile the app:

- `cProfile` for function-level profiling
- `py-spy` for sampling without restart
- SQLAlchemy echo logs for slow queries

## Load Testing

Validate performance under expected traffic.

- Use realistic datasets
- Ramp up gradually
- Monitor CPU, memory, and DB saturation

## Best Practices

- Measure before optimizing
- Fix database bottlenecks first
- Avoid unnecessary serialization work
- Cache aggressively for hot paths

## Next Steps

- [Background Jobs](./21-background-jobs.md) - Offload long tasks
- [WebSockets](./22-websockets.md) - Realtime delivery

---

[Previous: Observability](./19-observability.md) | [Back to Index](./README.md) | [Next: Background Jobs](./21-background-jobs.md)
