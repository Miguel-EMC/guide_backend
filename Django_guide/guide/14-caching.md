# Caching

Caching reduces database load and improves response time. This chapter covers Django cache backends, view caching, low-level API, and invalidation strategies.

## Step 1: Choose a Cache Backend

### Local memory (dev)

```python
# config/settings.py
CACHES = {
    "default": {
        "BACKEND": "django.core.cache.backends.locmem.LocMemCache",
        "LOCATION": "unique-dev-cache",
    }
}
```

### Redis (recommended for production)

Django ships a Redis cache backend.

```python
# config/settings.py
CACHES = {
    "default": {
        "BACKEND": "django.core.cache.backends.redis.RedisCache",
        "LOCATION": "redis://127.0.0.1:6379",
    }
}
```

## Step 2: Low-Level Cache API

```python
from django.core.cache import cache

cache.set("doctor:1", {"name": "Dr. Ada"}, timeout=300)
value = cache.get("doctor:1")
cache.delete("doctor:1")
```

## Step 3: View-Level Cache

```python
from django.utils.decorators import method_decorator
from django.views.decorators.cache import cache_page
from rest_framework import generics


@method_decorator(cache_page(60 * 5), name="dispatch")
class DoctorListView(generics.ListAPIView):
    queryset = Doctor.objects.all()
    serializer_class = DoctorReadSerializer
```

### Vary on Headers

Use `Vary` headers when responses depend on headers such as `Authorization` or `Accept-Language`.

```python
from django.views.decorators.vary import vary_on_headers

@method_decorator(vary_on_headers("Authorization"), name="dispatch")
class DoctorListView(generics.ListAPIView):
    queryset = Doctor.objects.all()
    serializer_class = DoctorReadSerializer
```

## Step 4: Manual Caching in Views

```python
from django.core.cache import cache
from rest_framework.response import Response


class DoctorDetailView(generics.RetrieveAPIView):
    queryset = Doctor.objects.all()
    serializer_class = DoctorReadSerializer

    def get(self, request, *args, **kwargs):
        cache_key = f"doctor:{kwargs['pk']}"
        cached = cache.get(cache_key)
        if cached:
            return Response(cached)

        response = super().get(request, *args, **kwargs)
        cache.set(cache_key, response.data, timeout=300)
        return response
```

## Step 5: Invalidation Strategies

### Invalidate on writes

```python
from django.core.cache import cache


class DoctorViewSet(viewsets.ModelViewSet):
    queryset = Doctor.objects.all()
    serializer_class = DoctorReadSerializer

    def perform_create(self, serializer):
        doctor = serializer.save()
        cache.delete("doctor:list")
        cache.delete(f"doctor:{doctor.pk}")
```

### Invalidate using signals

```python
from django.db.models.signals import post_save, post_delete
from django.dispatch import receiver
from django.core.cache import cache
from .models import Doctor


@receiver([post_save, post_delete], sender=Doctor)
def invalidate_doctor_cache(sender, instance, **kwargs):
    cache.delete("doctor:list")
    cache.delete(f"doctor:{instance.pk}")
```

## Step 6: Cache Keys

A good cache key includes resource, identifiers, and query params:

```python
key = f"doctor:list:{request.GET.urlencode()}"
```

## Tips and Best Practices

- Use Redis for shared cache across multiple instances.
- Do not store file-based cache inside `MEDIA_ROOT` or `STATIC_ROOT`.
- Cache only public or non-personalized responses, unless your key includes user context.
- Invalidate on writes to avoid stale data.

## References

- [Django Cache Framework](https://docs.djangoproject.com/en/6.0/topics/cache/)

## Next Steps

- [Signals](./15-signals.md)
- [File Uploads](./16-file-uploads.md)

---

[Previous: Throttling](./13-throttling.md) | [Back to Index](./README.md) | [Next: Signals](./15-signals.md)
