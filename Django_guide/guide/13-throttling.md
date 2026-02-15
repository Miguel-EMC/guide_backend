# Throttling (Rate Limiting)

Throttling limits request rates to protect your API from abuse and spikes.

## Step 1: Enable Throttles

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

## Step 2: Per-View Throttling

```python
from rest_framework.throttling import UserRateThrottle


class DoctorListView(generics.ListAPIView):
    throttle_classes = [UserRateThrottle]
```

## Step 3: ScopedRateThrottle

Use scopes to define different limits per endpoint.

```python
# config/settings.py
REST_FRAMEWORK = {
    "DEFAULT_THROTTLE_CLASSES": [
        "rest_framework.throttling.ScopedRateThrottle",
    ],
    "DEFAULT_THROTTLE_RATES": {
        "auth": "5/minute",
        "doctors": "200/hour",
    },
}
```

```python
class LoginView(generics.GenericAPIView):
    throttle_scope = "auth"
    throttle_classes = [ScopedRateThrottle]
```

## Step 4: Custom Throttle

```python
from rest_framework.throttling import SimpleRateThrottle


class WriteThrottle(SimpleRateThrottle):
    scope = "write"
    rate = "100/hour"

    def get_cache_key(self, request, view):
        if request.method in {"GET", "HEAD", "OPTIONS"}:
            return None
        if request.user.is_authenticated:
            return f"write_{request.user.pk}"
        return f"write_{self.get_ident(request)}"
```

## Step 5: Throttle Response

DRF returns a 429 with a retry-after message when throttled.

## Tips

- Use Redis cache for distributed throttling.
- Apply strict throttles to auth endpoints.
- Combine burst and sustained limits when needed.

## References

- [DRF Throttling](https://www.django-rest-framework.org/api-guide/throttling/)

## Next Steps

- [Caching](./14-caching.md)
- [Signals](./15-signals.md)

---

[Previous: Pagination](./12-pagination.md) | [Back to Index](./README.md) | [Next: Caching](./14-caching.md)
