# 13 - API Resources (Data Transformation)

API Resources transform models into consistent JSON responses. They prevent accidental data leaks and keep your API schema stable over time.

## Goals

- Control your public JSON shape
- Include relationships safely
- Add metadata and pagination

## 1. Create a Resource

```bash
php artisan make:resource PostResource
```

```php
// app/Http/Resources/PostResource.php
namespace App\Http\Resources;

use App\Http\Resources\UserResource;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

class PostResource extends JsonResource
{
    public function toArray(Request $request): array
    {
        return [
            'id' => $this->id,
            'title' => $this->title,
            'summary' => $this->summary,
            'author' => new UserResource($this->whenLoaded('author')),
            'created_at' => $this->created_at?->toISOString(),
            'updated_at' => $this->updated_at?->toISOString(),
        ];
    }
}
```

## 2. Use a Resource in Controllers

```php
use App\Http\Resources\PostResource;
use App\Models\Post;

public function show(Post $post): PostResource
{
    $post->load('author');
    return new PostResource($post);
}
```

## 3. Collections and Pagination

```php
use App\Http\Resources\PostResource;
use App\Models\Post;
use Illuminate\Http\Resources\Json\AnonymousResourceCollection;

public function index(): AnonymousResourceCollection
{
    $posts = Post::query()->with('author')->paginate(15);
    return PostResource::collection($posts);
}
```

Laravel automatically adds pagination `links` and `meta` when the resource wraps a paginator.

## 4. Conditional Fields

```php
return [
    'id' => $this->id,
    'title' => $this->title,
    $this->mergeWhen($request->user()?->isAdmin(), [
        'internal_notes' => $this->internal_notes,
    ]),
];
```

## 5. Custom Meta Data

```php
return (PostResource::collection($posts))
    ->additional([
        'meta' => [
            'api_version' => '1.0',
            'request_id' => $request->header('x-request-id'),
        ],
    ]);
```

## 6. Wrapping and Response Customization

Disable the default `data` wrapper if you need a flat response:

```php
use Illuminate\Http\Resources\Json\JsonResource;

JsonResource::withoutWrapping();
```

Add headers or status codes:

```php
public function withResponse(Request $request, $response): void
{
    $response->header('X-Resource-Version', '1');
}
```

## 7. Dedicated Resource Collections

```bash
php artisan make:resource PostCollection
```

```php
namespace App\Http\Resources;

use Illuminate\Http\Resources\Json\ResourceCollection;

class PostCollection extends ResourceCollection
{
    public function toArray($request): array
    {
        return [
            'data' => $this->collection,
            'meta' => [
                'total' => $this->collection->count(),
            ],
        ];
    }
}
```

## Tips

- Always eager‑load relationships used by resources.
- Keep resource names stable to avoid breaking clients.
- Use resources to enforce API versioning boundaries.

---

[Previous: Authentication with Sanctum](./12-authentication-sanctum.md) | [Back to Index](./README.md) | [Next: Service Container and Providers ->](./14-service-container-and-providers.md)
