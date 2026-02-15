# CI/CD Pipeline

This chapter provides a pragmatic CI/CD setup for FastAPI using GitHub Actions and uv.

## Goals

- Reproducible installs
- Automated linting and tests
- Safe deploys with migrations

## Example GitHub Actions Workflow

```yaml
name: CI

on:
  push:
    branches: ["main"]
  pull_request:

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: "3.12"

      - name: Install uv
        run: pip install uv

      - name: Install dependencies
        run: uv sync

      - name: Lint
        run: uv run ruff check .

      - name: Tests
        run: uv run pytest -q
```

## Deployment Notes

- Run migrations before starting the new release
- Use blue/green or rolling deployments for zero downtime
- Use health checks and automatic rollback on failure

## Secrets and Environments

- Store secrets in the CI provider
- Use separate environments for staging and production
- Rotate credentials regularly

## Next Steps

- [Production Checklist](./25-production-checklist.md) - Launch readiness

---

[Previous: OpenAPI Customization](./23-openapi-customization.md) | [Back to Index](./README.md) | [Next: Production Checklist](./25-production-checklist.md)
