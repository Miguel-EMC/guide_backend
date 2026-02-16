# 26 - Full‑Text Search with Laravel Scout

Laravel Scout adds full‑text search to Eloquent models with a clean, driver‑based API. It supports a built‑in database engine and external engines for more advanced search features.

## Goals

- Install and configure Scout
- Make models searchable
- Choose the right search engine

## 1. Install Scout

```bash
composer require laravel/scout
php artisan vendor:publish --provider="Laravel\\Scout\\ScoutServiceProvider"
```

This creates `config/scout.php`.

## 2. Choose a Driver

In `.env`:

```env
SCOUT_DRIVER=database
```

Common options:

- `database` (built‑in, no external service)
- `algolia`
- `meilisearch`
- `typesense`
- `collection` (local/dev only)

## 3. Make a Model Searchable

```php
namespace App\Models;

use Laravel\Scout\Searchable;
use Illuminate\Database\Eloquent\Model;

class Post extends Model
{
    use Searchable;

    public function toSearchableArray(): array
    {
        return [
            'id' => $this->id,
            'title' => $this->title,
            'body' => $this->body,
        ];
    }
}
```

## 4. Database Engine Tuning

For the database engine you can optimize search behavior using attributes:

```php
namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Laravel\Scout\Attributes\SearchUsingFullText;
use Laravel\Scout\Attributes\SearchUsingPrefix;
use Laravel\Scout\Searchable;

class Post extends Model
{
    use Searchable;

    #[SearchUsingPrefix(['id', 'title'])]
    #[SearchUsingFullText(['body'])]
    public function toSearchableArray(): array
    {
        return [
            'id' => $this->id,
            'title' => $this->title,
            'body' => $this->body,
        ];
    }
}
```

## 5. Searching

```php
use App\Models\Post;

$results = Post::search('laravel')->get();
$paginated = Post::search('laravel')->paginate(15);
```

## 6. Import and Syncing

Sync existing records:

```bash
php artisan scout:import "App\\Models\\Post"
```

Pause syncing for bulk updates:

```php
use App\Models\Post;

Post::withoutSyncingToSearch(function () {
    Post::query()->update(['synced' => true]);
});
```

## 7. Queueing Indexing

Enable queueing in `config/scout.php` for non‑database engines:

```php
'queue' => true,
```

Then run a worker:

```bash
php artisan queue:work redis --queue=scout
```

## Tips

- Use the database engine for simple search needs.
- Use Algolia/Meilisearch/Typesense for typo tolerance and facets.
- Keep searchable data small and intentional.

---

[Previous: Caching Strategies](./25-caching-strategies.md) | [Back to Index](./README.md) | [Next: Laravel 12 Features ->](./27-laravel-12-features.md)
