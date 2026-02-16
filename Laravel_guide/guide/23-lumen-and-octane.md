# 23 - Lumen and Octane

This chapter covers two performance-oriented options for Laravel: Lumen (micro-framework) and Laravel Octane (high-performance application server). Both serve different use cases for building fast PHP APIs.

## Goals

- Understand when Lumen still makes sense
- Install and configure Laravel Octane properly
- Avoid common pitfalls with long-running processes
- Benchmark and optimize performance

## 1. Lumen: Status and Use Cases

### What Is Lumen?

Lumen is a stripped-down version of Laravel designed for microservices and APIs. It removes many Laravel features (Blade, sessions, most middleware) to achieve faster bootstrap times.

### Current Status

As of Laravel 11+, Lumen is in maintenance mode. New projects should consider:

1. **Laravel with API starter**: Full Laravel trimmed for APIs
2. **Laravel Octane**: High-performance without sacrificing features
3. **Lumen**: Only for existing projects or extreme constraints

### When Lumen Still Makes Sense

| Use Case | Why Lumen |
| --- | --- |
| Existing Lumen project | Migration cost too high |
| Extremely resource-constrained | Smaller memory footprint |
| Simple stateless APIs | No need for Laravel features |
| Serverless functions | Minimal cold start time |

### Lumen Project Structure

```plaintext
lumen-api/
├── app/
│   ├── Console/
│   ├── Events/
│   ├── Exceptions/
│   ├── Http/
│   │   ├── Controllers/
│   │   └── Middleware/
│   ├── Jobs/
│   ├── Listeners/
│   ├── Models/
│   └── Providers/
├── bootstrap/
│   └── app.php
├── database/
├── routes/
│   └── web.php
├── storage/
├── tests/
├── .env
└── composer.json
```

### Lumen Bootstrap

```php
// bootstrap/app.php
require_once __DIR__.'/../vendor/autoload.php';

$app = new Laravel\Lumen\Application(
    dirname(__DIR__)
);

// Enable facades (optional, adds overhead)
// $app->withFacades();

// Enable Eloquent (optional)
$app->withEloquent();

// Register service providers
$app->register(App\Providers\AppServiceProvider::class);

// Load routes
$app->router->group([
    'namespace' => 'App\Http\Controllers',
], function ($router) {
    require __DIR__.'/../routes/web.php';
});

return $app;
```

### Lumen Controller Example

```php
// app/Http/Controllers/UserController.php
namespace App\Http\Controllers;

use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class UserController extends Controller
{
    public function index(): JsonResponse
    {
        return response()->json(User::all());
    }

    public function show(int $id): JsonResponse
    {
        $user = User::findOrFail($id);
        return response()->json($user);
    }

    public function store(Request $request): JsonResponse
    {
        $this->validate($request, [
            'name' => 'required|string|max:255',
            'email' => 'required|email|unique:users',
        ]);

        $user = User::create($request->all());
        return response()->json($user, 201);
    }
}
```

### Migrating from Lumen to Laravel

```bash
# 1. Create new Laravel project
composer create-project laravel/laravel new-api

# 2. Copy models, controllers, migrations
cp -r lumen-api/app/Models/* new-api/app/Models/
cp -r lumen-api/app/Http/Controllers/* new-api/app/Http/Controllers/

# 3. Update namespaces and imports
# Controllers extend App\Http\Controllers\Controller
# Request validation uses FormRequest classes

# 4. Convert routes from Lumen to Laravel syntax
# Lumen: $router->get('/users', 'UserController@index');
# Laravel: Route::get('/users', [UserController::class, 'index']);

# 5. Run tests and fix any breaking changes
php artisan test
```

## 2. Laravel Octane Overview

### What Is Octane?

Laravel Octane supercharges your application by serving it using high-performance servers that keep the application in memory between requests.

### How It Works

```plaintext
Traditional PHP-FPM:
Request → Boot Framework → Handle → Response → Destroy

Octane:
Boot Framework (once)
    ↓
Request 1 → Handle → Response
Request 2 → Handle → Response
Request 3 → Handle → Response
...
```

### Supported Servers

| Server | Description | Best For |
| --- | --- | --- |
| **FrankenPHP** | PHP built into Caddy | Easy setup, modern choice |
| **RoadRunner** | Go-based PHP server | Production stability |
| **Swoole** | C-based PHP extension | Maximum performance |

### Performance Comparison

```plaintext
Requests per second (example benchmark):

PHP-FPM:     ~500 req/s
RoadRunner:  ~3,000 req/s
Swoole:      ~5,000 req/s
FrankenPHP:  ~4,000 req/s

Note: Actual numbers vary based on application complexity
```

## 3. Installing Octane

### Install Package

```bash
composer require laravel/octane
```

### Install with FrankenPHP (Recommended)

```bash
php artisan octane:install --server=frankenphp

# Or download standalone binary
php artisan octane:frankenphp --download
```

### Install with RoadRunner

```bash
php artisan octane:install --server=roadrunner

# Downloads RoadRunner binary automatically
```

### Install with Swoole

```bash
# Install Swoole extension first
pecl install swoole

# Add to php.ini
# extension=swoole.so

php artisan octane:install --server=swoole
```

### Start Development Server

```bash
# FrankenPHP
php artisan octane:start --server=frankenphp

# RoadRunner
php artisan octane:start --server=roadrunner

# Swoole
php artisan octane:start --server=swoole

# With file watching for development
php artisan octane:start --watch
```

## 4. Octane Configuration

### Configuration File

```php
// config/octane.php
return [
    'server' => env('OCTANE_SERVER', 'frankenphp'),

    'https' => env('OCTANE_HTTPS', false),

    'listeners' => [
        WorkerStarting::class => [
            EnsureUploadedFilesAreValid::class,
            EnsureUploadedFilesCanBeMoved::class,
        ],
        RequestReceived::class => [
            // Custom listeners
        ],
        RequestHandled::class => [
            // Log slow requests
        ],
        RequestTerminated::class => [
            FlushTemporaryContainerInstances::class,
        ],
    ],

    // Directories to watch for changes
    'watch' => [
        'app',
        'bootstrap',
        'config',
        'database',
        'public/**/*.php',
        'resources/**/*.php',
        'routes',
        'composer.lock',
        '.env',
    ],

    // Number of workers
    'workers' => env('OCTANE_WORKERS', 'auto'),

    // Task workers for concurrent tasks
    'task_workers' => env('OCTANE_TASK_WORKERS', 'auto'),

    // Max requests before worker restart
    'max_requests' => env('OCTANE_MAX_REQUESTS', 500),

    // Tables for caching (Swoole only)
    'tables' => [
        'example:1000' => [
            'name' => 'string:1000',
            'votes' => 'int',
        ],
    ],

    // Tick interval for periodic tasks
    'tick' => true,
    'tick_interval' => 10000,

    // Flush services between requests
    'flush' => [
        // Services to flush
    ],

    // Garbage collection
    'garbage' => 50,
];
```

### Environment Variables

```env
OCTANE_SERVER=frankenphp
OCTANE_WORKERS=auto
OCTANE_TASK_WORKERS=auto
OCTANE_MAX_REQUESTS=500
OCTANE_HTTPS=false
```

## 5. Octane Pitfalls and Best Practices

### The Stale State Problem

Because the application stays in memory, static properties and singletons persist:

```php
// BAD: Static state persists across requests
class CartService
{
    private static array $items = [];

    public function addItem(array $item): void
    {
        self::$items[] = $item;
    }

    public function getItems(): array
    {
        return self::$items;
    }
}

// User A adds item, User B sees it!
```

### Solution: Use Request-Scoped Services

```php
// GOOD: Request-scoped via container
class CartService
{
    private array $items = [];

    public function addItem(array $item): void
    {
        $this->items[] = $item;
    }

    public function getItems(): array
    {
        return $this->items;
    }
}

// AppServiceProvider.php
public function register(): void
{
    // Fresh instance per request
    $this->app->bind(CartService::class);
}
```

### Avoid Global State

```php
// BAD: Global variable
$currentUser = null;

function setUser($user) {
    global $currentUser;
    $currentUser = $user;
}

// GOOD: Use Laravel's context or request
use Illuminate\Support\Facades\Context;

Context::add('user', $user);
$user = Context::get('user');

// Or use request attributes
$request->attributes->set('user', $user);
```

### Memory Leaks

```php
// BAD: Growing array across requests
class MetricsCollector
{
    private array $metrics = [];

    public function record(string $name, float $value): void
    {
        $this->metrics[] = ['name' => $name, 'value' => $value];
    }
}

// GOOD: Flush periodically or use bounded storage
class MetricsCollector
{
    private array $metrics = [];
    private int $maxSize = 1000;

    public function record(string $name, float $value): void
    {
        if (count($this->metrics) >= $this->maxSize) {
            $this->flush();
        }
        $this->metrics[] = ['name' => $name, 'value' => $value];
    }

    public function flush(): void
    {
        // Send to metrics service
        $this->sendToService($this->metrics);
        $this->metrics = [];
    }
}
```

### Database Connections

```php
// Octane automatically handles connection pooling
// But watch for connection limits

// config/database.php
'mysql' => [
    'driver' => 'mysql',
    // Reduce connections per worker
    'pool' => [
        'min_connections' => 1,
        'max_connections' => 10,
    ],
],
```

### Service Container Gotchas

```php
// Singletons persist across requests by default
// Mark services that need fresh instances

// config/octane.php
'flush' => [
    // Flush these services between requests
    \App\Services\UserContext::class,
    \App\Services\RequestLogger::class,
],

// Or use scoped bindings
$this->app->scoped(UserContext::class);
```

## 6. Concurrent Tasks

Octane allows running tasks concurrently:

```php
use Laravel\Octane\Facades\Octane;

// Run tasks in parallel
[$users, $orders, $products] = Octane::concurrently([
    fn () => User::all(),
    fn () => Order::recent()->get(),
    fn () => Product::featured()->get(),
]);

// With timeout
$results = Octane::concurrently([
    'users' => fn () => Http::get('http://users-api/users'),
    'orders' => fn () => Http::get('http://orders-api/orders'),
], 5000); // 5 second timeout

$users = $results['users'];
$orders = $results['orders'];
```

### Task Workers

```php
// Dispatch to task worker (doesn't block main worker)
Octane::task(function () {
    // Heavy computation
    $this->processLargeDataset();
});

// With callback
Octane::task(function () {
    return $this->expensiveCalculation();
})->then(function ($result) {
    Cache::put('calculation_result', $result);
});
```

## 7. Swoole-Specific Features

### Swoole Tables (In-Memory Cache)

```php
// config/octane.php
'tables' => [
    'cache:10000' => [
        'key' => 'string:255',
        'value' => 'string:10000',
        'expiry' => 'int',
    ],
],

// Usage
use Laravel\Octane\Facades\Octane;

Octane::table('cache')->set('user:1', [
    'key' => 'user:1',
    'value' => json_encode($userData),
    'expiry' => time() + 3600,
]);

$data = Octane::table('cache')->get('user:1');
```

### Swoole Tick (Periodic Tasks)

```php
// config/octane.php
'tick' => true,
'tick_interval' => 10000, // 10 seconds

// In a service provider
use Laravel\Octane\Facades\Octane;

Octane::tick('metrics', function () {
    $this->flushMetrics();
})->seconds(30);

Octane::tick('health-check', function () {
    $this->checkDependencies();
})->minutes(1);
```

## 8. Production Deployment

### Supervisor Configuration

```ini
; /etc/supervisor/conf.d/octane.conf
[program:octane]
process_name=%(program_name)s
command=php /var/www/app/artisan octane:start --server=frankenphp --host=0.0.0.0 --port=8000 --workers=auto
autostart=true
autorestart=true
stopasgroup=true
killasgroup=true
user=www-data
redirect_stderr=true
stdout_logfile=/var/www/app/storage/logs/octane.log
stopwaitsecs=3600
```

### Docker Configuration

```dockerfile
# Dockerfile for Octane with FrankenPHP
FROM dunglas/frankenphp:latest-php8.3

# Install PHP extensions
RUN install-php-extensions \
    pdo_mysql \
    redis \
    pcntl \
    opcache

# Copy application
COPY . /app
WORKDIR /app

# Install dependencies
RUN composer install --no-dev --optimize-autoloader

# Set permissions
RUN chown -R www-data:www-data /app

# Configure FrankenPHP
ENV SERVER_NAME=:8000
ENV FRANKENPHP_CONFIG="worker ./public/index.php"

EXPOSE 8000

CMD ["php", "artisan", "octane:frankenphp"]
```

### Docker Compose

```yaml
# docker-compose.yml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8000:8000"
    environment:
      - APP_ENV=production
      - OCTANE_SERVER=frankenphp
      - OCTANE_WORKERS=auto
    depends_on:
      - redis
      - mysql
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8000/up"]
      interval: 10s
      timeout: 5s
      retries: 3

  redis:
    image: redis:7-alpine

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: secret
      MYSQL_DATABASE: app
```

### Nginx as Reverse Proxy

```nginx
# /etc/nginx/sites-available/app.conf
upstream octane {
    server 127.0.0.1:8000;
    keepalive 32;
}

server {
    listen 80;
    server_name api.example.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.example.com;

    ssl_certificate /etc/ssl/certs/api.example.com.crt;
    ssl_certificate_key /etc/ssl/private/api.example.com.key;

    location / {
        proxy_pass http://octane;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Connection "";
        proxy_buffering off;
        proxy_read_timeout 300;
    }

    # Static files served by Nginx
    location /storage {
        alias /var/www/app/storage/app/public;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

### Reloading Workers

```bash
# Graceful reload (waits for current requests)
php artisan octane:reload

# Or send SIGUSR1 to the process
kill -SIGUSR1 $(pgrep -f "octane:start")
```

## 9. Monitoring Octane

### Health Check Endpoint

```php
// routes/api.php
Route::get('/health', function () {
    return response()->json([
        'status' => 'healthy',
        'server' => config('octane.server'),
        'workers' => config('octane.workers'),
        'memory' => [
            'usage' => memory_get_usage(true),
            'peak' => memory_get_peak_usage(true),
        ],
    ]);
});
```

### Prometheus Metrics

```php
// app/Providers/OctaneMetricsProvider.php
namespace App\Providers;

use Illuminate\Support\ServiceProvider;
use Laravel\Octane\Events\RequestReceived;
use Laravel\Octane\Events\RequestTerminated;
use Prometheus\CollectorRegistry;

class OctaneMetricsProvider extends ServiceProvider
{
    public function boot(CollectorRegistry $registry): void
    {
        $histogram = $registry->getOrRegisterHistogram(
            'octane',
            'request_duration_seconds',
            'Octane request duration',
            ['method', 'route', 'status']
        );

        $gauge = $registry->getOrRegisterGauge(
            'octane',
            'memory_usage_bytes',
            'Worker memory usage'
        );

        Event::listen(RequestReceived::class, function ($event) {
            $event->sandbox->instance('request_start', microtime(true));
        });

        Event::listen(RequestTerminated::class, function ($event) use ($histogram, $gauge) {
            $duration = microtime(true) - $event->sandbox->make('request_start');

            $histogram->observe($duration, [
                $event->request->method(),
                $event->request->route()?->getName() ?? 'unknown',
                $event->response->getStatusCode(),
            ]);

            $gauge->set(memory_get_usage(true));
        });
    }
}
```

### Logging Slow Requests

```php
// app/Listeners/LogSlowRequests.php
namespace App\Listeners;

use Illuminate\Support\Facades\Log;
use Laravel\Octane\Events\RequestTerminated;

class LogSlowRequests
{
    private float $threshold = 1.0; // 1 second

    public function handle(RequestTerminated $event): void
    {
        $duration = $event->request->server('REQUEST_TIME_FLOAT')
            ? microtime(true) - $event->request->server('REQUEST_TIME_FLOAT')
            : 0;

        if ($duration > $this->threshold) {
            Log::warning('Slow request detected', [
                'method' => $event->request->method(),
                'uri' => $event->request->getRequestUri(),
                'duration' => round($duration, 3),
                'memory' => memory_get_peak_usage(true),
            ]);
        }
    }
}
```

## 10. When to Use Octane

### Good Candidates

| Scenario | Why Octane Helps |
| --- | --- |
| High-traffic APIs | Handles more requests per worker |
| CPU-bound operations | Avoid repeated bootstrap cost |
| Real-time features | WebSockets, SSE support |
| Low-latency requirements | Sub-millisecond response possible |

### Not Ideal For

| Scenario | Why to Avoid |
| --- | --- |
| Heavy static state usage | Requires significant refactoring |
| Memory-intensive per-request | Workers may run out of memory |
| Legacy codebases | Global state issues |
| Simple CRUD with low traffic | PHP-FPM is simpler |

## 11. Benchmarking

### Load Testing with k6

```javascript
// load-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 100 },
        { duration: '1m', target: 100 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<200'],
        http_req_failed: ['rate<0.01'],
    },
};

export default function () {
    const res = http.get('http://localhost:8000/api/users');
    check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 200ms': (r) => r.timings.duration < 200,
    });
    sleep(0.1);
}
```

### Running Benchmark

```bash
# Install k6
brew install k6

# Run load test
k6 run load-test.js

# Compare PHP-FPM vs Octane
# Run against PHP-FPM first, then Octane
```

### Profiling with Blackfire

```bash
# Profile Octane application
blackfire curl http://localhost:8000/api/users

# Profile specific routes
blackfire --samples=10 curl http://localhost:8000/api/expensive-operation
```

## Tips

- Always load test before and after enabling Octane.
- Review code for static properties and global state.
- Use `php artisan octane:reload` after deployments.
- Monitor memory usage per worker.
- Start with fewer workers and scale up based on load.
- Use concurrent tasks for I/O-bound operations.
- Consider RoadRunner for production stability, FrankenPHP for ease of use.

---

[Previous: Microservices Introduction](./22-microservices-introduction.md) | [Back to Index](./README.md) | [Next: Microservices Communication ->](./24-microservices-communication.md)
