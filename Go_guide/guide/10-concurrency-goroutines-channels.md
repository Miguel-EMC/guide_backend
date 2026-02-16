# 10 - Concurrency: Goroutines and Channels

Go's concurrency model is one of its biggest strengths. Use goroutines for parallel work and channels for safe communication.

## Goals

- Launch goroutines safely
- Communicate with channels
- Control lifecycles with context

## 1. Goroutines

```go
import "fmt"

go func() {
    fmt.Println("async")
}()
```

## 2. WaitGroup

```go
import (
    "fmt"
    "sync"
)

var wg sync.WaitGroup

for i := 0; i < 3; i++ {
    wg.Add(1)
    go func(n int) {
        defer wg.Done()
        fmt.Println(n)
    }(i)
}

wg.Wait()
```

## 3. Channels

```go
import "fmt"

ch := make(chan int)

go func() {
    ch <- 42
}()

value := <-ch
```

Buffered channels:

```go
ch := make(chan int, 2)
ch <- 1
ch <- 2
```

## 4. Select

```go
import "time"

select {
case v := <-ch:
    fmt.Println(v)
case <-time.After(1 * time.Second):
    fmt.Println("timeout")
}
```

## 5. Context Cancellation

```go
import (
    "context"
    "time"
)

ctx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
defer cancel()

select {
case <-ctx.Done():
    fmt.Println("cancelled")
}
```

## 6. Worker Pool Example

```go
import "sync"

jobs := make(chan int, 5)
results := make(chan int, 5)

worker := func(id int, jobs <-chan int, results chan<- int) {
    for j := range jobs {
        results <- j * 2
    }
}

for w := 1; w <= 3; w++ {
    go worker(w, jobs, results)
}

for j := 1; j <= 5; j++ {
    jobs <- j
}
close(jobs)

for a := 1; a <= 5; a++ {
    <-results
}
```

## Tips

- Avoid shared memory when possible.
- Use context for timeouts and cancellation.
- Close channels only from the sender side.

---

[Previous: Interfaces](./09-interfaces.md) | [Back to Index](./README.md) | [Next: HTTP Server with net/http ->](./11-http-server-net-http.md)
