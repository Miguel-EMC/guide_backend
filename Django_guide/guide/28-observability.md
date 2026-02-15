# Observability

This chapter covers logs, metrics, tracing, and health checks for production.

## Step 1: Structured Logs

Use JSON logs for easier parsing in log platforms.

```python
# config/settings.py
LOGGING = {
    "version": 1,
    "disable_existing_loggers": False,
    "formatters": {
        "json": {
            "()": "pythonjsonlogger.jsonlogger.JsonFormatter",
            "format": "%(asctime)s %(levelname)s %(name)s %(message)s",
        },
    },
    "handlers": {
        "console": {"class": "logging.StreamHandler", "formatter": "json"},
    },
    "root": {"handlers": ["console"], "level": "INFO"},
}
```

## Step 2: Request IDs

```python
# core/middleware.py
import uuid


class RequestIdMiddleware:
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        request_id = request.headers.get("X-Request-ID") or str(uuid.uuid4())
        request.request_id = request_id
        response = self.get_response(request)
        response["X-Request-ID"] = request_id
        return response
```

## Step 3: Metrics

Use a metrics exporter (e.g., Prometheus) to track latency and error rates.

```python
# Example: add a /metrics endpoint with a third-party library
# Keep metrics low-cardinality and aggregate at the client side.
```

## Step 4: Tracing

OpenTelemetry is the standard for distributed tracing. Use it when your app talks to multiple services.

## Step 5: Health Checks

```python
# core/views.py
from django.http import JsonResponse


def healthz(request):
    return JsonResponse({"status": "ok"})
```

## Tips

- Always include request IDs in logs.
- Track p95 and p99 latency.
- Avoid high-cardinality labels in metrics.

## Next Steps

- [Performance](./29-performance.md)
- [CI/CD](./30-ci-cd.md)

---

[Previous: Architecture](./27-architecture-diagrams.md) | [Back to Index](./README.md) | [Next: Performance](./29-performance.md)
