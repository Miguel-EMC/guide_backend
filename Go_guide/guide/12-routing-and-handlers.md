# 12 - Routing and Handlers with net/http

This chapter shows how to build routing and handlers using only the standard library.

## Goals

- Understand `http.Handler`
- Build routes with `ServeMux`
- Handle methods and path params

## 1. Handlers

```go
import "net/http"

func listUsers(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    w.Header().Set("Content-Type", "application/json")
    w.Write([]byte(`{"data":[]}`))
}
```

## 2. ServeMux Routing

```go
import "net/http"

mux := http.NewServeMux()

mux.HandleFunc("/users", listUsers)
```

## 3. Method‑Specific Routing

```go
import "net/http"

func users(w http.ResponseWriter, r *http.Request) {
    switch r.Method {
    case http.MethodGet:
        listUsers(w, r)
    case http.MethodPost:
        createUser(w, r)
    default:
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
    }
}
```

## 4. Path Parameters (Manual)

For simple cases you can parse the path directly:

```go
import (
    "net/http"
    "strings"
)

func getUser(w http.ResponseWriter, r *http.Request) {
    // /users/{id}
    parts := strings.Split(strings.Trim(r.URL.Path, "/"), "/")
    if len(parts) != 2 {
        http.NotFound(w, r)
        return
    }

    id := parts[1]
    _ = id
}
```

## 5. Composing Middleware

```go
import "net/http"

mux.Handle("/users", logging(http.HandlerFunc(users)))
```

## Tips

- Keep handlers small and focused.
- Use `net/http` for simple services.
- Move to a router framework when your routes grow.

---

[Previous: HTTP Server](./11-http-server-net-http.md) | [Back to Index](./README.md) | [Next: Working with JSON ->](./13-working-with-json.md)
