# Observability: Logs, Metrics, Traces

This chapter covers the three pillars of observability for Django + DRF: structured logs, metrics with Prometheus, and distributed tracing with OpenTelemetry.

## Observability Pillars

| Pillar | Goal | Questions Answered |
|--------|------|-------------------|
| Logs | Explain what happened | Why did this request fail? |
| Metrics | Measure system health | How many requests/second? What's the p99 latency? |
| Traces | Follow requests across services | Where did this request spend time? |

## Request IDs and Correlation

Attach a request ID to every request for end-to-end correlation.

```python
# core/middleware.py
import uuid
from contextvars import ContextVar

request_id_var: ContextVar[str] = ContextVar("request_id", default="")


class RequestIdMiddleware:
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        request_id = request.headers.get("X-Request-ID") or str(uuid.uuid4())
        request.request_id = request_id
        request_id_var.set(request_id)

        response = self.get_response(request)
        response["X-Request-ID"] = request_id

        return response
```

## Structured Logging

JSON logs with context for log aggregation.

```python
# config/settings.py
LOGGING = {
    "version": 1,
    "disable_existing_loggers": False,
    "formatters": {
        "json": {
            "()": "pythonjsonlogger.jsonlogger.JsonFormatter",
            "format": "%(asctime)s %(levelname)s %(name)s %(request_id)s %(message)s",
        },
    },
    "filters": {
        "request_id": {"()": "core.logging.RequestIdFilter"},
    },
    "handlers": {
        "console": {
            "class": "logging.StreamHandler",
            "formatter": "json",
            "filters": ["request_id"],
        },
    },
    "root": {"handlers": ["console"], "level": "INFO"},
}
```

## Metrics with Prometheus

### Install django-prometheus

```bash
uv add django-prometheus
```

### Configure

```python
# config/settings.py
INSTALLED_APPS = [
    "django_prometheus",
    # ...
]

MIDDLEWARE = [
    "django_prometheus.middleware.PrometheusBeforeMiddleware",
    # ... other middleware
    "django_prometheus.middleware.PrometheusAfterMiddleware",
]

# Database metrics
DATABASES = {
    "default": {
        "ENGINE": "django_prometheus.db.backends.postgresql",
        # ...
    }
}

# Cache metrics
CACHES = {
    "default": {
        "BACKEND": "django_prometheus.cache.backends.redis.RedisCache",
        "LOCATION": "redis://localhost:6379/0",
    }
}
```

```python
# config/urls.py
urlpatterns = [
    path("", include("django_prometheus.urls")),
    # ...
]
```

### Default Metrics

django-prometheus automatically exports:

- `django_http_requests_total` - Total requests by method, view, status
- `django_http_request_duration_seconds` - Request latency histogram
- `django_db_execute_total` - Database query counts
- `django_cache_get_total` - Cache operations

### Custom Metrics

```python
# core/metrics.py
from prometheus_client import Counter, Histogram, Gauge

# Counters
user_signups = Counter(
    "app_user_signups_total",
    "Total user signups",
    ["source"]
)

# Histograms
payment_duration = Histogram(
    "app_payment_duration_seconds",
    "Time spent processing payments",
    buckets=[0.1, 0.5, 1.0, 2.0, 5.0]
)

# Gauges
active_connections = Gauge(
    "app_active_connections",
    "Number of active WebSocket connections"
)
```

```python
# core/views.py
from core.metrics import user_signups, payment_duration

class SignupView(APIView):
    def post(self, request):
        # Track signup source
        user_signups.labels(source=request.data.get("source", "web")).inc()
        # ...

@payment_duration.time()
def process_payment(payment_id):
    # Function is automatically timed
    pass
```

### Metrics Best Practices

| Practice | Description |
|----------|-------------|
| Use labels sparingly | High cardinality labels cause memory issues |
| Track the 4 golden signals | Latency, Traffic, Errors, Saturation |
| Use histograms for latency | Enable percentile calculations |
| Name metrics clearly | `app_<domain>_<metric>_<unit>` |

## Distributed Tracing with OpenTelemetry

### Install OpenTelemetry

```bash
uv add opentelemetry-api opentelemetry-sdk opentelemetry-instrumentation-django opentelemetry-exporter-otlp
```

### Configure Tracing

```python
# config/tracing.py
from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.sdk.resources import Resource, SERVICE_NAME
from opentelemetry.instrumentation.django import DjangoInstrumentor
from opentelemetry.instrumentation.requests import RequestsInstrumentor
from opentelemetry.instrumentation.psycopg2 import Psycopg2Instrumentor

def configure_tracing(service_name: str, otlp_endpoint: str):
    resource = Resource(attributes={
        SERVICE_NAME: service_name,
    })

    provider = TracerProvider(resource=resource)

    # Export traces to OTLP collector
    otlp_exporter = OTLPSpanExporter(endpoint=otlp_endpoint, insecure=True)
    provider.add_span_processor(BatchSpanProcessor(otlp_exporter))

    trace.set_tracer_provider(provider)

    # Auto-instrument Django
    DjangoInstrumentor().instrument()

    # Auto-instrument HTTP requests
    RequestsInstrumentor().instrument()

    # Auto-instrument PostgreSQL
    Psycopg2Instrumentor().instrument()
```

```python
# config/settings.py
import os
from config.tracing import configure_tracing

if not DEBUG:
    configure_tracing(
        service_name="doctor-api",
        otlp_endpoint=os.environ.get("OTLP_ENDPOINT", "localhost:4317"),
    )
```

### Manual Spans

Add custom spans for critical operations:

```python
from opentelemetry import trace

tracer = trace.get_tracer(__name__)


async def fetch_patient_data(patient_id: int):
    with tracer.start_as_current_span("fetch_patient_data") as span:
        span.set_attribute("patient.id", patient_id)

        patient = await Patient.objects.aget(id=patient_id)

        span.set_attribute("patient.found", True)
        return patient


def process_appointment(appointment_id: int):
    with tracer.start_as_current_span("process_appointment") as span:
        span.set_attribute("appointment.id", appointment_id)

        # Child span for notification
        with tracer.start_as_current_span("send_notification"):
            send_email_notification(appointment_id)

        # Child span for calendar sync
        with tracer.start_as_current_span("sync_calendar"):
            sync_to_google_calendar(appointment_id)
```

### Zero-Code Instrumentation

Run with automatic instrumentation:

```bash
opentelemetry-instrument \
    --traces_exporter otlp \
    --metrics_exporter otlp \
    --service_name doctor-api \
    --exporter_otlp_endpoint http://localhost:4317 \
    uv run gunicorn config.wsgi:application
```

### Span Attributes

Add context to spans:

```python
from opentelemetry import trace

span = trace.get_current_span()
span.set_attribute("user.id", request.user.id)
span.set_attribute("http.route", "/api/appointments/")
span.add_event("validation_complete", {"fields": 5})
```

## Health Checks

### Liveness and Readiness

```python
# core/views.py
from django.http import JsonResponse
from django.db import connection
from django.core.cache import cache


def healthz(request):
    """Liveness probe - is the app running?"""
    return JsonResponse({"status": "ok"})


def readyz(request):
    """Readiness probe - can the app serve traffic?"""
    checks = {}

    # Database check
    try:
        with connection.cursor() as cursor:
            cursor.execute("SELECT 1")
        checks["database"] = "ok"
    except Exception as e:
        checks["database"] = str(e)

    # Cache check
    try:
        cache.set("health_check", "ok", timeout=1)
        if cache.get("health_check") == "ok":
            checks["cache"] = "ok"
        else:
            checks["cache"] = "failed"
    except Exception as e:
        checks["cache"] = str(e)

    all_ok = all(v == "ok" for v in checks.values())
    status_code = 200 if all_ok else 503

    return JsonResponse(
        {"status": "ready" if all_ok else "not ready", "checks": checks},
        status=status_code,
    )
```

```python
# config/urls.py
from core.views import healthz, readyz

urlpatterns = [
    path("healthz/", healthz),
    path("readyz/", readyz),
    # ...
]
```

## Alerting Rules

Example Prometheus alerting rules:

```yaml
# alerts.yml
groups:
  - name: django
    rules:
      - alert: HighErrorRate
        expr: |
          sum(rate(django_http_requests_total{status=~"5.."}[5m]))
          / sum(rate(django_http_requests_total[5m])) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is above 5% for 5 minutes"

      - alert: HighLatency
        expr: |
          histogram_quantile(0.95,
            sum(rate(django_http_request_duration_seconds_bucket[5m])) by (le)
          ) > 1.0
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High latency detected"
          description: "p95 latency is above 1 second"

      - alert: DatabaseConnectionsExhausted
        expr: django_db_connections_usage_ratio > 0.9
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Database connection pool nearly exhausted"
```

## Grafana Dashboards

Key panels to include:

| Panel | Metric | Description |
|-------|--------|-------------|
| Request Rate | `rate(django_http_requests_total[5m])` | Requests per second |
| Error Rate | `sum(rate(django_http_requests_total{status=~"5.."}[5m]))` | 5xx errors |
| Latency p50/p95/p99 | `histogram_quantile(0.95, ...)` | Response time percentiles |
| DB Query Time | `rate(django_db_execute_total[5m])` | Database performance |
| Cache Hit Rate | `rate(django_cache_hits_total[5m])` | Cache effectiveness |

## Complete Observability Stack

### docker-compose.yml

```yaml
version: "3.9"

services:
  app:
    build: .
    environment:
      - OTLP_ENDPOINT=otel-collector:4317
    depends_on:
      - otel-collector

  otel-collector:
    image: otel/opentelemetry-collector:latest
    command: ["--config=/etc/otel-collector-config.yaml"]
    volumes:
      - ./otel-collector-config.yaml:/etc/otel-collector-config.yaml
    ports:
      - "4317:4317"  # OTLP gRPC
      - "4318:4318"  # OTLP HTTP

  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    ports:
      - "9090:9090"

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin

  jaeger:
    image: jaegertracing/all-in-one:latest
    ports:
      - "16686:16686"  # UI
      - "14250:14250"  # gRPC
```

### otel-collector-config.yaml

```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

processors:
  batch:

exporters:
  prometheus:
    endpoint: "0.0.0.0:8889"
  jaeger:
    endpoint: jaeger:14250
    tls:
      insecure: true

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [jaeger]
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [prometheus]
```

## Best Practices

1. **Correlate across pillars**: Use request IDs in logs and trace IDs in spans
2. **Sample traces**: In high-traffic services, sample 1-10% of traces
3. **Use meaningful span names**: `db.query.users` not `span1`
4. **Keep health checks fast**: No complex logic in `/healthz`
5. **Alert on symptoms**: High error rate, not "database slow"
6. **Track the 4 golden signals**: Latency, Traffic, Errors, Saturation

## References

- [OpenTelemetry Python](https://opentelemetry.io/docs/languages/python/)
- [django-prometheus](https://github.com/korfuri/django-prometheus)
- [Prometheus Best Practices](https://prometheus.io/docs/practices/naming/)
- [Grafana Django Dashboard](https://grafana.com/grafana/dashboards/)

## Next Steps

- [Performance](./29-performance.md)
- [CI/CD](./30-ci-cd.md)

---

[Previous: Architecture](./27-architecture-diagrams.md) | [Back to Index](./README.md) | [Next: Performance](./29-performance.md)
