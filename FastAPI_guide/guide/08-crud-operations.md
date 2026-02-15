# CRUD Operations

This guide covers Create, Read, Update, Delete patterns, pagination, filtering, and transaction handling using SQLAlchemy 2.0 async sessions.

## CRUD Blueprint

| Operation | HTTP Method | Route | Status Code |
|-----------|-------------|-------|-------------|
| List | GET | `/resources` | 200 |
| Get One | GET | `/resources/{id}` | 200 |
| Create | POST | `/resources` | 201 |
| Update (full) | PUT | `/resources/{id}` | 200 |
| Update (partial) | PATCH | `/resources/{id}` | 200 |
| Delete | DELETE | `/resources/{id}` | 204 |

## Project Structure

```
app/
├── models/
│   └── book.py
├── schemas/
│   └── book.py
├── services/
│   └── book.py
├── routers/
│   └── book.py
└── main.py
```

## Models (SQLAlchemy 2.0)

```python
# models/book.py
from sqlalchemy import String, Integer, Text
from sqlalchemy.orm import Mapped, mapped_column
from app.core.database import Base


class Book(Base):
    __tablename__ = "books"

    id: Mapped[int] = mapped_column(primary_key=True)
    title: Mapped[str] = mapped_column(String(200), index=True)
    author: Mapped[str] = mapped_column(String(100))
    year: Mapped[int] = mapped_column(Integer)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
```

## Schemas (Pydantic)

```python
# schemas/book.py
from pydantic import BaseModel, Field, ConfigDict


class BookBase(BaseModel):
    title: str = Field(min_length=1, max_length=200)
    author: str = Field(min_length=1, max_length=100)
    year: int = Field(ge=0, le=2100)
    description: str | None = Field(default=None, max_length=2000)


class BookCreate(BookBase):
    pass


class BookUpdate(BaseModel):
    title: str | None = Field(default=None, min_length=1, max_length=200)
    author: str | None = Field(default=None, min_length=1, max_length=100)
    year: int | None = Field(default=None, ge=0, le=2100)
    description: str | None = None


class BookRead(BookBase):
    id: int

    model_config = ConfigDict(from_attributes=True)
```

## Service Layer (AsyncSession)

```python
# services/book.py
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from fastapi import HTTPException
from app.models.book import Book
from app.schemas.book import BookCreate, BookUpdate


async def list_books(db: AsyncSession, skip: int = 0, limit: int = 100) -> list[Book]:
    stmt = select(Book).offset(skip).limit(limit)
    result = await db.execute(stmt)
    return result.scalars().all()


async def get_book(db: AsyncSession, book_id: int) -> Book | None:
    return await db.get(Book, book_id)


async def create_book(db: AsyncSession, payload: BookCreate) -> Book:
    book = Book(**payload.model_dump())
    db.add(book)
    await db.commit()
    await db.refresh(book)
    return book


async def update_book(db: AsyncSession, book_id: int, payload: BookUpdate) -> Book:
    book = await get_book(db, book_id)
    if not book:
        raise HTTPException(status_code=404, detail="Book not found")

    update_data = payload.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(book, field, value)

    await db.commit()
    await db.refresh(book)
    return book


async def delete_book(db: AsyncSession, book_id: int) -> None:
    book = await get_book(db, book_id)
    if not book:
        raise HTTPException(status_code=404, detail="Book not found")

    await db.delete(book)
    await db.commit()
```

## Router Layer

```python
# routers/book.py
from typing import Annotated
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.schemas.book import BookCreate, BookUpdate, BookRead
from app.services import book as service

router = APIRouter(prefix="/books", tags=["Books"])

DbSession = Annotated[AsyncSession, Depends(get_db)]


@router.get("/", response_model=list[BookRead])
async def list_books(
    db: DbSession,
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=100),
):
    return await service.list_books(db, skip, limit)


@router.get("/{book_id}", response_model=BookRead)
async def get_book(book_id: int, db: DbSession):
    book = await service.get_book(db, book_id)
    if not book:
        raise HTTPException(status_code=404, detail="Book not found")
    return book


@router.post("/", response_model=BookRead, status_code=status.HTTP_201_CREATED)
async def create_book(payload: BookCreate, db: DbSession):
    return await service.create_book(db, payload)


@router.patch("/{book_id}", response_model=BookRead)
async def patch_book(book_id: int, payload: BookUpdate, db: DbSession):
    return await service.update_book(db, book_id, payload)


@router.delete("/{book_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_book(book_id: int, db: DbSession):
    await service.delete_book(db, book_id)
```

## Pagination with Total Count

```python
from sqlalchemy import func, select
from pydantic import BaseModel


class Page(BaseModel):
    items: list[BookRead]
    total: int
    skip: int
    limit: int


async def list_books_with_total(db: AsyncSession, skip: int, limit: int) -> Page:
    total_stmt = select(func.count()).select_from(Book)
    total = (await db.execute(total_stmt)).scalar_one()

    data_stmt = select(Book).offset(skip).limit(limit)
    items = (await db.execute(data_stmt)).scalars().all()

    return Page(items=items, total=total, skip=skip, limit=limit)
```

## Filtering and Search

```python
from sqlalchemy import select


async def search_books(
    db: AsyncSession,
    author: str | None = None,
    year: int | None = None,
    query: str | None = None,
    skip: int = 0,
    limit: int = 50,
):
    stmt = select(Book)

    if author:
        stmt = stmt.where(Book.author == author)
    if year:
        stmt = stmt.where(Book.year == year)
    if query:
        stmt = stmt.where(Book.title.ilike(f"%{query}%"))

    stmt = stmt.offset(skip).limit(limit)
    result = await db.execute(stmt)
    return result.scalars().all()
```

## Transactions

Use `session.begin()` to group multiple writes with automatic commit/rollback.

```python
from sqlalchemy.ext.asyncio import AsyncSession


async def create_order_with_items(db: AsyncSession, order, items):
    async with db.begin():
        db.add(order)
        await db.flush()  # assigns order.id

        for item in items:
            db.add(item)
```

## Soft Delete

```python
from datetime import datetime, timezone
from sqlalchemy import Boolean


class Book(Base):
    __tablename__ = "books"

    id: Mapped[int] = mapped_column(primary_key=True)
    title: Mapped[str]
    is_deleted: Mapped[bool] = mapped_column(Boolean, default=False)
    deleted_at: Mapped[datetime | None] = mapped_column(nullable=True)


async def soft_delete_book(db: AsyncSession, book_id: int) -> None:
    book = await get_book(db, book_id)
    if not book:
        raise HTTPException(404, "Book not found")

    book.is_deleted = True
    book.deleted_at = datetime.now(timezone.utc)
    await db.commit()
```

## Best Practices

- Keep session scope per request.
- Use `PATCH` with `exclude_unset=True` for partial updates.
- Use `async with session.begin()` for multi-step writes.
- Use pagination (and total count if needed).
- Favor service layers for business logic.

## Summary

| Pattern | Purpose |
|---------|---------|
| Service layer | Isolate business logic |
| Pagination | Control response size |
| Transactions | Maintain data integrity |
| Soft delete | Preserve data history |

## References

- [SQLAlchemy AsyncSession](https://docs.sqlalchemy.org/en/20/orm/extensions/asyncio.html)
- [SQLAlchemy ORM Querying](https://docs.sqlalchemy.org/en/20/orm/queryguide/index.html)

## Next Steps

- [Database Relationships](./09-database-relationships.md) - Model relationships
- [Database Migrations](./09a-database-migrations.md) - Manage schema changes

---

[Previous: Database Setup](./07-database-setup.md) | [Back to Index](./README.md) | [Next: Database Migrations](./09a-database-migrations.md)
