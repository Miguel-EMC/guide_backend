# 06 - Routing

Routing maps HTTP requests to controllers. Laravel's routing system is fast and expressive.

## Basic Routes

```php
use Illuminate\Support\Facades\Route;

Route::get('/health', function () {
    return response()->json(['status' => 'ok']);
});
```

## Route Parameters

```php
Route::get('/users/{id}', function (string $id) {
    return response()->json(['id' => $id]);
});
```

## Route Constraints

```php
Route::get('/users/{id}', function (string $id) {
    return response()->json(['id' => $id]);
})->whereNumber('id');
```

## Named Routes

```php
Route::get('/users/{id}', [UserController::class, 'show'])
    ->name('users.show');
```

## Route Groups

```php
Route::prefix('v1')->group(function () {
    Route::get('/users', [UserController::class, 'index']);
    Route::get('/users/{user}', [UserController::class, 'show']);
});
```

## Route Model Binding

```php
Route::get('/users/{user}', [UserController::class, 'show']);

// In controller
public function show(User $user)
{
    return $user;
}
```

## Resource Routes

```php
Route::apiResource('posts', PostController::class);
```

## Fallback Routes

```php
Route::fallback(function () {
    return response()->json(['message' => 'Not found'], 404);
});
```

## Tips

- Use `apiResource` for CRUD APIs.
- Keep versioned routes isolated.
- Prefer route model binding for clean controllers.

---

[Previous: Project Structure](./05-project-structure.md) | [Back to Index](./README.md) | [Next: Controllers ->](./07-controllers.md)
