# 05 - Project Structure

Understanding Laravel's structure helps you scale the codebase cleanly. This chapter explains where things belong and how to organize larger applications.

## Goals

- Understand the default Laravel structure
- Know where to place business logic
- Scale into a feature-based layout

## 1. Default Structure (Laravel 12)

```
app/
  Console/
  Exceptions/
  Http/
    Controllers/
    Middleware/
    Requests/
    Resources/
  Models/
  Policies/
  Providers/
  Rules/
  Jobs/
  Events/
  Listeners/
  Notifications/
bootstrap/
config/
database/
  factories/
  migrations/
  seeders/
public/
resources/
routes/
storage/
tests/
```

## 2. Key Files and Folders

- `bootstrap/app.php`: Application configuration (routes, middleware, exceptions).
- `routes/api.php`: API routes for stateless JSON.
- `routes/web.php`: Browser routes with sessions and cookies.
- `routes/channels.php`: Broadcast channels.
- `app/Http/Controllers`: Request handlers.
- `app/Http/Requests`: Form Requests for validation and authorization.
- `app/Http/Resources`: JSON transformers for API responses.
- `app/Models`: Eloquent models.
- `app/Jobs`, `app/Events`, `app/Listeners`: async and event-driven logic.
- `config/`: All app configuration.
- `storage/`: Logs, cache, compiled views, file uploads.

## 3. Where Business Logic Should Live

Keep controllers thin. Put logic in service classes or domain classes.

Common choices:

- `app/Services` for application services
- `app/Actions` for single-purpose operations
- `app/Domain/<Feature>` for domain-driven modules

## 4. Feature-Based Structure (Recommended for Large Apps)

```
app/
  Features/
    Users/
      Controllers/
      Requests/
      Resources/
      Services/
      Policies/
    Billing/
      Controllers/
      Requests/
      Services/
      Events/
```

This keeps related files together and reduces cross-folder jumps.

## 5. API-First Conventions

- Routes live in `routes/api.php`.
- Controllers in `app/Http/Controllers/Api`.
- Resources in `app/Http/Resources`.
- Form Requests in `app/Http/Requests`.

## 6. Example: Service Layer

```php
// app/Services/OrderService.php
namespace App\Services;

use App\Events\OrderPlaced;
use App\Models\Order;
use Illuminate\Support\Facades\DB;

class OrderService
{
    public function create(array $data): Order
    {
        return DB::transaction(function () use ($data) {
            $order = Order::create($data);
            OrderPlaced::dispatch($order)->afterCommit();
            return $order;
        });
    }
}
```

## 7. Testing Structure

```
tests/
  Feature/
  Unit/
```

- Feature tests hit HTTP endpoints.
- Unit tests target services and domain logic.

## Tips

- Keep controllers thin and push logic into services.
- Group by feature as the app grows.
- Use Form Requests and API Resources consistently.

---

[Previous: macOS Installation](./04-installation-macos.md) | [Back to Index](./README.md) | [Next: Routing ->](./06-routing.md)
