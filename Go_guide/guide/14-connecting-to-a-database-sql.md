# 14 - Connecting to a Database with database/sql

Go's `database/sql` package provides a standard API for SQL databases. You bring a driver like PostgreSQL or MySQL.

## Goals

- Connect to PostgreSQL with `database/sql`
- Use context and prepared statements
- Configure connection pooling

## 1. Install a Driver

PostgreSQL (pgx stdlib):

```bash
go get github.com/jackc/pgx/v5/stdlib
```

## 2. Open a Connection

```go
import (
    "context"
    "database/sql"
    "time"

    _ "github.com/jackc/pgx/v5/stdlib"
)

func openDB(dsn string) (*sql.DB, error) {
    db, err := sql.Open("pgx", dsn)
    if err != nil {
        return nil, err
    }

    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(5)
    db.SetConnMaxLifetime(30 * time.Minute)

    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()

    if err := db.PingContext(ctx); err != nil {
        return nil, err
    }

    return db, nil
}
```

## 3. Querying Data

```go
import (
    "context"
    "database/sql"
    "errors"
)

type User struct {
    ID    int64
    Name  string
    Email string
}

func getUser(ctx context.Context, db *sql.DB, id int64) (*User, error) {
    row := db.QueryRowContext(ctx, "SELECT id, name, email FROM users WHERE id=$1", id)

    var u User
    if err := row.Scan(&u.ID, &u.Name, &u.Email); err != nil {
        if errors.Is(err, sql.ErrNoRows) {
            return nil, nil
        }
        return nil, err
    }

    return &u, nil
}
```

## 4. Executing Statements

```go
import "context"

func createUser(ctx context.Context, db *sql.DB, u User) (int64, error) {
    var id int64
    err := db.QueryRowContext(
        ctx,
        "INSERT INTO users(name, email) VALUES ($1,$2) RETURNING id",
        u.Name, u.Email,
    ).Scan(&id)

    return id, err
}
```

## 5. Transactions

```go
import (
    "context"
    "database/sql"
)

tx, err := db.BeginTx(ctx, &sql.TxOptions{Isolation: sql.LevelReadCommitted})
if err != nil {
    return err
}

if _, err := tx.ExecContext(ctx, "UPDATE users SET name=$1 WHERE id=$2", name, id); err != nil {
    _ = tx.Rollback()
    return err
}

return tx.Commit()
```

## Tips

- Always use context with queries.
- Configure the connection pool for your workload.
- Handle `sql.ErrNoRows` explicitly.

---

[Previous: Working with JSON](./13-working-with-json.md) | [Back to Index](./README.md) | [Next: Intro to Gin ->](./15-intro-to-gin.md)
