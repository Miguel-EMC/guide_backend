# 22 - Authentication with JWT

JWTs provide stateless authentication for APIs. This chapter shows how to issue and validate tokens securely in Go.

## Goals

- Generate JWTs on login
- Validate tokens in middleware
- Keep secrets out of code

## 1. Install the JWT Library

```bash
go get github.com/golang-jwt/jwt/v5
```

## 2. Configuration

Load your secret from environment variables:

```go
import "os"

var jwtKey = []byte(os.Getenv("JWT_SECRET"))
```

## 3. Token Generation

```go
import (
    "net/http"
    "time"

    "github.com/gin-gonic/gin"
    "github.com/golang-jwt/jwt/v5"
)

type LoginRequest struct {
    Username string `json:"username" binding:"required"`
    Password string `json:"password" binding:"required"`
}

func login(c *gin.Context) {
    var req LoginRequest
    if err := c.ShouldBindJSON(&req); err != nil {
        c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request"})
        return
    }

    // Validate user credentials (replace with DB lookup)
    if req.Username != "admin" || req.Password != "password" {
        c.JSON(http.StatusUnauthorized, gin.H{"error": "invalid credentials"})
        return
    }

    expiresAt := time.Now().Add(15 * time.Minute)
    claims := jwt.RegisteredClaims{
        Subject:   req.Username,
        ExpiresAt: jwt.NewNumericDate(expiresAt),
        Issuer:    "go-api",
        Audience:  []string{"go-clients"},
    }

    token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
    tokenString, err := token.SignedString(jwtKey)
    if err != nil {
        c.JSON(http.StatusInternalServerError, gin.H{"error": "token error"})
        return
    }

    c.JSON(http.StatusOK, gin.H{"token": tokenString})
}
```

## 4. Auth Middleware

```go
import (
    "fmt"
    "strings"

    "github.com/gin-gonic/gin"
    "github.com/golang-jwt/jwt/v5"
)

func authMiddleware() gin.HandlerFunc {
    return func(c *gin.Context) {
        auth := c.GetHeader("Authorization")
        if auth == "" || !strings.HasPrefix(auth, "Bearer ") {
            c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"error": "missing token"})
            return
        }

        tokenString := strings.TrimPrefix(auth, "Bearer ")
        claims := &jwt.RegisteredClaims{}

        token, err := jwt.ParseWithClaims(tokenString, claims, func(token *jwt.Token) (interface{}, error) {
            if token.Method != jwt.SigningMethodHS256 {
                return nil, fmt.Errorf("unexpected signing method")
            }
            return jwtKey, nil
        })

        if err != nil || !token.Valid {
            c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"error": "invalid token"})
            return
        }

        c.Set("user", claims.Subject)
        c.Next()
    }
}
```

## Tips

- Keep tokens short‑lived.
- Rotate secrets periodically.
- Use HTTPS in production.

---

[Previous: Deployment](./21-deployment-docker-binary.md) | [Back to Index](./README.md)
