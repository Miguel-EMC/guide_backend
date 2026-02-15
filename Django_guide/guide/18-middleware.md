# Middleware

Middleware processes requests and responses globally. This chapter covers Django middleware patterns, async middleware, and common use cases for DRF APIs.

## Middleware Concepts

### Request/Response Flow

```
Request → M1 → M2 → M3 → View → M3 → M2 → M1 → Response
```

Middleware executes:
- **Request phase**: Top to bottom (before view)
- **Response phase**: Bottom to top (after view)

### Middleware Order Matters

```python
# config/settings.py
MIDDLEWARE = [
    # Security (first)
    "django.middleware.security.SecurityMiddleware",
    "corsheaders.middleware.CorsMiddleware",

    # Session/Auth
    "django.contrib.sessions.middleware.SessionMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware",

    # Messages
    "django.contrib.messages.middleware.MessageMiddleware",
    "django.middleware.clickjacking.XFrameOptionsMiddleware",

    # Custom (last)
    "core.middleware.RequestIdMiddleware",
    "core.middleware.RequestTimingMiddleware",
    "core.middleware.RequestLoggingMiddleware",
]
```

## Class-Based Middleware

### Basic Structure

```python
# core/middleware.py
class SimpleMiddleware:
    def __init__(self, get_response):
        self.get_response = get_response
        # One-time configuration and initialization

    def __call__(self, request):
        # Code executed on each request BEFORE the view
        response = self.get_response(request)
        # Code executed on each response AFTER the view
        return response
```

### Request ID Middleware

```python
# core/middleware.py
import uuid
from contextvars import ContextVar

request_id_var: ContextVar[str] = ContextVar("request_id", default="")


class RequestIdMiddleware:
    """Attach unique request ID to each request."""

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        # Get from header or generate new
        request_id = request.headers.get("X-Request-ID") or str(uuid.uuid4())

        # Store on request object
        request.request_id = request_id

        # Store in context var for logging
        request_id_var.set(request_id)

        response = self.get_response(request)

        # Add to response headers
        response["X-Request-ID"] = request_id

        return response
```

### Request Timing Middleware

```python
# core/middleware.py
import time
import logging

logger = logging.getLogger(__name__)


class RequestTimingMiddleware:
    """Track request processing time."""

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        start_time = time.perf_counter()

        response = self.get_response(request)

        duration_ms = (time.perf_counter() - start_time) * 1000

        # Add timing header
        response["X-Process-Time-Ms"] = f"{duration_ms:.2f}"

        # Log slow requests
        if duration_ms > 1000:
            logger.warning(
                "Slow request",
                extra={
                    "path": request.path,
                    "method": request.method,
                    "duration_ms": duration_ms,
                },
            )

        return response
```

### Request Logging Middleware

```python
# core/middleware.py
import logging
import json

logger = logging.getLogger(__name__)


class RequestLoggingMiddleware:
    """Log all API requests."""

    SENSITIVE_HEADERS = {"authorization", "cookie", "x-api-key"}
    SENSITIVE_FIELDS = {"password", "token", "secret", "api_key"}

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        # Skip health checks
        if request.path in ("/healthz/", "/readyz/"):
            return self.get_response(request)

        response = self.get_response(request)

        self.log_request(request, response)

        return response

    def log_request(self, request, response):
        logger.info(
            "api_request",
            extra={
                "method": request.method,
                "path": request.path,
                "status": response.status_code,
                "user_id": getattr(request.user, "id", None),
                "ip": self.get_client_ip(request),
                "user_agent": request.headers.get("User-Agent", ""),
            },
        )

    def get_client_ip(self, request):
        x_forwarded_for = request.META.get("HTTP_X_FORWARDED_FOR")
        if x_forwarded_for:
            return x_forwarded_for.split(",")[0].strip()
        return request.META.get("REMOTE_ADDR")
```

## Exception Handling Middleware

```python
# core/middleware.py
import logging
import traceback
from django.http import JsonResponse

logger = logging.getLogger(__name__)


class ExceptionHandlingMiddleware:
    """Catch unhandled exceptions and return JSON response."""

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        return self.get_response(request)

    def process_exception(self, request, exception):
        """Called when view raises exception."""
        logger.exception(
            "Unhandled exception",
            extra={
                "path": request.path,
                "method": request.method,
                "exception": str(exception),
            },
        )

        # Return JSON error in production
        from django.conf import settings
        if not settings.DEBUG:
            return JsonResponse(
                {
                    "error": "Internal server error",
                    "request_id": getattr(request, "request_id", None),
                },
                status=500,
            )

        # In DEBUG mode, let Django show the error page
        return None
```

## Async Middleware (Django 4.1+)

```python
# core/middleware.py
import asyncio


class AsyncRequestTimingMiddleware:
    """Async-compatible timing middleware."""

    async_capable = True
    sync_capable = True

    def __init__(self, get_response):
        self.get_response = get_response
        if asyncio.iscoroutinefunction(self.get_response):
            self._is_async = True
        else:
            self._is_async = False

    async def __acall__(self, request):
        import time
        start = time.perf_counter()

        response = await self.get_response(request)

        duration_ms = (time.perf_counter() - start) * 1000
        response["X-Process-Time-Ms"] = f"{duration_ms:.2f}"

        return response

    def __call__(self, request):
        if self._is_async:
            return self.__acall__(request)

        import time
        start = time.perf_counter()

        response = self.get_response(request)

        duration_ms = (time.perf_counter() - start) * 1000
        response["X-Process-Time-Ms"] = f"{duration_ms:.2f}"

        return response
```

## Security Middleware

### Security Headers

```python
# core/middleware.py
class SecurityHeadersMiddleware:
    """Add security headers to all responses."""

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        response = self.get_response(request)

        # Content Security Policy
        response["Content-Security-Policy"] = (
            "default-src 'self'; "
            "script-src 'self'; "
            "style-src 'self' 'unsafe-inline'; "
            "img-src 'self' data: https:; "
            "frame-ancestors 'none';"
        )

        # Prevent MIME type sniffing
        response["X-Content-Type-Options"] = "nosniff"

        # Clickjacking protection
        response["X-Frame-Options"] = "DENY"

        # XSS protection
        response["X-XSS-Protection"] = "1; mode=block"

        # Referrer policy
        response["Referrer-Policy"] = "strict-origin-when-cross-origin"

        # Permissions policy
        response["Permissions-Policy"] = (
            "geolocation=(), microphone=(), camera=()"
        )

        return response
```

### IP Allowlist Middleware

```python
# core/middleware.py
import ipaddress
from django.http import HttpResponseForbidden


class IPAllowlistMiddleware:
    """Restrict access to specific IP ranges."""

    ALLOWED_NETWORKS = [
        "10.0.0.0/8",
        "172.16.0.0/12",
        "192.168.0.0/16",
    ]

    def __init__(self, get_response):
        self.get_response = get_response
        self.allowed_networks = [
            ipaddress.ip_network(net) for net in self.ALLOWED_NETWORKS
        ]

    def __call__(self, request):
        # Only restrict admin paths
        if not request.path.startswith("/admin/"):
            return self.get_response(request)

        client_ip = self.get_client_ip(request)

        if not self.is_allowed(client_ip):
            return HttpResponseForbidden("Access denied")

        return self.get_response(request)

    def get_client_ip(self, request):
        x_forwarded_for = request.META.get("HTTP_X_FORWARDED_FOR")
        if x_forwarded_for:
            return x_forwarded_for.split(",")[0].strip()
        return request.META.get("REMOTE_ADDR")

    def is_allowed(self, ip_string):
        try:
            ip = ipaddress.ip_address(ip_string)
            return any(ip in network for network in self.allowed_networks)
        except ValueError:
            return False
```

## Rate Limiting Middleware

```python
# core/middleware.py
from django.core.cache import cache
from django.http import JsonResponse


class RateLimitMiddleware:
    """Simple rate limiting middleware."""

    RATE_LIMIT = 100  # requests
    WINDOW = 60  # seconds

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        # Skip for authenticated users (handle via DRF throttling)
        if request.user.is_authenticated:
            return self.get_response(request)

        client_ip = self.get_client_ip(request)
        cache_key = f"ratelimit:{client_ip}"

        # Get current count
        request_count = cache.get(cache_key, 0)

        if request_count >= self.RATE_LIMIT:
            return JsonResponse(
                {"error": "Rate limit exceeded"},
                status=429,
                headers={"Retry-After": str(self.WINDOW)},
            )

        # Increment count
        cache.set(cache_key, request_count + 1, timeout=self.WINDOW)

        response = self.get_response(request)
        response["X-RateLimit-Limit"] = str(self.RATE_LIMIT)
        response["X-RateLimit-Remaining"] = str(self.RATE_LIMIT - request_count - 1)

        return response

    def get_client_ip(self, request):
        x_forwarded_for = request.META.get("HTTP_X_FORWARDED_FOR")
        if x_forwarded_for:
            return x_forwarded_for.split(",")[0].strip()
        return request.META.get("REMOTE_ADDR")
```

## Maintenance Mode Middleware

```python
# core/middleware.py
from django.http import JsonResponse
from django.conf import settings


class MaintenanceModeMiddleware:
    """Return 503 when in maintenance mode."""

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        if getattr(settings, "MAINTENANCE_MODE", False):
            # Allow health checks
            if request.path == "/healthz/":
                return self.get_response(request)

            # Allow admin access
            if request.path.startswith("/admin/"):
                return self.get_response(request)

            return JsonResponse(
                {
                    "error": "Service temporarily unavailable",
                    "message": "We are performing maintenance. Please try again later.",
                },
                status=503,
            )

        return self.get_response(request)
```

## JSON Body Parsing Middleware

```python
# core/middleware.py
import json


class JsonBodyMiddleware:
    """Parse JSON body and attach to request."""

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        if request.content_type == "application/json" and request.body:
            try:
                request.json = json.loads(request.body)
            except json.JSONDecodeError:
                request.json = None
        else:
            request.json = None

        return self.get_response(request)
```

## Middleware Hooks

### process_view

Called just before the view is called.

```python
class ViewPreprocessMiddleware:
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        return self.get_response(request)

    def process_view(self, request, view_func, view_args, view_kwargs):
        """Called just before view execution."""
        # Return None to continue, or HttpResponse to short-circuit
        return None
```

### process_template_response

Called after view returns TemplateResponse.

```python
class TemplateResponseMiddleware:
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        return self.get_response(request)

    def process_template_response(self, request, response):
        """Called for TemplateResponse objects."""
        # Modify response.context_data if needed
        return response
```

## Testing Middleware

```python
# tests/test_middleware.py
import pytest
from django.test import RequestFactory
from core.middleware import RequestIdMiddleware, RequestTimingMiddleware


class TestRequestIdMiddleware:
    def test_generates_request_id(self):
        factory = RequestFactory()
        request = factory.get("/")

        def get_response(req):
            from django.http import HttpResponse
            return HttpResponse()

        middleware = RequestIdMiddleware(get_response)
        response = middleware(request)

        assert hasattr(request, "request_id")
        assert "X-Request-ID" in response

    def test_uses_provided_request_id(self):
        factory = RequestFactory()
        request = factory.get("/", HTTP_X_REQUEST_ID="custom-id-123")

        def get_response(req):
            from django.http import HttpResponse
            return HttpResponse()

        middleware = RequestIdMiddleware(get_response)
        response = middleware(request)

        assert request.request_id == "custom-id-123"
        assert response["X-Request-ID"] == "custom-id-123"


class TestRequestTimingMiddleware:
    def test_adds_timing_header(self):
        factory = RequestFactory()
        request = factory.get("/")

        def get_response(req):
            from django.http import HttpResponse
            return HttpResponse()

        middleware = RequestTimingMiddleware(get_response)
        response = middleware(request)

        assert "X-Process-Time-Ms" in response
        assert float(response["X-Process-Time-Ms"]) >= 0
```

## Best Practices

1. **Keep middleware lightweight** - Avoid DB queries, heavy computation
2. **Order matters** - Security first, logging last
3. **Use async when possible** - For I/O-bound operations
4. **Handle exceptions** - Don't let errors crash the request
5. **Log appropriately** - Use structured logging
6. **Test thoroughly** - Middleware affects all requests
7. **Skip when not needed** - Check paths to avoid unnecessary processing

## References

- [Django Middleware](https://docs.djangoproject.com/en/5.2/topics/http/middleware/)
- [Async Middleware](https://docs.djangoproject.com/en/5.2/topics/async/)
- [Custom Middleware](https://docs.djangoproject.com/en/5.2/topics/http/middleware/#writing-your-own-middleware)

## Next Steps

- [Admin Customization](./19-admin-customization.md)
- [Deployment](./20-deployment.md)

---

[Previous: Celery and Tasks](./17-celery-tasks.md) | [Back to Index](./README.md) | [Next: Admin Customization](./19-admin-customization.md)
