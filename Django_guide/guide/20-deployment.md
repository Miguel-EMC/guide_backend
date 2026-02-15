# Production Deployment

This guide covers production servers, Docker, environment configuration, health checks, and a deployment checklist for Django + DRF.

## Deployment Options

| Option | Best For | Notes |
|--------|----------|-------|
| Gunicorn | Most production apps | WSGI, multiple workers |
| Uvicorn | ASGI/async views | Use with Gunicorn for workers |
| Docker | Containers | Portable and reproducible |
| Kubernetes | Large scale | Orchestrated workloads |

## Running in Production

### Gunicorn (WSGI)

```bash
gunicorn config.wsgi:application \
  --bind 0.0.0.0:8000 \
  --workers 4 \
  --access-logfile - \
  --error-logfile -
```

A typical formula is `workers = (2 * cpu_cores) + 1`, but tune for your workload.

### Gunicorn Config File

```python
# gunicorn.conf.py
import multiprocessing

bind = "0.0.0.0:8000"
workers = multiprocessing.cpu_count() * 2 + 1
worker_class = "sync"  # or "uvicorn.workers.UvicornWorker" for ASGI
accesslog = "-"
errorlog = "-"
loglevel = "info"
timeout = 30
keepalive = 2
```

Run with config:

```bash
gunicorn config.wsgi:application -c gunicorn.conf.py
```

### Uvicorn (ASGI)

For async views or Django Channels:

```bash
gunicorn config.asgi:application \
  -k uvicorn.workers.UvicornWorker \
  -w 4 \
  -b 0.0.0.0:8000
```

## Core Settings for Production

```python
# config/settings.py
import os

DEBUG = False
SECRET_KEY = os.environ.get("SECRET_KEY")
ALLOWED_HOSTS = os.environ.get("ALLOWED_HOSTS", "").split(",")

# Security settings
SECURE_SSL_REDIRECT = True
SECURE_PROXY_SSL_HEADER = ("HTTP_X_FORWARDED_PROTO", "https")
SESSION_COOKIE_SECURE = True
CSRF_COOKIE_SECURE = True
SECURE_HSTS_SECONDS = 31536000
SECURE_HSTS_INCLUDE_SUBDOMAINS = True
SECURE_HSTS_PRELOAD = True
SECURE_CONTENT_TYPE_NOSNIFF = True
X_FRAME_OPTIONS = "DENY"

# Database
DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.postgresql",
        "NAME": os.environ.get("DB_NAME"),
        "USER": os.environ.get("DB_USER"),
        "PASSWORD": os.environ.get("DB_PASSWORD"),
        "HOST": os.environ.get("DB_HOST", "localhost"),
        "PORT": os.environ.get("DB_PORT", "5432"),
        "CONN_MAX_AGE": 60,
        "OPTIONS": {
            "connect_timeout": 10,
        },
    }
}

# Static and media
STATIC_URL = "/static/"
STATIC_ROOT = "/app/staticfiles"
MEDIA_URL = "/media/"
MEDIA_ROOT = "/app/mediafiles"
```

## Docker (uv-based)

### Dockerfile

```dockerfile
FROM python:3.12-slim

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

# Install uv
COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

WORKDIR /app

# Install dependencies first (better caching)
COPY pyproject.toml uv.lock ./
RUN uv sync --locked --no-install-project

# Copy application code
COPY . /app
RUN uv sync --locked

# Collect static files
RUN uv run python manage.py collectstatic --noinput

# Create non-root user
RUN useradd -m -u 1000 appuser && chown -R appuser:appuser /app
USER appuser

EXPOSE 8000

CMD ["uv", "run", "gunicorn", "config.wsgi:application", "--bind", "0.0.0.0:8000", "--workers", "4"]
```

### docker-compose.yml

```yaml
version: "3.9"

services:
  web:
    build: .
    ports:
      - "8000:8000"
    environment:
      - DEBUG=false
      - SECRET_KEY=${SECRET_KEY}
      - ALLOWED_HOSTS=localhost,127.0.0.1
      - DB_HOST=db
      - DB_NAME=app
      - DB_USER=postgres
      - DB_PASSWORD=${DB_PASSWORD}
      - REDIS_URL=redis://redis:6379/0
    depends_on:
      db:
        condition: service_healthy
      redis:
        condition: service_started
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8000/healthz/"]
      interval: 30s
      timeout: 10s
      retries: 3

  db:
    image: postgres:16-alpine
    environment:
      - POSTGRES_DB=app
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    volumes:
      - redis_data:/data

  celery:
    build: .
    command: uv run celery -A config worker -l info
    environment:
      - DEBUG=false
      - SECRET_KEY=${SECRET_KEY}
      - DB_HOST=db
      - DB_NAME=app
      - DB_USER=postgres
      - DB_PASSWORD=${DB_PASSWORD}
      - REDIS_URL=redis://redis:6379/0
    depends_on:
      - db
      - redis

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./staticfiles:/app/staticfiles:ro
    depends_on:
      - web

volumes:
  postgres_data:
  redis_data:
```

## Environment Configuration

Use a `.env` file for local development (never commit to git):

```env
# .env
DEBUG=false
SECRET_KEY=your-very-long-secret-key-here
ALLOWED_HOSTS=localhost,127.0.0.1,example.com

# Database
DB_NAME=app
DB_USER=postgres
DB_PASSWORD=secure-password
DB_HOST=db
DB_PORT=5432

# Redis
REDIS_URL=redis://redis:6379/0

# Email
EMAIL_HOST=smtp.example.com
EMAIL_PORT=587
EMAIL_HOST_USER=user@example.com
EMAIL_HOST_PASSWORD=email-password
```

Load environment variables in settings:

```python
# config/settings.py
from pathlib import Path
import os

# Or use django-environ
# import environ
# env = environ.Env()
# environ.Env.read_env()
```

## Nginx Configuration

```nginx
# nginx.conf
events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    upstream django {
        server web:8000;
    }

    server {
        listen 80;
        server_name example.com;

        # Redirect HTTP to HTTPS
        return 301 https://$server_name$request_uri;
    }

    server {
        listen 443 ssl http2;
        server_name example.com;

        ssl_certificate /etc/nginx/ssl/cert.pem;
        ssl_certificate_key /etc/nginx/ssl/key.pem;
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;
        ssl_prefer_server_ciphers off;

        client_max_body_size 10M;

        # Security headers
        add_header X-Frame-Options "DENY" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header X-XSS-Protection "1; mode=block" always;
        add_header Referrer-Policy "strict-origin-when-cross-origin" always;

        location /static/ {
            alias /app/staticfiles/;
            expires 30d;
            add_header Cache-Control "public, immutable";
        }

        location /media/ {
            alias /app/mediafiles/;
            expires 7d;
        }

        location / {
            proxy_pass http://django;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_connect_timeout 30s;
            proxy_read_timeout 30s;
        }

        location /healthz/ {
            proxy_pass http://django;
            access_log off;
        }
    }
}
```

## Health Checks

```python
# core/views.py
from django.http import JsonResponse
from django.db import connection


def healthz(request):
    """Liveness probe - is the app running?"""
    return JsonResponse({"status": "ok"})


def readyz(request):
    """Readiness probe - can the app serve traffic?"""
    try:
        with connection.cursor() as cursor:
            cursor.execute("SELECT 1")
        return JsonResponse({"status": "ready", "database": "ok"})
    except Exception as e:
        return JsonResponse(
            {"status": "not ready", "database": str(e)},
            status=503
        )
```

```python
# config/urls.py
from core.views import healthz, readyz

urlpatterns = [
    # ...
    path("healthz/", healthz, name="healthz"),
    path("readyz/", readyz, name="readyz"),
]
```

## Static and Media Files

```bash
# Collect static files
uv run python manage.py collectstatic --noinput
```

For production, use a CDN or object storage (S3, GCS) for media files:

```python
# config/settings.py
# Using django-storages for S3
DEFAULT_FILE_STORAGE = "storages.backends.s3boto3.S3Boto3Storage"
AWS_ACCESS_KEY_ID = os.environ.get("AWS_ACCESS_KEY_ID")
AWS_SECRET_ACCESS_KEY = os.environ.get("AWS_SECRET_ACCESS_KEY")
AWS_STORAGE_BUCKET_NAME = os.environ.get("AWS_BUCKET_NAME")
AWS_S3_REGION_NAME = "us-east-1"
AWS_DEFAULT_ACL = None
AWS_S3_OBJECT_PARAMETERS = {"CacheControl": "max-age=86400"}
```

## Database Migrations

Always run migrations before starting the app:

```bash
uv run python manage.py migrate --noinput
```

In Docker, add a migration step to your entrypoint:

```bash
#!/bin/bash
# entrypoint.sh
set -e

echo "Running migrations..."
uv run python manage.py migrate --noinput

echo "Collecting static files..."
uv run python manage.py collectstatic --noinput

echo "Starting server..."
exec "$@"
```

## Production Checklist

### Security

- [ ] `DEBUG = False`
- [ ] Strong `SECRET_KEY` from environment
- [ ] HTTPS enabled (`SECURE_SSL_REDIRECT = True`)
- [ ] Secure cookies (`SESSION_COOKIE_SECURE`, `CSRF_COOKIE_SECURE`)
- [ ] HSTS configured
- [ ] `ALLOWED_HOSTS` properly set
- [ ] Security headers (X-Frame-Options, X-Content-Type-Options)
- [ ] CORS configured properly

### Database

- [ ] PostgreSQL in production
- [ ] Connection pooling (`CONN_MAX_AGE`)
- [ ] Backups configured
- [ ] Migrations run before deploy

### Performance

- [ ] Gunicorn workers tuned
- [ ] Static files served by Nginx/CDN
- [ ] Caching configured (Redis)
- [ ] Database indexes added

### Monitoring

- [ ] Health check endpoints (`/healthz/`, `/readyz/`)
- [ ] Structured JSON logs
- [ ] Error tracking (Sentry)
- [ ] Metrics collection

### Operations

- [ ] Environment variables for all secrets
- [ ] Non-root user in Docker
- [ ] Graceful shutdown handling
- [ ] Log rotation configured

## References

- [Django Deployment Checklist](https://docs.djangoproject.com/en/5.2/howto/deployment/checklist/)
- [Gunicorn Documentation](https://docs.gunicorn.org/)
- [Docker Best Practices](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/)

## Next Steps

- [Logging](./21-logging.md)
- [Security](./22-security.md)

---

[Previous: Admin Customization](./19-admin-customization.md) | [Back to Index](./README.md) | [Next: Logging](./21-logging.md)
