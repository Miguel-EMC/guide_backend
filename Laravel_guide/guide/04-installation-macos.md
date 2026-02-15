# 04 - Installation on macOS

The recommended approach on macOS is **Laravel Herd**. It installs PHP, Composer, and a local server.

## Option A: Laravel Herd (Recommended)

### Step 1: Install Herd

Download and install Laravel Herd for macOS. It includes PHP and Composer.

### Step 2: Verify Tooling

```bash
php --version
composer --version
```

### Step 3: Create a Project

```bash
composer create-project laravel/laravel my-laravel-api
```

Or use the Laravel installer:

```bash
composer global require laravel/installer
laravel new my-laravel-api
```

### Step 4: Run the App

```bash
cd my-laravel-api
php artisan serve
```

## Option B: Valet (Alternative)

Valet is another popular local environment for macOS. Use it if you prefer a lightweight local server.

## Tips

- Keep PHP updated to 8.2+.
- Use Herd for the simplest setup.
- Prefer a separate database container for local development.

---

[Previous: Windows Installation](./03-installation-windows.md) | [Back to Index](./README.md) | [Next: Project Structure ->](./05-project-structure.md)
