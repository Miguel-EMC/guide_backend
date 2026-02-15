# Project: Todo List API

A complete Todo API applying authentication, CRUD, validation, testing, and production-ready patterns.

## Features

- User registration + JWT login
- CRUD for todos
- Filtering by completion
- User data isolation
- Async SQLAlchemy
- Test suite with async clients

## Project Structure

```
todo_api/
├── app/
│   ├── main.py
│   ├── core/
│   │   ├── config.py
│   │   ├── database.py
│   │   └── security.py
│   ├── models/
│   │   ├── user.py
│   │   └── todo.py
│   ├── schemas/
│   │   ├── user.py
│   │   └── todo.py
│   ├── services/
│   │   ├── user.py
│   │   └── todo.py
│   └── routers/
│       ├── auth.py
│       └── todos.py
├── tests/
│   ├── conftest.py
│   ├── test_auth.py
│   └── test_todos.py
├── pyproject.toml
└── .env
```

## Installation

### With uv

```bash
uv init
uv add fastapi[standard] sqlalchemy aiosqlite pydantic-settings
uv add pyjwt[crypto] pwdlib[argon2]
uv add --dev pytest pytest-asyncio httpx
```

### With pip

```bash
pip install "fastapi[standard]" sqlalchemy aiosqlite pydantic-settings
pip install "pyjwt[crypto]" "pwdlib[argon2]"
pip install pytest pytest-asyncio httpx
```

## Core Configuration

### core/config.py

```python
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "Todo API"
    debug: bool = False
    database_url: str = "sqlite+aiosqlite:///./todos.db"

    secret_key: str = "change-me"
    algorithm: str = "HS256"
    access_token_expire_minutes: int = 30

    model_config = SettingsConfigDict(env_file=".env")


settings = Settings()
```

### core/database.py

```python
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from sqlalchemy.orm import DeclarativeBase
from app.core.config import settings

engine = create_async_engine(settings.database_url, echo=settings.debug)
AsyncSessionLocal = async_sessionmaker(engine, expire_on_commit=False)


class Base(DeclarativeBase):
    pass


async def get_db() -> AsyncSession:
    async with AsyncSessionLocal() as session:
        yield session


async def init_db() -> None:
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
```

### core/security.py

```python
from datetime import datetime, timedelta, timezone
import jwt
from jwt import InvalidTokenError
from pwdlib import PasswordHash
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.config import settings
from app.core.database import get_db

password_hash = PasswordHash.recommended()
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="auth/token")


def hash_password(password: str) -> str:
    return password_hash.hash(password)


def verify_password(password: str, password_hash_value: str) -> bool:
    return password_hash.verify(password, password_hash_value)


def create_access_token(subject: str) -> str:
    expire = datetime.now(timezone.utc) + timedelta(
        minutes=settings.access_token_expire_minutes
    )
    payload = {"sub": subject, "exp": expire}
    return jwt.encode(payload, settings.secret_key, algorithm=settings.algorithm)


def decode_token(token: str) -> dict:
    try:
        return jwt.decode(token, settings.secret_key, algorithms=[settings.algorithm])
    except InvalidTokenError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token",
            headers={"WWW-Authenticate": "Bearer"},
        )


async def get_current_user(
    token: str = Depends(oauth2_scheme),
    db: AsyncSession = Depends(get_db),
):
    payload = decode_token(token)
    email = payload.get("sub")
    if not email:
        raise HTTPException(status_code=401, detail="Invalid token")

    from app.models.user import User

    result = await db.execute(select(User).where(User.email == email))
    user = result.scalar_one_or_none()
    if not user:
        raise HTTPException(status_code=401, detail="User not found")
    return user
```

## Models

### models/user.py

```python
from sqlalchemy import String, Boolean
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.core.database import Base


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(primary_key=True)
    email: Mapped[str] = mapped_column(String(100), unique=True, index=True)
    hashed_password: Mapped[str] = mapped_column(String(200))
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)

    todos: Mapped[list["Todo"]] = relationship(
        back_populates="owner",
        cascade="all, delete-orphan",
    )
```

### models/todo.py

```python
from datetime import datetime, timezone
from sqlalchemy import String, Boolean, ForeignKey, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.core.database import Base


class Todo(Base):
    __tablename__ = "todos"

    id: Mapped[int] = mapped_column(primary_key=True)
    title: Mapped[str] = mapped_column(String(200))
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    completed: Mapped[bool] = mapped_column(Boolean, default=False)
    created_at: Mapped[datetime] = mapped_column(default=lambda: datetime.now(timezone.utc))
    owner_id: Mapped[int] = mapped_column(ForeignKey("users.id"))

    owner: Mapped["User"] = relationship(back_populates="todos")
```

## Schemas

### schemas/user.py

```python
from pydantic import BaseModel, EmailStr, ConfigDict


class UserCreate(BaseModel):
    email: EmailStr
    password: str


class UserRead(BaseModel):
    id: int
    email: EmailStr
    is_active: bool

    model_config = ConfigDict(from_attributes=True)


class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"
```

### schemas/todo.py

```python
from pydantic import BaseModel, Field, ConfigDict
from datetime import datetime


class TodoCreate(BaseModel):
    title: str = Field(min_length=1, max_length=200)
    description: str | None = None


class TodoUpdate(BaseModel):
    title: str | None = Field(default=None, min_length=1, max_length=200)
    description: str | None = None
    completed: bool | None = None


class TodoRead(BaseModel):
    id: int
    title: str
    description: str | None
    completed: bool
    created_at: datetime
    owner_id: int

    model_config = ConfigDict(from_attributes=True)
```

## Services

### services/user.py

```python
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from fastapi import HTTPException
from app.models.user import User
from app.schemas.user import UserCreate
from app.core.security import hash_password


async def get_user_by_email(db: AsyncSession, email: str) -> User | None:
    result = await db.execute(select(User).where(User.email == email))
    return result.scalar_one_or_none()


async def create_user(db: AsyncSession, payload: UserCreate) -> User:
    if await get_user_by_email(db, payload.email):
        raise HTTPException(status_code=400, detail="Email already registered")

    user = User(email=payload.email, hashed_password=hash_password(payload.password))
    db.add(user)
    await db.commit()
    await db.refresh(user)
    return user
```

### services/todo.py

```python
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from fastapi import HTTPException
from app.models.todo import Todo
from app.models.user import User
from app.schemas.todo import TodoCreate, TodoUpdate


async def list_todos(
    db: AsyncSession,
    user: User,
    completed: bool | None = None,
):
    stmt = select(Todo).where(Todo.owner_id == user.id)
    if completed is not None:
        stmt = stmt.where(Todo.completed == completed)
    result = await db.execute(stmt)
    return result.scalars().all()


async def get_todo(db: AsyncSession, todo_id: int, user: User) -> Todo:
    result = await db.execute(
        select(Todo).where(Todo.id == todo_id, Todo.owner_id == user.id)
    )
    todo = result.scalar_one_or_none()
    if not todo:
        raise HTTPException(status_code=404, detail="Todo not found")
    return todo


async def create_todo(db: AsyncSession, payload: TodoCreate, user: User) -> Todo:
    todo = Todo(**payload.model_dump(), owner_id=user.id)
    db.add(todo)
    await db.commit()
    await db.refresh(todo)
    return todo


async def update_todo(db: AsyncSession, todo_id: int, payload: TodoUpdate, user: User) -> Todo:
    todo = await get_todo(db, todo_id, user)
    for field, value in payload.model_dump(exclude_unset=True).items():
        setattr(todo, field, value)
    await db.commit()
    await db.refresh(todo)
    return todo


async def delete_todo(db: AsyncSession, todo_id: int, user: User) -> None:
    todo = await get_todo(db, todo_id, user)
    await db.delete(todo)
    await db.commit()
```

## Routers

### routers/auth.py

```python
from typing import Annotated
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.core.security import verify_password, create_access_token, get_current_user
from app.schemas.user import UserCreate, UserRead, Token
from app.services import user as user_service

router = APIRouter(prefix="/auth", tags=["Auth"])


@router.post("/register", response_model=UserRead, status_code=201)
async def register(payload: UserCreate, db: AsyncSession = Depends(get_db)):
    return await user_service.create_user(db, payload)


@router.post("/token", response_model=Token)
async def login(
    form_data: Annotated[OAuth2PasswordRequestForm, Depends()],
    db: AsyncSession = Depends(get_db),
):
    user = await user_service.get_user_by_email(db, form_data.username)
    if not user or not verify_password(form_data.password, user.hashed_password):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid credentials")

    token = create_access_token(subject=user.email)
    return Token(access_token=token)


@router.get("/me", response_model=UserRead)
async def me(current_user = Depends(get_current_user)):
    return current_user
```

### routers/todos.py

```python
from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.core.security import get_current_user
from app.models.user import User
from app.schemas.todo import TodoCreate, TodoUpdate, TodoRead
from app.services import todo as todo_service

router = APIRouter(prefix="/todos", tags=["Todos"])


@router.get("/", response_model=list[TodoRead])
async def list_todos(
    completed: bool | None = Query(default=None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    return await todo_service.list_todos(db, current_user, completed)


@router.post("/", response_model=TodoRead, status_code=status.HTTP_201_CREATED)
async def create_todo(
    payload: TodoCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    return await todo_service.create_todo(db, payload, current_user)


@router.get("/{todo_id}", response_model=TodoRead)
async def get_todo(
    todo_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    return await todo_service.get_todo(db, todo_id, current_user)


@router.patch("/{todo_id}", response_model=TodoRead)
async def update_todo(
    todo_id: int,
    payload: TodoUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    return await todo_service.update_todo(db, todo_id, payload, current_user)


@router.delete("/{todo_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_todo(
    todo_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    await todo_service.delete_todo(db, todo_id, current_user)
```

## Main Application

```python
from contextlib import asynccontextmanager
from fastapi import FastAPI
from app.core.database import init_db
from app.core.config import settings
from app.routers import auth, todos


@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_db()
    yield


app = FastAPI(title=settings.app_name, lifespan=lifespan)
app.include_router(auth.router)
app.include_router(todos.router)


@app.get("/health")
async def health():
    return {"status": "ok"}
```

## Tests (Async)

### tests/conftest.py

```python
import pytest
from httpx import AsyncClient, ASGITransport
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker
from sqlalchemy.pool import StaticPool

from app.main import app
from app.core.database import Base, get_db

TEST_DATABASE_URL = "sqlite+aiosqlite:///:memory:"

engine = create_async_engine(
    TEST_DATABASE_URL,
    connect_args={"check_same_thread": False},
    poolclass=StaticPool,
)
AsyncSessionLocal = async_sessionmaker(engine, expire_on_commit=False)


@pytest.fixture
async def db_session():
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    async with AsyncSessionLocal() as session:
        yield session
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)


@pytest.fixture
async def async_client(db_session):
    async def override_get_db():
        yield db_session

    app.dependency_overrides[get_db] = override_get_db
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        yield client
    app.dependency_overrides.clear()
```

### tests/test_todos.py

```python
import pytest


@pytest.mark.anyio
async def test_create_todo(async_client, auth_headers):
    response = await async_client.post(
        "/todos/",
        json={"title": "Test", "description": "x"},
        headers=auth_headers,
    )
    assert response.status_code == 201


@pytest.mark.anyio
async def test_list_todos(async_client, auth_headers):
    await async_client.post("/todos/", json={"title": "Todo 1"}, headers=auth_headers)
    response = await async_client.get("/todos/", headers=auth_headers)
    assert response.status_code == 200
```

### tests/test_auth.py

```python
import pytest


@pytest.mark.anyio
async def test_register_and_login(async_client):
    await async_client.post(
        "/auth/register",
        json={"email": "test@example.com", "password": "secret123"},
    )

    response = await async_client.post(
        "/auth/token",
        data={"username": "test@example.com", "password": "secret123"},
    )

    assert response.status_code == 200
    assert "access_token" in response.json()
```

### tests/fixtures

Add this to `tests/conftest.py`:

```python
@pytest.fixture
async def auth_headers(async_client):
    await async_client.post(
        "/auth/register",
        json={"email": "test@example.com", "password": "testpassword"},
    )
    response = await async_client.post(
        "/auth/token",
        data={"username": "test@example.com", "password": "testpassword"},
    )
    token = response.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}
```

## Running the Project

```bash
uv run uvicorn app.main:app --reload
pytest -v
```

## Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/register` | Register user |
| POST | `/auth/token` | Login (OAuth2 form) |
| GET | `/auth/me` | Current user |
| GET | `/todos/` | List todos |
| POST | `/todos/` | Create todo |
| GET | `/todos/{id}` | Get todo |
| PATCH | `/todos/{id}` | Update todo |
| DELETE | `/todos/{id}` | Delete todo |

## References

- [FastAPI](https://fastapi.tiangolo.com/)
- [SQLAlchemy Async](https://docs.sqlalchemy.org/en/20/orm/extensions/asyncio.html)
- [PyJWT](https://pyjwt.readthedocs.io/)
- [pwdlib](https://pwdlib.readthedocs.io/)

---

[Previous: Deployment](./15-deployment.md) | [Back to Index](./README.md) | [Next: GenAI Integration](./17-genai-integration.md)
