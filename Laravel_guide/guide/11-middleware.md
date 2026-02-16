# 11 - Middleware in Laravel

Middleware are request filters that run before and after your controllers. They are the correct place for cross‑cutting concerns like authentication, rate limiting, CORS, tenant resolution, and request logging.

## Goals

- Register middleware the Laravel 12 way
- Compose middleware for routes and groups
- Build custom middleware with parameters and ordering

## 1. Where Middleware Is Registered (Laravel 12 Skeleton)

Laravel 12 uses `bootstrap/app.php` to register middleware instead of only `app/Http/Kernel.php`.

```php
// bootstrap/app.php
use Illuminate\Foundation\Application;
use Illuminate\Foundation\Configuration\Middleware;

return Application::configure(basePath: dirname(__DIR__))
    ->withMiddleware(function (Middleware $middleware) {
        // Global middleware
        $middleware->use([
            \App\Http\Middleware\TrustProxies::class,
            \Illuminate\Http\Middleware\HandleCors::class,
        ]);

        // Middleware groups
        $middleware->group('api', [
            \Illuminate\Routing\Middleware\ThrottleRequests::class . ':api',
            \Illuminate\Routing\Middleware\SubstituteBindings::class,
        ]);

        // Route middleware aliases
        $middleware->alias([
            'auth' => \App\Http\Middleware\Authenticate::class,
            'verified' => \Illuminate\Auth\Middleware\EnsureEmailIsVerified::class,
        ]);
    })
    ->create();
```

If you maintain an older app, middleware may still live in `app/Http/Kernel.php`. The concepts are the same.

## 2. Creating Middleware

```bash
php artisan make:middleware EnsureApiKey
```

```php
// app/Http/Middleware/EnsureApiKey.php
namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class EnsureApiKey
{
    public function handle(Request $request, Closure $next): Response
    {
        $apiKey = $request->header('X-API-KEY');
        if ($apiKey !== config('services.api.key')) {
            return response()->json(['message' => 'Invalid API key'], 401);
        }

        return $next($request);
    }
}
```

## 3. Attaching Middleware

### Route Level

```php
use Illuminate\Support\Facades\Route;

Route::get('/me', fn () => auth()->user())
    ->middleware(['auth:sanctum']);
```

### Group Level

```php
use App\Http\Controllers\AccountController;
use Illuminate\Support\Facades\Route;

Route::middleware(['auth:sanctum', 'verified'])->group(function () {
    Route::get('/account', [AccountController::class, 'show']);
});
```

## 4. Middleware Parameters

Define parameters after a colon and parse them inside your middleware.

```php
// routes/api.php
use App\Http\Controllers\AdminController;
use Illuminate\Support\Facades\Route;

Route::get('/admin', [AdminController::class, 'index'])
    ->middleware('role:admin');
```

```php
// app/Http/Middleware/EnsureRole.php
namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class EnsureRole
{
    public function handle(Request $request, Closure $next, string $role): Response
    {
        if (! $request->user() || ! $request->user()->hasRole($role)) {
            return response()->json(['message' => 'Forbidden'], 403);
        }

        return $next($request);
    }
}
```

## 5. Middleware Order and Priority

When middleware depend on each other, define priority order:

```php
// bootstrap/app.php
$middleware->priority([
    \Illuminate\Session\Middleware\StartSession::class,
    \Illuminate\View\Middleware\ShareErrorsFromSession::class,
    \App\Http\Middleware\Authenticate::class,
]);
```

In legacy apps, use `protected $middlewarePriority` in `app/Http/Kernel.php`.

## 6. Rate Limiting Middleware

Define rate limiters in a service provider:

```php
// app/Providers/AppServiceProvider.php
use Illuminate\Cache\RateLimiting\Limit;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\RateLimiter;

public function boot(): void
{
    RateLimiter::for('api', function (Request $request) {
        $userId = optional($request->user())->id;
        return Limit::perMinute(120)->by($userId ?: $request->ip());
    });
}
```

Then attach `throttle:api` to routes or groups.

## 7. Terminable Middleware

For actions after the response is sent:

```php
namespace App\Http\Middleware;

use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class LogApiRequest
{
    public function terminate(Request $request, Response $response): void
    {
        activity()->withProperties([
            'path' => $request->path(),
            'status' => $response->getStatusCode(),
        ])->log('api_request');
    }
}
```

## 8. Practical Middleware Patterns

- **Request IDs**: generate and attach `X-Request-ID` for traceability.
- **Tenant Resolution**: map subdomains or headers to tenants.
- **ETag/Cache**: short‑circuit responses for read endpoints.
- **Security**: enforce HTTPS and strict CORS in production.

## Tips

- Keep middleware small and focused.
- Avoid heavy database queries in middleware.
- Prefer middleware groups to avoid repetitive route chains.

---

[Previous: Requests and Validation](./10-requests-and-validation.md) | [Back to Index](./README.md) | [Next: Authentication with Sanctum ->](./12-authentication-sanctum.md)
