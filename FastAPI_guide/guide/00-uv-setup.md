# Fast Project Setup with uv (2026 Best Practices)

This chapter covers modern FastAPI project setup using `uv`, the ultra-fast Python package manager that's 10-100x faster than pip.

## Why uv for FastAPI?

| Feature | uv | pip |
|---------|-----|-----|
| **Installation Speed** | 10-100x faster | Standard speed |
| **Development Server** | 2-3x faster startup | Normal startup |
| **Dependency Resolution** | Excellent, no conflicts | Basic, occasional conflicts |
| **Project Management** | Built-in project init | Manual setup |
| **Reproducibility** | Lock files included | Manual requirements.txt |
| **Memory Usage** | 30-50% less | Higher memory usage |

## Installing uv

### Method 1: Install Script (Recommended)

```bash
# Linux/macOS
curl -LsSf https://astral.sh/uv/install.sh | sh

# Windows (PowerShell)
powershell -c "irm https://astral.sh/uv/install.ps1 | iex"
```

### Method 2: pip install

```bash
# Install with pip (slower, universal)
pip install uv
```

### Method 3: Package Managers

```bash
# macOS with Homebrew
brew install uv

# Arch Linux
pacman -S uv

# Windows with Scoop
scoop install uv
```

## Creating FastAPI Projects with uv

### Option 1: Project Template (Fastest)

```bash
# Create FastAPI project from template
uv init --template fastapi my-api
cd my-api

# Install dependencies automatically
uv sync
```

**Generated Structure:**
```
my-api/
├── .gitignore
├── .python-version
├── pyproject.toml
├── uv.lock
├── README.md
├── app/
│   ├── __init__.py
│   └── main.py
└── tests/
    ├── __init__.py
    └── test_main.py
```

### Option 2: Manual Setup (Full Control)

```bash
# Create new project
mkdir my-fastapi-app
cd my-fastapi-app

# Initialize with uv
uv init

# Add FastAPI dependencies
uv add "fastapi[standard]"
uv add httpx pytest pytest-asyncio

# Add optional packages
uv add sqlalchemy alembic  # For database
uv add redis             # For caching
uv add celery            # For background tasks
```

## Advanced uv Configuration

### pyproject.toml Optimization

```toml
[project]
name = "my-fastapi-app"
version = "0.1.0"
description = "Modern FastAPI application with uv"
authors = [
    {name = "Your Name", email = "your.email@example.com"},
]
dependencies = [
    "fastapi[standard] >=0.128.0",
    "pydantic >=2.10.0",
    "uvicorn[standard] >=0.32.0",
]
requires-python = ">=3.9"

[project.optional-dependencies]
dev = [
    "pytest >=8.0.0",
    "pytest-asyncio >=0.24.0",
    "pytest-cov >=4.0.0",
    "httpx >=0.28.0",
    "black >=24.0.0",
    "ruff >=0.5.0",
    "mypy >=1.10.0",
]
database = [
    "sqlalchemy >=2.0.0",
    "alembic >=1.13.0",
    "psycopg2-binary >=2.9.0",
]
ai = [
    "openai >=1.30.0",
    "anthropic >=0.8.0",
    "tiktoken >=0.7.0",
]

[build-system]
requires = ["hatchling"]
build-backend = "hatchling.build"

[tool.uv]
dev-dependencies = [
    "ruff",
    "black",
    "mypy",
]

[tool.uv.sources]
# Custom package sources if needed
# my-package = { workspace = true }

[tool.black]
line-length = 88
target-version = ['py39']

[tool.ruff]
line-length = 88
select = ["E", "F", "I"]
target-version = "py39"

[tool.mypy]
python_version = "3.9"
warn_return_any = true
warn_unused_configs = true
```

## Development Workflow with uv

### Installing Dependencies

```bash
# Install from pyproject.toml
uv sync

# Install specific optional groups
uv sync --extra dev
uv sync --extra database
uv sync --extra dev,database

# Add new dependency
uv add "pydantic-settings>=2.5.0"

# Add development dependency
uv add --dev "pytest-mock>=3.14.0"

# Remove dependency
uv remove sqlalchemy
```

### Running Commands

```bash
# Run FastAPI app
uv run uvicorn app.main:app --reload

# Run with custom settings
uv run uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# Run tests
uv run pytest
uv run pytest tests/ -v --cov=app

# Run type checking
uv run mypy app/

# Code formatting
uv run black .
uv run ruff check --fix .
```

### Virtual Environment Management

```bash
# Create virtual environment explicitly
uv venv

# Activate (works like traditional venv)
source .venv/bin/activate  # Linux/macOS
# .venv\Scripts\activate  # Windows

# Remove environment
rm -rf .venv

# Python version management
uv python install 3.12.0  # Install specific Python
uv python pin 3.12.0       # Set as project default
```

## Performance Optimization with uv

### Caching Configuration

```bash
# Configure cache directory (default: ~/.cache/uv)
export UV_CACHE_DIR=/path/to/cache

# Clear cache
uv cache clean

# Show cache info
uv cache info

# Limit cache size
uv cache prune --bytes=1GB
```

### Parallel Installation

```bash
# Install packages in parallel (default)
uv sync

# Control parallelism
uv sync --workers 4

# Network optimization
uv sync --offline  # Use cache only
uv sync --index-strategy unsafe-best-match  # Faster index lookup
```

## Production Deployment with uv

### Docker Integration

```dockerfile
# Multi-stage Docker with uv
FROM python:3.12-slim AS base

# Install uv
COPY --from=ghcr.io/astral-sh/uv:latest /uv /uv

# Project dependencies
FROM base AS dependencies
WORKDIR /app
COPY pyproject.toml uv.lock ./

# Install dependencies
RUN /uv/uv sync --frozen --no-dev

# Application stage
FROM base AS app
WORKDIR /app

# Copy uv and dependencies
COPY --from=ghcr.io/astral-sh/uv:latest /uv /uv
COPY --from=dependencies /app /app

# Copy application code
COPY app/ ./app/

# Runtime user
RUN adduser --disabled-password --gecos "" app
USER app

# Health check
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8000/health || exit 1

EXPOSE 8000

# Run application
CMD ["/uv/uv", "run", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

### CI/CD Integration

```yaml
# .github/workflows/ci.yml
name: CI
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up uv
      uses: astral-sh/setup-uv@v3
      
    - name: Set up Python
      run: uv python pin 3.12
      
    - name: Install dependencies
      run: uv sync --all-extras --dev
      
    - name: Run tests
      run: uv run pytest --cov=app
      
    - name: Type check
      run: uv run mypy app/
      
    - name: Lint
      run: uv run ruff check .
```

## Advanced uv Features

### Workspace Management

```toml
# pyproject.toml with workspace
[tool.uv.workspace]
members = [
    "app",
    "packages/*",
    "shared",
]

[tool.uv.sources]
# Local package development
my-local-package = { workspace = true }
```

### Custom Index Configuration

```bash
# Use custom package index
uv sync --index-url https://pypi.org/simple --extra-index-url https://download.pytorch.org/whl

# Configure multiple indexes
uv sync --index-strategy unsafe-best-match
```

### Dependency Visualization

```bash
# Show dependency tree
uv tree

# Show outdated packages
uv pip list --outdated

# Security audit
uv pip audit

# Generate requirements.txt (for compatibility)
uv pip compile requirements.txt > requirements.txt
```

## Migration from pip to uv

### Step-by-Step Migration

```bash
# 1. Backup current environment
pip freeze > requirements-backup.txt

# 2. Initialize uv project
uv init

# 3. Import from requirements.txt
uv add -r requirements-backup.txt

# 4. Test installation
uv sync

# 5. Update scripts and workflows
# Replace 'python' with 'uv run python'
# Replace 'pip install' with 'uv add'
```

### Common Migration Issues

| Issue | pip Command | uv Equivalent | Solution |
|-------|-------------|---------------|-----------|
| **Install editable package** | `pip install -e .` | `uv add --editable .` | Use `--editable` flag |
| **Install specific version** | `pip install package==1.2.3` | `uv add "package==1.2.3"` | Same syntax |
| **Install from VCS** | `pip install git+https://...` | `uv add "git+https://..."` | Same syntax |
| **User install** | `pip install --user` | Not supported | Use virtual environment |

## Best Practices

### Do's

```bash
# ✅ Use project templates
uv init --template fastapi my-project

# ✅ Leverage optional dependencies
uv add fastapi "sqlalchemy[postgresql]" "pytest[cov]"

# ✅ Use lock files for reproducibility
uv sync --frozen  # Use exact lock versions

# ✅ Configure proper Python version
uv python pin 3.12.0

# ✅ Use workspace for monorepos
[tool.uv.workspace]
members = ["packages/*"]
```

### Don'ts

```bash
# ❌ Mix pip and uv in same project
# Use only one package manager

# ❌ Ignore lock files
# Always commit uv.lock to version control

# ❌ Use global Python packages
# Always use project-specific environments

# ❌ Skip version pinning
# Always specify Python version in pyproject.toml
```

## Performance Benchmarks

Typical performance improvements with uv:

| Operation | pip | uv | Improvement |
|------------|------|-----|-------------|
| **FastAPI install** | 45s | 3s | **15x faster** |
| **Dependencies sync** | 120s | 8s | **15x faster** |
| **Development server start** | 2.3s | 0.8s | **3x faster** |
| **Test suite run** | 35s | 12s | **3x faster** |

## Next Steps

- [Introduction](./01-introduction.md) - Basic FastAPI concepts
- [Routing](./02-routing.md) - API routing and endpoints
- [Data Validation](./03-data-validation.md) - Pydantic v2 validation

---

[Back to Index](./README.md) | [Next: Routing](./02-routing.md)