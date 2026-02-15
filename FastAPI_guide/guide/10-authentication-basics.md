# Authentication Basics

This guide covers HTTP Basic, API keys, bearer tokens, session cookies, and the OAuth2 password flow setup in FastAPI.

## Installation

```bash
pip install "fastapi[standard]"
# Required for OAuth2PasswordRequestForm (form data)
pip install python-multipart
```

## Authentication Methods

| Method | Use Case | Notes |
|--------|----------|-------|
| HTTP Basic | Internal tools | Sends credentials on every request |
| API Key | Server-to-server | Simple, rotates easily |
| HTTP Bearer | Simple tokens | Not OAuth2, but common |
| OAuth2 Password | APIs + clients | Standardized flow for token login |
| Session Cookies | Web apps | Requires server-side session store |

## HTTP Basic Authentication

```python
from typing import Annotated
import secrets
from fastapi import Depends, FastAPI, HTTPException, status
from fastapi.security import HTTPBasic, HTTPBasicCredentials

app = FastAPI()
security = HTTPBasic()


def verify_credentials(
    credentials: Annotated[HTTPBasicCredentials, Depends(security)],
) -> str:
    is_user = secrets.compare_digest(credentials.username, "admin")
    is_pass = secrets.compare_digest(credentials.password, "secret")
    if not (is_user and is_pass):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid credentials",
            headers={"WWW-Authenticate": "Basic"},
        )
    return credentials.username


@app.get("/protected")
async def protected_route(user: str = Depends(verify_credentials)):
    return {"user": user}
```

## API Key Authentication

FastAPI provides API key helpers for headers, query params, and cookies.

### Header-based API Key

```python
from typing import Annotated
from fastapi import Depends, FastAPI, HTTPException, Security
from fastapi.security import APIKeyHeader

app = FastAPI()

API_KEY = "your-secret"
api_key_header = APIKeyHeader(name="X-API-Key")


def get_api_key(api_key: Annotated[str, Security(api_key_header)]) -> str:
    if api_key != API_KEY:
        raise HTTPException(status_code=403, detail="Could not validate API key")
    return api_key


@app.get("/data")
async def read_data(api_key: str = Depends(get_api_key)):
    return {"ok": True}
```

### API Key Locations

- `APIKeyHeader` for headers
- `APIKeyQuery` for query string
- `APIKeyCookie` for cookies

## HTTP Bearer Tokens

Use `HTTPBearer` when you want a simple Bearer token without OAuth2.

```python
from typing import Annotated
from fastapi import Depends, FastAPI, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

app = FastAPI()
http_bearer = HTTPBearer()


def get_token(
    credentials: Annotated[HTTPAuthorizationCredentials, Depends(http_bearer)],
) -> str:
    if credentials.scheme.lower() != "bearer":
        raise HTTPException(401, "Invalid auth scheme")
    return credentials.credentials


@app.get("/secure")
async def secure(token: str = Depends(get_token)):
    return {"token": token}
```

## OAuth2 Password Flow (Basic Setup)

OAuth2 password flow requires `username` and `password` sent as form fields. Use `OAuth2PasswordRequestForm` for the login endpoint.

```python
from typing import Annotated
from fastapi import Depends, FastAPI, HTTPException, status
from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm

app = FastAPI()

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="token")

# Fake user store
fake_users = {"johndoe": "secret"}


@app.post("/token")
async def login(form_data: Annotated[OAuth2PasswordRequestForm, Depends()]):
    if fake_users.get(form_data.username) != form_data.password:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return {"access_token": form_data.username, "token_type": "bearer"}


@app.get("/users/me")
async def read_users_me(token: Annotated[str, Depends(oauth2_scheme)]):
    return {"token": token}
```

### Strict Form Validation

OAuth2 spec requires a `grant_type=password` field. Use `OAuth2PasswordRequestFormStrict` if you want to enforce it.

## Session Cookies (Server-side)

Cookie-based sessions are common for web apps. Store sessions in Redis or your DB.

```python
from fastapi import Cookie, FastAPI, HTTPException, Response

app = FastAPI()

sessions = {}


@app.post("/login")
async def login(response: Response):
    session_id = "session-123"
    sessions[session_id] = {"user": "johndoe"}
    response.set_cookie("session_id", session_id, httponly=True)
    return {"ok": True}


@app.get("/me")
async def me(session_id: str | None = Cookie(default=None)):
    if not session_id or session_id not in sessions:
        raise HTTPException(401, "Not authenticated")
    return sessions[session_id]
```

## Best Practices

- Always use HTTPS in production.
- Do not store plaintext passwords.
- Use `secrets.compare_digest()` for credential comparisons.
- Store secrets in environment variables.

## References

- [Security Tools](https://fastapi.tiangolo.com/reference/security/)
- [Simple OAuth2 with Password and Bearer](https://fastapi.tiangolo.com/tutorial/security/simple-oauth2/)
- [Form Data](https://fastapi.tiangolo.com/tutorial/request-forms/)

## Next Steps

- [JWT Authentication](./11-jwt-authentication.md) - Token-based auth
- [Role-Based Access](./12-rbac.md) - Permission control

---

[Previous: Database Relationships](./09-database-relationships.md) | [Back to Index](./README.md) | [Next: JWT Authentication](./11-jwt-authentication.md)
