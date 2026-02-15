# Testing FastAPI Applications

This guide covers pytest setup, sync and async clients, lifespan events, dependency overrides, database isolation, mocking, WebSockets, and test organization for production-grade FastAPI apps.

## Installation

```bash
# With uv
uv add --dev pytest httpx

# Async support (choose one)
uv add --dev pytest-asyncio
# or
uv add --dev anyio

# Optional
uv add --dev pytest-cov pytest-xdist
uv add --dev asgi-lifespan respx pytest-mock
```

| Package | Purpose |
|---------|---------|
| `pytest` | Test runner |
| `httpx` | Required by `TestClient` and `AsyncClient` |
| `pytest-asyncio` | Asyncio-based async tests/fixtures |
| `anyio` | AnyIO pytest plugin (`pytest.mark.anyio`) |
| `pytest-cov` | Coverage reports |
| `pytest-xdist` | Parallel test execution |
| `asgi-lifespan` | Lifespan management in async tests |
| `respx` | Mock HTTPX requests |
| `pytest-mock` | `mocker` fixture for cleaner mocks |

## Running Tests

```bash
# With uv
uv run pytest

# Traditional
pytest

# Quick filters
pytest -q
pytest -k "auth and not refresh"
pytest -x --maxfail=1
```

## Project Structure

```
project/
├── app/
│   ├── main.py
│   └── ...
├── tests/
│   ├── __init__.py
│   ├── conftest.py       # Shared fixtures
│   ├── test_users.py
│   ├── test_auth.py
│   └── test_items.py
├── pytest.ini            # Pytest configuration
└── pyproject.toml
```

## Pytest Configuration

### Option A: pytest-asyncio

**pytest.ini**

```ini
[pytest]
testpaths = tests
python_files = test_*.py
python_functions = test_*
asyncio_mode = auto
asyncio_default_fixture_loop_scope = function
asyncio_default_test_loop_scope = function
filterwarnings =
    ignore::DeprecationWarning
```

**Notes**

- `asyncio_mode = auto` lets you use `async def` tests and async fixtures without extra markers.
- If you omit `asyncio_mode`, pytest-asyncio defaults to `strict` and you should mark async tests with `@pytest.mark.asyncio`.
- Set both default loop scopes to `function` to prevent event-loop reuse across tests. Future pytest-asyncio defaults are moving in this direction.

### Option B: AnyIO plugin (used in FastAPI async tests docs)

**pyproject.toml**

```toml
[tool.pytest.ini_options]
testpaths = ["tests"]
anyio_mode = "auto"
```

**Notes**

- AnyIO auto mode can conflict with pytest-asyncio auto mode. If you keep pytest-asyncio installed, leave it in strict mode (don’t set `asyncio_mode = auto`).

## TestClient Basics

`fastapi.testclient.TestClient` is the same client provided by Starlette and is built on HTTPX. Use regular `def` tests and normal (non-`await`) calls.

### Synchronous Testing

```python
from fastapi.testclient import TestClient
from app.main import app


def test_read_root():
    with TestClient(app) as client:
        response = client.get("/")
        assert response.status_code == 200
        assert response.json() == {"message": "Hello World"}


def test_create_item():
    with TestClient(app) as client:
        response = client.post(
            "/items/",
            json={"name": "Test Item", "price": 9.99}
        )
        assert response.status_code == 201
        assert response.json()["name"] == "Test Item"


def test_read_item_not_found():
    with TestClient(app) as client:
        response = client.get("/items/999")
        assert response.status_code == 404
        assert "not found" in response.json()["detail"].lower()
```

## Async Testing with HTTPX

When you need `async` tests, use `httpx.AsyncClient` with `ASGITransport`. Set `base_url` so relative URLs like `"/"` work correctly.

```python
import pytest
from httpx import ASGITransport, AsyncClient
from app.main import app


@pytest.mark.anyio
async def test_read_users():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.get("/users/")
        assert response.status_code == 200
        assert isinstance(response.json(), list)
```

**If you use pytest-asyncio**, replace `@pytest.mark.anyio` with `@pytest.mark.asyncio` (or keep `asyncio_mode = auto`).

### Lifespan Events in Async Tests

`AsyncClient` does **not** trigger lifespan events (startup/shutdown). Use `asgi-lifespan` when you need to ensure they run.

```python
import pytest
from asgi_lifespan import LifespanManager
from httpx import ASGITransport, AsyncClient
from app.main import app


@pytest.mark.anyio
async def test_with_lifespan():
    async with LifespanManager(app) as manager:
        transport = ASGITransport(app=manager.app)
        async with AsyncClient(transport=transport, base_url="http://test") as client:
            response = await client.get("/health")
            assert response.status_code == 200
```

## Lifespan Events in Sync Tests

For sync tests, use `TestClient` as a context manager to ensure lifespan startup/shutdown is executed.

```python
from fastapi import FastAPI
from fastapi.testclient import TestClient
from contextlib import asynccontextmanager

items = {}


@asynccontextmanager
async def lifespan(app: FastAPI):
    items["foo"] = {"name": "Fighters"}
    yield
    items.clear()


app = FastAPI(lifespan=lifespan)


@app.get("/items/{item_id}")
async def read_items(item_id: str):
    return items[item_id]


def test_read_items():
    assert items == {}
    with TestClient(app) as client:
        assert items != {}
        response = client.get("/items/foo")
        assert response.status_code == 200
```

## Dependency Overrides

Use `app.dependency_overrides` to replace real dependencies (DB, external services) with test doubles.

```python
from app.core.database import get_db


def override_get_db():
    yield test_session


app.dependency_overrides[get_db] = override_get_db
# run tests...
app.dependency_overrides.clear()
```

## Fixtures

### conftest.py (sync DB + TestClient)

```python
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.main import app
from app.core.database import Base, get_db

TEST_DATABASE_URL = "sqlite:///:memory:"


@pytest.fixture(scope="session")
def engine():
    engine = create_engine(
        TEST_DATABASE_URL,
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    Base.metadata.create_all(bind=engine)
    yield engine
    Base.metadata.drop_all(bind=engine)


@pytest.fixture
def db_session(engine):
    TestingSessionLocal = sessionmaker(
        autocommit=False,
        autoflush=False,
        bind=engine
    )
    session = TestingSessionLocal()
    try:
        yield session
    finally:
        session.rollback()
        session.close()


@pytest.fixture
def client(db_session):
    def override_get_db():
        yield db_session

    app.dependency_overrides[get_db] = override_get_db

    with TestClient(app) as test_client:
        yield test_client

    app.dependency_overrides.clear()
```

### Async client fixture (HTTPX + ASGITransport)

```python
import pytest
from httpx import ASGITransport, AsyncClient

from app.main import app
from app.core.database import get_db


@pytest.fixture
async def async_client(db_session):
    def override_get_db():
        yield db_session

    app.dependency_overrides[get_db] = override_get_db

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        yield client

    app.dependency_overrides.clear()
```

### User Fixtures

```python
import pytest


@pytest.fixture
def test_user(db_session):
    from app.models.user import User
    from app.core.security import hash_password

    user = User(
        email="test@example.com",
        hashed_password=hash_password("testpassword"),
        is_active=True
    )
    db_session.add(user)
    db_session.commit()
    db_session.refresh(user)
    return user


@pytest.fixture
def auth_headers(client, test_user):
    response = client.post(
        "/auth/token",
        data={"username": "test@example.com", "password": "testpassword"}
    )
    token = response.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}
```

## Testing Patterns

### Testing CRUD Operations

```python
# tests/test_items.py

def test_create_item(client, auth_headers):
    response = client.post(
        "/items/",
        json={"name": "New Item", "price": 19.99},
        headers=auth_headers
    )
    assert response.status_code == 201
    data = response.json()
    assert data["name"] == "New Item"
    assert data["price"] == 19.99
    assert "id" in data


def test_read_items(client, auth_headers):
    client.post("/items/", json={"name": "Item 1", "price": 10}, headers=auth_headers)
    client.post("/items/", json={"name": "Item 2", "price": 20}, headers=auth_headers)

    response = client.get("/items/", headers=auth_headers)
    assert response.status_code == 200
    assert len(response.json()) >= 2


def test_update_item(client, auth_headers):
    create_response = client.post(
        "/items/",
        json={"name": "Original", "price": 10},
        headers=auth_headers
    )
    item_id = create_response.json()["id"]

    response = client.put(
        f"/items/{item_id}",
        json={"name": "Updated", "price": 15},
        headers=auth_headers
    )
    assert response.status_code == 200
    assert response.json()["name"] == "Updated"


def test_delete_item(client, auth_headers):
    create_response = client.post(
        "/items/",
        json={"name": "To Delete", "price": 10},
        headers=auth_headers
    )
    item_id = create_response.json()["id"]

    response = client.delete(f"/items/{item_id}", headers=auth_headers)
    assert response.status_code == 204

    response = client.get(f"/items/{item_id}", headers=auth_headers)
    assert response.status_code == 404
```

### Testing Authentication

```python
# tests/test_auth.py

def test_register_user(client):
    response = client.post(
        "/auth/register",
        json={
            "email": "newuser@example.com",
            "password": "strongpassword123"
        }
    )
    assert response.status_code == 201
    assert response.json()["email"] == "newuser@example.com"
    assert "password" not in response.json()


def test_login_success(client, test_user):
    response = client.post(
        "/auth/token",
        data={"username": "test@example.com", "password": "testpassword"}
    )
    assert response.status_code == 200
    assert "access_token" in response.json()
    assert response.json()["token_type"] == "bearer"


def test_login_wrong_password(client, test_user):
    response = client.post(
        "/auth/token",
        data={"username": "test@example.com", "password": "wrongpassword"}
    )
    assert response.status_code == 401


def test_protected_route_no_token(client):
    response = client.get("/users/me")
    assert response.status_code == 401


def test_protected_route_with_token(client, auth_headers):
    response = client.get("/users/me", headers=auth_headers)
    assert response.status_code == 200
    assert response.json()["email"] == "test@example.com"
```

### Testing Validation

```python
# tests/test_validation.py

def test_create_item_invalid_price(client, auth_headers):
    response = client.post(
        "/items/",
        json={"name": "Item", "price": -10},
        headers=auth_headers
    )
    assert response.status_code == 422


def test_create_item_missing_field(client, auth_headers):
    response = client.post(
        "/items/",
        json={"name": "Item"},
        headers=auth_headers
    )
    assert response.status_code == 422
    assert "price" in str(response.json())


def test_create_item_invalid_type(client, auth_headers):
    response = client.post(
        "/items/",
        json={"name": "Item", "price": "not a number"},
        headers=auth_headers
    )
    assert response.status_code == 422
```

## Testing Error Responses

By default, `TestClient` raises server exceptions. To assert on 500 responses, disable it.

```python
from fastapi.testclient import TestClient
from app.main import app


def test_internal_error_response():
    with TestClient(app, raise_server_exceptions=False) as client:
        response = client.get("/boom")
        assert response.status_code == 500
```

## Testing WebSockets

```python
from fastapi.testclient import TestClient
from app.main import app


def test_websocket_echo():
    with TestClient(app) as client:
        with client.websocket_connect("/ws") as ws:
            ws.send_text("ping")
            data = ws.receive_text()
            assert data == "ping"
```

## Mocking

### Mocking External Services

```python
import pytest
from unittest.mock import patch, AsyncMock


def test_send_email(client, auth_headers):
    with patch("app.services.email.send_email") as mock_send:
        mock_send.return_value = True

        response = client.post(
            "/users/forgot-password",
            json={"email": "test@example.com"}
        )

        assert response.status_code == 200
        mock_send.assert_called_once()


@pytest.mark.anyio
async def test_external_api_call(async_client):
    with patch("app.services.external.fetch_data", new_callable=AsyncMock) as mock_fetch:
        mock_fetch.return_value = {"data": "mocked"}

        response = await async_client.get("/external-data")

        assert response.status_code == 200
        assert response.json()["data"] == "mocked"
```

### Mocking HTTPX with respx

```python
import respx
from httpx import Response


@respx.mock
def test_external_http_call(client):
    route = respx.get(\"https://api.example.com/v1/data\").mock(
        return_value=Response(200, json={\"ok\": True})
    )

    response = client.get(\"/external-data\")

    assert route.called
    assert response.status_code == 200
    assert response.json()[\"ok\"] is True
```

### Mocking Database Errors

```python
from unittest.mock import patch


def test_database_error_handling(client, auth_headers):
    with patch("app.services.items.get_items") as mock_get:
        mock_get.side_effect = Exception("Database connection failed")

        response = client.get("/items/", headers=auth_headers)

        assert response.status_code == 500
```

## Async Database Testing

### Async Fixtures

```python
import pytest
from httpx import ASGITransport, AsyncClient
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker
from asgi_lifespan import LifespanManager

from app.main import app
from app.core.database import Base, get_db

TEST_DATABASE_URL = "sqlite+aiosqlite:///:memory:"


@pytest.fixture
async def async_engine():
    engine = create_async_engine(TEST_DATABASE_URL)
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield engine
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)
    await engine.dispose()


@pytest.fixture
async def async_db_session(async_engine):
    async_session = async_sessionmaker(
        async_engine,
        expire_on_commit=False
    )
    async with async_session() as session:
        yield session
        await session.rollback()


@pytest.fixture
async def async_client_db(async_db_session):
    async def override_get_db():
        yield async_db_session

    app.dependency_overrides[get_db] = override_get_db

    async with LifespanManager(app) as manager:
        transport = ASGITransport(app=manager.app)
        async with AsyncClient(transport=transport, base_url="http://test") as client:
            yield client

    app.dependency_overrides.clear()
```

### Async Tests

```python
import pytest


@pytest.mark.anyio
async def test_create_user_async(async_client_db):
    response = await async_client_db.post(
        "/users/",
        json={"email": "async@test.com", "password": "password123"}
    )
    assert response.status_code == 201


@pytest.mark.anyio
async def test_get_users_async(async_client_db):
    response = await async_client_db.get("/users/")
    assert response.status_code == 200
```

## Test Organization

### Parametrized Tests

```python
import pytest


@pytest.mark.parametrize("email,password,expected_status", [
    ("valid@email.com", "validpass123", 201),
    ("invalid-email", "validpass123", 422),
    ("valid@email.com", "short", 422),
    ("", "validpass123", 422),
])
def test_register_validation(client, email, password, expected_status):
    response = client.post(
        "/auth/register",
        json={"email": email, "password": password}
    )
    assert response.status_code == expected_status
```

### Test Classes

```python
class TestUserEndpoints:
    def test_create_user(self, client):
        response = client.post("/users/", json={"email": "new@test.com", "password": "pass123"})
        assert response.status_code == 201

    def test_get_user(self, client, test_user, auth_headers):
        response = client.get(f"/users/{test_user.id}", headers=auth_headers)
        assert response.status_code == 200

    def test_update_user(self, client, test_user, auth_headers):
        response = client.patch(
            f"/users/{test_user.id}",
            json={"email": "updated@test.com"},
            headers=auth_headers
        )
        assert response.status_code == 200
```

## CLI Cheatsheet

```bash
# Run all tests
pytest

# Run with verbose output
pytest -v

# Run specific file
pytest tests/test_users.py

# Run specific test
pytest tests/test_users.py::test_create_user

# Run last failures
pytest --lf

# Run with coverage
pytest --cov=app --cov-report=term-missing --cov-report=html

# Run only async tests (marker-based)
pytest -m anyio

# Run tests in parallel
pytest -n auto
```

## Coverage Report

```bash
pytest --cov=app --cov-report=term-missing --cov-report=html
```

```
----------- coverage: -----------
Name                    Stmts   Miss  Cover   Missing
-----------------------------------------------------
app/main.py                15      0   100%
app/routers/users.py       45      3    93%   78-80
app/services/user.py       30      2    93%   45, 67
-----------------------------------------------------
TOTAL                      90      5    94%
```

## Best Practices

| Practice | Description |
|----------|-------------|
| Isolate tests | Each test should be independent |
| Reset overrides | Clear `app.dependency_overrides` after each test |
| Use context managers | Use `TestClient` context to run lifespan events |
| Mock external services | Don’t call real third-party APIs |
| Test edge cases | Empty inputs, boundaries, errors |
| Keep tests fast | Aim for < 1 second per test |

## Summary

| Component | Purpose |
|-----------|---------|
| `TestClient` | Sync testing via Starlette/HTTPX |
| `AsyncClient` + `ASGITransport` | Async testing without a server |
| `LifespanManager` | Trigger lifespan events in async tests |
| `dependency_overrides` | Swap dependencies during tests |
| `pytest.mark.anyio` | AnyIO async tests |
| `pytest.mark.asyncio` | pytest-asyncio async tests |
| `pytest-cov` | Coverage reports |

## References

- [FastAPI Testing Documentation](https://fastapi.tiangolo.com/tutorial/testing/)
- [FastAPI Async Tests](https://fastapi.tiangolo.com/advanced/async-tests/)
- [FastAPI Testing Dependencies with Overrides](https://fastapi.tiangolo.com/advanced/testing-dependencies/)
- [FastAPI Testing Lifespan Events](https://fastapi.tiangolo.com/advanced/testing-events/)
- [Starlette TestClient](https://starlette.dev/testclient/)
- [Starlette Lifespan](https://www.starlette.dev/lifespan/)
- [HTTPX ASGI Transport](https://www.python-httpx.org/advanced/transports/)
- [pytest-asyncio Configuration](https://pytest-asyncio.readthedocs.io/en/latest/reference/configuration.html)
- [AnyIO Testing](https://anyio.readthedocs.io/en/stable/testing.html)
- [asgi-lifespan](https://github.com/florimondmanca/asgi-lifespan)

## Next Steps

- [Deployment](./15-deployment.md) - Deploy to production
- [Project: Todo API](./16-project-todo.md) - Complete project example

---

[Previous: Architecture](./13-architecture.md) | [Back to Index](./README.md) | [Next: Deployment](./15-deployment.md)
