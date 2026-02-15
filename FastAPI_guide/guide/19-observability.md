# Observability: Logs, Metrics, Traces

This chapter shows how to build observability into a FastAPI service with structured logs, request IDs, and OpenTelemetry tracing.

## Observability Pillars

| Pillar | Goal | Typical Tools |
|--------|------|---------------|
| Logs | Explain what happened | Structured logs, log aggregation |
| Metrics | Measure system health | Prometheus, StatsD |
| Traces | Follow a request across services | OpenTelemetry |

## Request IDs

Attach a request ID to every response and log line.

```python
import uuid
from fastapi import FastAPI, Request

app = FastAPI()


@app.middleware("http")
async def request_id_middleware(request: Request, call_next):
    request_id = request.headers.get("x-request-id") or str(uuid.uuid4())
    response = await call_next(request)
    response.headers["x-request-id"] = request_id
    return response
```

## Structured Logging

Use JSON logs for easier search and correlation.

```python
import structlog

structlog.configure(
    processors=[
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.JSONRenderer(),
    ]
)

logger = structlog.get_logger()
logger.info("request_complete", route="/users", status=200)
```

## OpenTelemetry Tracing

OpenTelemetry provides a vendor-neutral API and SDK for tracing. Use manual spans for critical operations, and add framework instrumentation for automatic spans.

### Manual Spans

```python
from opentelemetry import trace

tracer = trace.get_tracer(__name__)


async def fetch_user(user_id: int):
    with tracer.start_as_current_span("db.fetch_user"):
        return await repo.get_user(user_id)
```

### Auto Instrumentation (Zero-Code)

You can instrument a Python app without changing source code using the OpenTelemetry agent.

```bash
pip install opentelemetry-distro opentelemetry-exporter-otlp
opentelemetry-bootstrap -a install

opentelemetry-instrument \
  --traces_exporter console,otlp \
  --metrics_exporter console \
  --service_name my-fastapi-service \
  --exporter_otlp_endpoint 0.0.0.0:4317 \
  python -m uvicorn app.main:app
```

Configure exporters to ship traces and metrics to your collector or observability backend.

## Metrics

Expose metrics for latency, error rate, and request volume. Track:

- Request duration (p50, p95, p99)
- HTTP status counts
- Database latency
- Queue depth for background jobs

## Health and Readiness

Return minimal, fast health endpoints used by load balancers and monitors.

```python
@app.get("/healthz")
async def healthz():
    return {"status": "ok"}
```

## Best Practices

- Use a consistent request ID across logs and traces.
- Sample traces in high-traffic services to reduce cost.
- Keep health endpoints lightweight and dependency-free.

## References

- [OpenTelemetry Instrumentation for Python](https://opentelemetry.io/docs/languages/python/instrumentation/)
- [OpenTelemetry Exporters](https://opentelemetry.io/docs/languages/python/exporters/)
- [FastAPI Middleware](https://fastapi.tiangolo.com/tutorial/middleware/)

## Next Steps

- [Performance](./20-performance.md) - Profiling and optimization
- [Background Jobs](./21-background-jobs.md) - Async work patterns

---

[Previous: Security Hardening](./18-security-hardening.md) | [Back to Index](./README.md) | [Next: Performance](./20-performance.md)
