# Request Bodies and Form Data (Part 1)

This tutorial covers how to work with different types of input data in FastAPI applications, including JSON request bodies, form data, and file uploads.

## Setup

First, install FastAPI and its dependencies:

```bash
pip install fastapi uvicorn python-multipart
```

-   `fastapi`: The web framework
-   `uvicorn`: ASGI server for running the application
-   `python-multipart`: For handling form data and file uploads

## 1. Working with JSON Request Bodies

FastAPI makes it easy to work with JSON request bodies using Pydantic models.

### Basic Example

Create a file named `json_example.py`:

```python
from fastapi import FastAPI
from pydantic import BaseModel
from typing import List, Optional

app = FastAPI()

# Define a Pydantic model for the request body
class Item(BaseModel):
    name: str
    description: Optional[str] = None
    price: float
    tax: Optional[float] = None
    tags: List[str] = []

@app.post("/items/")
async def create_item(item: Item):
    # FastAPI will automatically validate and convert the JSON request body to the Item model

    # You can access the data as regular Python attributes
    item_dict = item.model_dump() # Convert to dictionary (model_dict() in older versions)

    # You can perform operations with the received data
    if item.tax:
        price_with_tax = item.price + item.tax
        item_dict["price_with_tax"] = price_with_tax
    return item_dict
```

Run with: `uvicorn json_example:app --reload`

**Testing with cURL:**

```bash
curl -X POST http://localhost:8000/items/ \
-H "Content-Type: application/json" \
-d '{"name": "Keyboard", "price": 59.99, "tax": 5.50, "tags": ["computer", "accessories"]}'
```

**Key Points:**

-   FastAPI automatically parses and validates JSON requests against the Pydantic model
-   Required and optional fields can be specified in the model
-   Default values can be provided
-   Type validation is automatic (strings, numbers, lists, etc.)
-   You don't need to explicitly extract JSON data - FastAPI does it for you

### Nested Models

You can use nested Pydantic models for more complex data structures:

```python
from fastapi import FastAPI
from pydantic import BaseModel
from typing import List, Optional

app = FastAPI()

class Address(BaseModel):
    street: str
    city: str
    postal_code: str
    country: str = "United States"

class User(BaseModel):
    username: str
    email: str
    full_name: Optional[str] = None
    addresses: List[Address]

@app.post("/users/")
async def create_user(user: User):
    return user
```

Run with: `uvicorn nested_models:app --reload`

**Testing with cURL:**

```bash
curl -X POST http://localhost:8000/users/ \
-H "Content-Type: application/json" \
-d '{"username":"johndoe","email":"john@example.com","addresses":[{"street":"123 Main St","city":"New York","postal_code":"10001"}]}'
```

## 2. Working with Form Data

For HTML forms or `multipart/form-data` requests, FastAPI provides form parameter handling.

### Basic Form Data

Create a file named `form_example.py`:

```python
from fastapi import FastAPI, Form
import uvicorn

app = FastAPI()

@app.post("/login/")
async def login(username: str = Form(), password: str = Form()):
    # Form() tells FastAPI to expect form data instead of JSON
    return {"username": username}
```

Run with: `uvicorn form_example:app --reload`

**Testing with cURL:**

```bash
curl -X POST http://localhost:8000/login/ \
-H "Content-Type: application/x-www-form-urlencoded" \
-d "username=johndoe&password=secret"
```

**Key Points:**

-   Use `Form()` to declare form fields
-   The `Content-Type` should be `application/x-www-form-urlencoded` or `multipart/form-data`
-   Install `python-multipart` for form handling

### Optional Form Fields

```python
from fastapi import FastAPI, Form
from typing import Optional

app = FastAPI()

@app.post("/profile/")
async def update_profile(
    username: str = Form(),
    full_name: Optional[str] = Form(None),
    bio: Optional[str] = Form(None)
):
    return {
        "username": username,
        "full_name": full_name,
        "bio": bio
    }
```

## 3. File Uploads and Handling

FastAPI provides easy file upload handling for both single and multiple files.

### Single File Upload

Create a file named `file_upload.py`:

```python
from fastapi import FastAPI, File, UploadFile
import shutil
from pathlib import Path
from tempfile import NamedTemporaryFile

app = FastAPI()

@app.post("/uploadfile/")
async def upload_file(file: UploadFile):
    # The UploadFile class has several useful attributes and methods
    content = await file.read() # Read the file content

    # Example: Save the file to disk
    temp_file = NamedTemporaryFile(delete=False)
    try:
        # Write the file content to the temporary file
        with open(temp_file.name, 'wb') as f:
            f.write(content)
        temp_file.close()

        # Move the temporary file to the desired location
        output_file = Path(f"uploaded_{file.filename}")
        shutil.move(temp_file.name, output_file)
    finally:
        # Close the file if it's still open
        if not temp_file.closed:
            temp_file.close()
    return {
        "filename": file.filename,
        "content_type": file.content_type,
        "file_size": len(content),
        "saved_as": str(output_file)
    }
```

Run with: `uvicorn file_upload:app --reload`

### Multiple File Upload

```python
from fastapi import FastAPI, File, UploadFile
from typing import List

app = FastAPI()

@app.post("/uploadfiles/")
async def upload_files(files: List[UploadFile]):
    return [
        {
            "filename": file.filename,
            "content_type": file.content_type,
            "file_size": len(await file.read())
        }
        for file in files
    ]
```

**Testing with cURL:**

-   **Single file upload**
    ```bash
    curl -X POST http://localhost:8000/uploadfile/ \
    -F "file=@/path/to/your/file.txt"
    ```
-   **Multiple file upload**
    ```bash
    curl -X POST http://localhost:8000/uploadfiles/ \
    -F "files=@/path/to/file1.txt" \
    -F "files=@/path/to/file2.png"
    ```

**Key Points:**

-   Use `UploadFile` for better handling (spooled file, async operations)
-   `UploadFile` provides attributes like `filename`, `content_type`, etc.
-   Methods: `file.read()`, `file.seek()`, `file.close()`
-   For binary data without a file, use `File()` instead

### File Upload With Additional Form Data

```python
from fastapi import FastAPI, File, UploadFile, Form

app = FastAPI()

@app.post("/upload-with-info/")
async def upload_with_info(
    file: UploadFile,
    description: str = Form(),
    category: str = Form()
):
    content = await file.read()
    return {
        "filename": file.filename,
        "size": len(content),
        "description": description,
        "category": category
    }
```
