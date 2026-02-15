# Database Relationships

This guide covers one-to-one, one-to-many, and many-to-many relationships using SQLAlchemy 2.0, plus loading strategies to avoid N+1 queries.

## Relationship Types

| Type | Example | Description |
|------|---------|-------------|
| One-to-One | User → Profile | Each user has a single profile |
| One-to-Many | User → Posts | One user has many posts |
| Many-to-Many | Post ↔ Tag | Posts and tags are linked by a join table |

## One-to-One

```python
from sqlalchemy import String, ForeignKey
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.core.database import Base


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(primary_key=True)
    username: Mapped[str] = mapped_column(String(50), unique=True)

    profile: Mapped["Profile"] = relationship(
        back_populates="user",
        uselist=False,
        lazy="selectin",
    )


class Profile(Base):
    __tablename__ = "profiles"

    id: Mapped[int] = mapped_column(primary_key=True)
    bio: Mapped[str | None] = mapped_column(String(500))
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), unique=True)

    user: Mapped["User"] = relationship(back_populates="profile")
```

## One-to-Many

```python
from sqlalchemy import ForeignKey, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(primary_key=True)
    username: Mapped[str] = mapped_column(String(50), unique=True)

    posts: Mapped[list["Post"]] = relationship(
        back_populates="author",
        cascade="all, delete-orphan",
        lazy="selectin",
    )


class Post(Base):
    __tablename__ = "posts"

    id: Mapped[int] = mapped_column(primary_key=True)
    title: Mapped[str] = mapped_column(String(200))
    content: Mapped[str] = mapped_column(Text)
    author_id: Mapped[int] = mapped_column(ForeignKey("users.id"))

    author: Mapped["User"] = relationship(back_populates="posts")
```

## Many-to-Many

```python
from sqlalchemy import Table, Column, ForeignKey, Integer, String
from sqlalchemy.orm import relationship

post_tags = Table(
    "post_tags",
    Base.metadata,
    Column("post_id", Integer, ForeignKey("posts.id"), primary_key=True),
    Column("tag_id", Integer, ForeignKey("tags.id"), primary_key=True),
)


class Post(Base):
    __tablename__ = "posts"

    id: Mapped[int] = mapped_column(primary_key=True)
    title: Mapped[str] = mapped_column(String(200))

    tags: Mapped[list["Tag"]] = relationship(
        secondary=post_tags,
        back_populates="posts",
        lazy="selectin",
    )


class Tag(Base):
    __tablename__ = "tags"

    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(50), unique=True)

    posts: Mapped[list["Post"]] = relationship(
        secondary=post_tags,
        back_populates="tags",
    )
```

## Association Object (Extra Columns)

Use a mapped association table when you need extra metadata.

```python
from datetime import datetime, timezone


class PostTag(Base):
    __tablename__ = "post_tags"

    post_id: Mapped[int] = mapped_column(ForeignKey("posts.id"), primary_key=True)
    tag_id: Mapped[int] = mapped_column(ForeignKey("tags.id"), primary_key=True)
    added_at: Mapped[datetime] = mapped_column(default=lambda: datetime.now(timezone.utc))

    post: Mapped["Post"] = relationship(back_populates="post_tags")
    tag: Mapped["Tag"] = relationship(back_populates="post_tags")


class Post(Base):
    __tablename__ = "posts"

    id: Mapped[int] = mapped_column(primary_key=True)
    title: Mapped[str] = mapped_column(String(200))

    post_tags: Mapped[list["PostTag"]] = relationship(back_populates="post")
```

## Loading Strategies

### Eager Loading

Use `selectinload` for collections and `joinedload` for single-object relationships to avoid N+1 queries.

```python
from sqlalchemy import select
from sqlalchemy.orm import selectinload, joinedload

# For collections
stmt = select(User).options(selectinload(User.posts))

# For single related rows
stmt = select(User).options(joinedload(User.profile))
```

### Avoid Implicit IO in Async

AsyncSession will perform implicit IO on lazy-loaded attributes. For async apps, prefer eager loading or use `lazy="raise"` to prevent accidental lazy loads.

```python
class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(primary_key=True)
    posts: Mapped[list["Post"]] = relationship(lazy="raise")
```

## N+1 Example

```python
# Bad
result = await db.execute(select(User))
for user in result.scalars():
    print(user.posts)  # triggers extra query per user

# Good
result = await db.execute(select(User).options(selectinload(User.posts)))
for user in result.scalars():
    print(user.posts)
```

## Summary

| Pattern | Use Case |
|---------|----------|
| `uselist=False` | One-to-one |
| `cascade="all, delete-orphan"` | Clean up child rows |
| `selectinload` | Efficient collection loading |
| `joinedload` | Efficient single-row joins |
| `lazy="raise"` | Prevent implicit IO in async |

## References

- [SQLAlchemy Relationships](https://docs.sqlalchemy.org/en/20/orm/relationship_api.html)
- [SQLAlchemy Loading Techniques](https://docs.sqlalchemy.org/en/20/orm/queryguide/relationships.html)
- [AsyncSession and Implicit IO](https://docs.sqlalchemy.org/en/20/orm/extensions/asyncio.html#preventing-implicit-io-when-using-asyncsession)

## Next Steps

- [Authentication](./10-authentication-basics.md) - Protect your API
- [Project Architecture](./13-architecture.md) - Organize large projects

---

[Previous: CRUD Operations](./08-crud-operations.md) | [Back to Index](./README.md) | [Next: Database Migrations](./09a-database-migrations.md)
