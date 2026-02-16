# 15 - Introduction to the Gin Web Framework

Gin is a fast, minimalistic HTTP framework that sits on top of `net/http`. It adds routing, middleware, validation, and JSON helpers with low overhead.

## Goals

- Install Gin
- Understand Gin's core concepts
- Build a basic API server

## 1. Install Gin

```bash
go get github.com/gin-gonic/gin@v1.11.0
```

## 2. Basic Gin Server

```go
package main

import (
    "net/http"

    "github.com/gin-gonic/gin"
)

func main() {
    r := gin.New()
    r.Use(gin.Logger(), gin.Recovery())

    r.GET("/health", func(c *gin.Context) {
        c.JSON(http.StatusOK, gin.H{"status": "ok"})
    })

    r.Run(":8080")
}
```

## 3. Why Use Gin

- Fast routing and middleware chain
- Built‑in JSON helpers
- Validation and binding
- Great ecosystem and community adoption

## Tips

- Use `gin.New()` and add middleware explicitly for production.
- Prefer `gin.Logger()` and `gin.Recovery()`.
- Keep handlers small and delegate to services.

---

[Previous: Database with database/sql](./14-connecting-to-a-database-sql.md) | [Back to Index](./README.md) | [Next: Gin Routing and Validation ->](./16-gin-routing-and-validation.md)
