# Request Bodies and Form Data (Part 2)

## 4. Headers and Cookies

FastAPI makes it easy to access and validate HTTP headers and cookies.

### Working with Headers

Create a file named `headers_example.py`:

```python
from fastapi import FastAPI, Header
from typing import Optional, List

app = FastAPI()

@app.get("/headers/")
async def read_headers(
    user_agent: Optional[str] = Header(None),
    x_token: Optional[str] = Header(None, convert_underscores=False),
    accept_language: Optional[List[str]] = Header(None)
):
    # Headers are automatically converted from "User-Agent" to "user_agent"
    # For headers like "X-Token", use convert_underscores=False
    # Some headers like Accept-Language can have multiple values
    return {
        "User-Agent": user_agent,
        "X-Token": x_token,
        "Accept-Language": accept_language
    }
```

Run with: `uvicorn headers_example:app --reload`

**Testing with cURL:**

```bash
curl -X GET http://localhost:8000/headers/ \
-H "User-Agent: My-Custom-User-Agent" \
-H "X-Token: my-secret-token" \
-H "Accept-Language: en-US,fr;q=0.9"
```

**Key Points:**

-   Use `Header()` to extract specific headers
-   Headers with hyphens (`-`) are converted to underscores (`_`) by default
-   Use `convert_underscores=False` for headers like `X-Token`
-   Some headers can be lists (comma-separated values)

### Working with Cookies

Create a file named `cookies_example.py`:

```python
from fastapi import FastAPI, Cookie, Response
from typing import Optional

app = FastAPI()

@app.get("/cookies/")
async def read_cookies(
    session_id: Optional[str] = Cookie(None),
    preference: Optional[str] = Cookie(None)
):
    # Get cookies using Cookie()
    return {
        "session_id": session_id,
        "preference": preference
    }

@app.post("/set-cookies/")
async def set_cookies(response: Response):
    # Set cookies in the response
    response.set_cookie(key="session_id", value="abc123")
    response.set_cookie(
        key="preference",
        value="dark_mode",
        max_age=3600, # seconds (1 hour)
        httponly=True # Cookie not accessible via JavaScript
    )
    return {"message": "Cookies have been set"}
```

Run with: `uvicorn cookies_example:app --reload`

**Testing with cURL:**

-   **Setting cookies**
    ```bash
    curl -X POST http://localhost:8000/set-cookies/ -v
    ```
-   **Reading cookies (after they've been set)**
    ```bash
    curl -X GET http://localhost:8000/cookies/ --cookie "session_id=abc123; preference=dark_mode"
    ```

**Key Points:**

-   Use `Cookie()` to access cookies from the request
-   Use `response.set_cookie()` to set cookies in the response
-   Cookie parameters:
    -   `key`: Cookie name
    -   `value`: Cookie value
    -   `max_age`: Cookie lifetime in seconds
    -   `expires`: Specific expiration datetime
    -   `path`: URL path where cookie is valid
    -   `domain`: Domain where cookie is valid
    -   `secure`: Only send over HTTPS
    -   `httponly`: Not accessible via JavaScript
    -   `samesite`: "lax", "strict", or "none"

## 5. Combining Different Types of Parameters

In a real application, you'll often need to mix different types of parameters.

### Combined Example

```python
from fastapi import FastAPI, Form, File, UploadFile, Header, Cookie
from typing import Optional, List
from pydantic import BaseModel

app = FastAPI()

class ProductMetadata(BaseModel):
    tags: List[str]
    categories: List[str]

@app.post("/products/")
async def create_product(
    # Form data
    name: str = Form(),
    price: float = Form(),
    # JSON body (must be sent as a form field named "metadata")
    metadata: Optional[str] = Form(None),
    # File uploads
    image: UploadFile = File(...),
    documents: Optional[List[UploadFile]] = File(None),
    # Headers
    x_token: Optional[str] = Header(None),
    # Cookies
    user_id: Optional[str] = Cookie(None)
):
    # Process the metadata JSON if provided
    metadata_obj = None
    if metadata:
        import json
        metadata_dict = json.loads(metadata)
        metadata_obj = ProductMetadata(**metadata_dict)

    # Process the image
    image_content = await image.read()
    image_size = len(image_content)

    # Process other documents if any
    document_info = []
    if documents:
        for doc in documents:
            doc_content = await doc.read()
            document_info.append({
                "filename": doc.filename,
                "size": len(doc_content),
                "content_type": doc.content_type
            })

    return {
        "product": {
            "name": name,
            "price": price,
            "metadata": metadata_obj.model_dump() if metadata_obj else None,
        },
        "image": {
            "filename": image.filename,
            "size": image_size,
            "content_type": image.content_type
        },
        "documents": document_info,
        "auth": {
            "token": x_token,
            "user_id": user_id
        }
    }
```

**Testing with cURL:**

```bash
curl -X POST http://localhost:8000/products/ \
-H "X-Token: my-token" \
--cookie "user_id=user123" \
-F "name=New Product" \
-F "price=99.99" \
-F "metadata={\"tags\":[\"electronics\",\"gadgets\"],\"categories\":[\"tech\"]}" \
-F "image=@/path/to/image.jpg" \
-F "documents=@/path/to/doc1.pdf" \
-F "documents=@/path/to/doc2.txt"
```

## 6. Advanced Topics

### Dependency Injection for Headers

For reusable header validation, use FastAPI's dependency injection system:

```python
from fastapi import FastAPI, Header, HTTPException, Depends
from typing import Optional

app = FastAPI()

async def verify_token(x_token: Optional[str] = Header(None)):
    if x_token is None:
        raise HTTPException(status_code=401, detail="X-Token header missing")
    if x_token != "valid-token":
        raise HTTPException(status_code=401, detail="Invalid X-Token")
    return x_token

@app.get("/protected/")
async def protected_route(token: str = Depends(verify_token)):
    return {"message": "This is protected data", "token": token}
```

### Custom Form Data Validation

For more complex form data validation, combine `Form` with Pydantic:

```python
from fastapi import FastAPI, Form, HTTPException
from pydantic import BaseModel, Field, field_validator
import json

app = FastAPI()

class UserProfile(BaseModel):
    username: str = Field(..., min_length=3, max_length=50)
    email: str
    age: int = Field(..., ge=18)

    @field_validator('email')
    def email_must_be_valid(cls, v):
        if '@' not in v:
            raise ValueError('must be a valid email')
        return v

@app.post("/validate-profile/")
async def validate_profile(
    profile_json: str = Form(...)
):
    try:
        # Parse the JSON from the form field
        profile_dict = json.loads(profile_json)
        # Validate using Pydantic
        profile = UserProfile(**profile_dict)
        return profile.model_dump()
    except json.JSONDecodeError:
        raise HTTPException(status_code=400, detail="Invalid JSON in form data")
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
```

## Conclusion

FastAPI provides powerful and intuitive tools for handling various types of input data:

-   **JSON Request Bodies**: Use Pydantic models for automatic validation
-   **Form Data**: Use `Form()` for handling form-encoded data
-   **File Uploads**: Use `UploadFile` for file handling with streaming support
-   **Headers**: Use `Header()` to extract and validate HTTP headers
-   **Cookies**: Use `Cookie()` to read cookies and `Response` methods to set them

When building real-world APIs, you'll often combine these features to create comprehensive endpoints that handle different types of data sources simultaneously.
