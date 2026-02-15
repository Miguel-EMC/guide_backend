# Project Architecture and Advanced Patterns

This guide covers app factories, configuration, dependency boundaries, concurrency, middleware, background work, observability, resilience, caching, rate limiting, and API versioning.

## App Factory and Settings

Use an app factory to keep setup explicit and testable.

```python
from fastapi import FastAPI
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "FastAPI App"
    debug: bool = False
    database_url: str


settings = Settings()


def create_app() -> FastAPI:
    app = FastAPI(title=settings.app_name, debug=settings.debug)
    register_routers(app)
    register_middleware(app)
    return app


app = create_app()
```

## Dependency Boundaries

Keep business logic out of routes. Routes should orchestrate dependencies.

```python
from fastapi import Depends


class UserService:
    def __init__(self, repo):
        self.repo = repo

    async def get_user(self, user_id: int):
        return await self.repo.get(user_id)


def get_user_service(db = Depends(get_db)) -> UserService:
    return UserService(UserRepository(db))


@app.get("/users/{user_id}")
async def read_user(user_id: int, service: UserService = Depends(get_user_service)):
    return await service.get_user(user_id)
```

## Async vs Sync Boundaries

Avoid blocking the event loop. Use threads for sync libraries.

```python
import asyncio


@app.get("/report")
async def generate_report():
    result = await asyncio.to_thread(generate_report_sync)
    return {"status": "ok", "result": result}
```

Parallelize I/O with TaskGroup when safe.

```python
import asyncio


@app.get("/dashboard")
async def dashboard():
    async with asyncio.TaskGroup() as tg:
        user_task = tg.create_task(fetch_user())
        metrics_task = tg.create_task(fetch_metrics())
    return {"user": user_task.result(), "metrics": metrics_task.result()}
```

## Lifespan and Shared Resources

Create and dispose long-lived resources once per process.

```python
from contextlib import asynccontextmanager
import httpx


@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.http = httpx.AsyncClient(timeout=10.0)
    yield
    await app.state.http.aclose()


app = FastAPI(lifespan=lifespan)
```

## Middleware vs Dependencies

Use middleware for cross-cutting concerns and dependencies for request-scoped needs.

```python
from fastapi import Request
import time


@app.middleware("http")
async def add_process_time(request: Request, call_next):
    start = time.time()
    response = await call_next(request)
    response.headers["X-Process-Time"] = str(time.time() - start)
    return response
```

## Background Tasks vs Worker Queues

`BackgroundTasks` is lightweight and tied to the request lifecycle. For durable jobs, use a queue.

```python
from fastapi import BackgroundTasks


def send_email(email: str):
    pass


@app.post("/register")
async def register(email: str, background_tasks: BackgroundTasks):
    background_tasks.add_task(send_email, email)
    return {"ok": True}
```

Use Celery, RQ, or Dramatiq for long or retryable jobs.

## Caching

Cache read-heavy data in Redis to reduce DB load.

```python
import json


@app.get("/config")
async def get_config():
    cached = await redis.get("config")
    if cached:
        return json.loads(cached)

    data = await load_config_from_db()
    await redis.set("config", json.dumps(data), ex=300)
    return data
```

## Resilience: Timeouts and Retries

```python
import httpx
from tenacity import retry, stop_after_attempt, wait_exponential


@retry(stop=stop_after_attempt(3), wait=wait_exponential(min=1, max=8))
async def call_service(url: str):
    async with httpx.AsyncClient(timeout=5.0) as client:
        resp = await client.get(url)
        resp.raise_for_status()
        return resp.json()
```

## Rate Limiting

```python
from slowapi import Limiter
from slowapi.util import get_remote_address

limiter = Limiter(key_func=get_remote_address)


@app.get("/api/resource")
@limiter.limit("5/minute")
async def get_resource(request: Request):
    return {"data": "limited"}
```

## Observability

### Structured Logs

```python
import structlog

structlog.configure(
    processors=[
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.JSONRenderer(),
    ]
)

logger = structlog.get_logger()
logger.info("api_started")
```

### Tracing and Metrics

Use OpenTelemetry for distributed tracing and Prometheus for metrics.

## CORS

```python
from fastapi.middleware.cors import CORSMiddleware

app.add_middleware(
    CORSMiddleware,
    allow_origins=["https://myapp.com", "http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
```

## API Versioning

```python
from fastapi import APIRouter

v1 = APIRouter(prefix="/api/v1")
v2 = APIRouter(prefix="/api/v2")


@v1.get("/users")
async def users_v1():
    return {"version": "1"}


@v2.get("/users")
async def users_v2():
    return {"version": "2"}
```

## Production Structure

```
project/
├── app/
│   ├── main.py
│   ├── core/
│   ├── middleware/
│   ├── routers/
│   ├── services/
│   ├── repositories/
│   ├── models/
│   └── schemas/
├── tests/
├── alembic/
└── pyproject.toml
```

## Best Practices

- Keep routes thin and move logic into services.
- Centralize config with environment-based settings.
- Avoid blocking I/O inside async routes.
- Prefer shared clients in lifespan over creating new ones per request.
- Add rate limiting and caching early for public APIs.
- Ship logs and traces to a centralized backend.

## References

- [FastAPI Async](https://fastapi.tiangolo.com/async/)
- [FastAPI Middleware](https://fastapi.tiangolo.com/tutorial/middleware/)
- [Starlette Middleware](https://www.starlette.dev/middleware/)
- [FastAPI CORS](https://fastapi.tiangolo.com/tutorial/cors/)
- [FastAPI Background Tasks](https://fastapi.tiangolo.com/tutorial/background-tasks/)

## Next Steps

- [Testing](./14-testing.md) - Test your application
- [Deployment](./15-deployment.md) - Production deployment

---

[Previous: RBAC](./12-rbac.md) | [Back to Index](./README.md) | [Next: Testing](./14-testing.md)
