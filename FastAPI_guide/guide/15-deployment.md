# Production Deployment

This guide covers production servers, Docker, environment configuration, health checks, and a deployment checklist.

## Deployment Options

| Option | Best For | Notes |
|--------|----------|-------|
| Uvicorn | Small services | Single process, simple |
| Gunicorn + Uvicorn | Production | Multiple workers |
| Docker | Containers | Portable and reproducible |
| Kubernetes | Large scale | Orchestrated workloads |

## Running in Production

### Uvicorn (simple)

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

### Gunicorn + Uvicorn Workers (common)

```bash
gunicorn app.main:app \
  -k uvicorn.workers.UvicornWorker \
  -w 4 \
  -b 0.0.0.0:8000
```

A typical formula is `workers = (2 * cpu_cores) + 1`, but tune for your workload.

### Gunicorn Config

```python
# gunicorn.conf.py
import multiprocessing

bind = "0.0.0.0:8000"
workers = multiprocessing.cpu_count()
worker_class = "uvicorn.workers.UvicornWorker"
accesslog = "-"
errorlog = "-"
loglevel = "info"
```

Run:

```bash
gunicorn app.main:app -c gunicorn.conf.py
```

## Docker (uv-based)

```dockerfile
FROM python:3.12-slim

# Install uv
COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

WORKDIR /app

# Install dependencies
COPY pyproject.toml uv.lock ./
RUN uv sync --locked --no-install-project

# Copy app
COPY . /app
RUN uv sync --locked

EXPOSE 8000
CMD ["uv", "run", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

## Environment Configuration

Use `pydantic-settings` and a `.env` file.

```env
DEBUG=false
SECRET_KEY=your-very-long-secret
DATABASE_URL=postgresql+asyncpg://user:pass@db:5432/app
REDIS_URL=redis://redis:6379/0
```

```python
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    debug: bool = False
    secret_key: str
    database_url: str
    redis_url: str | None = None

    model_config = SettingsConfigDict(env_file=".env")


settings = Settings()
```

## Health Checks

```python
from fastapi import FastAPI

app = FastAPI()


@app.get("/health")
async def health():
    return {"status": "ok"}
```

## Reverse Proxy (Nginx)

```nginx
upstream fastapi {
    server 127.0.0.1:8000;
}

server {
    listen 80;
    server_name example.com;

    location / {
        proxy_pass http://fastapi;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## Production Checklist

### Security

- [ ] HTTPS enabled
- [ ] Secrets stored in environment variables
- [ ] CORS configured
- [ ] Rate limiting in place

### Performance

- [ ] Gunicorn workers tuned
- [ ] DB connection pooling
- [ ] Caching where appropriate

### Monitoring

- [ ] Health checks
- [ ] Structured logs
- [ ] Error tracking (Sentry or similar)

## References

- [FastAPI Deployment](https://fastapi.tiangolo.com/deployment/)
- [Gunicorn Documentation](https://docs.gunicorn.org/)
- [Uvicorn Documentation](https://www.uvicorn.org/)

## Next Steps

- [Project: Todo API](./16-project-todo.md) - Complete project example

---

[Previous: Testing](./14-testing.md) | [Back to Index](./README.md) | [Next: Project Todo](./16-project-todo.md)
