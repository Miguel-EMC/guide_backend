# Deployment

This chapter covers a production-ready Django deployment checklist.

## Step 1: Core Settings

```python
# config/settings.py
DEBUG = False
SECRET_KEY = os.environ.get("SECRET_KEY")
ALLOWED_HOSTS = os.environ.get("ALLOWED_HOSTS", "").split(",")

SECURE_SSL_REDIRECT = True
SECURE_PROXY_SSL_HEADER = ("HTTP_X_FORWARDED_PROTO", "https")
SESSION_COOKIE_SECURE = True
CSRF_COOKIE_SECURE = True
SECURE_HSTS_SECONDS = 31536000
SECURE_HSTS_INCLUDE_SUBDOMAINS = True
SECURE_HSTS_PRELOAD = True
```

## Step 2: Static and Media

```bash
uv run python manage.py collectstatic
```

Use a CDN or object storage for media.

## Step 3: Application Server

Common options:

- `gunicorn` (WSGI)
- `uvicorn` (ASGI)

```bash
gunicorn config.wsgi:application --bind 0.0.0.0:8000 --workers 4
```

## Step 4: Database

Use PostgreSQL in production and keep migrations in CI/CD.

```bash
uv run python manage.py migrate
```

## Step 5: Environment Variables

Use env vars for secrets and configuration. Avoid committing secrets.

## Step 6: Health Checks

```python
# core/views.py
from django.http import JsonResponse


def healthz(request):
    return JsonResponse({"status": "ok"})
```

## Tips

- Disable DEBUG in production.
- Use HTTPS everywhere.
- Add a reverse proxy (Nginx) for TLS termination.

## References

- [Django Deployment Checklist](https://docs.djangoproject.com/en/5.2/howto/deployment/checklist/)

## Next Steps

- [Logging](./21-logging.md)
- [Security](./22-security.md)

---

[Previous: Admin Customization](./19-admin-customization.md) | [Back to Index](./README.md) | [Next: Logging](./21-logging.md)
