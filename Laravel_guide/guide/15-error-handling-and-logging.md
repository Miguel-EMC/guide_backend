# 15 - Error Handling and Logging

Robust error handling and structured logging are required for production. Laravel gives you a centralized place to standardize API errors and ship logs to any backend.

## Goals

- Return consistent JSON errors
- Capture exceptions with context
- Configure channels for production logging

## 1. Exception Handling in Laravel 12

Laravel 12 configures exceptions in `bootstrap/app.php`:

```php
// bootstrap/app.php
use Illuminate\Foundation\Application;
use Illuminate\Foundation\Configuration\Exceptions;
use Illuminate\Http\Request;
use Illuminate\Validation\ValidationException;
use Symfony\Component\HttpFoundation\Response;
use Throwable;

return Application::configure(basePath: dirname(__DIR__))
    ->withExceptions(function (Exceptions $exceptions) {
        $exceptions->render(function (ValidationException $e, Request $request) {
            if ($request->expectsJson()) {
                return response()->json([
                    'message' => 'Validation failed',
                    'errors' => $e->errors(),
                ], Response::HTTP_UNPROCESSABLE_ENTITY);
            }
        });

        $exceptions->render(function (Throwable $e, Request $request) {
            if ($request->is('api/*')) {
                return response()->json([
                    'message' => 'Server error',
                ], Response::HTTP_INTERNAL_SERVER_ERROR);
            }
        });
    })
    ->create();
```

Older applications may still use `app/Exceptions/Handler.php`, but the concepts are the same.

## 2. Custom Exception Classes

```php
namespace App\Exceptions;

use Illuminate\Http\Request;
use RuntimeException;
use Symfony\Component\HttpFoundation\Response;

class PaymentFailed extends RuntimeException
{
    public function render(Request $request): Response
    {
        return response()->json([
            'message' => 'Payment failed',
        ], 402);
    }
}
```

## 3. Reporting Exceptions

```php
use Throwable;

$exceptions->reportable(function (Throwable $e) {
    if (app()->bound('sentry')) {
        app('sentry')->captureException($e);
    }
});
```

## 4. Logging Channels

Configure channels in `config/logging.php`:

```php
'channels' => [
    'stack' => [
        'driver' => 'stack',
        'channels' => ['daily', 'stderr'],
        'ignore_exceptions' => false,
    ],
    'daily' => [
        'driver' => 'daily',
        'path' => storage_path('logs/laravel.log'),
        'level' => 'info',
        'days' => 14,
    ],
],
```

Use in code:

```php
use Illuminate\Support\Facades\Log;

Log::info('user_logged_in', ['user_id' => $user->id]);
Log::channel('stack')->error('payment_failed', ['order_id' => $order->id]);
```

## 5. Add Context to Every Log

```php
Log::withContext([
    'request_id' => request()->header('x-request-id'),
    'user_id' => optional(auth()->user())->id,
]);
```

## 6. HTTP Exceptions and Status Codes

```php
use Symfony\Component\HttpKernel\Exception\HttpException;

abort(404, 'Resource not found');
throw new HttpException(429, 'Too many requests');
```

## 7. Debug vs Production

- `APP_DEBUG=false` in production.
- Never expose stack traces to clients.
- Use `LOG_LEVEL=info` or `warning` in production.

## Tips

- Standardize error shapes across the API.
- Include a `request_id` in every error response.
- Send high‑severity logs to alerting systems.

---

[Previous: Service Container and Providers](./14-service-container-and-providers.md) | [Back to Index](./README.md) | [Next: Testing with Pest and PHPUnit ->](./16-testing-pest-phpunit.md)
