# Routing & Endpoints

This guide covers HTTP methods, path/query parameters, validation with `Path`/`Query`, route order, and router organization with `APIRouter`.

## HTTP Methods

FastAPI provides decorators for all standard HTTP methods:

```python
from fastapi import FastAPI

app = FastAPI()


@app.get("/items")
async def get_items():
    return {"items": ["item1", "item2"]}


@app.post("/items")
async def create_item():
    return {"status": "created"}


@app.put("/items/{item_id}")
async def update_item(item_id: int):
    return {"status": "updated", "id": item_id}


@app.patch("/items/{item_id}")
async def patch_item(item_id: int):
    return {"status": "patched", "id": item_id}


@app.delete("/items/{item_id}")
async def delete_item(item_id: int):
    return {"status": "deleted", "id": item_id}
```

### HTTP Methods Reference

| Method | Purpose | Idempotent | Has Body |
|--------|---------|------------|----------|
| `GET` | Retrieve resources | Yes | No |
| `POST` | Create resources | No | Yes |
| `PUT` | Replace resources | Yes | Yes |
| `PATCH` | Update partially | No | Yes |
| `DELETE` | Remove resources | Yes | No |

## Route Matching Order

FastAPI evaluates routes in the order they are defined. Specific routes must come before dynamic ones.

```python
@app.get("/users/me")
async def get_current_user():
    return {"user": "me"}


@app.get("/users/{user_id}")
async def get_user(user_id: str):
    return {"user_id": user_id}
```

## Path Parameters

Path parameters capture values from the URL path and are automatically converted based on type hints.

### Basic Usage

```python
@app.get("/items/{item_id}")
async def read_item(item_id: int):
    return {"item_id": item_id}
```

### Validation with `Path`

```python
from typing import Annotated
from fastapi import Path

@app.get("/items/{item_id}")
async def read_item(
    item_id: Annotated[int, Path(title="Item ID", ge=1, le=1000)]
):
    return {"item_id": item_id}
```

### Path Parameters Containing Paths

Use the `:path` converter to capture a full path including slashes:

```python
@app.get("/files/{file_path:path}")
async def read_file(file_path: str):
    return {"path": file_path}
```

### Predefined Values with `Enum`

```python
from enum import Enum

class ModelName(str, Enum):
    alexnet = "alexnet"
    resnet = "resnet"
    lenet = "lenet"


@app.get("/models/{model_name}")
async def get_model(model_name: ModelName):
    if model_name is ModelName.alexnet:
        return {"model": model_name, "type": "image classification"}
    return {"model": model_name}
```

## Query Parameters

Query parameters are key-value pairs after `?` in the URL.

### Required vs Optional

```python
@app.get("/search")
async def search_items(q: str, limit: int = 10):
    return {"q": q, "limit": limit}
```

When a default is provided, the parameter is optional. Without a default, it is required.

### Validation with `Query`

```python
from typing import Annotated
from fastapi import Query

@app.get("/items/")
async def read_items(
    q: Annotated[
        str | None,
        Query(
            min_length=3,
            max_length=50,
            pattern="^[a-z0-9-]+$",
            alias="item-query",
            deprecated=True,
        ),
    ] = None
):
    return {"q": q}
```

### Boolean Conversion

FastAPI converts query values to `bool` automatically:

```python
@app.get("/items/{item_id}")
async def read_item(item_id: str, short: bool = False):
    return {"item_id": item_id, "short": short}
```

### Multiple Values

```python
from typing import Annotated
from fastapi import Query

@app.get("/items/")
async def read_items(q: Annotated[list[str], Query()] = []):
    return {"q": q}
```

**Request:** `GET /items/?q=foo&q=bar` → `{"q": ["foo", "bar"]}`

## Combining Path, Query, and Body

FastAPI uses type hints to determine where each parameter comes from:

```python
from pydantic import BaseModel

class Item(BaseModel):
    name: str
    price: float


@app.put("/items/{item_id}")
async def update_item(
    item_id: int,  # path
    q: str | None = None,  # query
    item: Item | None = None,  # body
):
    return {"item_id": item_id, "q": q, "item": item}
```

## Router Organization

Use `APIRouter` to group endpoints and reuse common configuration.

```python
# routers/items.py
from fastapi import APIRouter

router = APIRouter(
    prefix="/items",
    tags=["items"],
    responses={404: {"description": "Not found"}},
)

@router.get("/")
async def list_items():
    return {"items": []}

@router.get("/{item_id}")
async def get_item(item_id: int):
    return {"item_id": item_id}
```

```python
# main.py
from fastapi import FastAPI, Depends
from routers import items
from app.dependencies import get_token_header

app = FastAPI()
app.include_router(items.router)

# Include with extra settings
app.include_router(
    items.router,
    prefix="/v2",
    tags=["items"],
    dependencies=[Depends(get_token_header)],
    responses={418: {"description": "I'm a teapot"}},
)
```

### Dependencies with `Depends`

Use `Depends` to inject shared logic into routes:

```python
from typing import Annotated
from fastapi import Depends

async def common_parameters(q: str | None = None, skip: int = 0, limit: int = 100):
    return {"q": q, "skip": skip, "limit": limit}


@app.get("/items/")
async def read_items(commons: Annotated[dict, Depends(common_parameters)]):
    return commons
```

## Response Metadata

You can document and constrain responses at the route level.

```python
from fastapi import status
from pydantic import BaseModel

class ItemOut(BaseModel):
    id: int
    name: str


@app.post("/items/", response_model=ItemOut, status_code=status.HTTP_201_CREATED)
async def create_item():
    return {"id": 1, "name": "Item"}
```

## Best Practices

- Keep routes resource-based: `/users`, `/users/{user_id}`.
- Prefer `Annotated` with `Path`/`Query` for clarity and documentation.
- Group related endpoints with `APIRouter`.
- Keep route order deterministic and place static routes before dynamic ones.

## Summary

You learned:

- How to define routes with HTTP method decorators
- How path and query parameters work with validation
- How to capture full paths with `:path`
- How to organize routes with `APIRouter` and `include_router`
- How to attach dependencies and metadata to routes

## Next Steps

- [Data Validation](./03-data-validation.md) - Master Pydantic validation
- [Request Bodies](./04-request-bodies.md) - Handle JSON and form data

---

[Previous: Introduction](./01-introduction.md) | [Back to Index](./README.md) | [Next: Data Validation](./03-data-validation.md)
