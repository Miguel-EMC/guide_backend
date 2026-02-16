# 16 - Gin Routing and Validation

Gin provides expressive routing and a validation system built on `validator/v10`.

## Goals

- Define routes and groups
- Bind and validate requests
- Return consistent errors

## 1. Routing Basics

```go
import "github.com/gin-gonic/gin"

r.GET("/users", listUsers)
r.GET("/users/:id", getUser)
r.POST("/users", createUser)
```

## 2. Route Groups

```go
import "github.com/gin-gonic/gin"

api := r.Group("/api/v1")
api.Use(authMiddleware())
api.GET("/posts", listPosts)
```

## 3. Query and Path Params

```go
import "github.com/gin-gonic/gin"

id := c.Param("id")
page := c.DefaultQuery("page", "1")
```

## 4. JSON Binding and Validation

```go
import "github.com/gin-gonic/gin"

type CreateUserRequest struct {
    Name  string `json:"name" binding:"required,min=2"`
    Email string `json:"email" binding:"required,email"`
}

func createUser(c *gin.Context) {
    var req CreateUserRequest
    if err := c.ShouldBindJSON(&req); err != nil {
        c.JSON(400, gin.H{"error": err.Error()})
        return
    }

    c.JSON(201, gin.H{"id": 1, "name": req.Name})
}
```

## 5. Custom Validation

```go
import (
    "github.com/gin-gonic/gin/binding"
    "github.com/go-playground/validator/v10"
)

var validate = func() {
    if v, ok := binding.Validator.Engine().(*validator.Validate); ok {
        _ = v.RegisterValidation("role", func(fl validator.FieldLevel) bool {
            return fl.Field().String() == "admin" || fl.Field().String() == "user"
        })
    }
}
```

Use in a struct tag:

```go
type RoleRequest struct {
    Role string `json:"role" binding:"required,role"`
}
```

## 6. Consistent Error Responses

```go
import "github.com/gin-gonic/gin"

func validationError(err error) gin.H {
    return gin.H{"message": "validation_failed", "details": err.Error()}
}
```

## Tips

- Prefer struct validation over manual checks.
- Return the same error shape across endpoints.
- Keep controllers thin and delegate to services.

---

[Previous: Intro to Gin](./15-intro-to-gin.md) | [Back to Index](./README.md) | [Next: Gin Middleware ->](./17-gin-middleware.md)
