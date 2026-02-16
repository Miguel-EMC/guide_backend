# 25 - Caching Strategies

Caching reduces load, lowers latency, and improves scalability. Laravel provides a clean API for multiple cache backends.

## Goals

- Choose the right cache store
- Use caching patterns safely
- Prevent cache stampedes

## 1. Cache Stores

Configured in `config/cache.php`:

- `redis` (recommended for production)
- `memcached`
- `database`
- `file`
- `array` (tests only)

Set the default in `.env`:

```env
CACHE_DRIVER=redis
```

## 2. Basic Cache API

```php
use Illuminate\Support\Facades\Cache;
use App\Models\User;

Cache::put('user:1', $user, 600);
$user = Cache::get('user:1');
Cache::forget('user:1');
```

## 3. Cache‑Aside (Most Common)

```php
$user = Cache::remember("user:{$id}", 600, function () use ($id) {
    return User::findOrFail($id);
});
```

## 4. Flexible Cache Expiration

Use a longer TTL with early refresh to avoid stampedes:

```php
$user = Cache::flexible("user:{$id}", [60, 600], function () use ($id) {
    return User::findOrFail($id);
});
```

## 5. Cache Tags

```php
Cache::tags(['user:1', 'profile'])->put('user:1:profile', $profile, 600);
Cache::tags('user:1')->flush();
```

Tags require Redis or Memcached.

## 6. Locks to Prevent Stampedes

```php
Cache::lock("lock:user:{$id}", 10)->block(5, function () use ($id) {
    $user = User::findOrFail($id);
    Cache::put("user:{$id}", $user, 600);
});
```

## 7. Versioned Keys

```php
use App\Models\Post;

$key = "posts:v2:{$page}";
$posts = Cache::remember($key, 300, fn () => Post::latest()->paginate());
```

## 8. Multiple Stores

```php
$redisCache = Cache::store('redis');
$fileCache = Cache::store('file');
```

## Tips

- Cache only hot or expensive reads.
- Always set TTLs.
- Invalidate intentionally with events or versioned keys.

---

[Previous: Microservices Communication](./24-microservices-communication.md) | [Back to Index](./README.md) | [Next: Full‑Text Search with Scout ->](./26-full-text-search-scout.md)
