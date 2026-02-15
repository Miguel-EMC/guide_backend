# CI/CD Pipeline

This chapter shows a production-ready CI workflow with uv and GitHub Actions.

## Step 1: Add a CI Workflow

```yaml
# .github/workflows/ci.yml
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

      - name: Cache uv
        uses: actions/cache@v4
        with:
          path: ~/.cache/uv
          key: ${{ runner.os }}-uv-${{ hashFiles('**/uv.lock') }}
          restore-keys: ${{ runner.os }}-uv-

      - name: Install dependencies
        run: uv sync

      - name: Lint
        run: uv run ruff check .

      - name: Run tests
        run: uv run pytest -q

      - name: Check migrations
        run: uv run python manage.py makemigrations --check --dry-run
```

## Step 2: Deployment Stage

In deployment jobs, run:

- `python manage.py migrate`
- `python manage.py collectstatic --noinput`
- Health checks after deployment

## Tips

- Separate staging and production environments.
- Run migrations before serving traffic.
- Use build artifacts for faster deploys.

## References

- [GitHub Actions](https://docs.github.com/actions)

## Next Steps

- Review deployment and security chapters

---

[Previous: Performance](./29-performance.md) | [Back to Index](./README.md)
