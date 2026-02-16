# 01 - Introduction to Laravel 12

Laravel is a full-stack PHP framework that is exceptionally strong for backend APIs. It gives you routing, validation, ORM, queues, caching, and testing out of the box so you can focus on product logic instead of wiring.

## Goals

- Understand what Laravel provides for backend work
- See the request lifecycle at a high level
- Know what you will build in this guide

## Why Laravel for Backend APIs

- A complete ecosystem: Eloquent ORM, migrations, queues, events, caching, testing.
- Consistent developer experience: Artisan CLI, expressive syntax, strong defaults.
- Production tooling: job workers, schedulers, caching, and Octane.
- First-party packages: Sanctum (API auth), Horizon (queues), Scout (search).

## Baseline Requirements

Laravel 12 requires PHP 8.2+. Most teams run PHP 8.3 or newer for performance and security.

You will also need:

- Composer (dependency manager)
- A database (PostgreSQL or MySQL recommended)
- Node.js for asset tooling and Vite when needed

## Request Lifecycle (High Level)

```
Request -> Route -> Middleware -> Controller -> Response
                      |-> Validation
                      |-> Authorization
Exceptions -> Error handler -> JSON response
```

Understanding this flow helps you place code in the right layer.

## What You Will Build

- REST APIs with consistent JSON responses
- Token-based authentication with Sanctum
- Background jobs and events
- Production-ready deployments and performance patterns

## Laravel Concepts You Will Use

- Routes and route model binding
- Controllers and Form Requests
- Eloquent relationships and query scopes
- Migrations, seeders, and factories
- Jobs, events, and listeners
- Caching and queues

## Backend Use Cases

- JSON APIs and mobile backends
- Admin APIs and internal services
- Webhooks and integrations
- Real-time systems with WebSockets

## Useful Artisan Commands

```bash
php artisan list
php artisan route:list
php artisan make:controller UserController
php artisan make:model Post -mfs
php artisan migrate
php artisan test
```

## Guide Structure

1. Setup and environment by OS
2. Routing, controllers, and Eloquent
3. Validation, auth, and resources
4. Testing, queues, and events
5. Production, scaling, and architecture

---

[Previous: Index](./README.md) | [Back to Index](./README.md) | [Next: Linux Installation ->](./02-installation-linux.md)
