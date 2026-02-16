# 04 - Installation on macOS

The recommended approach on macOS is Laravel Herd. It installs PHP, Composer, and a local web server with minimal setup.

## Goals

- Install PHP and Composer quickly
- Create a Laravel project
- Keep local services consistent with production

## Option A: Laravel Herd (Recommended)

1. Install Laravel Herd for macOS.
2. Verify tooling:

```bash
php --version
composer --version
```

3. Create a project:

```bash
composer create-project laravel/laravel my-laravel-api
```

Or use the installer:

```bash
composer global require laravel/installer
laravel new my-laravel-api
```

4. Start the app:

```bash
cd my-laravel-api
php artisan serve
```

## Option B: Valet (Alternative)

Valet is a lightweight local environment based on Nginx. Use it if you already have Homebrew and want a minimal setup.

## Node.js Tooling

If you use Vite or frontend tooling, install Node.js with `fnm` or `nvm`.

## Database and Redis (Recommended)

Use Docker for local databases to match production:

```yaml
# docker-compose.yml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: laravel
    ports:
      - "5432:5432"
  redis:
    image: redis:7
    ports:
      - "6379:6379"
```

## Tips

- Keep PHP 8.2+ updated.
- Use Docker for databases to avoid local conflicts.
- Keep your `.env` checked into `.env.example` only.

---

[Previous: Windows Installation](./03-installation-windows.md) | [Back to Index](./README.md) | [Next: Project Structure ->](./05-project-structure.md)
