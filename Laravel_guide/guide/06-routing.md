# 06 - Routing

Routing maps HTTP requests to controllers. Laravel routing is fast, expressive, and supports everything from simple endpoints to complex APIs.

## Goals

- Define clean API routes
- Use route model binding correctly
- Organize large route files with groups

## 1. Basic Routes

```php
// routes/api.php
use App\Http\Controllers\UserController;
use Illuminate\Support\Facades\Route;

Route::get('/health', fn () => response()->json(['status' => 'ok']));
Route::post('/users', [UserController::class, 'store']);
Route::put('/users/{user}', [UserController::class, 'update']);
Route::delete('/users/{user}', [UserController::class, 'destroy']);
```

## 2. Route Parameters and Constraints

```php
use App\Http\Controllers\OrderController;
use App\Http\Controllers\UserController;
use Illuminate\Support\Facades\Route;

Route::get('/orders/{uuid}', [OrderController::class, 'show'])
    ->whereUuid('uuid');

Route::get('/users/{id}', [UserController::class, 'show'])
    ->whereNumber('id');
```

Define global patterns in a provider:

```php
use Illuminate\Support\Facades\Route;

public function boot(): void
{
    Route::pattern('id', '[0-9]+');
}
```

## 3. Route Model Binding

### Implicit Binding

```php
use App\Http\Controllers\PostController;
use App\Http\Resources\PostResource;
use App\Models\Post;
use Illuminate\Support\Facades\Route;

Route::get('/posts/{post}', [PostController::class, 'show']);

public function show(Post $post): PostResource
{
    return new PostResource($post);
}
```

### Custom Keys

```php
// app/Models/Post.php
public function getRouteKeyName(): string
{
    return 'slug';
}
```

### Explicit Binding

```php
use App\Models\Tenant;
use Illuminate\Support\Facades\Route;

Route::bind('tenant', function (string $value) {
    return Tenant::where('slug', $value)->firstOrFail();
});
```

## 4. Resource Routes

```php
use App\Http\Controllers\CommentController;
use App\Http\Controllers\PostController;
use App\Http\Controllers\UserController;
use Illuminate\Support\Facades\Route;

Route::apiResource('posts', PostController::class);
Route::apiResource('users', UserController::class)->only(['index', 'show']);
```

Nested resources:

```php
Route::apiResource('posts.comments', CommentController::class)->shallow();
```

## 5. Route Groups

```php
use App\Http\Controllers\PostController;
use App\Http\Controllers\UserController;
use Illuminate\Support\Facades\Route;

Route::prefix('v1')
    ->middleware(['auth:sanctum', 'throttle:api'])
    ->name('api.v1.')
    ->group(function () {
        Route::apiResource('posts', PostController::class);
        Route::apiResource('users', UserController::class);
    });
```

## 6. Named Routes

```php
use App\Http\Controllers\UserController;
use Illuminate\Support\Facades\Route;

Route::get('/users/{user}', [UserController::class, 'show'])
    ->name('users.show');

$url = route('users.show', ['user' => $user->id]);
```

## 7. Middleware per Route

```php
use App\Http\Controllers\AdminController;
use Illuminate\Support\Facades\Route;

Route::get('/admin', [AdminController::class, 'index'])
    ->middleware(['auth:sanctum', 'can:admin']);
```

## 8. Fallback Routes

```php
use Illuminate\Support\Facades\Route;

Route::fallback(function () {
    return response()->json(['message' => 'Not Found'], 404);
});
```

## 9. Signed Routes

```php
use Illuminate\Support\Facades\URL;

$url = URL::temporarySignedRoute('email.verify', now()->addMinutes(30), [
    'id' => $user->id,
]);
```

## 10. Route Caching

```bash
php artisan route:cache
php artisan route:clear
```

Do not use closures in routes if you want to cache them.

## 11. Versioning Strategies

- URI prefix: `/v1/posts`
- Header versioning: `X-API-Version: 1`
- Media type versioning: `application/vnd.api+json;version=1`

## Tips

- Keep routes small and descriptive.
- Prefer route model binding for consistency.
- Group routes by version and middleware.

---

[Previous: Project Structure](./05-project-structure.md) | [Back to Index](./README.md) | [Next: Controllers ->](./07-controllers.md)
