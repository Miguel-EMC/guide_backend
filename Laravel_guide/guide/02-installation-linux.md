# 02 - Installation on Linux (Ubuntu/Debian)

This chapter sets up Laravel 12 on Ubuntu/Debian. It uses PHP 8.2+ and Composer.

## Prerequisites

- A user with `sudo` privileges
- Basic terminal knowledge

## Step 1: Install PHP and Extensions

Laravel 12 requires PHP 8.2 or higher. Install PHP and common extensions:

```bash
sudo apt-get update
sudo apt-get install -y \
  php8.3 php8.3-cli php8.3-fpm \
  php8.3-mysql php8.3-pgsql php8.3-sqlite3 \
  php8.3-mbstring php8.3-xml php8.3-curl php8.3-zip php8.3-bcmath

php --version
```

If your distribution does not include PHP 8.3, use PHP 8.2 or add a trusted PPA.

## Step 2: Install Composer

```bash
curl -sS https://getcomposer.org/installer -o /tmp/composer-setup.php
HASH=$(curl -sS https://composer.github.io/installer.sig)
php -r "if (hash_file('SHA384', '/tmp/composer-setup.php') === '$HASH') { echo 'Installer verified'; } else { echo 'Installer corrupt'; unlink('/tmp/composer-setup.php'); } echo PHP_EOL;"

sudo php /tmp/composer-setup.php --install-dir=/usr/local/bin --filename=composer
composer --version
```

## Step 3: Install the Laravel Installer (Optional)

```bash
composer global require laravel/installer
```

Make sure Composer bin is on your PATH:

```bash
echo 'export PATH="$HOME/.config/composer/vendor/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc
```

## Step 4: Create a Project

### Option A: Laravel Installer

```bash
laravel new my-laravel-api
```

### Option B: Composer

```bash
composer create-project laravel/laravel my-laravel-api
```

## Step 5: Configure Environment

```bash
cd my-laravel-api
cp .env.example .env
php artisan key:generate
```

Update `.env` with your database credentials.

## Step 6: Run the App

```bash
php artisan serve
```

Visit `http://127.0.0.1:8000`.

## Tips

- Use PostgreSQL or MySQL for production.
- Use SQLite for quick local testing.
- Keep PHP and Composer updated.

---

[Previous: Introduction](./01-introduction.md) | [Back to Index](./README.md) | [Next: Windows Installation ->](./03-installation-windows.md)
