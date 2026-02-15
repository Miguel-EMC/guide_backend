# 21 - CI/CD for Express

A strong CI/CD pipeline validates every change and ships safely.

## Goals

- Fail fast with lint and tests
- Build artifacts once and promote
- Keep deploys safe and repeatable

## 1. GitHub Actions CI Workflow

Create `.github/workflows/ci.yml`:

```yaml
name: CI

on:
  push:
    branches: [ main ]
  pull_request:

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_USER: postgres
          POSTGRES_PASSWORD: postgres
          POSTGRES_DB: express_db
        ports:
          - 5432:5432
      redis:
        image: redis:7
        ports:
          - 6379:6379
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 24
          cache: npm
      - run: npm ci
      - run: npm run lint
      - run: npm run build --if-present
      - run: npm test
        env:
          DATABASE_URL: postgresql://postgres:postgres@localhost:5432/express_db
          REDIS_URL: redis://localhost:6379
```

## 2. Type Checking and Formatting

Add scripts in `package.json`:

```json
{
  "scripts": {
    "lint": "eslint .",
    "typecheck": "tsc -p tsconfig.json --noEmit",
    "format:check": "prettier --check ."
  }
}
```

Include them in CI:

```yaml
      - run: npm run typecheck
      - run: npm run format:check
```

## 3. Build and Push Docker Images

```yaml
  docker:
    runs-on: ubuntu-latest
    needs: test
    steps:
      - uses: actions/checkout@v4
      - uses: docker/setup-buildx-action@v3
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - uses: docker/build-push-action@v6
        with:
          context: .
          push: true
          tags: ghcr.io/my-org/express-api:${{ github.sha }}
```

## 4. Deploy Job (Example)

```yaml
  deploy:
    runs-on: ubuntu-latest
    needs: docker
    if: github.ref == 'refs/heads/main'
    steps:
      - run: echo "Deploy step goes here"
```

## 5. Security Scans (Optional)

- Run `npm audit` or a dedicated dependency scanner.
- Scan Docker images for known CVEs.
- Fail PRs on critical vulnerabilities.

## Tips

- Run migrations in a release pipeline, not in PR checks.
- Keep staging and production close in configuration.
- Use blue/green or canary deployments for safer rollouts.

---

[Previous: Background Jobs](./20-background-jobs-and-queues.md) | [Back to Index](./README.md) | [Next: OpenAPI and Versioning ->](./22-openapi-and-versioning.md)
