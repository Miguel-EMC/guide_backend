# 21 - Deployment of Go Applications

Go ships as a single static binary, which makes deployments fast and reliable. This chapter covers binaries, systemd, and Docker.

## Goals

- Build production binaries
- Deploy with systemd
- Create minimal Docker images

## 1. Build a Static Binary

```bash
CGO_ENABLED=0 GOOS=linux GOARCH=amd64 \
  go build -trimpath -ldflags="-s -w" -o api-server ./cmd/api
```

## 2. systemd Service

```ini
[Unit]
Description=Go API Service
After=network.target

[Service]
User=app
Group=app
WorkingDirectory=/opt/api
ExecStart=/opt/api/api-server
Restart=always
EnvironmentFile=/opt/api/.env

[Install]
WantedBy=multi-user.target
```

## 3. Docker (Multi‑Stage)

```dockerfile
# Build stage
FROM golang:1.25-alpine AS builder
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -trimpath -ldflags="-s -w" -o /api-server ./cmd/api

# Final stage
FROM gcr.io/distroless/static-debian12
COPY --from=builder /api-server /api-server
EXPOSE 8080
USER nonroot:nonroot
ENTRYPOINT ["/api-server"]
```

## 4. Docker Compose (Example)

```yaml
services:
  api:
    build: .
    ports:
      - "8080:8080"
    env_file:
      - .env
```

## Tips

- Use `-trimpath` and strip symbols for smaller binaries.
- Prefer distroless or scratch for minimal images.
- Run containers as non‑root.

---

[Previous: Project Structure](./20-project-structure-and-best-practices.md) | [Back to Index](./README.md) | [Next: Authentication with JWT ->](./22-authentication-jwt.md)
