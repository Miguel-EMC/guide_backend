# 20 - Package Development

Laravel packages let you share reusable features across projects: SDKs, integrations, shared UI, and internal tooling.

## Goals

- Structure a Laravel package
- Register service providers and config
- Test packages cleanly

## 1. Package Structure

```
acme/my-package/
  src/
    MyPackageServiceProvider.php
    Services/
  config/
    my-package.php
  routes/
    api.php
  database/
    migrations/
  tests/
  composer.json
```

## 2. Composer Autoloading

```json
{
  "name": "acme/my-package",
  "type": "library",
  "autoload": {
    "psr-4": {
      "Acme\\MyPackage\\": "src/"
    }
  },
  "extra": {
    "laravel": {
      "providers": [
        "Acme\\MyPackage\\MyPackageServiceProvider"
      ]
    }
  }
}
```

Auto‑discovery will register the provider.

## 3. Service Provider

```php
namespace Acme\MyPackage;

use Illuminate\Support\ServiceProvider;

class MyPackageServiceProvider extends ServiceProvider
{
    public function register(): void
    {
        $this->mergeConfigFrom(__DIR__.'/../config/my-package.php', 'my-package');
    }

    public function boot(): void
    {
        $this->publishes([
            __DIR__.'/../config/my-package.php' => config_path('my-package.php'),
        ], 'my-package-config');

        $this->loadRoutesFrom(__DIR__.'/../routes/api.php');
        $this->loadMigrationsFrom(__DIR__.'/../database/migrations');
    }
}
```

## 4. Publishing Assets

```php
$this->publishes([
    __DIR__.'/../resources/views' => resource_path('views/vendor/my-package'),
], 'my-package-views');
```

## 5. Package Configuration

```php
// config/my-package.php
return [
    'enabled' => true,
    'api_key' => env('MY_PACKAGE_KEY'),
];
```

## 6. Testing with Testbench

```bash
composer require orchestra/testbench --dev
```

```php
use Orchestra\Testbench\TestCase;

class PackageTest extends TestCase
{
    protected function getPackageProviders($app): array
    {
        return [\Acme\MyPackage\MyPackageServiceProvider::class];
    }
}
```

## 7. Versioning and Releases

- Follow SemVer (`MAJOR.MINOR.PATCH`).
- Tag releases in Git.
- Maintain a `CHANGELOG.md`.

## Tips

- Keep the public API small and stable.
- Avoid hard dependencies on the host app.
- Provide sensible defaults via config.

---

[Previous: WebSockets with Reverb](./19-websockets-laravel-reverb.md) | [Back to Index](./README.md) | [Next: Deployment ->](./21-deployment.md)
