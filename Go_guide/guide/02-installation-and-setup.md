# 02 - Installation and Setup

This chapter installs Go, verifies the toolchain, and creates your first module.

## Goals

- Install Go on Linux, macOS, or Windows
- Configure PATH and environment
- Initialize a Go module

## 1. Download Go

Always download Go from the official site:

- Go Downloads: `https://go.dev/dl/`

Pick the latest **stable** version (Go 1.25 at time of writing). Replace version strings in commands with the current release.

## 2. Installation by OS

### A. Linux (Ubuntu/Debian)

```bash
GO_VERSION=1.25.0
wget https://go.dev/dl/go${GO_VERSION}.linux-amd64.tar.gz
sudo rm -rf /usr/local/go
sudo tar -C /usr/local -xzf go${GO_VERSION}.linux-amd64.tar.gz
```

Add Go to your PATH:

```bash
echo 'export PATH=$PATH:/usr/local/go/bin' >> ~/.bashrc
source ~/.bashrc
```

Verify:

```bash
go version
```

### B. macOS

Option 1: PKG installer from `go.dev/dl`.

Option 2: Homebrew:

```bash
brew install go
```

Verify:

```bash
go version
```

### C. Windows

- Download the `.msi` installer from `go.dev/dl`.
- Run the installer, it updates PATH automatically.

Verify in PowerShell:

```powershell
go version
```

## 3. Go Environment Basics

```bash
go env
```

Key variables:

- `GOPATH`: workspace for tooling and module cache
- `GOBIN`: install location for binaries (optional)

Go modules let you keep projects anywhere, not just inside `GOPATH`.

## 4. Your First Go Module

```bash
mkdir -p ~/go-workspace/hello
cd ~/go-workspace/hello

go mod init example.com/hello
```

Create `main.go`:

```go
package main

import "fmt"

func main() {
    fmt.Println("Hello, Go Backend!")
}
```

Run:

```bash
go run .
```

## 5. Recommended Editor

VS Code with the official Go extension provides formatting, linting, and debugging. Install it and allow the Go tools prompt to install dependencies.

## Tips

- Use the latest stable Go version.
- Keep `go.mod` committed to source control.
- Run `go fmt ./...` regularly.

---

[Previous: Introduction](./01-introduction-to-go.md) | [Back to Index](./README.md) | [Next: Go Basics ->](./03-go-basics-variables-types-structs.md)
