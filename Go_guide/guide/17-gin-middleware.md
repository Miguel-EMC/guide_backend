# 17 - Gin Middleware

Middleware are functions that run before and after your handlers. Use them for auth, logging, CORS, rate limiting, and request IDs.

## Goals

- Add global and group middleware
- Build custom middleware
- Apply security defaults

## 1. Built‑In Middleware

```go
import "github.com/gin-gonic/gin"

r := gin.New()
r.Use(gin.Logger(), gin.Recovery())
```

## 2. Custom Middleware

```go
import (
    "github.com/gin-gonic/gin"
    "github.com/google/uuid"
)

func requestID() gin.HandlerFunc {
    return func(c *gin.Context) {
        id := c.GetHeader("X-Request-ID")
        if id == "" {
            id = uuid.NewString()
        }
        c.Set("request_id", id)
        c.Header("X-Request-ID", id)
        c.Next()
    }
}
```

## 3. CORS (Recommended)

```bash
go get github.com/gin-contrib/cors
```

```go
import "github.com/gin-contrib/cors"

corsCfg := cors.Config{
    AllowOrigins:     []string{"https://app.example.com"},
    AllowMethods:     []string{"GET", "POST", "PUT", "DELETE"},
    AllowHeaders:     []string{"Authorization", "Content-Type"},
    AllowCredentials: true,
}

r.Use(cors.New(corsCfg))
```

## 4. Auth Middleware Example

```go
import "github.com/gin-gonic/gin"

func authMiddleware() gin.HandlerFunc {
    return func(c *gin.Context) {
        token := c.GetHeader("Authorization")
        if token == "" {
            c.AbortWithStatusJSON(401, gin.H{"error": "missing token"})
            return
        }
        c.Next()
    }
}
```

## 5. Group Middleware

```go
api := r.Group("/api")
api.Use(authMiddleware())
api.GET("/me", meHandler)
```

## Tips

- Keep middleware small and focused.
- Avoid DB calls inside middleware when possible.
- Always include `Recovery()` in production.

---

[Previous: Gin Routing and Validation](./16-gin-routing-and-validation.md) | [Back to Index](./README.md) | [Next: CRUD API with Gin ->](./18-building-a-crud-api-with-gin.md)
