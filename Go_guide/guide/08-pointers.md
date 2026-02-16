# 08 - Pointers

Pointers store the address of a value. They are essential for performance and for mutating values in functions.

## Goals

- Understand pointer semantics
- Use pointers with structs
- Avoid common mistakes with nil

## 1. Basics

```go
import "fmt"

x := 10
p := &x

fmt.Println(*p) // 10
*p = 20
fmt.Println(x)  // 20
```

## 2. Pointers in Functions

```go
func increment(n *int) {
    *n++
}
```

## 3. Pointers to Structs

```go
type User struct {
    Name string
}

func rename(u *User, name string) {
    u.Name = name
}
```

## 4. new vs &

```go
u1 := new(User)  // zero value
u2 := &User{Name: "Ana"}
```

## 5. Nil Pointers

```go
var u *User
if u == nil {
    // avoid dereference
}
```

## Tips

- Prefer passing small structs by value.
- Use pointers for large structs or when mutation is required.
- Always check for nil before dereferencing.

---

[Previous: Arrays, Slices, and Maps](./07-arrays-slices-maps.md) | [Back to Index](./README.md) | [Next: Interfaces ->](./09-interfaces.md)
