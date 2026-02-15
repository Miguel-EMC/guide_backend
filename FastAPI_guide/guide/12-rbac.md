# Role-Based Access Control (RBAC)

This guide covers role and permission modeling, policy checks, resource ownership, multi-tenancy scoping, and advanced patterns for production-grade authorization in FastAPI.

## RBAC Concepts

| Concept | Description |
|---------|-------------|
| Role | A named collection of permissions (admin, editor, viewer) |
| Permission | A specific action (user:read, post:delete) |
| Resource | What the permission applies to (users, posts, settings) |
| Policy | A rule that decides access for a specific context |

## RBAC vs ABAC vs PBAC

| Model | Best For | Tradeoff |
|-------|----------|----------|
| RBAC | Simple orgs and clear job roles | Coarse for complex rules |
| ABAC | Rules based on attributes (team, plan, region) | More complex to reason about |
| PBAC | Central policy engine | Extra infra and integration |

## Data Modeling Options

### Option A: Simple Role Column

Good for small apps with a small number of roles.

```python
from enum import Enum
from sqlalchemy import String
from sqlalchemy.orm import Mapped, mapped_column
from app.core.database import Base


class UserRole(str, Enum):
    ADMIN = "admin"
    MODERATOR = "moderator"
    USER = "user"


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(primary_key=True)
    email: Mapped[str] = mapped_column(String(100), unique=True)
    hashed_password: Mapped[str] = mapped_column(String(200))
    role: Mapped[str] = mapped_column(String(20), default=UserRole.USER)
    is_active: Mapped[bool] = mapped_column(default=True)
```

### Option B: Roles + Permissions (Many-to-Many)

Good for larger apps with flexible permissions.

```python
from sqlalchemy import Table, Column, ForeignKey, String
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.core.database import Base

role_permissions = Table(
    "role_permissions",
    Base.metadata,
    Column("role_id", ForeignKey("roles.id"), primary_key=True),
    Column("permission_id", ForeignKey("permissions.id"), primary_key=True),
)

user_roles = Table(
    "user_roles",
    Base.metadata,
    Column("user_id", ForeignKey("users.id"), primary_key=True),
    Column("role_id", ForeignKey("roles.id"), primary_key=True),
)


class Role(Base):
    __tablename__ = "roles"

    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(50), unique=True)
    permissions = relationship("Permission", secondary=role_permissions)


class Permission(Base):
    __tablename__ = "permissions"

    id: Mapped[int] = mapped_column(primary_key=True)
    code: Mapped[str] = mapped_column(String(100), unique=True)


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(primary_key=True)
    email: Mapped[str] = mapped_column(String(100), unique=True)
    hashed_password: Mapped[str] = mapped_column(String(200))
    roles = relationship("Role", secondary=user_roles)
```

## Permission Resolution

Compute permissions once per request and reuse them across dependencies.

```python
from functools import lru_cache
from typing import Iterable


def resolve_permissions(roles: Iterable[Role]) -> set[str]:
    perms: set[str] = set()
    for role in roles:
        for perm in role.permissions:
            perms.add(perm.code)
    return perms
```

## Dependency Patterns

### Permission Dependency

```python
from fastapi import Depends, HTTPException, status
from app.core.security import get_current_user


def require_permissions(*required: str):
    def checker(user = Depends(get_current_user)):
        user_permissions = getattr(user, "permissions", set())
        missing = [p for p in required if p not in user_permissions]
        if missing:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"Missing permissions: {', '.join(missing)}",
            )
        return user
    return checker


@app.delete("/users/{user_id}")
async def delete_user(
    user_id: int,
    user = Depends(require_permissions("user:delete")),
):
    return {"message": f"User {user_id} deleted"}
```

### Minimum Role Dependency

```python
ROLE_HIERARCHY = {
    "admin": 3,
    "moderator": 2,
    "user": 1,
}


def require_minimum_role(minimum_role: str):
    def checker(user = Depends(get_current_user)):
        if ROLE_HIERARCHY.get(user.role, 0) < ROLE_HIERARCHY.get(minimum_role, 0):
            raise HTTPException(status_code=403, detail="Insufficient role level")
        return user
    return checker
```

## OAuth2 Scopes (Built-in FastAPI Support)

Scopes map nicely to permissions when you use OAuth2.

```python
from fastapi import Security
from fastapi.security import OAuth2PasswordBearer, SecurityScopes

oauth2_scheme = OAuth2PasswordBearer(
    tokenUrl="/auth/token",
    scopes={"items:read": "Read items", "items:write": "Write items"},
)


async def get_current_user_with_scopes(
    security_scopes: SecurityScopes,
    token: str = Depends(oauth2_scheme),
):
    user = decode_token(token)
    token_scopes = set(user.get("scopes", []))
    required_scopes = set(security_scopes.scopes)
    if not required_scopes.issubset(token_scopes):
        raise HTTPException(status_code=403, detail="Not enough scopes")
    return user


@app.get("/items")
async def read_items(user = Security(get_current_user_with_scopes, scopes=["items:read"])):
    return []
```

## Resource Ownership (Object-Level Access)

Always enforce ownership checks for user-owned resources.

```python
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession


async def get_own_post(
    post_id: int,
    current_user = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(select(Post).where(Post.id == post_id))
    post = result.scalar_one_or_none()

    if not post:
        raise HTTPException(404, "Post not found")

    if current_user.role == "admin":
        return post

    if post.author_id != current_user.id:
        raise HTTPException(403, "Not authorized to access this post")

    return post
```

## Query Scoping (Prevent Data Leaks)

Prefer scoping queries so that users can never see rows they do not own.

```python
def scope_posts(stmt, user):
    if user.role == "admin":
        return stmt
    return stmt.where(Post.author_id == user.id)


@app.get("/posts")
async def list_posts(
    user = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    stmt = scope_posts(select(Post), user)
    result = await db.execute(stmt)
    return result.scalars().all()
```

## Multi-Tenancy

If your app supports multiple tenants, include `tenant_id` in every table and scope all queries by tenant.

```python
class TenantScoped:
    tenant_id: Mapped[int] = mapped_column(index=True)


async def scope_tenant(stmt, user):
    return stmt.where(Model.tenant_id == user.tenant_id)
```

## Policy Layer (Simple PBAC)

Centralize complex rules in a policy registry.

```python
POLICIES = {
    "post:delete": lambda user, post: user.role == "admin" or post.author_id == user.id,
    "post:update": lambda user, post: post.is_locked is False,
}


def enforce(policy: str, user, resource):
    allowed = POLICIES.get(policy, lambda *_: False)(user, resource)
    if not allowed:
        raise HTTPException(status_code=403, detail="Access denied")
```

## JWT Claims vs Database Checks

| Strategy | Benefit | Risk |
|----------|---------|------|
| Embed roles/permissions in token | Fewer DB calls | Token invalidation is harder |
| Always load from DB | Real-time changes | More database load |

A common approach is to include roles in the token and still query the DB for sensitive endpoints.

## Audit Logging

Log authorization decisions for compliance and incident response.

```python
logger.info(
    "authz_decision",
    user_id=current_user.id,
    action="post:delete",
    resource_id=post.id,
    allowed=True,
)
```

## Testing RBAC

```python
import pytest


@pytest.mark.parametrize("role,expected", [
    ("admin", 200),
    ("moderator", 403),
    ("user", 403),
])
def test_admin_only_endpoint(client, role, expected, make_user_with_role):
    user = make_user_with_role(role)
    token = login_as(user)
    response = client.get("/admin", headers={"Authorization": f"Bearer {token}"})
    assert response.status_code == expected
```

## Best Practices

- Default to least privilege and expand intentionally.
- Scope all queries at the database layer where possible.
- Use policy helpers to avoid authorization logic scattered across routes.
- Cache permissions per request, not globally.
- Add audit logs for sensitive actions.

## References

- [FastAPI Security](https://fastapi.tiangolo.com/tutorial/security/)
- [OWASP Access Control Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Access_Control_Cheat_Sheet.html)

## Next Steps

- [Project Architecture](./13-architecture.md) - Advanced patterns
- [Testing](./14-testing.md) - Test your RBAC implementation

---

[Previous: JWT Authentication](./11-jwt-authentication.md) | [Back to Index](./README.md) | [Next: Project Architecture](./13-architecture.md)
