# 13 - Working with JSON

Go's `encoding/json` package is fast and widely used. This chapter covers encoding, decoding, and validation basics.

## Goals

- Marshal and unmarshal JSON
- Use struct tags correctly
- Decode safely with strict settings

## 1. Marshal (Go -> JSON)

```go
import "encoding/json"

type User struct {
    ID    int64  `json:"id"`
    Name  string `json:"name"`
    Email string `json:"email"`
}

b, err := json.Marshal(User{ID: 1, Name: "Ana", Email: "ana@example.com"})
```

## 2. Unmarshal (JSON -> Go)

```go
import "encoding/json"

var u User
err := json.Unmarshal([]byte(`{"id":1,"name":"Ana"}`), &u)
```

## 3. Streaming Decoder (Recommended for APIs)

```go
import "encoding/json"

decoder := json.NewDecoder(r.Body)
decoder.DisallowUnknownFields()

var input CreateUserRequest
if err := decoder.Decode(&input); err != nil {
    http.Error(w, "invalid json", http.StatusBadRequest)
    return
}
```

## 4. Optional Fields

```go
type PatchUser struct {
    Name  *string `json:"name"`
    Email *string `json:"email"`
}
```

## 5. JSON Numbers

```go
import "encoding/json"

decoder := json.NewDecoder(r.Body)
decoder.UseNumber()
```

## Tips

- Always close request bodies.
- Use `DisallowUnknownFields()` to avoid silent errors.
- Use pointers for optional fields.

---

[Previous: Routing and Handlers](./12-routing-and-handlers.md) | [Back to Index](./README.md) | [Next: Database with database/sql ->](./14-connecting-to-a-database-sql.md)
