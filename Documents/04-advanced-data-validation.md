# Data Validation with Pydantic (Part 2)

## Nested Models and Complex Data Structures

### Nested Models

Pydantic models can be nested to represent complex data structures:

```Python
from pydantic import BaseModel
from typing import Optional

class Address(BaseModel):
    street: str
    city: str
    state: str
    zip_code: str
    country: str

class User(BaseModel):
    id: int
    name: str
    email: str
    address: Address
```

Example use:

```Python
# Example use
user = User(
    id=1,
    name="John Doe",
    email="john@example.com",
    address=Address(
        street="123 Main St",
        city="Anytown",
        state="CA",
        zip_code="12345",
        country="USA"
    )
)
```

### Lists of Models

You can work with lists of models for more complex data:

```Python
from pydantic import BaseModel
from typing import List

class Tag(BaseModel):
    id: int
    name: str

class Post(BaseModel):
    id: int
    title: str
    content: str
    tags: list[Tag] = []

# Example
post = Post(
    id=1,
    title="Hello World",
    content="This is my first post",
    tags=[
        Tag(id=1, name="python"),
        Tag(id=2, name="pydantic"),
        Tag(id=3, name="fastapi")
    ]
)
```

### Recursive Models

For self-referential structures like trees, use postponed annotations:

```Python
from pydantic import BaseModel
from typing import List, Optional

class Comment(BaseModel):
    id: int
    text: str
    replies: list['Comment'] = []

Comment.model_rebuild() # Required in Pydantic V2 for recursive models

# Example
comment = Comment(
    id=1,
    text="Great article!",
    replies=[
        Comment(id=2, text="Thanks!"),
        Comment(
            id=3,
            text="I agree!",
            replies=[
                Comment(id=4, text="Me too!")
            ]
        )
    ]
)
```

## Complex Data Structures in FastAPI

Putting it all together with FastAPI:

```Python
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime

app = FastAPI()

class AddressCreate(BaseModel):
    street: str
    city: str
    state: str
    zip_code: str
    country: str

class Address(AddressCreate):
    id: int

class OrderItem(BaseModel):
    product_id: int
    quantity: int = Field(gt=0)
    unit_price: float = Field(gt=0)

class OrderCreate(BaseModel):
    user_id: int
    shipping_address_id: int
    items: List[OrderItem]
    notes: Optional[str] = None

class OrderItemResponse(OrderItem):
    total: float

class OrderResponse(BaseModel):
    id: int
    user_id: int
    shipping_address_id: int
    items: List[OrderItemResponse]
    notes: Optional[str] = None
    created_at: datetime
    total_amount: float

@app.post("/orders/", response_model=OrderResponse)
async def create_order(order: OrderCreate):
    # Convert OrderItems to OrderItemResponse with calculated totals
    response_items = []
    for item in order.items:
        response_items.append(
            OrderItemResponse(
                product_id=item.product_id,
                quantity=item.quantity,
                unit_price=item.unit_price,
                total=item.quantity * item.unit_price
            )
        )

    # Calculate total amount
    total_amount = sum(item.total for item in response_items)

    # Create and return the response object
    return OrderResponse(
        id=1,
        user_id=order.user_id,
        shipping_address_id=order.shipping_address_id,
        items=response_items,
        notes=order.notes,
        created_at=datetime.now(),
        total_amount=total_amount
    )
```

Use following data for testing:

```JSON
{
    "user_id": 42,
    "shipping_address_id": 123,
    "items": [
        {
            "product_id": 1001,
            "quantity": 2,
            "unit_price": 29.99
        },
        {
            "product_id": 1002,
            "quantity": 1,
            "unit_price": 49.99
        }
    ],
    "notes": "Please deliver before 5 PM"
}
```

## Best Practices for Using Pydantic with FastAPI

### Define separate models for input and output:

```Python
class UserCreate(BaseModel): # Input model
    username: str
    email: EmailStr
    password: str

class UserResponse(BaseModel): # Output model
    id: int
    username: str
    email: str
```

### Use strict validation when needed:

```Python
from pydantic import BaseModel, ConfigDict

class StrictModel(BaseModel):
    model_config = ConfigDict(strict=True)

    # This will now only accept exact types, not coerce them
    age: int
```

### Leverage model inheritance for DRY code:

```Python
class UserBase(BaseModel):
    username: str
    email: EmailStr

class UserCreate(UserBase):
    password: str

class UserUpdate(BaseModel):
    username: Optional[str] = None
    email: Optional[EmailStr] = None
    password: Optional[str] = None

class UserInDB(UserBase):
    id: int
    hashed_password: str
```

### Use computed fields and validators for complex logic:

```Python
class Product(BaseModel):
    name: str
    price: float
    tax: float = 0.0

    @model_validator(mode='after')
    def calculate_total_price(self) -> 'Product':
        self.total_price = self.price + self.tax
        return self
```

### Document your models for better API docs:

```Python
class User(BaseModel):
    """
    User information for authentication and identification.
    """
    username: str = Field(description="Username for login")
    email: EmailStr = Field(description="User's email address")
    role: str = Field(
        default="user",
        description="User role for permissions",
        examples=["user", "admin", "moderator"]
    )
```

## Conclusion

Pydantic V2 provides a powerful foundation for data validation in FastAPI applications. By using Pydantic models, you can:

* Ensure data correctness through type validation
* Reduce boilerplate code with automatic parsing and validation
* Create self-documenting APIs with clear data structures
* Build complex validation logic through validators and nested models

The combination of FastAPI and Pydantic offers a robust, high-performance framework for developing APIs with strong type safety and excellent developer experience.
