# 03 - Installation on Windows

There are three common approaches for Laravel on Windows. Pick the one that matches your workflow and team environment.

## Goals

- Install PHP and Composer quickly
- Run Laravel with minimal friction
- Keep dev and production close

## Option A: Laravel Herd (Recommended)

Laravel Herd provides PHP, Composer, and a local web server with minimal setup.

1. Install Laravel Herd for Windows.
2. Open the Herd terminal.
3. Verify tools:

```bash
php --version
composer --version
```

4. Create a project:

```bash
composer create-project laravel/laravel my-laravel-api
```

Or use the installer:

```bash
composer global require laravel/installer
laravel new my-laravel-api
```

5. Start the app:

```bash
cd my-laravel-api
php artisan serve
```

## Option B: WSL2 (Best Linux Parity)

WSL2 gives you a real Linux environment and matches production more closely.

1. Install WSL2 and Ubuntu.
2. Follow the Linux installation chapter inside WSL2.
3. Use VS Code with the WSL extension for a clean workflow.

## Option C: Laragon (Alternative)

Laragon is an all-in-one stack with Apache/Nginx, PHP, and MySQL. It is fast and convenient if you already use it.

## Database and Redis (Recommended)

Use Docker Desktop to run PostgreSQL and Redis:

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

- Use WSL2 for teams that deploy on Linux.
- Keep PHP 8.2+ and Composer updated.
- Use Windows Terminal and Git for a better workflow.

---

[Previous: Linux Installation](./02-installation-linux.md) | [Back to Index](./README.md) | [Next: macOS Installation ->](./04-installation-macos.md)
