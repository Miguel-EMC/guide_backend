# 08 - Eloquent ORM and Models

Eloquent is Laravel's Active Record ORM. It provides a fluent API for querying and modeling data.

## Create a Model

```bash
php artisan make:model Post -m
```

## Basic Model

```php
namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Post extends Model
{
    protected $fillable = ['title', 'content', 'user_id'];
}
```

## Query Examples

```php
$posts = Post::query()->latest()->paginate(20);
$post = Post::query()->findOrFail($id);
```

## Mass Assignment

Use `$fillable` or `$guarded` to protect fields.

```php
Post::create([
    'title' => 'Hello',
    'content' => 'World',
    'user_id' => 1,
]);
```

## Relationships

```php
class User extends Model
{
    public function posts()
    {
        return $this->hasMany(Post::class);
    }
}

class Post extends Model
{
    public function user()
    {
        return $this->belongsTo(User::class);
    }
}
```

## Eager Loading

```php
$posts = Post::with('user')->paginate(20);
```

## Scopes

```php
class Post extends Model
{
    public function scopePublished($query)
    {
        return $query->whereNotNull('published_at');
    }
}

$posts = Post::published()->get();
```

## Casting

```php
protected $casts = [
    'published_at' => 'datetime',
    'meta' => 'array',
];
```

## Tips

- Prefer `findOrFail` for API controllers.
- Use eager loading to avoid N+1 queries.
- Keep model logic small and move business logic to services.

---

[Previous: Controllers](./07-controllers.md) | [Back to Index](./README.md) | [Next: Migrations and Seeding ->](./09-database-migrations-and-seeding.md)
