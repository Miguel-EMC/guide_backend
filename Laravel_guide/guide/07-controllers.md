# 07 - Controllers

Controllers organize request handling and keep routes clean.

## Basic Controller

```bash
php artisan make:controller UserController
```

```php
namespace App\Http\Controllers;

use App\Models\User;

class UserController extends Controller
{
    public function index()
    {
        return User::query()->paginate(20);
    }
}
```

## Resource Controller

```bash
php artisan make:controller PostController --api
```

```php
class PostController extends Controller
{
    public function index() {}
    public function show(Post $post) {}
    public function store(Request $request) {}
    public function update(Request $request, Post $post) {}
    public function destroy(Post $post) {}
}
```

## Single-Action Controller

```bash
php artisan make:controller HealthController --invokable
```

```php
class HealthController extends Controller
{
    public function __invoke()
    {
        return response()->json(['ok' => true]);
    }
}
```

## Dependency Injection

```php
public function store(PostService $service, StorePostRequest $request)
{
    return $service->create($request->validated());
}
```

## Tips

- Keep controllers thin.
- Delegate business logic to services.
- Use Form Requests for validation.

---

[Previous: Routing](./06-routing.md) | [Back to Index](./README.md) | [Next: Eloquent ORM and Models ->](./08-eloquent-orm-models.md)
