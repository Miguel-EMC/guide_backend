# Introduction & Setup

This guide covers FastAPI fundamentals, installation, and creating your first application.

## What is FastAPI?

FastAPI is a modern, high-performance web framework for building APIs with Python based on standard Python type hints. It's one of the fastest Python frameworks available.

### Key Features

| Feature | Description |
|---------|-------------|
| **High Performance** | Built on Starlette and Pydantic, comparable to Node.js and Go |
| **Type Safety** | Leverages Python type hints for validation and documentation |
| **Auto Documentation** | Swagger UI and ReDoc generated automatically |
| **Async Support** | Full async/await support for concurrent requests |
| **GenAI Ready** | Native support for AI/LLM integration and streaming responses |
| **Standards Based** | Built on OpenAPI and JSON Schema standards |

### Why Choose FastAPI?

1. **Performance**: One of the fastest Python frameworks
2. **Developer Experience**: Intuitive API, excellent IDE support
3. **Automatic Validation**: Request data validated automatically
4. **Interactive Docs**: Test your API directly in the browser
5. **Modern Python**: Uses type hints and async/await

## Prerequisites

Before starting, ensure you have:

- Python 3.9+ installed (3.10+ recommended)
- Basic Python knowledge
- A code editor (VS Code recommended)
- Terminal/command line access

## Installation

### Step 1: Set Up Python Environment

We recommend using `uv` for faster package management and better performance. `venv` is still supported as an alternative.

#### Option 1: uv (Recommended for 2026)

`uv` is 10-100x faster than pip and includes excellent project management.

**Install uv:**

```bash
# Linux/macOS
curl -LsSf https://astral.sh/uv/install.sh | sh

# Windows (PowerShell)
powershell -c "irm https://astral.sh/uv/install.ps1 | iex"

# Using pip (slower)
pip install uv
```

**Create project with uv:**

```bash
# Create new FastAPI project
uv init fastapi-project
cd fastapi-project

# Install FastAPI with uv
uv add "fastapi[standard]"

# Or create in existing directory
uv venv
source .venv/bin/activate  # Linux/macOS
# .venv\Scripts\activate  # Windows
uv pip install "fastapi[standard]"
```

**Benefits of uv:**
- ⚡ 10-100x faster than pip
- 🔄 Better dependency resolution
- 📦 Built-in virtual environment management
- 🔒 Lock files for reproducible builds
- 🌐 Parallel downloads and installations

#### Option 2: Traditional venv (Still Supported)

Classic approach with `venv` for compatibility.

**Linux / macOS:**

```bash
python3 -m venv venv
source venv/bin/activate
```

**Windows:**

```bash
python -m venv venv
venv\Scripts\activate
```

#### Environment Comparison

| Feature | uv (Recommended) | venv (Traditional) |
|---------|------------------|---------------------|
| **Speed** | 10-100x faster | Standard pip speed |
| **Dependency Resolution** | Excellent | Basic |
| **Lock Files** | `uv.lock` | `requirements.txt` |
| **Project Management** | Built-in | Manual |
| **Compatibility** | New standard | Universal |

### Step 2: Install FastAPI

#### With uv (Recommended)

```bash
# Install FastAPI with all standard dependencies
uv add "fastapi[standard]"

# Or install additional packages
uv add httpx pytest pytest-asyncio
```

#### With pip (Traditional)

```bash
# Standard Installation
pip install fastapi uvicorn

# Full Installation (Recommended)
pip install "fastapi[standard]"
```

This includes:
- `uvicorn` - ASGI server
- `pydantic` - Data validation
- `python-multipart` - Form data support
- Additional utilities

#### Verify Installation

**With uv:**

```bash
# Show installed packages
uv pip list

# Verify FastAPI version
uv run python -c "import fastapi; print(f'FastAPI: {fastapi.__version__}')"
```

**With venv:**

```bash
python check_install.py
```

### Step 3: Create Verification Script

Create `check_install.py`:

```python
import fastapi
import uvicorn
import pydantic

print(f"FastAPI version: {fastapi.__version__}")
print(f"Uvicorn version: {uvicorn.__version__}")
print(f"Pydantic version: {pydantic.__version__}")

# Check installation method
import sys
import os

if os.path.exists('uv.lock'):
    print("Installation method: uv (recommended)")
    print("✅ Using modern fast package manager")
elif os.path.exists('venv'):
    print("Installation method: venv (traditional)")
    print("ℹ️  Consider migrating to uv for better performance")
else:
    print("Installation method: unknown")
    print("⚠️  Consider using uv for optimal experience")

print(f"Python version: {sys.version}")
```

**Run with uv:**

```bash
uv run python check_install.py
```

**Run with venv:**

```bash
python check_install.py
```

Expected output (2026):

```
FastAPI version: 0.128.0
Uvicorn version: 0.32.x
Pydantic version: 2.10.x
Installation method: uv (recommended)
✅ Using modern fast package manager
Python version: 3.11.x
```

## Running Your Development Server

### With uv (Recommended)

```bash
# Run with uv (faster startup)
uv run uvicorn main:app --reload

# Or if using uv venv
uvicorn main:app --reload

# With custom settings
uv run uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

### With Traditional Setup

```bash
# Run with traditional venv
uvicorn main:app --reload

# With custom settings
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

### uv Performance Benefits

Using `uv run` provides:
- 🚀 **Faster startup** - 2-3x quicker cold starts
- 💾 **Memory efficiency** - Better memory management
- 🔒 **Dependency isolation** - Cleaner dependency resolution
- 📊 **Performance monitoring** - Built-in performance stats

## Your First Application

### Step 1: Create Basic App

Create `main.py`:

```python
from fastapi import FastAPI

# Create FastAPI instance
app = FastAPI()

# Define root endpoint
@app.get("/")
def read_root():
    return {"message": "Hello, World!"}

# Define path parameter endpoint
@app.get("/items/{item_id}")
def read_item(item_id: int):
    return {"item_id": item_id}
```

**Code Breakdown:**

| Element | Description |
|---------|-------------|
| `FastAPI()` | Creates the application instance |
| `@app.get("/")` | Decorator defining GET endpoint at root path |
| `item_id: int` | Type hint that validates and converts the parameter |

### Step 2: Run the Server

```bash
uvicorn main:app --reload
```

**Command explanation:**

| Part | Meaning |
|------|---------|
| `main` | The file `main.py` |
| `app` | The FastAPI instance in the file |
| `--reload` | Auto-restart on code changes (development only) |

### Step 3: Test Your API

Open your browser:

- `http://127.0.0.1:8000/` - Returns: `{"message": "Hello, World!"}`
- `http://127.0.0.1:8000/items/5` - Returns: `{"item_id": 5}`
- `http://127.0.0.1:8000/items/abc` - Returns: 422 validation error

### Step 4: Explore Documentation

FastAPI generates interactive documentation automatically:

| URL | Documentation Type |
|-----|-------------------|
| `http://127.0.0.1:8000/docs` | Swagger UI - Interactive testing |
| `http://127.0.0.1:8000/redoc` | ReDoc - Clean, readable format |

**Features:**
- View all endpoints
- See expected parameters
- Test API directly
- View response schemas

## Adding Request Body

Enhance your app with POST endpoints:

```python
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI()

# Define request model
class Item(BaseModel):
    name: str
    price: float
    is_offer: bool = False

@app.get("/")
def read_root():
    return {"message": "Hello, World!"}

@app.get("/items/{item_id}")
def read_item(item_id: int, q: str = None):
    return {"item_id": item_id, "q": q}

@app.post("/items/")
def create_item(item: Item):
    return item

@app.put("/items/{item_id}")
def update_item(item_id: int, item: Item):
    return {"item_id": item_id, **item.model_dump()}
```

**Model Fields:**

| Field | Type | Required | Default |
|-------|------|----------|---------|
| `name` | `str` | Yes | - |
| `price` | `float` | Yes | - |
| `is_offer` | `bool` | No | `False` |

## Application Configuration

Add metadata to improve documentation:

```python
from fastapi import FastAPI

app = FastAPI(
    title="My API",
    description="A sample API built with FastAPI",
    version="1.0.0",
    docs_url="/docs",        # Swagger UI path
    redoc_url="/redoc",      # ReDoc path
    openapi_url="/openapi.json"  # OpenAPI schema
)
```

**Configuration Options:**

| Parameter | Description | Default |
|-----------|-------------|---------|
| `title` | API name in docs | "FastAPI" |
| `description` | API description | "" |
| `version` | API version | "0.1.0" |
| `docs_url` | Swagger UI URL | "/docs" |
| `redoc_url` | ReDoc URL | "/redoc" |
| `openapi_url` | OpenAPI JSON URL | "/openapi.json" |

## Project Structure

For simple projects:

```
my_project/
├── main.py           # Application entry
├── requirements.txt  # Dependencies
└── .env             # Environment variables
```

For larger projects, see [Project Architecture](./13-architecture.md).

## Common Issues

### Port Already in Use

```bash
# Find process using port 8000
lsof -i :8000  # Linux/macOS
netstat -ano | findstr :8000  # Windows

# Use different port
uvicorn main:app --reload --port 8001
```

### Module Not Found

```bash
# Ensure virtual environment is activated
source venv/bin/activate

# Reinstall dependencies
pip install "fastapi[standard]"
```

### Type Hints Not Working

Ensure Python 3.9+ or use:

```python
from typing import Optional, List, Union
```

### Pydantic v2 Migration Issues

FastAPI 0.128.0 dropped Pydantic v1 support completely. Common issues:

```python
# Old Pydantic v1 (NO LONGER SUPPORTED)
from pydantic import BaseModel

# New Pydantic v2 (REQUIRED)
from pydantic import BaseModel, Field

class Item(BaseModel):
    name: str = Field(..., description="Item name")
    price: float = Field(gt=0, description="Price must be positive")
```

**Key Changes:**
- `Config` class → `model_config`
- `@validator` → `@field_validator`
- `dict()` method → `model_dump()`
```

## Summary

You learned:

- What FastAPI is and its advantages
- How to install FastAPI and dependencies
- Creating your first API endpoint
- Using path parameters with type hints
- Working with request bodies (Pydantic models)
- Accessing automatic documentation

## FastAPI for GenAI in 2026

FastAPI has become the backbone of modern AI applications. Key advantages for GenAI:

### Streaming Responses
```python
from fastapi import FastAPI
from fastapi.responses import StreamingResponse
import asyncio

app = FastAPI()

async def generate_stream():
    for i in range(10):
        yield f"data: {i}\n\n"
        await asyncio.sleep(0.1)

@app.get("/stream")
async def stream_llm_response():
    return StreamingResponse(generate_stream(), media_type="text/plain")
```

### AI SDK Integration
FastAPI seamlessly integrates with major AI platforms:
- OpenAI SDK
- Anthropic Claude
- Hugging Face Transformers
- LangChain
- Local LLMs (Ollama, Llama.cpp)

### Async-First Architecture
Perfect for I/O-heavy AI workloads:
- Multiple concurrent LLM calls
- Vector database queries
- Background processing for embeddings

## Next Steps

- [Routing & Endpoints](./02-routing.md) - Learn about HTTP methods and parameters
- [Data Validation](./03-data-validation.md) - Master Pydantic validation
- [GenAI Integration](./genai-integration.md) - Build AI-powered APIs *(Coming Soon)*

---

[Back to Index](./README.md) | [Next: Routing & Endpoints](./02-routing.md)
