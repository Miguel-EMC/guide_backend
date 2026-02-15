# CI/CD Pipeline

This chapter shows production-ready CI/CD workflows for Django + DRF using GitHub Actions, including testing, linting, security scanning, and deployment.

## CI/CD Overview

| Stage | Purpose |
|-------|---------|
| Lint | Code style and quality checks |
| Test | Unit, integration, and e2e tests |
| Security | Dependency and code scanning |
| Build | Docker image creation |
| Deploy | Staging and production rollout |

## Complete GitHub Actions Workflow

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  PYTHON_VERSION: "3.12"
  UV_CACHE_DIR: /tmp/.uv-cache

jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: ${{ env.PYTHON_VERSION }}

      - name: Install uv
        uses: astral-sh/setup-uv@v4
        with:
          enable-cache: true
          cache-dependency-glob: "uv.lock"

      - name: Install dependencies
        run: uv sync --frozen

      - name: Run ruff linter
        run: uv run ruff check .

      - name: Run ruff formatter check
        run: uv run ruff format --check .

      - name: Run mypy
        run: uv run mypy .

  test:
    runs-on: ubuntu-latest
    needs: lint

    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: test_db
          POSTGRES_USER: postgres
          POSTGRES_PASSWORD: postgres
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v4

      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: ${{ env.PYTHON_VERSION }}

      - name: Install uv
        uses: astral-sh/setup-uv@v4
        with:
          enable-cache: true
          cache-dependency-glob: "uv.lock"

      - name: Install dependencies
        run: uv sync --frozen

      - name: Run migrations
        env:
          DATABASE_URL: postgres://postgres:postgres@localhost:5432/test_db
          SECRET_KEY: test-secret-key
        run: uv run python manage.py migrate

      - name: Run tests with coverage
        env:
          DATABASE_URL: postgres://postgres:postgres@localhost:5432/test_db
          SECRET_KEY: test-secret-key
          REDIS_URL: redis://localhost:6379/0
        run: |
          uv run pytest \
            --cov=. \
            --cov-report=xml \
            --cov-report=term-missing \
            --cov-fail-under=80 \
            -v

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v4
        with:
          token: ${{ secrets.CODECOV_TOKEN }}
          files: ./coverage.xml
          fail_ci_if_error: true

  security:
    runs-on: ubuntu-latest
    needs: lint

    steps:
      - uses: actions/checkout@v4

      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: ${{ env.PYTHON_VERSION }}

      - name: Install uv
        uses: astral-sh/setup-uv@v4
        with:
          enable-cache: true

      - name: Install dependencies
        run: uv sync --frozen

      - name: Run safety check
        run: uv run pip-audit

      - name: Run bandit security linter
        run: uv run bandit -r . -x tests/

      - name: Check Django deployment settings
        env:
          SECRET_KEY: test-secret-key
          DEBUG: "false"
          ALLOWED_HOSTS: localhost
        run: uv run python manage.py check --deploy

  migrations:
    runs-on: ubuntu-latest
    needs: lint

    steps:
      - uses: actions/checkout@v4

      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: ${{ env.PYTHON_VERSION }}

      - name: Install uv
        uses: astral-sh/setup-uv@v4
        with:
          enable-cache: true

      - name: Install dependencies
        run: uv sync --frozen

      - name: Check for missing migrations
        env:
          SECRET_KEY: test-secret-key
        run: uv run python manage.py makemigrations --check --dry-run

  build:
    runs-on: ubuntu-latest
    needs: [test, security, migrations]
    if: github.ref == 'refs/heads/main'

    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Container Registry
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ghcr.io/${{ github.repository }}
          tags: |
            type=sha,prefix=
            type=ref,event=branch
            type=semver,pattern={{version}}

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  deploy-staging:
    runs-on: ubuntu-latest
    needs: build
    if: github.ref == 'refs/heads/main'
    environment: staging

    steps:
      - uses: actions/checkout@v4

      - name: Deploy to staging
        env:
          DEPLOY_KEY: ${{ secrets.STAGING_DEPLOY_KEY }}
          STAGING_HOST: ${{ secrets.STAGING_HOST }}
        run: |
          echo "Deploying to staging..."
          # Add your deployment script here
          # Examples:
          # - kubectl apply -f k8s/staging/
          # - ssh user@$STAGING_HOST "cd /app && docker-compose pull && docker-compose up -d"
          # - aws ecs update-service --cluster staging --service api --force-new-deployment

      - name: Run migrations
        env:
          STAGING_HOST: ${{ secrets.STAGING_HOST }}
        run: |
          echo "Running migrations on staging..."
          # ssh user@$STAGING_HOST "cd /app && docker-compose exec -T web python manage.py migrate"

      - name: Health check
        run: |
          echo "Checking staging health..."
          # curl -f https://staging.example.com/healthz/ || exit 1

  deploy-production:
    runs-on: ubuntu-latest
    needs: deploy-staging
    if: github.ref == 'refs/heads/main'
    environment: production

    steps:
      - uses: actions/checkout@v4

      - name: Deploy to production
        env:
          DEPLOY_KEY: ${{ secrets.PRODUCTION_DEPLOY_KEY }}
          PRODUCTION_HOST: ${{ secrets.PRODUCTION_HOST }}
        run: |
          echo "Deploying to production..."
          # Add your deployment script here

      - name: Run migrations
        run: |
          echo "Running migrations on production..."

      - name: Health check
        run: |
          echo "Checking production health..."
          # curl -f https://api.example.com/healthz/ || exit 1

      - name: Notify on success
        if: success()
        run: |
          echo "Deployment successful!"
          # curl -X POST ${{ secrets.SLACK_WEBHOOK }} \
          #   -H 'Content-Type: application/json' \
          #   -d '{"text": "Production deployment successful!"}'
```

## Test Configuration

### pytest.ini

```ini
[pytest]
DJANGO_SETTINGS_MODULE = config.settings
python_files = tests.py test_*.py *_tests.py
addopts = -ra -q --strict-markers
markers =
    slow: marks tests as slow (deselect with '-m "not slow"')
    integration: marks tests as integration tests
```

### conftest.py

```python
# tests/conftest.py
import pytest
from django.contrib.auth import get_user_model
from rest_framework.test import APIClient

User = get_user_model()


@pytest.fixture
def api_client():
    return APIClient()


@pytest.fixture
def user(db):
    return User.objects.create_user(
        username="testuser",
        email="test@example.com",
        password="testpass123",
    )


@pytest.fixture
def authenticated_client(api_client, user):
    api_client.force_authenticate(user=user)
    return api_client


@pytest.fixture
def doctor(db):
    from doctors.models import Doctor
    return Doctor.objects.create(
        name="Dr. Smith",
        email="smith@hospital.com",
    )
```

### Coverage Configuration

```toml
# pyproject.toml
[tool.coverage.run]
branch = true
source = ["."]
omit = [
    "*/migrations/*",
    "*/tests/*",
    "manage.py",
    "config/asgi.py",
    "config/wsgi.py",
]

[tool.coverage.report]
exclude_lines = [
    "pragma: no cover",
    "def __repr__",
    "raise NotImplementedError",
    "if TYPE_CHECKING:",
]
fail_under = 80
show_missing = true
```

## Linting Configuration

### ruff.toml

```toml
# ruff.toml
target-version = "py312"
line-length = 88

[lint]
select = [
    "E",   # pycodestyle errors
    "W",   # pycodestyle warnings
    "F",   # pyflakes
    "I",   # isort
    "B",   # flake8-bugbear
    "C4",  # flake8-comprehensions
    "UP",  # pyupgrade
    "DJ",  # flake8-django
]
ignore = [
    "E501",  # line too long (handled by formatter)
]

[lint.isort]
known-first-party = ["config", "core", "doctors", "patients"]
```

### mypy.ini

```ini
[mypy]
python_version = 3.12
plugins = ["mypy_django_plugin.main", "mypy_drf_plugin.main"]
strict = true
warn_unused_ignores = true
ignore_missing_imports = true

[mypy.plugins.django-stubs]
django_settings_module = "config.settings"

[mypy-*.migrations.*]
ignore_errors = true
```

## Matrix Testing

Test across multiple Python and Django versions:

```yaml
# .github/workflows/matrix.yml
name: Matrix Tests

on:
  push:
    branches: [main]
  pull_request:

jobs:
  test:
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        python-version: ["3.11", "3.12", "3.13"]
        django-version: ["4.2", "5.0", "5.1"]
        exclude:
          - python-version: "3.11"
            django-version: "5.1"

    steps:
      - uses: actions/checkout@v4

      - name: Set up Python ${{ matrix.python-version }}
        uses: actions/setup-python@v5
        with:
          python-version: ${{ matrix.python-version }}

      - name: Install uv
        uses: astral-sh/setup-uv@v4

      - name: Install dependencies
        run: |
          uv sync --frozen
          uv pip install "Django~=${{ matrix.django-version }}"

      - name: Run tests
        run: uv run pytest
```

## Pre-commit Hooks

### .pre-commit-config.yaml

```yaml
repos:
  - repo: https://github.com/astral-sh/ruff-pre-commit
    rev: v0.4.4
    hooks:
      - id: ruff
        args: [--fix]
      - id: ruff-format

  - repo: https://github.com/pre-commit/pre-commit-hooks
    rev: v4.6.0
    hooks:
      - id: trailing-whitespace
      - id: end-of-file-fixer
      - id: check-yaml
      - id: check-added-large-files
      - id: check-merge-conflict

  - repo: https://github.com/pre-commit/mirrors-mypy
    rev: v1.10.0
    hooks:
      - id: mypy
        additional_dependencies:
          - django-stubs
          - djangorestframework-stubs
```

Install hooks:

```bash
uv add --dev pre-commit
uv run pre-commit install
```

## Deployment Strategies

### Blue-Green Deployment

```yaml
deploy-blue-green:
  steps:
    - name: Deploy to green
      run: |
        kubectl apply -f k8s/green/
        kubectl rollout status deployment/api-green

    - name: Health check green
      run: |
        curl -f https://green.example.com/healthz/

    - name: Switch traffic
      run: |
        kubectl patch service api -p '{"spec":{"selector":{"version":"green"}}}'

    - name: Scale down blue
      run: |
        kubectl scale deployment api-blue --replicas=0
```

### Rolling Update

```yaml
deploy-rolling:
  steps:
    - name: Update deployment
      run: |
        kubectl set image deployment/api api=ghcr.io/org/api:${{ github.sha }}
        kubectl rollout status deployment/api --timeout=5m

    - name: Rollback on failure
      if: failure()
      run: |
        kubectl rollout undo deployment/api
```

## Environment Management

### GitHub Environments

```yaml
# In workflow
deploy-production:
  environment:
    name: production
    url: https://api.example.com
  # Requires manual approval if configured
```

### Secrets Management

Required secrets:

| Secret | Purpose |
|--------|---------|
| `SECRET_KEY` | Django secret key |
| `DATABASE_URL` | Production database |
| `SENTRY_DSN` | Error tracking |
| `CODECOV_TOKEN` | Coverage reports |
| `STAGING_DEPLOY_KEY` | Staging SSH key |
| `PRODUCTION_DEPLOY_KEY` | Production SSH key |

## Notifications

### Slack Integration

```yaml
- name: Notify Slack on failure
  if: failure()
  uses: slackapi/slack-github-action@v1
  with:
    payload: |
      {
        "text": "CI Failed: ${{ github.repository }}",
        "blocks": [
          {
            "type": "section",
            "text": {
              "type": "mrkdwn",
              "text": "*CI Failed*\nRepo: ${{ github.repository }}\nBranch: ${{ github.ref }}\nCommit: ${{ github.sha }}"
            }
          }
        ]
      }
  env:
    SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK }}
```

## Caching Strategies

### uv Cache

```yaml
- name: Install uv
  uses: astral-sh/setup-uv@v4
  with:
    enable-cache: true
    cache-dependency-glob: "uv.lock"
```

### Docker Layer Cache

```yaml
- name: Build and push
  uses: docker/build-push-action@v5
  with:
    cache-from: type=gha
    cache-to: type=gha,mode=max
```

## Best Practices

1. **Fail fast**: Run linting before tests
2. **Parallel jobs**: Run independent jobs concurrently
3. **Cache dependencies**: Use uv/pip caching
4. **Test in isolation**: Use service containers
5. **Require approvals**: Use environments for production
6. **Monitor deployments**: Health checks after deploy
7. **Automate rollbacks**: On health check failure

## References

- [GitHub Actions Documentation](https://docs.github.com/actions)
- [uv GitHub Action](https://github.com/astral-sh/setup-uv)
- [Docker Build Push Action](https://github.com/docker/build-push-action)
- [Codecov GitHub Action](https://github.com/codecov/codecov-action)

---

[Previous: Performance](./29-performance.md) | [Back to Index](./README.md)
