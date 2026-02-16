# 09 - Interfaces

Interfaces define behavior. In Go, types implicitly satisfy interfaces, enabling flexible design without heavy inheritance.

## Goals

- Define small, composable interfaces
- Use type assertions safely
- Build decoupled components

## 1. Defining an Interface

```go
type Store interface {
    Get(id int64) (*User, error)
    Save(user *User) error
}
```

Any type with those methods satisfies the interface.

## 2. Implicit Implementation

```go
type SQLStore struct {}

func (s *SQLStore) Get(id int64) (*User, error) { /* ... */ return nil, nil }
func (s *SQLStore) Save(user *User) error { /* ... */ return nil }
```

`SQLStore` automatically implements `Store`.

## 3. Type Assertions

```go
import "fmt"

var v any = "hello"
str, ok := v.(string)
if ok {
    fmt.Println(str)
}
```

## 4. Type Switch

```go
import "fmt"

switch v := anyVal.(type) {
case string:
    fmt.Println("string", v)
case int:
    fmt.Println("int", v)
default:
    fmt.Println("unknown")
}
```

## 5. Interface Composition

```go
type Reader interface { Read(p []byte) (int, error) }
type Writer interface { Write(p []byte) (int, error) }

type ReadWriter interface {
    Reader
    Writer
}
```

## Tips

- Keep interfaces small (one responsibility).
- Accept interfaces, return concrete types.
- Avoid exposing unnecessary methods in public contracts.

---

[Previous: Pointers](./08-pointers.md) | [Back to Index](./README.md) | [Next: Concurrency ->](./10-concurrency-goroutines-channels.md)
