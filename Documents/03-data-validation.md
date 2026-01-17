# Data Validation with Pydantic

## Introduction to Pydantic Models

Pydantic V2, released in June 2023, is a major rewrite that brings significant performance improvements and new features to one of Python's most popular data validation libraries. Pydantic seamlessly integrates with FastAPI to provide robust data validation, serialization, and documentation.

### What is Pydantic?

Pydantic is a data validation and settings management library that uses Python type annotations to:

* Validate data at runtime
* Convert incoming data to Python types
* Generate JSON Schema for your models
* Provide autocomplete and type checking in your IDE

## Basic Model Example

```Python
from pydantic import BaseModel
from datetime import datetime
from typing import Optional

class User(BaseModel):
    id: int
    name: str
    email: str
    is_active: bool = True
    created_at: datetime = datetime.now()
    tags: list[str] = []
    profile_picture: Optional[str] = None
```

When you create a Pydantic model, it enforces type validation automatically:

```Python
# Valid data
user = User(id=1, name="John Doe", email="john@example.com")
print(user.model_dump())

# Invalid data raises ValidationError
try:
    User(id="not-an-integer", name=123, email="invalid-email")
except Exception as e:
    print(f"Validation error: {e}")
```

## Data Validation and Serialization

### Basic Validation with Type Annotations

Pydantic uses Python type annotations to validate data:

```Python
from pydantic import BaseModel, Field, EmailStr
from typing import Annotated

class Product(BaseModel):
    id: int
    name: str
    price: float = Field(gt=0) # Must be greater than 0
    in_stock: bool = True
    tags: list[str] = []
    description: str = Field(min_length=10, max_length=1000)
    contact_email: EmailStr # Specialized email validation
    quantity: Annotated[int, Field(ge=0)] # Must be greater than or equal to 0
```

## Field Validation with Pydantic Validators

Pydantic V2 introduces new ways to create validators:

### 1. Field validators (replacing V1 validators)

```Python
from pydantic import BaseModel, field_validator

class SignupForm(BaseModel):
    username: str
    passwordl: str
    password2: str

    @field_validator('username')
    @classmethod
    def username_alphanumeric(cls, v: str) -> str:
        if not v.isalnum():
            raise ValueError('Username must be alphanumeric')
        return v

    @field_validator('password2')
    @classmethod
    def passwords_match(cls, v: str, info):
        if 'passwordl' in info.data and v != info.data['passwordl']:
            raise ValueError('Passwords do not match')
        return v
```

### 2. Model validators

```Python
from pydantic import BaseModel, model_validator

class Order(BaseModel):
    items: list[str]
    item_count: int

    @model_validator(mode='after')
    def check_item_count(self) -> 'Order':
        if len(self.items) != self.item_count:
            raise ValueError('Item count does not match number of items')
        return self
```

## Serialization and Deserialization

Pydantic makes it easy to convert models to and from various formats:

```Python
from pydantic import BaseModel
from datetime import datetime
import json

class Article(BaseModel):
    id: int
    title: str
    content: str
    published: datetime
    tags: list[str] = []

# Create a model from Python objects
article = Article(
    id=1,
    title="Pydantic V2 is Amazing",
    content="This is the content of the article.",
    published=datetime.now(),
    tags=["pydantic", "python", "fastapi"]
)

# Serialize to JSON
json_data = article.model_dump_json()
print(json_data)

# Deserialize from JSON
article_dict = json.loads(json_data)
# Need to manually parse datetime when deserializing from raw JSON
article_dict['published'] = datetime.fromisoformat(article_dict['published'])
new_article = Article.model_validate(article_dict)
```

## Using Pydantic with FastAPI

In FastAPI, Pydantic models automatically validate request data:

```Python
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field, EmailStr

app = FastAPI()

class UserCreate(BaseModel):
    username: str = Field(min_length=3, max_length=50)
    email: EmailStr
    password: str = Field(min_length=8)

class UserResponse(BaseModel):
    id: int
    username: str
    email: str

@app.post("/users/", response_model=UserResponse)
async def create_user(user: UserCreate):
    # FastAPI automatically validates user data based on the Pydantic model
    # If validation fails, it returns a 422 Unprocessable Entity error

    # Here you would typically save the user to a database
    # For this example, we'll just return a mock response
    return UserResponse(id=1, username=user.username, email=user.email)
```

## Custom Data Types and Validators

### Custom Data Types

You can create custom data types for specialized validation:

```Python
from pydantic import BaseModel, Field, field_validator
from typing import Annotated, NewType
import re

# Option 1: Using NewType with Pydantic validators
UserId = NewType('UserId', str)

class User(BaseModel):
    id: UserId
    name: str

    @field_validator('id')
    @classmethod
    def validate_user_id(cls, v: str) -> str:
        if not re.match(r'^USER_\d{6}$', v):
            raise ValueError('User ID must be in format USER_XXXXXX')
        return v

# Option 2: Using Annotated with Field constraints
ISBN = Annotated[str, Field(
    pattern=r'^[0-9]{9}[0-9X]$|^[0-9]{13}$|^[0-9]{1,5}-[0-9]+-[0-9]+-[0-9X]$|^[0-9]{3}-[0-9]{1,5}-[0-9]+-[0-9]+-[0-9]$'
)]

class Book(BaseModel):
    title: str
    isbn: ISBN
```

### Custom Validators with Dependencies

You can create reusable validator functions:

```Python
from pydantic import BaseModel, field_validator, model_validator
from datetime import date

def validate_future_date(value: date) -> date:
    if value <= date.today():
        raise ValueError("Date must be in the future")
    return value

class Event(BaseModel):
    name: str
    event_date: date

    @field_validator('event_date')
    @classmethod
    def check_future_date(cls, v: date) -> date:
        return validate_future_date(v)
```

### Complex Validators with Context

For complex validation scenarios, you can access other field values:

```Python
from pydantic import BaseModel, field_validator, model_validator
from datetime import datetime

class DateRange(BaseModel):
    start_date: datetime
    end_date: datetime

    @model_validator(mode='after')
    def check_dates_order(self) -> 'DateRange':
        if self.start_date >= self.end_date:
            raise ValueError('End date must be after start date')
        return self
```
