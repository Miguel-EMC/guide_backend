# Django Project Setup with uv

This chapter covers modern Django project setup using `uv`, the fast Python package manager from Astral.

## Why uv for Django?

- Fast dependency resolution and installs
- Reproducible lockfiles (`uv.lock`)
- Simple CLI (`uv add`, `uv sync`, `uv run`)

## Install uv

```bash
# Linux/macOS
curl -LsSf https://astral.sh/uv/install.sh | sh

# Windows PowerShell
powershell -c "irm https://astral.sh/uv/install.ps1 | iex"

# Or with pip (slower)
pip install uv
```

## Create a Django + DRF Project

### Recommended (Django 5.2 LTS for DRF)

```bash
mkdir my-django-api
cd my-django-api

uv init

# Core dependencies
uv add "django>=5.2,<6.0"
uv add "djangorestframework>=3.16"
uv add "drf-spectacular>=0.28"

# Database + async
uv add "psycopg[binary]>=3.1"  # PostgreSQL
uv add "redis>=5.0"            # Cache/sessions
uv add "channels>=4.1"         # WebSockets (optional)

# Create project
uv run django-admin startproject config .
uv run python manage.py startapp api
```

### If You Need Django 6.0

Use Django 6.0 only after validating DRF compatibility for your stack.

```bash
uv add "django>=6.0,<6.1"
```

## Dev Dependencies

```bash
uv add --dev pytest pytest-django pytest-cov
uv add --dev ruff black mypy django-stubs
uv add --dev django-debug-toolbar
```

## Common uv Commands

```bash
# Sync to lockfile
uv sync

# Run server
uv run python manage.py runserver

# Run tests
uv run pytest
```

## Minimal pyproject.toml

```toml
[project]
name = "my-django-api"
version = "0.1.0"
dependencies = [
    "django>=5.2,<6.0",
    "djangorestframework>=3.16",
    "drf-spectacular>=0.28",
]
requires-python = ">=3.12"

[tool.uv]
dev-dependencies = [
    "pytest",
    "pytest-django",
    "ruff",
    "black",
    "mypy",
]
```

## Next Steps

- [Introduction](./01-introduction.md) - Django and DRF basics
- [Models](./02-models.md) - Data modeling

---

[Back to Index](./README.md) | [Next: Introduction](./01-introduction.md)
