# 14 - The Service Container and Providers

Laravel's service container is an IoC container that resolves dependencies and keeps your code decoupled. Service providers are where you register bindings and boot application services.

## Goals

- Bind interfaces to implementations
- Use contextual and scoped bindings
- Build clean service providers

## 1. Dependency Injection Basics

```php
use App\Contracts\PaymentGateway;
use App\Http\Controllers\Controller;
use Illuminate\Http\Request;

class BillingController extends Controller
{
    public function __construct(private PaymentGateway $gateway) {}

    public function charge(Request $request)
    {
        $this->gateway->charge(5000, $request->input('token'));
    }
}
```

Laravel resolves `PaymentGateway` from the container.

## 2. Binding Implementations

### Transient (new instance every time)

```php
use App\Contracts\PaymentGateway;
use App\Services\StripeGateway;

$this->app->bind(PaymentGateway::class, StripeGateway::class);
```

### Singleton (shared instance)

```php
use App\Services\AnalyticsClient;

$this->app->singleton(AnalyticsClient::class, function ($app) {
    return new AnalyticsClient(config('services.analytics.key'));
});
```

### Scoped (shared per request)

```php
use App\Tenancy\CurrentTenant;

$this->app->scoped(CurrentTenant::class, function () {
    return new CurrentTenant();
});
```

## 3. Contextual Binding

Different implementations for different classes:

```php
use App\Contracts\PaymentGateway;
use App\Services\AdminBillingService;
use App\Services\PublicBillingService;
use App\Services\StripeGateway;
use App\Services\PayPalGateway;

$this->app->when(AdminBillingService::class)
    ->needs(PaymentGateway::class)
    ->give(StripeGateway::class);

$this->app->when(PublicBillingService::class)
    ->needs(PaymentGateway::class)
    ->give(PayPalGateway::class);
```

## 4. Tagging Bindings

```php
use App\Services\PayPalGateway;
use App\Services\StripeGateway;

$this->app->tag([StripeGateway::class, PayPalGateway::class], 'gateways');

$gateways = $this->app->tagged('gateways');
```

## 5. Extending Existing Bindings

```php
use App\Logging\RequestIdProcessor;
use Psr\Log\LoggerInterface;

$this->app->extend(LoggerInterface::class, function ($logger) {
    $logger->pushProcessor(new RequestIdProcessor());
    return $logger;
});
```

## 6. Resolving Services Manually

```php
use App\Contracts\PaymentGateway;

$gateway = app(PaymentGateway::class);
$gateway = resolve(PaymentGateway::class);
```

Use manual resolution only when constructor injection is not possible.

## 7. Service Providers

Create a provider:

```bash
php artisan make:provider BillingServiceProvider
```

```php
namespace App\Providers;

use App\Contracts\PaymentGateway;
use App\Services\StripeGateway;
use Illuminate\Support\ServiceProvider;

class BillingServiceProvider extends ServiceProvider
{
    public function register(): void
    {
        $this->app->singleton(PaymentGateway::class, StripeGateway::class);
        $this->mergeConfigFrom(__DIR__.'/../../config/billing.php', 'billing');
    }

    public function boot(): void
    {
        $this->publishes([
            __DIR__.'/../../config/billing.php' => config_path('billing.php'),
        ], 'billing-config');
    }
}
```

## 8. Deferred Providers

If a provider only registers container bindings, you can defer it:

```php
namespace App\Providers;

use App\Contracts\PaymentGateway;
use Illuminate\Contracts\Support\DeferrableProvider;
use Illuminate\Support\ServiceProvider;

class BillingServiceProvider extends ServiceProvider implements DeferrableProvider
{
    public function provides(): array
    {
        return [PaymentGateway::class];
    }
}
```

## Tips

- Prefer constructor injection for clarity and testability.
- Use interfaces for public contracts.
- Keep `register()` free of side effects.

---

[Previous: API Resources](./13-api-resources.md) | [Back to Index](./README.md) | [Next: Error Handling and Logging ->](./15-error-handling-and-logging.md)
