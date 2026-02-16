# 20 - Project Structure and Best Practices

Go is flexible, but consistent structure keeps teams productive and services maintainable.

## Goals

- Organize code for growth
- Separate domain, transport, and storage layers
- Apply Go best practices in production

## 1. Standard Layout (Common Pattern)

```
/
├── cmd/
│   └── api/
│       └── main.go
├── internal/
│   ├── server/
│   ├── user/
│   └── auth/
├── pkg/        // optional shared libs
├── configs/
├── migrations/
├── go.mod
└── go.sum
```

## 2. Domain‑Oriented Packages

```
internal/
  user/
    handler.go
    service.go
    repository.go
  auth/
    handler.go
    service.go
```

Keep transport concerns in handlers, business logic in services, and DB logic in repositories.

## 3. Dependency Injection

```go
func main() {
    cfg := LoadConfig()
    db := ConnectDB(cfg)

    userRepo := user.NewRepository(db)
    userService := user.NewService(userRepo)
    userHandler := user.NewHandler(userService)

    server := server.New(userHandler)
    server.Run()
}
```

## 4. Configuration Management

- Read environment variables once at startup.
- Store config in a struct.
- Validate required fields before booting.

## 5. Logging

- Use structured logs in production.
- Include request IDs.
- Avoid logging secrets.

## 6. Error Handling

- Wrap errors with context.
- Return typed errors for domain use cases.
- Map domain errors to HTTP responses in handlers.

## Tips

- Keep packages small and focused.
- Avoid global variables.
- Prefer composition over inheritance.

---

[Previous: Testing in Go](./19-testing-in-go.md) | [Back to Index](./README.md) | [Next: Deployment ->](./21-deployment-docker-binary.md)
