# Database Setup and Configuration

FastAPI is database-agnostic. This guide covers SQLModel (as shown in the FastAPI docs), SQLAlchemy 2.0 async setup, and practical patterns for dependencies and startup.

## Overview

| Approach | ORM/Toolkit | Use Case |
|----------|-------------|----------|
| SQLModel | Built on SQLAlchemy | FastAPI docs tutorial and quick start |
| SQLAlchemy 2.0 | Core ORM | Full control, sync or async |
| Tortoise ORM | Async ORM | Async-first alternative |
| Motor/Beanie | MongoDB | NoSQL workloads |

## Project Structure

```
project/
├── app/
│   ├── core/
│   │   ├── database.py
│   │   └── config.py
│   ├── models/
│   └── main.py
├── .env
└── pyproject.toml
```

## SQLModel Quick Start (Sync)

FastAPI's SQL database tutorial uses SQLModel. It provides a clean API and is built on SQLAlchemy.

### Installation

```bash
pip install sqlmodel
# or
uv add sqlmodel
```

### Engine and Session

```python
from sqlmodel import SQLModel, Field, Session, create_engine

sqlite_file_name = "database.db"
sqlite_url = f"sqlite:///{sqlite_file_name}"

connect_args = {"check_same_thread": False}
engine = create_engine(sqlite_url, connect_args=connect_args)


class Hero(SQLModel, table=True):
    id: int | None = Field(default=None, primary_key=True)
    name: str
    secret_name: str


def create_db_and_tables() -> None:
    SQLModel.metadata.create_all(engine)


def get_session():
    with Session(engine) as session:
        yield session
```

### Lifespan Startup

FastAPI recommends using lifespan events for startup and shutdown logic.

```python
from contextlib import asynccontextmanager
from fastapi import FastAPI


@asynccontextmanager
async def lifespan(app: FastAPI):
    create_db_and_tables()
    yield


app = FastAPI(lifespan=lifespan)
```

## SQLAlchemy 2.0 Async Setup

For async apps, use `create_async_engine` and `async_sessionmaker`.

### Installation

```bash
uv add sqlalchemy asyncpg
# or
pip install sqlalchemy asyncpg
```

### Database Configuration

```python
from sqlalchemy.ext.asyncio import (
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)

DATABASE_URL = "postgresql+asyncpg://user:pass@localhost:5432/app"

engine = create_async_engine(
    DATABASE_URL,
    echo=False,
    pool_pre_ping=True,
)

AsyncSessionLocal = async_sessionmaker(engine, expire_on_commit=False)


async def get_db() -> AsyncSession:
    async with AsyncSessionLocal() as session:
        yield session
```

### Async Lifespan Cleanup

```python
from contextlib import asynccontextmanager
from fastapi import FastAPI


@asynccontextmanager
async def lifespan(app: FastAPI):
    yield
    await engine.dispose()


app = FastAPI(lifespan=lifespan)
```

## Dependency Injection Pattern

```python
from typing import Annotated
from fastapi import Depends
from sqlalchemy.ext.asyncio import AsyncSession

DbSession = Annotated[AsyncSession, Depends(get_db)]


@app.get("/users/")
async def read_users(db: DbSession):
    result = await db.execute(select(User))
    return result.scalars().all()
```

## Migrations

For production, use Alembic to manage schema changes. SQLModel also recommends Alembic for migrations.

## Best Practices

- Prefer SQLModel for simple projects; use SQLAlchemy for full control.
- Use lifespan for startup/shutdown instead of `@app.on_event`.
- Keep DB sessions scoped to each request via dependencies.
- Use async drivers for async apps (e.g., `asyncpg` for PostgreSQL, `aiosqlite` for SQLite).

## References

- [FastAPI SQL Databases (SQLModel)](https://fastapi.tiangolo.com/tutorial/sql-databases/)
- [FastAPI Lifespan Events](https://fastapi.tiangolo.com/advanced/events/)
- [SQLAlchemy Asyncio](https://docs.sqlalchemy.org/en/20/orm/extensions/asyncio.html)

## Next Steps

- [CRUD Operations](./08-crud-operations.md) - Standard database patterns
- [Database Relationships](./09-database-relationships.md) - Model relationships

---

[Previous: Error Handling](./06-error-handling.md) | [Back to Index](./README.md) | [Next: CRUD Operations](./08-crud-operations.md)
