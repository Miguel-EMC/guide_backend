# Data Validation with Pydantic v2

This guide covers Pydantic v2 models, field constraints, validators, serialization, settings, and common FastAPI patterns.

## Core Concepts

Pydantic models inherit from `BaseModel` and use type hints to validate input data and generate JSON Schema used by FastAPI.

## Basic Model

```python
from datetime import datetime, timezone
from pydantic import BaseModel, Field, EmailStr, ConfigDict

class User(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)

    id: int
    name: str = Field(min_length=1, max_length=100)
    email: EmailStr
    is_active: bool = True
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))
```

Note: `EmailStr` requires the optional `email-validator` dependency.

### Using the Model

```python
user = User.model_validate({
    "id": "1",
    "name": "   Ada Lovelace ",
    "email": "ada@example.com",
})

payload = user.model_dump()
json_payload = user.model_dump_json()
```

## Field Constraints

```python
from pydantic import BaseModel, Field

class Product(BaseModel):
    name: str = Field(min_length=2, max_length=100)
    price: float = Field(gt=0)
    quantity: int = Field(ge=0, le=1000)
    sku: str = Field(pattern=r"^[A-Z0-9-]+$")
```

| Constraint | Description | Example |
|-----------|-------------|---------|
| `gt`, `ge` | Numeric bounds | `Field(gt=0)` |
| `lt`, `le` | Numeric bounds | `Field(le=100)` |
| `min_length`, `max_length` | String length | `Field(min_length=1)` |
| `pattern` | Regex pattern | `Field(pattern=r"^[A-Z]+$")` |

## Model Configuration

Use `ConfigDict` to control model behavior.

```python
from pydantic import BaseModel, ConfigDict

class User(BaseModel):
    model_config = ConfigDict(
        strict=False,
        extra="forbid",
        str_strip_whitespace=True,
        from_attributes=True,
    )

    id: int
    email: str
```

## Validators

### Field Validators

```python
from pydantic import BaseModel, field_validator

class User(BaseModel):
    username: str
    password: str

    @field_validator("username")
    @classmethod
    def username_is_alnum(cls, v: str) -> str:
        if not v.isalnum():
            raise ValueError("Username must be alphanumeric")
        return v.lower()
```

### Model Validators

```python
from pydantic import BaseModel, model_validator

class Signup(BaseModel):
    password: str
    password_confirm: str

    @model_validator(mode="after")
    def check_passwords(self):
        if self.password != self.password_confirm:
            raise ValueError("Passwords do not match")
        return self
```

Note: In Pydantic v2, use `field_validator` and `model_validator` instead of the v1 `validator` and `root_validator`.

## Nested and Recursive Models

```python
from pydantic import BaseModel

class Address(BaseModel):
    street: str
    city: str
    country: str

class User(BaseModel):
    id: int
    name: str
    address: Address
```

### Recursive Models

```python
class Comment(BaseModel):
    id: int
    text: str
    replies: list["Comment"] = []

Comment.model_rebuild()
```

## Discriminated Unions

```python
from typing import Annotated, Literal, Union
from pydantic import BaseModel, Field

class Cat(BaseModel):
    pet_type: Literal["cat"]
    meows: int

class Dog(BaseModel):
    pet_type: Literal["dog"]
    barks: float

Pet = Annotated[Union[Cat, Dog], Field(discriminator="pet_type")]

class Owner(BaseModel):
    name: str
    pet: Pet
```

## Serialization

### Field Serializers

```python
from decimal import Decimal
from pydantic import BaseModel, field_serializer

class Transaction(BaseModel):
    amount: Decimal

    @field_serializer("amount")
    def serialize_amount(self, value: Decimal) -> str:
        return f"{value:.2f}"
```

### Model Serializers

```python
from pydantic import BaseModel, model_serializer

class User(BaseModel):
    id: int
    name: str

    @model_serializer
    def serialize(self):
        return {"id": self.id, "name": self.name.upper()}
```

## TypeAdapter

Use `TypeAdapter` to validate arbitrary data without defining a full model.

```python
from pydantic import TypeAdapter

adapter = TypeAdapter(list[int])
validated = adapter.validate_python(["1", 2, "3"])  # -> [1, 2, 3]
```

## Settings Management

Use `pydantic-settings` for environment-based configuration.

```python
from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_prefix="APP_",
    )

    database_url: str = Field(validation_alias="DATABASE_URL")
    debug: bool = False
```

## FastAPI Integration Patterns

Separate input and output models and use `response_model` for responses.

```python
from datetime import datetime, timezone
from pydantic import BaseModel, ConfigDict, EmailStr
from fastapi import FastAPI

app = FastAPI()

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
    # Return ORM object or dict
    return {"id": 1, "email": user.email, "created_at": datetime.now(timezone.utc)}
```

## Best Practices

- Split input and output models to avoid leaking sensitive fields.
- Use `EmailStr`, `HttpUrl`, and other built-in types for strong validation.
- Enable strict mode for security-critical inputs.
- Use `from_attributes=True` when returning ORM objects.
- Prefer `Annotated` with `Field` for reusable constraints.

## Summary

You learned:

- How to build Pydantic models with field constraints
- How to validate with field and model validators
- How to serialize data and customize output
- How to validate data with `TypeAdapter`
- How to manage configuration with `pydantic-settings`
- How to integrate Pydantic models in FastAPI routes

## References

- [Pydantic Models](https://docs.pydantic.dev/latest/concepts/models/)
- [Pydantic Fields](https://docs.pydantic.dev/latest/concepts/fields/)
- [Pydantic Validators](https://docs.pydantic.dev/latest/concepts/validators/)
- [Pydantic Serialization](https://docs.pydantic.dev/latest/concepts/serialization/)
- [Pydantic Unions](https://docs.pydantic.dev/latest/concepts/unions/)
- [Pydantic Settings](https://docs.pydantic.dev/latest/concepts/pydantic_settings/)
- [FastAPI Response Models](https://fastapi.tiangolo.com/tutorial/response-model/)

## Next Steps

- [Request Bodies](./04-request-bodies.md) - Handle JSON and form data
- [Response Models](./05-response-models.md) - Configure API responses
- [GenAI Integration](./17-genai-integration.md) - Build AI-powered APIs

---

[Previous: Routing](./02-routing.md) | [Back to Index](./README.md) | [Next: Request Bodies](./04-request-bodies.md)
