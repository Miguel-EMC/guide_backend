# 04 - Control Flow: Loops and Conditionals

Control flow is how you make decisions and repeat work. Go keeps this simple and explicit.

## Goals

- Use `if`, `switch`, and `for` idiomatically
- Understand `range` and `defer`
- Avoid common control‑flow pitfalls

## 1. If Statements

```go
import "fmt"

if age >= 18 {
    fmt.Println("adult")
} else if age >= 13 {
    fmt.Println("teen")
} else {
    fmt.Println("child")
}
```

Short statement:

```go
import "fmt"

if n := len(name); n > 0 {
    fmt.Println("has name")
}
```

## 2. Switch

```go
import "fmt"

switch status {
case "pending":
    // ...
case "done":
    // ...
default:
    // ...
}
```

Switch with short statement:

```go
import "fmt"

switch n := len(items); {
case n == 0:
    fmt.Println("empty")
case n < 5:
    fmt.Println("small")
default:
    fmt.Println("large")
}
```

## 3. For Loops

Go has one loop: `for`.

```go
import "fmt"

for i := 0; i < 3; i++ {
    fmt.Println(i)
}
```

While style:

```go
for total < 10 {
    total += 2
}
```

Infinite loop:

```go
for {
    if ready {
        break
    }
}
```

## 4. Range

```go
import "fmt"

for i, v := range []string{"a", "b"} {
    fmt.Println(i, v)
}

for k, v := range map[string]int{"a": 1} {
    fmt.Println(k, v)
}
```

Use `_` to ignore a value.

## 5. Defer

`defer` runs after the surrounding function returns. Use it for cleanup.

```go
import "os"

file, err := os.Open("data.txt")
if err != nil {
    return err
}
defer file.Close()
```

## Tips

- Prefer `switch` over long `if` chains.
- Avoid `goto` unless absolutely necessary.
- Use `defer` for cleanup to prevent leaks.

---

[Previous: Go Basics](./03-go-basics-variables-types-structs.md) | [Back to Index](./README.md) | [Next: Functions and Error Handling ->](./05-functions-and-error-handling.md)
