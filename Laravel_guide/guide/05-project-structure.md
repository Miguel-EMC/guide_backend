# 05 - Project Structure

Understanding the Laravel directory structure makes it easier to navigate large codebases.

## Core Folders

```
app/
  Console/
  Exceptions/
  Http/
    Controllers/
    Middleware/
    Requests/
  Models/
  Providers/
bootstrap/
config/
database/
  migrations/
  seeders/
  factories/
public/
resources/
routes/
storage/
tests/
```

## Key Directories

- `app/Http/Controllers`: Request handlers and API controllers.
- `app/Http/Requests`: Form Request validation classes.
- `app/Models`: Eloquent models.
- `config`: Application configuration.
- `routes`: Route definitions (`web.php`, `api.php`).
- `database/migrations`: Schema changes.
- `storage`: Logs, cache, and generated files.
- `tests`: Feature and unit tests.

## Routes Overview

- `routes/api.php`: Stateless JSON APIs
- `routes/web.php`: Browser routes (sessions/cookies)

For API-only backends, you will mostly work in `routes/api.php`.

## Environment Files

- `.env`: Local environment configuration
- `.env.example`: Template for new environments

## Common Conventions

- Keep controllers thin and move logic to services.
- Use Form Requests for validation.
- Group domain logic by features when the codebase grows.

## Example: Feature-Based Structure (Optional)

```
app/
  Features/
    Users/
      Controllers/
      Requests/
      Services/
    Posts/
      Controllers/
      Requests/
      Services/
```

## Tips

- Avoid putting heavy logic in controllers.
- Use `app/Services` or feature folders for business logic.
- Keep `routes/api.php` clean by using route groups.

---

[Previous: macOS Installation](./04-installation-macos.md) | [Back to Index](./README.md) | [Next: Routing ->](./06-routing.md)
