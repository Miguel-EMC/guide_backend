# 02 - Installation on Linux (Ubuntu/Debian)

This chapter sets up a modern Laravel 12 environment on Linux with PHP 8.2+ and Composer.

## Goals

- Install PHP with required extensions
- Install Composer and Node.js tooling
- Create and run a Laravel project

## Prerequisites

- A user with `sudo` privileges
- Basic terminal knowledge
- Git installed

```bash
sudo apt-get update
sudo apt-get install -y git curl unzip
```

## 1. Install PHP and Extensions

Laravel needs PHP 8.2+ with common extensions:

```bash
sudo apt-get install -y \
  php8.3 php8.3-cli php8.3-fpm \
  php8.3-mysql php8.3-pgsql php8.3-sqlite3 \
  php8.3-mbstring php8.3-xml php8.3-curl php8.3-zip php8.3-bcmath \
  php8.3-intl php8.3-gd

php --version
```

If your distro does not include PHP 8.3, use 8.2 or install from a trusted PPA.

## 2. Install Composer

```bash
curl -sS https://getcomposer.org/installer -o /tmp/composer-setup.php
HASH=$(curl -sS https://composer.github.io/installer.sig)
php -r "if (hash_file('SHA384', '/tmp/composer-setup.php') === '$HASH') { echo 'Installer verified'; } else { echo 'Installer corrupt'; unlink('/tmp/composer-setup.php'); } echo PHP_EOL;"

sudo php /tmp/composer-setup.php --install-dir=/usr/local/bin --filename=composer
composer --version
```

## 3. Install Node.js (Recommended)

Vite and frontend tooling require Node.js. Use `fnm` or `nvm` for version management.

```bash
node --version
npm --version
```

## 4. Optional: Database via Docker

If you do not want to install a local database, use Docker Compose.

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

Start services:

```bash
docker compose up -d
```

## 5. Create a Project

### Option A: Laravel Installer

```bash
composer global require laravel/installer
export PATH="$HOME/.config/composer/vendor/bin:$PATH"
laravel new my-laravel-api
```

### Option B: Composer

```bash
composer create-project laravel/laravel my-laravel-api
```

## 6. Configure Environment

```bash
cd my-laravel-api
cp .env.example .env
php artisan key:generate
```

Update `.env` with database credentials:

```
DB_CONNECTION=pgsql
DB_HOST=127.0.0.1
DB_PORT=5432
DB_DATABASE=laravel
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

## 7. Run Migrations and Start the App

```bash
php artisan migrate
php artisan serve
```

Visit `http://127.0.0.1:8000`.

## 8. Optional: Enable Xdebug (Local Only)

Xdebug helps with debugging and code coverage. Install it only for local development.

## Tips

- Prefer PostgreSQL for production reliability.
- Keep PHP and Composer updated.
- Use Docker services for parity with production.

---

[Previous: Introduction](./01-introduction.md) | [Back to Index](./README.md) | [Next: Windows Installation ->](./03-installation-windows.md)
