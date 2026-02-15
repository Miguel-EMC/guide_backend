# Fast Project Setup with uv (2026 Best Practices)

This chapter covers modern FastAPI project setup using uv, including lockfiles, dependency groups, and reproducible installs.

## Quick Start

```bash
uv init my-api
cd my-api
uv add "fastapi[standard]"
uv run fastapi dev main.py
```

## Why uv

- `uv init` creates a ready-to-run project scaffold with `pyproject.toml` and `main.py`.
- `uv run`, `uv sync`, and `uv lock` keep your environment and lockfile in sync.
- `uv.lock` provides reproducible installs across machines.

## Installing uv

### Standalone installer (recommended)

```bash
# Linux/macOS
curl -LsSf https://astral.sh/uv/install.sh | sh

# Windows PowerShell
powershell -c "irm https://astral.sh/uv/install.ps1 | iex"
```

### Package managers

```bash
# macOS
brew install uv

# Windows
winget install --id=astral-sh.uv -e
scoop install main/uv

# Python
pipx install uv
pip install uv
```

## Create a FastAPI Project

```bash
uv init my-api
cd my-api
uv add "fastapi[standard]"
```

`uv init` creates the project files below. The `.venv` and `uv.lock` are created the first time you run `uv run`, `uv sync`, or `uv lock`.

```
my-api/
├── .gitignore
├── .python-version
├── README.md
├── main.py
├── pyproject.toml
├── .venv/          # Created on first run/sync/lock
└── uv.lock         # Created on first run/sync/lock
```

## FastAPI-Friendly Layout (Recommended)

For anything beyond a toy app, move code into a package.

```
my-api/
├── app/
│   ├── __init__.py
│   └── main.py
├── tests/
│   └── test_health.py
├── pyproject.toml
└── uv.lock
```

Then run:

```bash
uv run fastapi dev app/main.py
```

## pyproject.toml Baseline

```toml
[project]
name = "my-api"
version = "0.1.0"
description = "FastAPI service"
readme = "README.md"
requires-python = ">=3.10"
dependencies = [
    "fastapi[standard]",
    "pydantic-settings",
]

[dependency-groups]
dev = [
    "pytest",
    "pytest-asyncio",
    "httpx",
    "pytest-cov",
    "ruff",
    "mypy",
]

[project.optional-dependencies]
database = [
    "sqlalchemy",
    "alembic",
    "asyncpg",
]
```

## Locking and Syncing

```bash
# Create or update uv.lock
uv lock

# Sync environment to lockfile (removes extraneous packages by default)
uv sync

# Require the lockfile to be up to date
uv sync --locked

# Do not update uv.lock automatically
uv sync --frozen
```

### Development dependencies

- uv reads development dependencies from `[dependency-groups]`.
- The `dev` group is synced by default.
- Exclude it with `--no-dev`.

```bash
uv sync --no-dev
uv sync --only-dev
```

### Optional dependencies (extras)

- Extras live in `[project.optional-dependencies]`.
- They are not installed by default.

```bash
uv sync --extra database
uv sync --all-extras
```

### Running commands

`uv run` keeps your environment in sync before executing your command.

```bash
uv run fastapi dev app/main.py
uv run pytest
uv run python -c "import fastapi; print(fastapi.__version__)"
```

## Managing Dependencies

```bash
# Add and remove packages
uv add "httpx>=0.27"
uv remove httpx

# Add from requirements.txt
uv add -r requirements.txt

# Upgrade a specific package
uv lock --upgrade-package fastapi
```

## Python Version Management

```bash
uv python install 3.12
uv python pin 3.12
```

## GitHub Actions (CI)

```yaml
name: CI
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Install uv
        uses: astral-sh/setup-uv@v7
      - name: Install dependencies
        run: uv sync --all-extras
      - name: Run tests
        run: uv run pytest
```

## Docker Integration

```dockerfile
FROM python:3.12-slim

# Install uv
COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

WORKDIR /app

# Install dependencies first
COPY pyproject.toml uv.lock ./
RUN uv sync --locked --no-install-project

# Copy app and install it
COPY . /app
RUN uv sync --locked

EXPOSE 8000
CMD ["uv", "run", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

Best practice: add `.venv` to `.dockerignore` so your local environment is not copied into the image.

## Migration from pip

```bash
# Create a project
uv init

# Import requirements.txt
uv add -r requirements.txt

# Create the lockfile and environment
uv sync
```

## Best Practices

- Use `uv add` and `uv remove` instead of editing dependencies by hand.
- Commit `uv.lock` for reproducible builds.
- Avoid mixing `pip install` with uv-managed projects.
- Use `uv sync --locked` in CI and containers to enforce the lockfile.

## References

- [uv Documentation](https://docs.astral.sh/uv/)
- [uv Projects](https://docs.astral.sh/uv/concepts/projects/)
- [uv Syncing](https://docs.astral.sh/uv/concepts/projects/syncing/)
- [uv Docker Integration](https://docs.astral.sh/uv/guides/integration/docker/)
- [setup-uv GitHub Action](https://github.com/astral-sh/setup-uv)

## Next Steps

- [Introduction](./01-introduction.md) - FastAPI basics
- [Routing](./02-routing.md) - HTTP methods and parameters

---

[Back to Index](./README.md) | [Next: Introduction](./01-introduction.md)
