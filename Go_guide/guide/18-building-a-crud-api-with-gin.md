# 18 - Building a CRUD API with Gin

This chapter builds a small but production‑style CRUD API using Gin. It uses a repository layer and proper request validation.

## Goals

- Build a clean handler layer
- Validate requests
- Keep data access isolated

## 1. Data Model

```go
import (
    "net/http"
    "strconv"
    "sync"

    "github.com/gin-gonic/gin"
)

type Book struct {
    ID     int64  `json:"id"`
    Title  string `json:"title"`
    Author string `json:"author"`
}
```

## 2. Repository Interface

```go
type BookStore interface {
    List() []Book
    Get(id int64) (Book, bool)
    Create(b Book) Book
    Update(id int64, b Book) (Book, bool)
    Delete(id int64) bool
}
```

## 3. In‑Memory Store (Example)

```go
import "sync"

type MemoryStore struct {
    mu    sync.RWMutex
    next  int64
    books []Book
}

func NewMemoryStore() *MemoryStore {
    return &MemoryStore{next: 1, books: []Book{}}
}

func (s *MemoryStore) List() []Book {
    s.mu.RLock()
    defer s.mu.RUnlock()
    return append([]Book{}, s.books...)
}

func (s *MemoryStore) Get(id int64) (Book, bool) {
    s.mu.RLock()
    defer s.mu.RUnlock()
    for _, b := range s.books {
        if b.ID == id {
            return b, true
        }
    }
    return Book{}, false
}

func (s *MemoryStore) Create(b Book) Book {
    s.mu.Lock()
    defer s.mu.Unlock()
    b.ID = s.next
    s.next++
    s.books = append(s.books, b)
    return b
}

func (s *MemoryStore) Update(id int64, b Book) (Book, bool) {
    s.mu.Lock()
    defer s.mu.Unlock()
    for i, item := range s.books {
        if item.ID == id {
            b.ID = id
            s.books[i] = b
            return b, true
        }
    }
    return Book{}, false
}

func (s *MemoryStore) Delete(id int64) bool {
    s.mu.Lock()
    defer s.mu.Unlock()
    for i, item := range s.books {
        if item.ID == id {
            s.books = append(s.books[:i], s.books[i+1:]...)
            return true
        }
    }
    return false
}
```

## 4. Handler Layer

```go
type BookHandler struct {
    store BookStore
}

func NewBookHandler(store BookStore) *BookHandler {
    return &BookHandler{store: store}
}
```

### Request Model

```go
type CreateBookRequest struct {
    Title  string `json:"title" binding:"required,min=2"`
    Author string `json:"author" binding:"required,min=2"`
}
```

### List

```go
import (
    "net/http"

    "github.com/gin-gonic/gin"
)

func (h *BookHandler) List(c *gin.Context) {
    c.JSON(http.StatusOK, h.store.List())
}
```

### Create

```go
import (
    "net/http"

    "github.com/gin-gonic/gin"
)

func (h *BookHandler) Create(c *gin.Context) {
    var req CreateBookRequest
    if err := c.ShouldBindJSON(&req); err != nil {
        c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
        return
    }

    book := h.store.Create(Book{Title: req.Title, Author: req.Author})
    c.JSON(http.StatusCreated, book)
}
```

### Get

```go
import (
    "net/http"
    "strconv"

    "github.com/gin-gonic/gin"
)

func (h *BookHandler) Get(c *gin.Context) {
    id, err := strconv.ParseInt(c.Param("id"), 10, 64)
    if err != nil {
        c.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
        return
    }

    book, ok := h.store.Get(id)
    if !ok {
        c.JSON(http.StatusNotFound, gin.H{"error": "not found"})
        return
    }

    c.JSON(http.StatusOK, book)
}
```

### Update

```go
import (
    "net/http"
    "strconv"

    "github.com/gin-gonic/gin"
)

func (h *BookHandler) Update(c *gin.Context) {
    id, err := strconv.ParseInt(c.Param("id"), 10, 64)
    if err != nil {
        c.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
        return
    }

    var req CreateBookRequest
    if err := c.ShouldBindJSON(&req); err != nil {
        c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
        return
    }

    book, ok := h.store.Update(id, Book{Title: req.Title, Author: req.Author})
    if !ok {
        c.JSON(http.StatusNotFound, gin.H{"error": "not found"})
        return
    }

    c.JSON(http.StatusOK, book)
}
```

### Delete

```go
import (
    "net/http"
    "strconv"

    "github.com/gin-gonic/gin"
)

func (h *BookHandler) Delete(c *gin.Context) {
    id, err := strconv.ParseInt(c.Param("id"), 10, 64)
    if err != nil {
        c.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
        return
    }

    if !h.store.Delete(id) {
        c.JSON(http.StatusNotFound, gin.H{"error": "not found"})
        return
    }

    c.Status(http.StatusNoContent)
}
```

## 5. Router Setup

```go
import "github.com/gin-gonic/gin"

r := gin.New()
r.Use(gin.Logger(), gin.Recovery())

store := NewMemoryStore()
handler := NewBookHandler(store)

api := r.Group("/api")
api.GET("/books", handler.List)
api.POST("/books", handler.Create)
api.GET("/books/:id", handler.Get)
api.PUT("/books/:id", handler.Update)
api.DELETE("/books/:id", handler.Delete)

r.Run(":8080")
```

## Tips

- Separate handlers from storage logic.
- Validate input at the boundary.
- Replace the memory store with a DB repository in production.

---

[Previous: Gin Middleware](./17-gin-middleware.md) | [Back to Index](./README.md) | [Next: Testing in Go ->](./19-testing-in-go.md)
