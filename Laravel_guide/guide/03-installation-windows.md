# 03 - Installation on Windows

There are three common approaches for Laravel on Windows:

1. **Laravel Herd** (recommended)
2. **Laragon** (popular and easy)
3. **WSL2** (Linux environment inside Windows)

This guide uses Laravel Herd because it is the official, modern approach.

## Option A: Laravel Herd (Recommended)

### Step 1: Install Herd

Download and install Laravel Herd for Windows. It includes PHP, Composer, and a local web server.

### Step 2: Verify Tooling

Open the Herd terminal and run:

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

## Option B: Laragon (Alternative)

Laragon bundles Apache/Nginx, PHP, and MySQL in one installer. It is fast for local development.

## Option C: WSL2 (Advanced)

WSL2 gives you a full Linux environment. Follow the Linux installation chapter inside WSL2.

## Tips

- Keep PHP updated to 8.2+.
- Use Herd or WSL2 for the cleanest setup.
- Use Windows Terminal and Git for a better workflow.

---

[Previous: Linux Installation](./02-installation-linux.md) | [Back to Index](./README.md) | [Next: macOS Installation ->](./04-installation-macos.md)
