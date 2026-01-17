# Response Models and Status Codes (Part 1)

FastAPI is a modern, high-performance web framework for building APIs with Python. One of its most powerful features is its ability to validate, serialize, and document responses using Pydantic models and HTTP status codes. This tutorial will guide you through using response models, status codes, custom headers, and documentation in FastAPI.

## Table of Contents

1.  Response Models with Pydantic V2
2.  Status Codes and Their Usage
3.  Custom Response Headers
4.  Response Schemas and Documentation

## 1. Response Models with Pydantic V2

### Setting Up Your Environment

First, let's install the necessary packages:

```bash
pip install fastapi[all] pydantic
```

### Creating Basic Response Models

Let's start by creating a basic FastAPI application with a Pydantic model for responses:

```python
from fastapi import FastAPI
from pydantic import BaseModel, Field
from typing import List, Optional
import uuid

app = FastAPI()

# Pydantic V2 model for response
class Item(BaseModel):
    id: uuid.UUID = Field(default_factory=uuid.uuid4)
    name: str
    price: float
    is_offer: bool = False
    tags: List[str] = []
    description: Optional[str] = None

    # Pydantic V2 model config
    model_config = {
        "json_schema_extra": {
            "examples": [
                {
                    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                    "name": "Smartphone",
                    "price": 799.99,
                    "is_offer": True,
                    "tags": ["electronics", "mobile"],
                    "description": "Latest model smartphone with 5G capability"
                }
            ]
        }
    }

# Endpoint with response_model parameter
@app.get("/items/{item_id}", response_model=Item)
async def read_item(item_id: str):
    # In a real app, you'd fetch this from a database
    return {
        "id": item_id,
        "name": "Example Item",
        "price": 19.99,
        "is_offer": False,
        "tags": ["example", "sample"],
        "description": "This is an example item"
    }
```

### Response Model Benefits

Using `response_model` provides several advantages:

1.  **Validation**: FastAPI ensures that your response matches the structure defined in the model.
2.  **Filtering**: Only the fields defined in the response model will be included in the output.
3.  **Type Conversion**: Data is automatically converted to the appropriate types.
4.  **Documentation**: Response models are included in the OpenAPI documentation.

### Including/Excluding Fields

You can control which fields are included in the response:

```python
from fastapi import FastAPI
from pydantic import BaseModel
from typing import Optional

app = FastAPI()

class User(BaseModel):
    username: str
    email: str
    full_name: Optional[str] = None
    password: str # Sensitive field we don't want to expose

@app.get("/users/me", response_model=User, response_model_exclude={"password"})
async def read_user_me():
    return {
        "username": "johndoe",
        "email": "john@example.com",
        "full_name": "John Doe",
        "password": "secret-password" # This will be excluded
    }

@app.get("/users/{username}", response_model=User, response_model_include={"username", "email"})
async def read_user(username: str):
    return {
        "username": username,
        "email": f"{username}@example.com",
        "full_name": "John Doe",
        "password": "secret-password"
    }
```

### Using Multiple Response Models

Sometimes you need different response models for different scenarios:

```python
from fastapi import FastAPI
from pydantic import BaseModel
from typing import List, Union

app = FastAPI()

class Item(BaseModel):
    id: int
    name: str
    description: str

class Message(BaseModel):
    message: str

@app.get("/items/{item_id}", response_model=Union[Item, Message])
async def read_item(item_id: int):
    if item_id == 0:
        return {"message": "Item not found"}
    return {"id": item_id, "name": "Example", "description": "An example item"}
```

### Nested Response Models

For more complex scenarios, you can nest Pydantic models:

```python
from fastapi import FastAPI
from pydantic import BaseModel
from typing import List

app = FastAPI()

class Tag(BaseModel):
    id: int
    name: str

class Category(BaseModel):
    id: int
    name: str

class Item(BaseModel):
    id: int
    name: str
    tags: List[Tag]
    category: Category

@app.get("/items/{item_id}", response_model=Item)
async def read_item(item_id: int):
    return {
        "id": item_id,
        "name": "Example Item",
        "tags": [
            {"id": 1, "name": "red"},
            {"id": 2, "name": "fast"}
        ],
        "category": {
            "id": 1,
            "name": "Electronics"
        }
    }
```