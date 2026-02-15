# Middleware

Middleware runs on every request/response and is ideal for cross-cutting concerns like security headers, request IDs, and logging.

## Step 1: Understand Order

Middleware runs top-to-bottom on request and bottom-to-top on response:

```
Request -> M1 -> M2 -> View
Response <- M1 <- M2 <- View
```

Place security and CORS middleware early.

## Step 2: Custom Middleware (Class-Based)

```python
# core/middleware.py
import time


class RequestTimingMiddleware:
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        start = time.time()
        response = self.get_response(request)
        response["X-Process-Time"] = f"{time.time() - start:.3f}"
        return response
```

## Step 3: Request ID Middleware

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

## Step 4: Register Middleware

```python
# config/settings.py
MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
    "corsheaders.middleware.CorsMiddleware",
    "django.contrib.sessions.middleware.SessionMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware",
    "django.contrib.messages.middleware.MessageMiddleware",
    "django.middleware.clickjacking.XFrameOptionsMiddleware",
    "core.middleware.RequestTimingMiddleware",
    "core.middleware.RequestIdMiddleware",
]
```

## Tips

- Keep middleware fast and side-effect free.
- Avoid DB queries in middleware.
- Use middleware for headers, tracing IDs, and request timing.

## References

- [Django Middleware](https://docs.djangoproject.com/en/5.2/topics/http/middleware/)

## Next Steps

- [Admin Customization](./19-admin-customization.md)
- [Deployment](./20-deployment.md)

---

[Previous: Celery and Tasks](./17-celery-tasks.md) | [Back to Index](./README.md) | [Next: Admin Customization](./19-admin-customization.md)
