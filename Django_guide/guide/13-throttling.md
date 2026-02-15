# Throttling (Rate Limiting)

Throttling limits request rates to protect your API from abuse, ensure fair usage, and maintain system stability. This chapter covers DRF's built-in throttling, custom implementations, and distributed rate limiting with Redis.

## Overview

| Throttle Type | Use Case |
|--------------|----------|
| AnonRateThrottle | Limit unauthenticated users |
| UserRateThrottle | Limit authenticated users |
| ScopedRateThrottle | Different limits per endpoint |
| Custom Throttle | Business-specific limits |

## Why Throttling?

- **Prevent abuse** - Stop malicious actors from overwhelming your API
- **Ensure fairness** - Distribute resources among users
- **Cost control** - Limit expensive operations
- **Protect downstream** - Shield databases and external APIs
- **Compliance** - Meet SLA requirements

## Basic Configuration

### Global Throttling

```python
# config/settings.py
REST_FRAMEWORK = {
    "DEFAULT_THROTTLE_CLASSES": [
        "rest_framework.throttling.AnonRateThrottle",
        "rest_framework.throttling.UserRateThrottle",
    ],
    "DEFAULT_THROTTLE_RATES": {
        "anon": "100/hour",
        "user": "1000/hour",
    },
}
```

### Rate Format

Rates are specified as `number/period`:

| Format | Meaning |
|--------|---------|
| `100/second` | 100 requests per second |
| `1000/minute` | 1000 requests per minute |
| `10000/hour` | 10000 requests per hour |
| `100000/day` | 100000 requests per day |

### Per-View Throttling

```python
# doctors/views.py
from rest_framework.throttling import UserRateThrottle, AnonRateThrottle
from rest_framework import generics


class DoctorListView(generics.ListAPIView):
    queryset = Doctor.objects.all()
    serializer_class = DoctorSerializer
    throttle_classes = [UserRateThrottle, AnonRateThrottle]


class DoctorDetailView(generics.RetrieveAPIView):
    queryset = Doctor.objects.all()
    serializer_class = DoctorSerializer
    throttle_classes = []  # No throttling on detail view
```

### Disable Throttling Per View

```python
from rest_framework.views import APIView


class HealthCheckView(APIView):
    throttle_classes = []  # Exempt from throttling

    def get(self, request):
        return Response({"status": "healthy"})
```

## Scoped Rate Throttling

Use scopes to apply different limits to different endpoints.

### Configuration

```python
# config/settings.py
REST_FRAMEWORK = {
    "DEFAULT_THROTTLE_CLASSES": [
        "rest_framework.throttling.ScopedRateThrottle",
    ],
    "DEFAULT_THROTTLE_RATES": {
        "auth": "5/minute",
        "contacts": "100/hour",
        "uploads": "20/hour",
        "search": "30/minute",
        "exports": "10/hour",
    },
}
```

### Usage

```python
# auth/views.py
from rest_framework.throttling import ScopedRateThrottle


class LoginView(generics.GenericAPIView):
    throttle_scope = "auth"
    throttle_classes = [ScopedRateThrottle]

    def post(self, request):
        # Login logic
        pass


class PasswordResetView(generics.GenericAPIView):
    throttle_scope = "auth"
    throttle_classes = [ScopedRateThrottle]

    def post(self, request):
        # Password reset logic
        pass
```

```python
# doctors/views.py
class DoctorSearchView(generics.ListAPIView):
    throttle_scope = "search"
    throttle_classes = [ScopedRateThrottle]

    def get_queryset(self):
        query = self.request.query_params.get("q", "")
        return Doctor.objects.filter(name__icontains=query)


class DoctorExportView(generics.GenericAPIView):
    throttle_scope = "exports"
    throttle_classes = [ScopedRateThrottle]

    def get(self, request):
        # Export to CSV
        pass
```

## Custom Throttles

### Write-Only Throttle

Throttle only write operations (POST, PUT, PATCH, DELETE).

```python
# core/throttling.py
from rest_framework.throttling import SimpleRateThrottle


class WriteRateThrottle(SimpleRateThrottle):
    """Throttle only write operations."""

    scope = "write"

    def get_cache_key(self, request, view):
        # Skip read operations
        if request.method in ("GET", "HEAD", "OPTIONS"):
            return None

        if request.user.is_authenticated:
            return f"write_throttle_{request.user.pk}"
        return f"write_throttle_{self.get_ident(request)}"
```

```python
# config/settings.py
REST_FRAMEWORK = {
    "DEFAULT_THROTTLE_RATES": {
        "write": "100/hour",
    },
}
```

### Burst Rate Throttle

Allow bursts but limit sustained rate.

```python
# core/throttling.py
from rest_framework.throttling import SimpleRateThrottle


class BurstRateThrottle(SimpleRateThrottle):
    """Short-term burst limit."""
    scope = "burst"
    rate = "60/minute"


class SustainedRateThrottle(SimpleRateThrottle):
    """Long-term sustained limit."""
    scope = "sustained"
    rate = "1000/day"
```

```python
# doctors/views.py
class DoctorViewSet(viewsets.ModelViewSet):
    queryset = Doctor.objects.all()
    serializer_class = DoctorSerializer
    throttle_classes = [BurstRateThrottle, SustainedRateThrottle]
```

### Per-User Configurable Throttle

Different limits for different user tiers.

```python
# core/throttling.py
from rest_framework.throttling import UserRateThrottle


class TieredUserThrottle(UserRateThrottle):
    """Apply different rates based on user tier."""

    TIER_RATES = {
        "free": "100/hour",
        "basic": "1000/hour",
        "premium": "10000/hour",
        "enterprise": None,  # Unlimited
    }

    def get_rate(self):
        if not self.request.user.is_authenticated:
            return "50/hour"  # Anonymous rate

        tier = getattr(self.request.user, "tier", "free")
        rate = self.TIER_RATES.get(tier, "100/hour")

        return rate

    def allow_request(self, request, view):
        self.request = request

        # Get user's tier rate
        rate = self.get_rate()

        if rate is None:
            return True  # Unlimited for enterprise

        # Parse rate
        self.rate = rate
        self.num_requests, self.duration = self.parse_rate(rate)

        return super().allow_request(request, view)
```

### IP-Based Throttle

```python
# core/throttling.py
from rest_framework.throttling import SimpleRateThrottle


class IPRateThrottle(SimpleRateThrottle):
    """Throttle by IP address regardless of authentication."""

    scope = "ip"

    def get_cache_key(self, request, view):
        return f"ip_throttle_{self.get_ident(request)}"

    def get_ident(self, request):
        """Get client IP, considering proxies."""
        xff = request.META.get("HTTP_X_FORWARDED_FOR")
        if xff:
            return xff.split(",")[0].strip()
        return request.META.get("REMOTE_ADDR", "")
```

### Endpoint-Specific Throttle

```python
# core/throttling.py
from rest_framework.throttling import SimpleRateThrottle


class EndpointRateThrottle(SimpleRateThrottle):
    """Throttle per endpoint."""

    def get_cache_key(self, request, view):
        if request.user.is_authenticated:
            ident = request.user.pk
        else:
            ident = self.get_ident(request)

        # Include view name in cache key
        view_name = view.__class__.__name__
        return f"endpoint_{view_name}_{ident}"

    def get_rate(self):
        """Get rate from view or default."""
        view_rate = getattr(self.view, "throttle_rate", None)
        if view_rate:
            return view_rate
        return "100/hour"
```

## Distributed Throttling with Redis

For multi-instance deployments, use Redis as the cache backend.

### Configuration

```bash
uv add django-redis
```

```python
# config/settings.py
CACHES = {
    "default": {
        "BACKEND": "django_redis.cache.RedisCache",
        "LOCATION": "redis://localhost:6379/1",
        "OPTIONS": {
            "CLIENT_CLASS": "django_redis.client.DefaultClient",
        },
    },
}
```

### Sliding Window Throttle

More accurate than DRF's fixed window approach.

```python
# core/throttling.py
import time
from django.core.cache import cache
from rest_framework.throttling import BaseThrottle


class SlidingWindowThrottle(BaseThrottle):
    """
    Sliding window rate limiter using Redis sorted sets.
    More accurate than fixed window approach.
    """

    scope = "sliding"
    rate = "100/minute"

    def __init__(self):
        self.num_requests, self.duration = self.parse_rate(self.rate)

    def parse_rate(self, rate):
        num, period = rate.split("/")
        num = int(num)
        duration = {
            "second": 1,
            "minute": 60,
            "hour": 3600,
            "day": 86400,
        }.get(period, 60)
        return num, duration

    def get_cache_key(self, request, view):
        if request.user.is_authenticated:
            return f"sliding_{self.scope}_{request.user.pk}"
        return f"sliding_{self.scope}_{self.get_ident(request)}"

    def allow_request(self, request, view):
        key = self.get_cache_key(request, view)
        now = time.time()
        window_start = now - self.duration

        # Get Redis connection
        from django_redis import get_redis_connection
        redis = get_redis_connection("default")

        pipe = redis.pipeline()

        # Remove old entries outside the window
        pipe.zremrangebyscore(key, 0, window_start)

        # Count requests in current window
        pipe.zcard(key)

        # Add current request
        pipe.zadd(key, {str(now): now})

        # Set expiry
        pipe.expire(key, self.duration)

        results = pipe.execute()
        request_count = results[1]

        if request_count >= self.num_requests:
            return False

        return True

    def wait(self):
        """Return seconds until next request is allowed."""
        return self.duration


class SlidingWindowUserThrottle(SlidingWindowThrottle):
    scope = "user"
    rate = "1000/hour"


class SlidingWindowAnonThrottle(SlidingWindowThrottle):
    scope = "anon"
    rate = "100/hour"

    def allow_request(self, request, view):
        if request.user.is_authenticated:
            return True
        return super().allow_request(request, view)
```

### Token Bucket Throttle

Allows bursts while maintaining average rate.

```python
# core/throttling.py
import time
from django.core.cache import cache
from rest_framework.throttling import BaseThrottle


class TokenBucketThrottle(BaseThrottle):
    """
    Token bucket algorithm for rate limiting.
    Allows bursts up to bucket capacity.
    """

    scope = "bucket"
    capacity = 100  # Maximum tokens
    refill_rate = 10  # Tokens per second

    def get_cache_key(self, request, view):
        if request.user.is_authenticated:
            return f"bucket_{self.scope}_{request.user.pk}"
        return f"bucket_{self.scope}_{self.get_ident(request)}"

    def allow_request(self, request, view):
        key = self.get_cache_key(request, view)
        now = time.time()

        # Get current bucket state
        bucket = cache.get(key)

        if bucket is None:
            # Initialize bucket
            bucket = {
                "tokens": self.capacity - 1,
                "last_update": now,
            }
            cache.set(key, bucket, timeout=3600)
            return True

        # Calculate tokens to add based on time passed
        time_passed = now - bucket["last_update"]
        tokens_to_add = time_passed * self.refill_rate

        # Update token count (cap at capacity)
        tokens = min(self.capacity, bucket["tokens"] + tokens_to_add)

        if tokens < 1:
            return False

        # Consume a token
        bucket["tokens"] = tokens - 1
        bucket["last_update"] = now
        cache.set(key, bucket, timeout=3600)

        return True

    def wait(self):
        """Seconds until one token is available."""
        return 1 / self.refill_rate
```

## Throttle Response Customization

### Custom 429 Response

```python
# core/exceptions.py
from rest_framework.views import exception_handler
from rest_framework.exceptions import Throttled


def custom_exception_handler(exc, context):
    response = exception_handler(exc, context)

    if isinstance(exc, Throttled):
        response.data = {
            "error": "rate_limit_exceeded",
            "message": "Too many requests. Please slow down.",
            "retry_after": exc.wait,
        }

    return response
```

```python
# config/settings.py
REST_FRAMEWORK = {
    "EXCEPTION_HANDLER": "core.exceptions.custom_exception_handler",
}
```

### Add Rate Limit Headers

```python
# core/middleware.py
class RateLimitHeadersMiddleware:
    """Add rate limit headers to responses."""

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        response = self.get_response(request)

        # Add headers if throttle info is available
        if hasattr(request, "_throttle_info"):
            info = request._throttle_info
            response["X-RateLimit-Limit"] = str(info.get("limit", ""))
            response["X-RateLimit-Remaining"] = str(info.get("remaining", ""))
            response["X-RateLimit-Reset"] = str(info.get("reset", ""))

        return response
```

```python
# core/throttling.py
class RateLimitInfoThrottle(UserRateThrottle):
    """Throttle that attaches rate limit info to request."""

    def allow_request(self, request, view):
        result = super().allow_request(request, view)

        # Attach rate limit info to request for middleware
        request._throttle_info = {
            "limit": self.num_requests,
            "remaining": max(0, self.num_requests - len(self.history)),
            "reset": self.history[0] + self.duration if self.history else 0,
        }

        return result
```

## Throttle Testing

### Unit Tests

```python
# tests/test_throttling.py
import pytest
from django.test import override_settings
from rest_framework.test import APIClient
from django.contrib.auth import get_user_model

User = get_user_model()


@pytest.fixture
def api_client():
    return APIClient()


@pytest.fixture
def user():
    return User.objects.create_user(
        username="testuser",
        email="test@example.com",
        password="testpass123",
    )


@override_settings(
    REST_FRAMEWORK={
        "DEFAULT_THROTTLE_CLASSES": [
            "rest_framework.throttling.UserRateThrottle",
        ],
        "DEFAULT_THROTTLE_RATES": {
            "user": "3/minute",
        },
    }
)
@pytest.mark.django_db
class TestThrottling:
    def test_throttle_limits_requests(self, api_client, user):
        api_client.force_authenticate(user=user)

        # First 3 requests should succeed
        for _ in range(3):
            response = api_client.get("/api/doctors/")
            assert response.status_code == 200

        # 4th request should be throttled
        response = api_client.get("/api/doctors/")
        assert response.status_code == 429

    def test_throttle_includes_retry_after(self, api_client, user):
        api_client.force_authenticate(user=user)

        # Exhaust rate limit
        for _ in range(4):
            api_client.get("/api/doctors/")

        response = api_client.get("/api/doctors/")
        assert response.status_code == 429
        assert "Retry-After" in response.headers

    def test_anonymous_has_lower_limit(self, api_client):
        # Anonymous users should have stricter limits
        for _ in range(2):
            response = api_client.get("/api/doctors/")
            assert response.status_code in [200, 429]
```

### Integration Tests

```python
# tests/test_throttling_integration.py
import pytest
from django.core.cache import cache
from rest_framework.test import APIClient


@pytest.fixture(autouse=True)
def clear_cache():
    """Clear cache before and after each test."""
    cache.clear()
    yield
    cache.clear()


@pytest.mark.django_db
class TestScopedThrottling:
    def test_auth_endpoint_has_strict_limit(self, api_client):
        """Auth endpoints should have stricter limits."""
        for i in range(6):
            response = api_client.post("/api/auth/login/", {
                "email": "test@example.com",
                "password": "wrong",
            })

            if i < 5:
                assert response.status_code in [400, 401]
            else:
                assert response.status_code == 429

    def test_different_scopes_independent(self, api_client, user):
        """Different throttle scopes should be independent."""
        api_client.force_authenticate(user=user)

        # Exhaust auth scope
        for _ in range(5):
            api_client.post("/api/auth/login/", {})

        # Other endpoints should still work
        response = api_client.get("/api/doctors/")
        assert response.status_code == 200
```

## Monitoring Throttle Events

### Log Throttle Events

```python
# core/throttling.py
import logging
from rest_framework.throttling import UserRateThrottle

logger = logging.getLogger(__name__)


class LoggingUserThrottle(UserRateThrottle):
    """Log when users are throttled."""

    def throttle_failure(self):
        logger.warning(
            "Rate limit exceeded",
            extra={
                "user_id": getattr(self.request.user, "pk", None),
                "ip": self.get_ident(self.request),
                "path": self.request.path,
                "scope": self.scope,
            },
        )

    def allow_request(self, request, view):
        self.request = request
        result = super().allow_request(request, view)

        if not result:
            self.throttle_failure()

        return result
```

### Prometheus Metrics

```python
# core/throttling.py
from prometheus_client import Counter
from rest_framework.throttling import UserRateThrottle

throttle_counter = Counter(
    "api_throttle_total",
    "Number of throttled requests",
    ["scope", "user_type"],
)


class MetricsUserThrottle(UserRateThrottle):
    """Track throttle metrics with Prometheus."""

    def allow_request(self, request, view):
        result = super().allow_request(request, view)

        if not result:
            user_type = "authenticated" if request.user.is_authenticated else "anonymous"
            throttle_counter.labels(
                scope=self.scope,
                user_type=user_type,
            ).inc()

        return result
```

## Best Practices

1. **Start conservative** - Begin with strict limits, loosen as needed
2. **Use Redis in production** - Required for distributed deployments
3. **Different limits for auth** - Protect login/signup endpoints
4. **Combine burst and sustained** - Allow bursts while limiting abuse
5. **Log throttle events** - Monitor for abuse patterns
6. **Clear error messages** - Tell users when they can retry
7. **Exempt health checks** - Don't throttle monitoring endpoints
8. **Test thoroughly** - Verify limits work as expected
9. **Use scopes** - Different limits for different operations
10. **Consider user tiers** - Premium users may need higher limits

## References

- [DRF Throttling](https://www.django-rest-framework.org/api-guide/throttling/)
- [Redis Rate Limiting](https://redis.io/commands/incr/#pattern-rate-limiter-1)
- [Token Bucket Algorithm](https://en.wikipedia.org/wiki/Token_bucket)

## Next Steps

- [Caching](./14-caching.md)
- [Signals](./15-signals.md)

---

[Previous: Pagination](./12-pagination.md) | [Back to Index](./README.md) | [Next: Caching](./14-caching.md)
