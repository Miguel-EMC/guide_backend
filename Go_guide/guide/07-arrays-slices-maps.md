# 07 - Arrays, Slices, and Maps

Go's primary collection types are slices and maps. Arrays are mostly used as fixed‑size building blocks.

## Goals

- Understand array vs slice behavior
- Use slices safely and efficiently
- Work with maps idiomatically

## 1. Arrays (Fixed Size)

```go
var a [3]int
b := [3]int{1, 2, 3}
```

Arrays are value types and copied on assignment.

## 2. Slices (Dynamic)

```go
s := []int{1, 2, 3}

s = append(s, 4)
```

Length and capacity:

```go
import "fmt"

fmt.Println(len(s), cap(s))
```

Copy a slice:

```go
dst := make([]int, len(s))
copy(dst, s)
```

## 3. Slicing

```go
nums := []int{1, 2, 3, 4, 5}
sub := nums[1:4] // 2,3,4
```

## 4. Maps

```go
m := map[string]int{
    "alice": 10,
    "bob":   20,
}

m["carla"] = 30

v, ok := m["alice"]
```

Delete:

```go
delete(m, "bob")
```

## 5. Standard Helpers (`slices`, `maps`)

```go
import "slices"

nums := []int{1, 2, 3}
if slices.Contains(nums, 2) {
    // ...
}
```

## Tips

- Prefer slices over arrays in application code.
- Avoid sharing large underlying arrays unintentionally.
- Always check the `ok` value when reading from maps.

---

[Previous: Packages and Modules](./06-packages-and-modules.md) | [Back to Index](./README.md) | [Next: Pointers ->](./08-pointers.md)
