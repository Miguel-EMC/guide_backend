# Introduction and Setup

This chapter covers FastAPI fundamentals, installation, and creating your first application.

## What is FastAPI?

FastAPI is a modern, fast web framework for building APIs with Python based on standard type hints. It is built on Starlette and Pydantic and generates OpenAPI and JSON Schema automatically.

### Key Features

| Feature | Description |
|---------|-------------|
| High performance | Excellent performance for Python APIs |
| Type-driven | Uses type hints for validation and docs |
| Automatic docs | Swagger UI and ReDoc out of the box |
| Async support | Full async and sync endpoint support |
| Standards based | OpenAPI and JSON Schema |

## Prerequisites

- Python 3.9+ (3.10+ recommended)
- Basic Python knowledge
- A code editor
- Terminal/command line access

## Installation

### Option A: uv (recommended)

```bash
uv init fastapi-project
cd fastapi-project
uv add "fastapi[standard]"

# Run the dev server
uv run fastapi dev main.py
```

### Option B: venv + pip

```bash
python -m venv .venv
source .venv/bin/activate  # Linux/macOS
# .venv\Scripts\activate  # Windows

pip install "fastapi[standard]"
fastapi dev main.py
```

### What does `fastapi[standard]` include?

The standard extra bundles the FastAPI CLI and common runtime dependencies, including:

- `fastapi-cli[standard]` (adds the `fastapi` command)
- `uvicorn[standard]` (ASGI server)
- `httpx` (used by the CLI for testing)
- `jinja2` (templates)
- `python-multipart` (form data)
- `email-validator` (email validation)

If you do not want the FastAPI Cloud CLI, install:

```bash
pip install "fastapi[standard-no-fastapi-cloud-cli]"
```

## Your First Application

Create `main.py`:

```python
from fastapi import FastAPI

app = FastAPI()


@app.get("/")
async def read_root():
    return {"message": "Hello, World!"}


@app.get("/items/{item_id}")
async def read_item(item_id: int, q: str | None = None):
    return {"item_id": item_id, "q": q}
```

If you are on Python 3.9, use `Optional[str]` instead of `str | None`.

## Run the Server

### FastAPI CLI (recommended)

```bash
# Development (auto reload, 127.0.0.1)
fastapi dev main.py

# Production-like run (0.0.0.0)
fastapi run main.py
```

### With uv

```bash
uv run fastapi dev main.py
```

### With uvicorn

```bash
uvicorn main:app --reload
```

## Interactive Documentation

FastAPI generates docs automatically:

| URL | Description |
|-----|-------------|
| `http://127.0.0.1:8000/docs` | Swagger UI |
| `http://127.0.0.1:8000/redoc` | ReDoc |

## Application Configuration

```python
from fastapi import FastAPI

app = FastAPI(
    title="My API",
    description="Sample API built with FastAPI",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json",
)
```

## Project Structure

For small demos:

```
fastapi-project/
├── main.py
├── pyproject.toml
└── uv.lock
```

For production services:

```
fastapi-project/
├── app/
│   ├── __init__.py
│   └── main.py
├── tests/
│   └── test_health.py
├── pyproject.toml
└── uv.lock
```

## Common Issues

### Port already in use

```bash
uvicorn main:app --reload --port 8001
```

### Module not found

```bash
# Ensure your virtual environment is active
source .venv/bin/activate
```

### Validation errors (422)

If a request does not match your type hints or Pydantic model, FastAPI returns a 422 response. Check your request body and query parameters.

## Summary

You learned:

- What FastAPI is and why it is popular
- How to install FastAPI with uv or pip
- How to run the development server
- How to use automatic documentation
- How to structure a basic project

## References

- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [FastAPI CLI](https://fastapi.tiangolo.com/fastapi-cli/)
- [FastAPI Installation](https://fastapi.tiangolo.com/)

## Next Steps

- [Routing & Endpoints](./02-routing.md) - Learn HTTP methods and parameters
- [Data Validation](./03-data-validation.md) - Master Pydantic validation
- [GenAI Integration](./17-genai-integration.md) - Build AI-powered APIs

---

[Back to Index](./README.md) | [Next: Routing & Endpoints](./02-routing.md)
