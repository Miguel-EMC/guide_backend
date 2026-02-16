# 03 - Go Basics: Variables, Types, and Structs

This chapter covers variables, constants, core types, and structs. These are the building blocks of every Go service.

## Goals

- Declare variables and constants idiomatically
- Understand core types and zero values
- Define structs with tags and embedding

## 1. Variables

```go
package main

import "fmt"

func main() {
    var name string = "Alice"
    var age = 30
    var active bool

    city := "Lima"
    score := 98.5

    fmt.Println(name, age, active, city, score)
}
```

Key points:

- `:=` only works inside functions.
- Uninitialized variables get a **zero value**.

## 2. Constants

```go
const appName = "Go API"
const maxRetries int = 5

const (
    statusPending = "pending"
    statusDone    = "done"
)
```

Use `iota` for enumerations:

```go
const (
    RoleUser = iota
    RoleAdmin
)
```

## 3. Core Types

- `int`, `int64`, `uint`, `float64`, `bool`, `string`
- `byte` (`uint8`) and `rune` (`int32`)

```go
var id int64 = 42
var price float64 = 19.99
var enabled bool = true
var title string = "Hello"
```

## 4. Structs

Structs define composite types.

```go
import "time"

type User struct {
    ID        int64
    Name      string
    Email     string
    IsActive  bool
    CreatedAt time.Time
}
```

Initialize:

```go
user := User{ID: 1, Name: "Ana", Email: "ana@example.com"}
user.IsActive = true
```

## 5. Struct Tags (JSON)

```go
type Post struct {
    ID      int64  `json:"id"`
    Title   string `json:"title"`
    Content string `json:"content,omitempty"`
}
```

Tags are used by `encoding/json` and other libraries.

## 6. Embedded Structs

```go
import "time"

type Audit struct {
    CreatedAt time.Time
    UpdatedAt time.Time
}

type Product struct {
    ID    int64
    Name  string
    Audit // embedded
}
```

Embedded fields promote their methods and fields to the outer struct.

## Tips

- Prefer explicit types at package boundaries.
- Keep structs small and focused.
- Use tags only when you need external serialization.

---

[Previous: Installation and Setup](./02-installation-and-setup.md) | [Back to Index](./README.md) | [Next: Control Flow ->](./04-control-flow-loops-conditionals.md)
