# 16 - Deployment with Docker

This chapter shows a production-grade Docker setup for Express.

## 1. Dockerfile (Multi-stage)

```dockerfile
# syntax=docker/dockerfile:1
FROM node:24-alpine AS deps
WORKDIR /app
COPY package*.json ./
RUN npm ci

FROM node:24-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

FROM node:24-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
COPY package*.json ./
RUN npm ci --omit=dev && npm cache clean --force
COPY --from=builder /app/dist ./dist

USER node
EXPOSE 3000
CMD ["node", "dist/server.js"]
```

Use an Active LTS Node image for production (24.x at the time of writing).

## 2. Docker Compose (App + Postgres)

```yaml
services:
  db:
    image: postgres:16
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: express_db
    ports:
      - "5432:5432"

  api:
    build: .
    environment:
      DATABASE_URL: postgresql://postgres:postgres@db:5432/express_db
      PORT: 3000
    ports:
      - "3000:3000"
    depends_on:
      - db
```

## 3. Health Check

```typescript
app.get('/health', (_req, res) => {
  res.json({ status: 'ok', ts: new Date().toISOString() });
});
```

## 4. Production Tips

- Put Express behind a reverse proxy (NGINX or a cloud load balancer).
- Use `NODE_ENV=production`.
- Run migrations before starting the app.
- Use graceful shutdown to drain connections.

## 5. Healthcheck in Docker (Optional)

```dockerfile
HEALTHCHECK --interval=30s --timeout=3s CMD node -e \"fetch('http://localhost:3000/health').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))\"
```

--- 

[Previous: Testing](./15-testing-with-jest-and-supertest.md) | [Back to Index](./README.md) | [Next: Security Hardening ->](./17-security-hardening.md)
