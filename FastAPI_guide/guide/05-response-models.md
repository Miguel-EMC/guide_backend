# Response Models, Status Codes, and Response Classes

This guide covers `response_model`, filtering, status codes, and how to return different response types.

## Response Models

FastAPI uses Pydantic models to validate, serialize, and document responses.

### Basic Response Model

```python
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI()


class Item(BaseModel):
    id: int
    name: str
    price: float
    description: str | None = None


@app.get("/items/{item_id}", response_model=Item)
async def read_item(item_id: int):
    return {
        "id": item_id,
        "name": "Example Item",
        "price": 19.99,
        "description": "An example item",
    }
```

### Input vs Output Models

Use separate models to avoid leaking sensitive fields.

```python
from datetime import datetime, timezone
from pydantic import BaseModel, ConfigDict, EmailStr

class UserCreate(BaseModel):
    email: EmailStr
    password: str


class UserRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    email: EmailStr
    created_at: datetime


@app.post("/users/", response_model=UserRead)
async def create_user(user: UserCreate):
    return {
        "id": 1,
        "email": user.email,
        "created_at": datetime.now(timezone.utc),
    }
```

### Filtering Fields

Use these parameters to control output:

| Parameter | Purpose |
|-----------|---------|
| `response_model_include` | Include specific fields |
| `response_model_exclude` | Exclude specific fields |
| `response_model_exclude_unset` | Exclude fields not explicitly set |
| `response_model_exclude_defaults` | Exclude default values |
| `response_model_exclude_none` | Exclude `None` values |
| `response_model_by_alias` | Use field aliases in output |

```python
class User(BaseModel):
    id: int
    username: str
    email: str
    password: str


@app.get("/users/me", response_model=User, response_model_exclude={"password"})
async def read_current_user():
    return {
        "id": 1,
        "username": "john",
        "email": "john@example.com",
        "password": "secret",
    }
```

### Lists and Unions

```python
from typing import Union
from pydantic import BaseModel

class Item(BaseModel):
    id: int
    name: str

class Message(BaseModel):
    message: str


@app.get("/items/", response_model=list[Item])
async def read_items():
    return [{"id": 1, "name": "Item 1"}]


@app.get("/items/{item_id}", response_model=Union[Item, Message])
async def read_item(item_id: int):
    if item_id == 0:
        return Message(message="Item not found")
    return Item(id=item_id, name="Example")
```

## Status Codes

```python
from fastapi import FastAPI, HTTPException, status

app = FastAPI()


@app.post("/items/", status_code=status.HTTP_201_CREATED)
async def create_item():
    return {"created": True}


@app.delete("/items/{item_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_item(item_id: int):
    return None


@app.get("/items/{item_id}")
async def get_item(item_id: int):
    if item_id == 0:
        raise HTTPException(status_code=404, detail="Item not found")
    return {"id": item_id}
```

### Documenting Multiple Responses

```python
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI()


class Item(BaseModel):
    id: int
    name: str


class ErrorResponse(BaseModel):
    detail: str


@app.get(
    "/items/{item_id}",
    response_model=Item,
    responses={404: {"model": ErrorResponse, "description": "Not found"}},
)
async def read_item(item_id: int):
    if item_id == 0:
        raise HTTPException(status_code=404, detail="Item not found")
    return {"id": item_id, "name": "Example"}
```

## Response Headers

```python
from fastapi import Response


@app.get("/items/")
async def read_items(response: Response):
    response.headers["X-Rate-Limit"] = "100"
    return [{"id": 1, "name": "Item"}]
```

## Response Classes

FastAPI supports different response types via `response_class`.

```python
from fastapi import FastAPI
from fastapi.responses import HTMLResponse, PlainTextResponse, FileResponse, StreamingResponse

app = FastAPI()


@app.get("/text", response_class=PlainTextResponse)
async def text():
    return "hello"


@app.get("/html", response_class=HTMLResponse)
async def html():
    return "<h1>Hello</h1>"


@app.get("/file")
async def file():
    return FileResponse("./files/report.pdf")


@app.get("/stream")
async def stream():
    async def generator():
        yield b"data"

    return StreamingResponse(generator(), media_type="application/octet-stream")
```

If you return a `Response` directly, FastAPI skips response model validation and automatic docs for that route. Use `response_class` when you still want those features.

## Best Practices

- Use separate input/output models.
- Use `response_model` to filter output and protect sensitive fields.
- Prefer `response_class` over returning raw `Response` when you still want validation and OpenAPI docs.

## References

- [Response Model](https://fastapi.tiangolo.com/tutorial/response-model/)
- [Return a Response Directly](https://fastapi.tiangolo.com/advanced/response-directly/)
- [Custom Response - HTML, Stream, File, etc.](https://fastapi.tiangolo.com/advanced/custom-response/)

## Next Steps

- [Error Handling](./06-error-handling.md) - Handle errors gracefully
- [Database Setup](./07-database-setup.md) - Connect to databases

---

[Previous: Request Bodies](./04-request-bodies.md) | [Back to Index](./README.md) | [Next: Error Handling](./06-error-handling.md)
