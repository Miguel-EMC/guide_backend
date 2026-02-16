# 19 - Testing in Go

Go includes a fast standard testing package. Use it for unit tests, HTTP handlers, and integration tests.

## Goals

- Write table‑driven tests
- Test HTTP handlers and Gin routes
- Use race detection and coverage

## 1. Basics

```bash
go test ./...
go test -v ./...
go test -race ./...
go test -cover ./...
```

## 2. Unit Tests

```go
// math.go
package mathutil

func Add(a, b int) int { return a + b }
```

```go
// math_test.go
package mathutil

import "testing"

func TestAdd(t *testing.T) {
    if got := Add(2, 3); got != 5 {
        t.Fatalf("expected 5, got %d", got)
    }
}
```

## 3. Table‑Driven Tests

```go
func TestAddTable(t *testing.T) {
    cases := []struct {
        name string
        a, b int
        want int
    }{
        {"pos", 1, 2, 3},
        {"zero", 0, 0, 0},
    }

    for _, tc := range cases {
        t.Run(tc.name, func(t *testing.T) {
            if got := Add(tc.a, tc.b); got != tc.want {
                t.Fatalf("got %d want %d", got, tc.want)
            }
        })
    }
}
```

## 4. Testing HTTP Handlers

```go
import (
    "net/http"
    "net/http/httptest"
)

func TestHealth(t *testing.T) {
    req := httptest.NewRequest(http.MethodGet, "/health", nil)
    w := httptest.NewRecorder()

    health(w, req)

    if w.Code != http.StatusOK {
        t.Fatalf("expected 200, got %d", w.Code)
    }
}
```

## 5. Testing Gin Routes

```go
import (
    "net/http"
    "net/http/httptest"

    "github.com/gin-gonic/gin"
)

func setupRouter() *gin.Engine {
    r := gin.New()
    r.GET("/health", func(c *gin.Context) {
        c.JSON(200, gin.H{"status": "ok"})
    })
    return r
}

func TestGinHealth(t *testing.T) {
    r := setupRouter()

    req := httptest.NewRequest(http.MethodGet, "/health", nil)
    w := httptest.NewRecorder()
    r.ServeHTTP(w, req)

    if w.Code != http.StatusOK {
        t.Fatalf("expected 200, got %d", w.Code)
    }
}
```

## Tips

- Use `t.Helper()` inside test helpers.
- Run `go test -race` in CI.
- Keep tests deterministic and small.

---

[Previous: CRUD API with Gin](./18-building-a-crud-api-with-gin.md) | [Back to Index](./README.md) | [Next: Project Structure ->](./20-project-structure-and-best-practices.md)
