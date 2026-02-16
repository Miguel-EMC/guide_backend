# 11 - Building an HTTP Server with net/http

Go's standard library provides everything you need to build fast, production‑ready HTTP services.

## Goals

- Create an HTTP server
- Configure timeouts
- Implement graceful shutdown

## 1. Basic Server

```go
package main

import (
    "log"
    "net/http"
)

func health(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(http.StatusOK)
    w.Write([]byte(`{"status":"ok"}`))
}

func main() {
    mux := http.NewServeMux()
    mux.HandleFunc("/health", health)

    server := &http.Server{
        Addr:    ":8080",
        Handler: mux,
    }

    log.Println("listening on :8080")
    if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
        log.Fatal(err)
    }
}
```

## 2. Timeouts (Production Required)

```go
import "time"

server := &http.Server{
    Addr:              ":8080",
    Handler:           mux,
    ReadTimeout:       5 * time.Second,
    ReadHeaderTimeout: 5 * time.Second,
    WriteTimeout:      10 * time.Second,
    IdleTimeout:       60 * time.Second,
}
```

## 3. Graceful Shutdown

```go
import (
    "context"
    "os"
    "os/signal"
    "syscall"
    "time"
)

ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
defer stop()

<-ctx.Done()
shutdownCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
defer cancel()

_ = server.Shutdown(shutdownCtx)
```

## 4. Middleware with net/http

```go
import (
    "log"
    "net/http"
)

func logging(next http.Handler) http.Handler {
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        log.Printf("%s %s", r.Method, r.URL.Path)
        next.ServeHTTP(w, r)
    })
}
```

## Tips

- Always configure timeouts.
- Avoid global state in handlers.
- Prefer `http.Server` over `http.ListenAndServe` for control.

---

[Previous: Concurrency](./10-concurrency-goroutines-channels.md) | [Back to Index](./README.md) | [Next: Routing and Handlers ->](./12-routing-and-handlers.md)
