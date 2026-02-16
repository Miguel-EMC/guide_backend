# 05 - Functions and Error Handling

Go emphasizes explicit error handling and small, composable functions.

## Goals

- Write idiomatic functions
- Handle errors consistently
- Use error wrapping and inspection

## 1. Function Basics

```go
import "fmt"

func add(a, b int) int {
    return a + b
}
```

Multiple return values:

```go
import "fmt"

func divide(a, b float64) (float64, error) {
    if b == 0 {
        return 0, fmt.Errorf("divide by zero")
    }
    return a / b, nil
}
```

## 2. Named Returns

```go
func split(sum int) (x, y int) {
    x = sum * 4 / 9
    y = sum - x
    return
}
```

Use sparingly for clarity.

## 3. Variadic Functions

```go
func sum(nums ...int) int {
    total := 0
    for _, n := range nums {
        total += n
    }
    return total
}
```

## 4. Error Wrapping

```go
import "fmt"

if err := repo.Save(ctx, user); err != nil {
    return fmt.Errorf("save user: %w", err)
}
```

Inspect errors:

```go
import (
    "database/sql"
    "errors"
)

if errors.Is(err, sql.ErrNoRows) {
    // not found
}

var vErr *ValidationError
if errors.As(err, &vErr) {
    // handle custom error
}
```

## 5. Custom Errors

```go
type ValidationError struct {
    Field string
}

func (e *ValidationError) Error() string {
    return "invalid field: " + e.Field
}
```

## 6. Defer for Cleanup

```go
import "os"

f, err := os.Open(path)
if err != nil {
    return err
}
defer f.Close()
```

## Tips

- Return early on errors.
- Wrap errors with context.
- Keep functions short and focused.

---

[Previous: Control Flow](./04-control-flow-loops-conditionals.md) | [Back to Index](./README.md) | [Next: Packages and Modules ->](./06-packages-and-modules.md)
