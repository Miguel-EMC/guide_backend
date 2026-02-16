# 07 - Controllers

Controllers organize request handling logic and keep routes clean. This chapter covers controller patterns, dependency injection, resource controllers, and advanced API practices.

## Goals

- Keep controllers thin and testable
- Use Form Requests and Resources consistently
- Apply authorization and transactions correctly

## 1. Controller Types

| Type | Use Case |
| --- | --- |
| Basic | Simple endpoints |
| Resource | CRUD operations |
| Invokable | Single action endpoints |
| API Resource | RESTful APIs without create/edit forms |

## 2. Creating Controllers

```bash
php artisan make:controller UserController
php artisan make:controller Api/PostController --api
php artisan make:controller HealthController --invokable
```

## 3. Thin Controller Pattern

Controllers should orchestrate requests, not implement business logic.

```php
// app/Http/Controllers/Api/PostController.php
namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Http\Requests\StorePostRequest;
use App\Http\Resources\PostResource;
use App\Services\PostService;
use Illuminate\Http\JsonResponse;

class PostController extends Controller
{
    public function __construct(private PostService $service) {}

    public function store(StorePostRequest $request): JsonResponse
    {
        $post = $this->service->create($request->validated(), $request->user());
        return response()->json(new PostResource($post), 201);
    }
}
```

```php
// app/Services/PostService.php
namespace App\Services;

use App\Events\PostCreated;
use App\Models\Post;
use App\Models\User;
use Illuminate\Support\Facades\DB;

class PostService
{
    public function create(array $data, User $user): Post
    {
        return DB::transaction(function () use ($data, $user) {
            $post = Post::create([...$data, 'user_id' => $user->id]);
            PostCreated::dispatch($post)->afterCommit();
            return $post;
        });
    }
}
```

## 4. Resource Controllers (REST APIs)

```php
// app/Http/Controllers/Api/PostController.php
namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Http\Requests\StorePostRequest;
use App\Http\Requests\UpdatePostRequest;
use App\Http\Resources\PostResource;
use App\Models\Post;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\AnonymousResourceCollection;

class PostController extends Controller
{
    public function index(Request $request): AnonymousResourceCollection
    {
        $posts = Post::query()
            ->with('author:id,name')
            ->latest()
            ->paginate(20);

        return PostResource::collection($posts);
    }

    public function show(Post $post): PostResource
    {
        return new PostResource($post->load(['author', 'comments']));
    }

    public function store(StorePostRequest $request): JsonResponse
    {
        $post = Post::create([...$request->validated(), 'user_id' => $request->user()->id]);
        return response()->json(new PostResource($post), 201);
    }

    public function update(UpdatePostRequest $request, Post $post): PostResource
    {
        $post->update($request->validated());
        return new PostResource($post->fresh());
    }

    public function destroy(Post $post): JsonResponse
    {
        $post->delete();
        return response()->json(null, 204);
    }
}
```

## 5. Form Requests in Controllers

Use Form Requests for validation and authorization:

```php
use App\Http\Requests\UpdatePostRequest;
use App\Http\Resources\PostResource;
use App\Models\Post;

public function update(UpdatePostRequest $request, Post $post): PostResource
{
    $this->authorize('update', $post);
    $post->update($request->validated());
    return new PostResource($post->fresh());
}
```

## 6. Authorization in Controllers

```php
use App\Models\Post;

class PostController extends Controller
{
    public function __construct()
    {
        $this->authorizeResource(Post::class, 'post');
    }
}
```

This auto-calls your policy for `index`, `show`, `store`, `update`, and `destroy`.

## 7. Pagination, Filtering, and Sorting

```php
use App\Http\Resources\PostResource;
use App\Models\Post;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\AnonymousResourceCollection;

public function index(Request $request): AnonymousResourceCollection
{
    $query = Post::query()->with('author:id,name');

    if ($request->filled('search')) {
        $query->where('title', 'like', '%' . $request->input('search') . '%');
    }

    if ($request->filled('author_id')) {
        $query->where('user_id', $request->input('author_id'));
    }

    $posts = $query->orderBy('created_at', 'desc')->paginate(20);
    return PostResource::collection($posts);
}
```

## 8. Invokable Controllers

```php
// app/Http/Controllers/HealthController.php
namespace App\Http\Controllers;

use Illuminate\Http\JsonResponse;

class HealthController extends Controller
{
    public function __invoke(): JsonResponse
    {
        return response()->json(['status' => 'ok', 'ts' => now()->toIso8601String()]);
    }
}
```

```php
use App\Http\Controllers\HealthController;
use Illuminate\Support\Facades\Route;

Route::get('/health', HealthController::class);
```

## 9. Consistent Response Shapes

Use API Resources or a response helper:

```php
use App\Http\Resources\PostResource;

return response()->json([
    'data' => new PostResource($post),
    'meta' => ['request_id' => request()->header('x-request-id')],
]);
```

## 10. Handling Errors and Transactions

```php
use App\Http\Requests\StorePostRequest;
use App\Http\Resources\PostResource;
use App\Models\Post;
use Illuminate\Http\JsonResponse;
use Illuminate\Support\Facades\DB;

public function store(StorePostRequest $request): JsonResponse
{
    $post = DB::transaction(function () use ($request) {
        $post = Post::create([...$request->validated(), 'user_id' => $request->user()->id]);
        return $post;
    });

    return response()->json(new PostResource($post), 201);
}
```

## Tips

- Keep controllers thin and push logic into services.
- Prefer Form Requests and API Resources.
- Always authorize state-changing actions.

---

[Previous: Routing](./06-routing.md) | [Back to Index](./README.md) | [Next: Eloquent ORM and Models ->](./08-eloquent-orm-models.md)
