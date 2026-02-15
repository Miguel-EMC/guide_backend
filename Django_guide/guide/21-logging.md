# Logging

This chapter shows production-ready logging setups for Django + DRF, including structured logging, request correlation, and log aggregation patterns.

## Logging Architecture

| Component | Purpose |
|-----------|---------|
| Logger | Creates log records |
| Handler | Sends logs to destination (console, file, network) |
| Formatter | Formats log output |
| Filter | Controls which records pass through |

## Basic Logging Configuration

```python
# config/settings.py
LOGGING = {
    "version": 1,
    "disable_existing_loggers": False,
    "formatters": {
        "verbose": {
            "format": "{asctime} {levelname} {name} {module} {message}",
            "style": "{",
        },
        "simple": {
            "format": "{levelname} {message}",
            "style": "{",
        },
    },
    "handlers": {
        "console": {
            "class": "logging.StreamHandler",
            "formatter": "verbose",
        },
    },
    "root": {
        "handlers": ["console"],
        "level": "INFO",
    },
    "loggers": {
        "django": {
            "handlers": ["console"],
            "level": "INFO",
            "propagate": False,
        },
        "django.request": {
            "handlers": ["console"],
            "level": "WARNING",
            "propagate": False,
        },
    },
}
```

## Structured JSON Logging

JSON logs are easier to parse, search, and aggregate in log platforms like ELK, Datadog, or CloudWatch.

### Using python-json-logger

```bash
uv add python-json-logger
```

```python
# config/settings.py
LOGGING = {
    "version": 1,
    "disable_existing_loggers": False,
    "formatters": {
        "json": {
            "()": "pythonjsonlogger.jsonlogger.JsonFormatter",
            "format": "%(asctime)s %(levelname)s %(name)s %(module)s %(funcName)s %(lineno)d %(message)s",
        },
    },
    "handlers": {
        "console": {
            "class": "logging.StreamHandler",
            "formatter": "json",
        },
    },
    "root": {
        "handlers": ["console"],
        "level": "INFO",
    },
    "loggers": {
        "django": {
            "handlers": ["console"],
            "level": "INFO",
            "propagate": False,
        },
        "django.db.backends": {
            "handlers": ["console"],
            "level": "WARNING",  # Set to DEBUG to see SQL queries
            "propagate": False,
        },
        "app": {
            "handlers": ["console"],
            "level": "DEBUG",
            "propagate": False,
        },
    },
}
```

Output:

```json
{"asctime": "2024-01-15 10:30:45", "levelname": "INFO", "name": "app.views", "message": "User logged in", "user_id": 123}
```

### Using structlog

structlog provides a more flexible approach with processors.

```bash
uv add structlog
```

```python
# config/logging_config.py
import structlog

structlog.configure(
    processors=[
        structlog.contextvars.merge_contextvars,
        structlog.processors.add_log_level,
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.StackInfoRenderer(),
        structlog.processors.format_exc_info,
        structlog.processors.JSONRenderer(),
    ],
    wrapper_class=structlog.make_filtering_bound_logger(logging.INFO),
    context_class=dict,
    logger_factory=structlog.PrintLoggerFactory(),
    cache_logger_on_first_use=True,
)
```

Usage:

```python
import structlog

logger = structlog.get_logger()

# Simple logging
logger.info("user_created", user_id=123, email="user@example.com")

# With context binding
log = logger.bind(request_id="abc-123")
log.info("processing_request")
log.info("request_complete", status=200)
```

Output:

```json
{"event": "user_created", "user_id": 123, "email": "user@example.com", "level": "info", "timestamp": "2024-01-15T10:30:45.123456Z"}
```

## Request ID Middleware

Attach a unique request ID to every request for correlation across logs.

```python
# core/middleware.py
import uuid
import logging
import threading

_request_id = threading.local()


def get_request_id():
    return getattr(_request_id, "value", None)


class RequestIdMiddleware:
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        # Get or generate request ID
        request_id = request.headers.get("X-Request-ID") or str(uuid.uuid4())
        request.request_id = request_id
        _request_id.value = request_id

        response = self.get_response(request)
        response["X-Request-ID"] = request_id

        return response
```

```python
# config/settings.py
MIDDLEWARE = [
    "core.middleware.RequestIdMiddleware",
    # ... other middleware
]
```

### Request ID Filter

Include request ID in all log records:

```python
# core/logging.py
import logging
from core.middleware import get_request_id


class RequestIdFilter(logging.Filter):
    def filter(self, record):
        record.request_id = get_request_id() or "-"
        return True
```

```python
# config/settings.py
LOGGING = {
    "version": 1,
    "disable_existing_loggers": False,
    "filters": {
        "request_id": {
            "()": "core.logging.RequestIdFilter",
        },
    },
    "formatters": {
        "json": {
            "()": "pythonjsonlogger.jsonlogger.JsonFormatter",
            "format": "%(asctime)s %(levelname)s %(name)s %(request_id)s %(message)s",
        },
    },
    "handlers": {
        "console": {
            "class": "logging.StreamHandler",
            "formatter": "json",
            "filters": ["request_id"],
        },
    },
    "root": {
        "handlers": ["console"],
        "level": "INFO",
    },
}
```

## Request Logging Middleware

Log all incoming requests with timing:

```python
# core/middleware.py
import time
import logging

logger = logging.getLogger(__name__)


class RequestLoggingMiddleware:
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        start_time = time.time()

        response = self.get_response(request)

        duration_ms = (time.time() - start_time) * 1000

        logger.info(
            "request_complete",
            extra={
                "method": request.method,
                "path": request.path,
                "status": response.status_code,
                "duration_ms": round(duration_ms, 2),
                "user_id": getattr(request.user, "id", None),
                "ip": self.get_client_ip(request),
            },
        )

        return response

    def get_client_ip(self, request):
        x_forwarded_for = request.META.get("HTTP_X_FORWARDED_FOR")
        if x_forwarded_for:
            return x_forwarded_for.split(",")[0].strip()
        return request.META.get("REMOTE_ADDR")
```

## Logging in Views and Services

```python
# core/views.py
import logging
from rest_framework.views import APIView
from rest_framework.response import Response

logger = logging.getLogger(__name__)


class UserView(APIView):
    def get(self, request, user_id):
        logger.info("fetching_user", extra={"user_id": user_id})

        try:
            user = User.objects.get(id=user_id)
            logger.info("user_found", extra={"user_id": user_id})
            return Response(UserSerializer(user).data)
        except User.DoesNotExist:
            logger.warning("user_not_found", extra={"user_id": user_id})
            return Response({"error": "Not found"}, status=404)
        except Exception as e:
            logger.exception("error_fetching_user", extra={"user_id": user_id})
            raise
```

## SQL Query Logging

Enable SQL logging for debugging (development only):

```python
# config/settings.py
LOGGING = {
    # ...
    "loggers": {
        "django.db.backends": {
            "handlers": ["console"],
            "level": "DEBUG" if DEBUG else "WARNING",
            "propagate": False,
        },
    },
}
```

## File Logging with Rotation

For systems without log aggregation:

```python
# config/settings.py
LOGGING = {
    "version": 1,
    "disable_existing_loggers": False,
    "formatters": {
        "json": {
            "()": "pythonjsonlogger.jsonlogger.JsonFormatter",
        },
    },
    "handlers": {
        "console": {
            "class": "logging.StreamHandler",
            "formatter": "json",
        },
        "file": {
            "class": "logging.handlers.RotatingFileHandler",
            "filename": "/var/log/app/django.log",
            "maxBytes": 10 * 1024 * 1024,  # 10 MB
            "backupCount": 5,
            "formatter": "json",
        },
    },
    "root": {
        "handlers": ["console", "file"],
        "level": "INFO",
    },
}
```

## Error Tracking with Sentry

```bash
uv add sentry-sdk
```

```python
# config/settings.py
import sentry_sdk
from sentry_sdk.integrations.django import DjangoIntegration

if not DEBUG:
    sentry_sdk.init(
        dsn=os.environ.get("SENTRY_DSN"),
        integrations=[DjangoIntegration()],
        traces_sample_rate=0.1,  # 10% of requests
        send_default_pii=False,  # Don't send PII
        environment=os.environ.get("ENVIRONMENT", "production"),
    )
```

## Log Levels Guide

| Level | When to Use |
|-------|-------------|
| DEBUG | Detailed debugging info (dev only) |
| INFO | Normal operations, request completion |
| WARNING | Unexpected but handled situations |
| ERROR | Errors that need attention |
| CRITICAL | System failures |

## Best Practices

1. **Use structured logging**: JSON logs are searchable and parseable
2. **Include context**: Add user_id, request_id, and relevant data
3. **Use appropriate levels**: Don't log everything as ERROR
4. **Avoid logging secrets**: Never log passwords, tokens, or PII
5. **Keep logs actionable**: Log what helps debugging
6. **Correlate with request IDs**: Trace requests across services

## Example: Complete Production Config

```python
# config/settings.py
import os

LOGGING = {
    "version": 1,
    "disable_existing_loggers": False,
    "filters": {
        "request_id": {"()": "core.logging.RequestIdFilter"},
    },
    "formatters": {
        "json": {
            "()": "pythonjsonlogger.jsonlogger.JsonFormatter",
            "format": "%(asctime)s %(levelname)s %(name)s %(request_id)s %(message)s",
        },
    },
    "handlers": {
        "console": {
            "class": "logging.StreamHandler",
            "formatter": "json",
            "filters": ["request_id"],
        },
    },
    "root": {
        "handlers": ["console"],
        "level": os.environ.get("LOG_LEVEL", "INFO"),
    },
    "loggers": {
        "django": {
            "handlers": ["console"],
            "level": "INFO",
            "propagate": False,
        },
        "django.request": {
            "handlers": ["console"],
            "level": "WARNING",
            "propagate": False,
        },
        "django.db.backends": {
            "handlers": ["console"],
            "level": "WARNING",
            "propagate": False,
        },
        "celery": {
            "handlers": ["console"],
            "level": "INFO",
            "propagate": False,
        },
        "app": {
            "handlers": ["console"],
            "level": "DEBUG",
            "propagate": False,
        },
    },
}
```

## References

- [Django Logging](https://docs.djangoproject.com/en/5.2/topics/logging/)
- [python-json-logger](https://github.com/madzak/python-json-logger)
- [structlog](https://www.structlog.org/)
- [Sentry Django Integration](https://docs.sentry.io/platforms/python/integrations/django/)

## Next Steps

- [Security](./22-security.md)
- [Observability](./28-observability.md)

---

[Previous: Deployment](./20-deployment.md) | [Back to Index](./README.md) | [Next: Security](./22-security.md)
