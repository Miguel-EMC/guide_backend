# Data Validation with Pydantic v2 (2026 Edition)

This guide covers Pydantic v2 fundamentals, field validation, custom validators, and integration with FastAPI 0.128+.

## What's New in Pydantic v2?

⚠️ **Important**: FastAPI 0.128+ requires Pydantic v2. Pydantic v1 is no longer supported.

| Feature | Pydantic v1 | Pydantic v2 |
|---------|-------------|-------------|
| **Performance** | Good | ~2x faster validation |
| **Error Messages** | Basic | Detailed with context |
| **Customization** | Limited | Extensive config options |
| **Type Safety** | Good | Excellent with strict mode |
| **JSON Schema** | Draft 7 | Draft 2020-12 |

## What is Pydantic?

Pydantic v2 is a data validation library that uses Python type annotations to:

| Feature | Description |
|---------|-------------|
| **Validate Data** | Enforce types and constraints at runtime |
| **Convert Types** | Automatically convert compatible types |
| **Generate Schemas** | Create JSON Schema for documentation |
| **IDE Support** | Full autocomplete and type checking |
| **Performance** | Extremely fast with Rust core |
| **Strict Mode** | Optional strict validation |

## Basic Models

### Creating a Model (Pydantic v2)

```python
from pydantic import BaseModel, Field, field_validator
from datetime import datetime
from typing import Optional, List

class User(BaseModel):
    id: int = Field(..., description="User ID", gt=0)
    name: str = Field(..., min_length=1, max_length=100)
    email: str = Field(..., pattern=r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$')
    is_active: bool = True
    created_at: datetime = Field(default_factory=datetime.now)
    tags: List[str] = Field(default_factory=list)
    profile_picture: Optional[str] = None
    
    @field_validator('name')
    @classmethod
    def validate_name(cls, v):
        if v.strip() != v:
            raise ValueError('Name cannot have leading/trailing spaces')
        return v.title()
```

### Using the Model

```python
# Valid data - automatic type conversion
user = User(id="1", name="John", email="john@example.com")
print(user.id)  # 1 (converted to int)

# Access as dictionary
print(user.model_dump())

# Convert to JSON
print(user.model_dump_json())
```

## Pydantic v2 Migration Guide

### Key Breaking Changes

#### 1. Config Class → model_config

```python
# Pydantic v1 (OLD)
class User(BaseModel):
    name: str
    
    class Config:
        str_strip_whitespace = True
        validate_assignment = True

# Pydantic v2 (NEW)
class User(BaseModel):
    model_config = {
        "str_strip_whitespace": True,
        "validate_assignment": True
    }
    name: str
```

#### 2. Validator Decorator

```python
# Pydantic v1 (OLD)
from pydantic import validator

class User(BaseModel):
    name: str
    
    @validator('name')
    def validate_name(cls, v):
        return v.title()

# Pydantic v2 (NEW)
from pydantic import field_validator

class User(BaseModel):
    name: str
    
    @field_validator('name')
    @classmethod
    def validate_name(cls, v):
        return v.title()
```

#### 3. Method Changes

```python
user = User(name="john")

# OLD methods (no longer work)
user.dict()           # -> user.model_dump()
user.json()           # -> user.model_dump_json()
user.copy()           # -> user.model_copy()

# NEW methods
data = user.model_dump(exclude={'id'})
json_data = user.model_dump_json(indent=2)
copy_user = user.model_copy(update={'name': 'Jane'})
```

### Validation Errors

```python
from pydantic import ValidationError

try:
    user = User(id="not-a-number", name=123, email="invalid")
except ValidationError as e:
    print(e.errors())
```

## Field Validation

### Using Field Constraints

```python
from pydantic import BaseModel, Field, EmailStr

class Product(BaseModel):
    id: int
    name: str = Field(min_length=2, max_length=100)
    price: float = Field(gt=0, description="Must be positive")
    quantity: int = Field(ge=0, le=1000)
    description: str = Field(default="", max_length=1000)
    email: EmailStr  # Requires email-validator package
```

### Field Constraint Reference

| Constraint | Type | Description | Example |
|------------|------|-------------|---------|
| `gt` | Numeric | Greater than | `Field(gt=0)` |
| `ge` | Numeric | Greater than or equal | `Field(ge=0)` |
| `lt` | Numeric | Less than | `Field(lt=100)` |
| `le` | Numeric | Less than or equal | `Field(le=100)` |
| `min_length` | String | Minimum length | `Field(min_length=1)` |
| `max_length` | String | Maximum length | `Field(max_length=50)` |
| `pattern` | String | Regex pattern | `Field(pattern="^[a-z]+$")` |
| `default` | Any | Default value | `Field(default="")` |
| `description` | Any | Documentation | `Field(description="...")` |

### Special Types

```python
from pydantic import BaseModel, EmailStr, HttpUrl, Field
from typing import Annotated

class Contact(BaseModel):
    email: EmailStr                    # Validated email
    website: HttpUrl                   # Validated URL
    phone: Annotated[str, Field(pattern=r"^\d{10}$")]  # 10 digits
```

## Custom Validators

### Field Validators

Validate individual fields:

```python
from pydantic import BaseModel, field_validator

class User(BaseModel):
    username: str
    password: str

    @field_validator('username')
    @classmethod
    def username_alphanumeric(cls, v: str) -> str:
        if not v.isalnum():
            raise ValueError('Username must be alphanumeric')
        return v.lower()  # Transform to lowercase

    @field_validator('password')
    @classmethod
    def password_strength(cls, v: str) -> str:
        if len(v) < 8:
            raise ValueError('Password must be at least 8 characters')
        if not any(c.isdigit() for c in v):
            raise ValueError('Password must contain a digit')
        return v
```

### Model Validators

Validate across multiple fields:

```python
from pydantic import BaseModel, model_validator

class DateRange(BaseModel):
    start_date: datetime
    end_date: datetime

    @model_validator(mode='after')
    def check_dates(self) -> 'DateRange':
        if self.start_date >= self.end_date:
            raise ValueError('end_date must be after start_date')
        return self
```

### Validating with Dependencies

```python
from pydantic import BaseModel, field_validator

class SignupForm(BaseModel):
    password: str
    password_confirm: str

    @field_validator('password_confirm')
    @classmethod
    def passwords_match(cls, v: str, info) -> str:
        if 'password' in info.data and v != info.data['password']:
            raise ValueError('Passwords do not match')
        return v
```

## Nested Models

### Basic Nesting

```python
class Address(BaseModel):
    street: str
    city: str
    country: str
    zip_code: str

class User(BaseModel):
    id: int
    name: str
    address: Address  # Nested model
```

**JSON input:**

```json
{
  "id": 1,
  "name": "John",
  "address": {
    "street": "123 Main St",
    "city": "New York",
    "country": "USA",
    "zip_code": "10001"
  }
}
```

### Lists of Models

```python
class OrderItem(BaseModel):
    product_id: int
    quantity: int = Field(gt=0)
    unit_price: float = Field(gt=0)

class Order(BaseModel):
    id: int
    customer_id: int
    items: list[OrderItem]  # List of nested models
```

### Recursive Models

For self-referential structures:

```python
class Comment(BaseModel):
    id: int
    text: str
    replies: list['Comment'] = []  # Forward reference

# Required for recursive models in Pydantic v2
Comment.model_rebuild()
```

## Model Configuration

### Config Options

```python
from pydantic import BaseModel, ConfigDict

class User(BaseModel):
    model_config = ConfigDict(
        strict=True,              # No type coercion
        frozen=True,              # Immutable instances
        extra='forbid',           # Reject extra fields
        str_strip_whitespace=True # Strip whitespace from strings
    )

    name: str
    email: str
```

### Common Config Options

| Option | Description | Default |
|--------|-------------|---------|
| `strict` | No automatic type conversion | `False` |
| `frozen` | Make instances immutable | `False` |
| `extra` | Handle extra fields: `'allow'`, `'forbid'`, `'ignore'` | `'ignore'` |
| `str_strip_whitespace` | Strip whitespace from strings | `False` |
| `from_attributes` | Read from object attributes (for ORM) | `False` |

## Serialization

### Model to Dictionary

```python
user = User(id=1, name="John", email="john@example.com")

# All fields
data = user.model_dump()

# Exclude fields
data = user.model_dump(exclude={'password'})

# Include only specific fields
data = user.model_dump(include={'id', 'name'})

# Exclude unset fields (for partial updates)
data = user.model_dump(exclude_unset=True)
```

### Model to JSON

```python
json_str = user.model_dump_json()
json_str = user.model_dump_json(indent=2)  # Pretty print
```

### Dictionary to Model

```python
data = {"id": 1, "name": "John", "email": "john@example.com"}
user = User.model_validate(data)
```

## FastAPI Integration

### Request Validation

```python
from fastapi import FastAPI
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
    # FastAPI automatically validates request body
    # Returns 422 if validation fails
    return UserResponse(id=1, username=user.username, email=user.email)
```

### Separate Input/Output Models

```python
# Base fields shared between models
class UserBase(BaseModel):
    username: str
    email: EmailStr

# For creating users (input)
class UserCreate(UserBase):
    password: str

# For updating users (all optional)
class UserUpdate(BaseModel):
    username: str | None = None
    email: EmailStr | None = None
    password: str | None = None

# For responses (output)
class UserResponse(UserBase):
    id: int
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)
```

### ORM Mode

For SQLAlchemy/SQLModel integration:

```python
from pydantic import ConfigDict

class UserResponse(BaseModel):
    id: int
    username: str
    email: str

    model_config = ConfigDict(from_attributes=True)

# Now works with ORM objects
@app.get("/users/{user_id}", response_model=UserResponse)
async def get_user(user_id: int, db: Session = Depends(get_db)):
    db_user = db.query(User).filter(User.id == user_id).first()
    return db_user  # Pydantic reads from object attributes
```

## Complete Example

```python
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field, EmailStr, field_validator
from datetime import datetime
from typing import Optional

app = FastAPI()

class OrderItem(BaseModel):
    product_id: int
    quantity: int = Field(gt=0)
    unit_price: float = Field(gt=0)

class OrderCreate(BaseModel):
    customer_email: EmailStr
    items: list[OrderItem] = Field(min_length=1)
    notes: Optional[str] = Field(default=None, max_length=500)

    @field_validator('items')
    @classmethod
    def validate_items(cls, v):
        if len(v) > 100:
            raise ValueError('Maximum 100 items per order')
        return v

class OrderResponse(BaseModel):
    id: int
    customer_email: str
    items: list[OrderItem]
    total: float
    created_at: datetime

@app.post("/orders/", response_model=OrderResponse)
async def create_order(order: OrderCreate):
    total = sum(item.quantity * item.unit_price for item in order.items)

    return OrderResponse(
        id=1,
        customer_email=order.customer_email,
        items=order.items,
        total=total,
        created_at=datetime.now()
    )
```

## Best Practices

### Do's

```python
# Use separate models for input/output
class UserCreate(BaseModel): ...
class UserResponse(BaseModel): ...

# Add descriptive fields
name: str = Field(description="User's full name")

# Use EmailStr for emails
email: EmailStr

# Set sensible constraints
password: str = Field(min_length=8)
```

### Don'ts

```python
# Don't include passwords in responses
class UserResponse(BaseModel):
    password: str  # Security risk!

# Don't skip validation
name: str  # Add constraints: Field(min_length=1)

# Don't mix input/output concerns
class User(BaseModel):  # Split into UserCreate and UserResponse
    id: int
    password: str
```

## Summary

You learned:

- Creating Pydantic models with type annotations
- Field validation with constraints
- Custom validators for complex logic
- Nested and recursive models
- Serialization and deserialization
- FastAPI integration patterns

## Advanced Pydantic v2 Features

### Strict Mode

```python
from pydantic import BaseModel, ConfigDict

class StrictUser(BaseModel):
    model_config = ConfigDict(strict=True)
    
    id: int
    name: str

# This will fail - no type conversion in strict mode
try:
    user = StrictUser(id="123", name="John")  # String to int fails
except ValidationError as e:
    print(e)  # Validation error
```

### Computed Fields

```python
from pydantic import BaseModel, computed_field

class Product(BaseModel):
    name: str
    price: float
    quantity: int
    
    @computed_field
    @property
    def total_value(self) -> float:
        return self.price * self.quantity
    
    @computed_field
    @property
    def is_expensive(self) -> bool:
        return self.total_value > 1000

product = Product(name="Laptop", price=999.99, quantity=2)
print(product.total_value)  # 1999.98
print(product.is_expensive)  # True
```

### Type Adapters

```python
from pydantic import TypeAdapter
from typing import List, Dict, Any

# Validate arbitrary data structures
list_adapter = TypeAdapter(List[int])
data = ["1", 2, "3.0"]  # Mixed types
validated = list_adapter.validate_python(data)  # [1, 2, 3]

# For complex structures
dict_adapter = TypeAdapter(Dict[str, Any])
validated_dict = dict_adapter.validate_python('{"key": "value"}')  # {"key": "value"}
```

### Custom JSON Encoders

```python
from pydantic import BaseModel, field_serializer
from datetime import datetime
from decimal import Decimal

class Transaction(BaseModel):
    amount: Decimal
    created_at: datetime
    
    @field_serializer('amount')
    def serialize_amount(self, value: Decimal) -> str:
        return f"${value:.2f}"
    
    @field_serializer('created_at', when_used='json')
    def serialize_datetime(self, value: datetime) -> str:
        return value.isoformat()

transaction = Transaction(
    amount=Decimal("123.456"),
    created_at=datetime.now()
)

print(transaction.model_dump_json())
# {"amount": "$123.46", "created_at": "2026-02-02T10:30:45.123456"}
```

### Discriminated Unions

```python
from pydantic import BaseModel, Field, Tag, Discriminator
from typing import Union

class Square(BaseModel):
    type: Tag('square')
    side_length: float

class Rectangle(BaseModel):
    type: Tag('rectangle')
    width: float
    height: float

class Circle(BaseModel):
    type: Tag('circle')
    radius: float

Shape = Union[Square, Rectangle, Circle] = Field(discriminator='type')

class Drawing(BaseModel):
    shapes: list[Shape]

# FastAPI knows which model to use based on 'type' field
drawing_data = {
    "shapes": [
        {"type": "square", "side_length": 5.0},
        {"type": "rectangle", "width": 3.0, "height": 4.0},
        {"type": "circle", "radius": 2.5}
    ]
}

drawing = Drawing.model_validate(drawing_data)
```

### Pydantic Settings

```python
from pydantic_settings import BaseSettings
from pydantic import Field

class AppSettings(BaseSettings):
    database_url: str = Field(validation_alias='DATABASE_URL')
    debug: bool = Field(default=False, alias='DEBUG')
    max_connections: int = Field(default=10, ge=1, le=100)
    
    class Config:
        env_file = ".env"
        env_file_encoding = 'utf-8'
        case_sensitive = False

# Automatically loads from environment variables
settings = AppSettings()
print(settings.database_url)  # From DATABASE_URL env var
```

## Performance Tips

### Use TypeVar for Generic Models

```python
from typing import TypeVar, Generic
from pydantic import BaseModel

T = TypeVar('T')

class Response(BaseModel, Generic[T]):
    success: bool
    data: T
    message: str

UserResponse = Response[User]
ProductResponse = Response[Product]
```

### Validate Once, Use Many Times

```python
# Cache the validator for repeated validation
user_validator = TypeAdapter(User)

# Use multiple times
users = [
    {"id": 1, "name": "John"},
    {"id": 2, "name": "Jane"},
    {"id": 3, "name": "Bob"}
]

validated_users = [user_validator.validate_python(u) for u in users]
```

## Error Handling Improvements

### Detailed Error Context

```python
from pydantic import ValidationError, BaseModel

try:
    user = User(id=-1, name="", email="invalid")
except ValidationError as e:
    errors = e.errors(include_url=False, include_context=True)
    
    for error in errors:
        print(f"Field: {error['loc']}")
        print(f"Message: {error['msg']}")
        print(f"Type: {error['type']}")
        if 'ctx' in error:
            print(f"Context: {error['ctx']}")
```

### Custom Error Messages

```python
from pydantic import BaseModel, Field, field_validator, ValidationInfo

class User(BaseModel):
    age: int = Field(ge=0, le=150)
    
    @field_validator('age')
    @classmethod
    def validate_age(cls, v: int, info: ValidationInfo) -> int:
        if v < 18:
            raise ValueError('User must be at least 18 years old')
        if v > 65:
            raise ValueError('User cannot be older than 65 years')
        return v
```

## Next Steps

- [Request Bodies](./04-request-bodies.md) - Handle JSON and form data
- [Response Models](./05-response-models.md) - Configure API responses
- [GenAI Integration](./17-genai-integration.md) - Build AI-powered APIs

---

[Previous: Routing](./02-routing.md) | [Back to Index](./README.md) | [Next: Request Bodies](./04-request-bodies.md)
