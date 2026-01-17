# Response Models and Status Codes (Part 2)

## 2. Status Codes and Their Usage

### Common HTTP Status Codes

FastAPI makes it easy to specify status codes for your responses:

```python
from fastapi import FastAPI, status, HTTPException

app = FastAPI()

@app.post("/items/", status_code=status.HTTP_201_CREATED)
async def create_item(name: str):
    return {"name": name}

@app.get("/items/{item_id}")
async def read_item(item_id: int):
    if item_id == 0:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Item not found"
        )
    return {"id": item_id, "name": "Example Item"}
```

Here's a list of commonly used status codes:

| Status Code | Name                 | Description                               |
| :---------- | :------------------- | :---------------------------------------- |
| 200         | OK                   | Request succeeded                         |
| 201         | Created              | Resource created successfully             |
| 204         | No Content           | Request succeeded but no content to return|
| 400         | Bad Request          | Client error, invalid request             |
| 401         | Unauthorized         | Authentication required                   |
| 403         | Forbidden            | Authenticated but not authorized          |
| 404         | Not Found            | Resource not found                        |
| 422         | Unprocessable Entity | Request validated but failed business logic |
| 500         | Internal Server Error| Server error                              |

### Combining Status Codes with Response Models

You can combine status codes with specific response models:

```python
from fastapi import FastAPI, status
from pydantic import BaseModel
from typing import Union, List

app = FastAPI()

class Item(BaseModel):
    id: int
    name: str

class ItemList(BaseModel):
    items: List[Item]
    count: int

class Error(BaseModel):
    detail: str

@app.get(
    "/items/",
    response_model=ItemList,
    status_code=status.HTTP_200_OK,
    responses={
        404: {"model": Error, "description": "Items not found"},
        500: {"model": Error, "description": "Internal server error"}
    }
)
async def read_items():
    return {
        "items": [
            {"id": 1, "name": "Item 1"},
            {"id": 2, "name": "Item 2"}
        ],
        "count": 2
    }
```

## 3. Custom Response Headers

### Adding Headers to Responses

FastAPI allows you to customize response headers:

```python
from fastapi import FastAPI, Response, status

app = FastAPI()

@app.get("/items/")
async def read_items(response: Response):
    response.headers["X-Custom-Header"] = "custom-value"
    response.headers["Cache-Control"] = "max-age=3600"
    return {"items": [{"id": 1, "name": "Item 1"}]}
```

### Headers with HTTPException

You can also add headers when raising exceptions:

```python
from fastapi import FastAPI, HTTPException

app = FastAPI()

@app.get("/items/{item_id}")
async def read_item(item_id: int):
    if item_id == 0:
        raise HTTPException(
            status_code=404,
            detail="Item not found",
            headers={"X-Error-Code": "ITEM_NOT_FOUND"}
        )
    return {"id": item_id, "name": "Example Item"}
```

### Using Response Class

For more control over responses, use a `Response` class:

```python
from fastapi import FastAPI
from fastapi.responses import JSONResponse, PlainTextResponse, RedirectResponse

app = FastAPI()

@app.get("/items/json")
async def get_json():
    return JSONResponse(
        content={"message": "Hello World"},
        status_code=200,
        headers={"X-Custom-Header": "json-value"}
    )

@app.get("/items/text")
async def get_text():
    return PlainTextResponse(
        content="Hello World",
        status_code=200,
        headers={"X-Custom-Header": "text-value"}
    )

@app.get("/redirect")
async def redirect():
    return RedirectResponse(
        url="/items/json",
        status_code=307,
        headers={"X-Redirect-Reason": "temporary"}
    )
```