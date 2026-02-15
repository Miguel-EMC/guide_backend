# JWT Authentication

This guide follows the current FastAPI OAuth2 + JWT tutorial using PyJWT and pwdlib for hashing.

## What is JWT?

JWT (JSON Web Token) is a signed token format. It is **not encrypted**, so do not store sensitive data inside the payload.

## Installation

```bash
pip install "fastapi[standard]" python-multipart
pip install "pyjwt[crypto]" "pwdlib[argon2]"
```

- `python-multipart` is required for `OAuth2PasswordRequestForm`.
- `pyjwt[crypto]` enables RSA/ECDSA algorithms. Use plain `pyjwt` for HS256.
- `pwdlib[argon2]` provides modern password hashing.

## Project Layout

```
app/
├── core/
│   ├── config.py
│   └── security.py
├── models/
├── schemas/
│   └── auth.py
├── routers/
│   └── auth.py
└── main.py
```

## Configuration

```python
# core/config.py
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    secret_key: str = "change-me"
    algorithm: str = "HS256"
    access_token_expire_minutes: int = 30

    model_config = SettingsConfigDict(env_file=".env")


settings = Settings()
```

## Security Utilities

```python
# core/security.py
from datetime import datetime, timedelta, timezone
from typing import Any
import jwt
from jwt import InvalidTokenError
from pwdlib import PasswordHash
from fastapi import HTTPException, status
from app.core.config import settings

password_hash = PasswordHash.recommended()
DUMMY_HASH = password_hash.hash("not-the-password")


def hash_password(password: str) -> str:
    return password_hash.hash(password)


def verify_password(password: str, password_hash_value: str) -> bool:
    return password_hash.verify(password, password_hash_value)


def verify_password_and_mitigate_timing(password: str, password_hash_value: str | None) -> bool:
    if password_hash_value is None:
        password_hash.verify(password, DUMMY_HASH)
        return False
    return password_hash.verify(password, password_hash_value)


def create_access_token(subject: str) -> str:
    expire = datetime.now(timezone.utc) + timedelta(
        minutes=settings.access_token_expire_minutes
    )
    payload = {"sub": subject, "exp": expire}
    return jwt.encode(payload, settings.secret_key, algorithm=settings.algorithm)


def decode_token(token: str) -> dict[str, Any]:
    try:
        return jwt.decode(token, settings.secret_key, algorithms=[settings.algorithm])
    except InvalidTokenError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Could not validate credentials",
            headers={"WWW-Authenticate": "Bearer"},
        )
```

## Schemas

```python
# schemas/auth.py
from pydantic import BaseModel


class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"


class TokenData(BaseModel):
    username: str | None = None
```

## Auth Routes

```python
# routers/auth.py
from typing import Annotated
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm
from app.core.security import (
    create_access_token,
    decode_token,
    hash_password,
    verify_password_and_mitigate_timing,
)
from app.schemas.auth import Token

router = APIRouter(prefix="/auth", tags=["Auth"])

# OAuth2 password flow
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="auth/token")

# Fake user DB
users_db = {
    "johndoe": {"username": "johndoe", "hashed_password": hash_password("secret")},
}


@router.post("/token", response_model=Token)
async def login(form_data: Annotated[OAuth2PasswordRequestForm, Depends()]):
    user = users_db.get(form_data.username)
    if not user:
        verify_password_and_mitigate_timing(form_data.password, None)
        raise HTTPException(status_code=400, detail="Incorrect username or password")

    if not verify_password_and_mitigate_timing(
        form_data.password, user["hashed_password"]
    ):
        raise HTTPException(status_code=400, detail="Incorrect username or password")

    token = create_access_token(subject=user["username"])
    return Token(access_token=token)


def get_current_user(token: Annotated[str, Depends(oauth2_scheme)]):
    payload = decode_token(token)
    username = payload.get("sub")
    if not username:
        raise HTTPException(status_code=401, detail="Invalid token")
    user = users_db.get(username)
    if not user:
        raise HTTPException(status_code=401, detail="User not found")
    return user
```

## Protect Routes

```python
from fastapi import Depends
from app.routers.auth import get_current_user


@app.get("/me")
async def read_me(user: dict = Depends(get_current_user)):
    return user
```

## Best Practices

- Keep access tokens short-lived (15-30 minutes).
- Rotate and protect your `SECRET_KEY`.
- Do not store tokens in localStorage for web apps; prefer HttpOnly cookies.
- Add refresh tokens only if you can store and revoke them securely.

## References

- [OAuth2 Password + JWT](https://fastapi.tiangolo.com/tutorial/security/oauth2-jwt/)
- [Simple OAuth2 with Password and Bearer](https://fastapi.tiangolo.com/tutorial/security/simple-oauth2/)
- [Security Tools](https://fastapi.tiangolo.com/reference/security/)

## Next Steps

- [Role-Based Access](./12-rbac.md) - Permission control
- [Project Architecture](./13-architecture.md) - Structuring larger apps

---

[Previous: Basic Authentication](./10-authentication-basics.md) | [Back to Index](./README.md) | [Next: Role-Based Access](./12-rbac.md)
