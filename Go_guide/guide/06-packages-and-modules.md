# 06 - Packages and Modules

Go uses packages for code organization and modules for dependency management. This chapter focuses on the modern Go module workflow.

## Goals

- Structure code with packages
- Manage dependencies with modules
- Understand module versions and workspaces

## 1. Packages

A package is a directory of `.go` files that compile together.

```go
// internal/mathutil/add.go
package mathutil

func Add(a, b int) int {
    return a + b
}
```

Import a package by module path + directory:

```go
import "example.com/myapp/internal/mathutil"
```

## 2. Exported vs Unexported

- Names starting with uppercase are exported.
- Lowercase names are package‑private.

```go
func Add(a, b int) int  // exported
func subtract(a, b int) int // unexported
```

## 3. Initialize a Module

```bash
go mod init example.com/myapp
```

This creates `go.mod`.

## 4. Add Dependencies

```bash
go get github.com/gin-gonic/gin@v1.11.0
```

Go will update `go.mod` and `go.sum`.

## 5. Clean Up Dependencies

```bash
go mod tidy
```

## 6. Semantic Import Versioning

For major versions v2+:

```go
import "github.com/example/lib/v2"
```

## 7. Local Development with Workspaces

Use workspaces for multi‑module repos.

```bash
go work init ./service-a ./service-b
```

## 8. Replace Directives (Advanced)

```go
replace github.com/example/lib => ../lib
```

Use only for local development, not in production releases.

## Tips

- Keep packages small and cohesive.
- Avoid circular dependencies.
- Run `go mod tidy` before commits.

---

[Previous: Functions and Error Handling](./05-functions-and-error-handling.md) | [Back to Index](./README.md) | [Next: Arrays, Slices, and Maps ->](./07-arrays-slices-maps.md)
