# 09 - Database Migrations and Seeding

Migrations version your schema, while factories and seeders generate data for development and testing.

## Goals

- Write safe, reversible migrations
- Build realistic seed data
- Apply production-friendly schema changes

## 1. Create a Migration

```bash
php artisan make:migration create_posts_table
```

```php
use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration {
    public function up(): void
    {
        Schema::create('posts', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->string('title');
            $table->text('content');
            $table->boolean('is_published')->default(false);
            $table->timestamps();
            $table->softDeletes();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('posts');
    }
};
```

## 2. Common Column Types

```php
$table->uuid('uuid');
$table->string('name', 120);
$table->json('settings')->nullable();
$table->decimal('price', 10, 2);
$table->timestamp('published_at')->nullable();
```

## 3. Indexes and Constraints

```php
$table->unique('email');
$table->index(['status', 'created_at']);
$table->foreignId('team_id')->constrained()->cascadeOnDelete();
```

## 4. Modifying Tables

```bash
php artisan make:migration add_status_to_orders_table
```

```php
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

Schema::table('orders', function (Blueprint $table) {
    $table->string('status')->default('pending')->after('total');
});
```

## 5. Running Migrations

```bash
php artisan migrate
php artisan migrate:rollback
php artisan migrate:fresh --seed
```

## 6. Zero-Downtime Pattern

For large tables, add nullable columns first, backfill in batches, then make them required.

Step 1: add nullable column

```php
$table->string('external_id')->nullable();
```

Step 2: backfill in a job

Step 3: make non-nullable in a follow-up migration

## 7. Seeders

```bash
php artisan make:seeder DatabaseSeeder
```

```php
use App\Models\Post;
use App\Models\User;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        User::factory()
            ->count(10)
            ->has(Post::factory()->count(5))
            ->create();
    }
}
```

Run seeders:

```bash
php artisan db:seed
```

## 8. Factories and States

```php
use Illuminate\Database\Eloquent\Factories\Factory;

class PostFactory extends Factory
{
    public function definition(): array
    {
        return [
            'title' => $this->faker->sentence,
            'content' => $this->faker->paragraph,
            'is_published' => false,
        ];
    }

    public function published(): static
    {
        return $this->state(fn () => ['is_published' => true]);
    }
}
```

```php
use App\Models\Post;

Post::factory()->published()->count(3)->create();
```

## 9. Seeding for Tests

Feature tests often use factories directly:

```php
use App\Models\User;

$user = User::factory()->create();
$this->actingAs($user);
```

## Tips

- Keep migrations small and reversible.
- Avoid destructive changes without backups.
- Use factories to create realistic datasets.

---

[Previous: Eloquent ORM and Models](./08-eloquent-orm-models.md) | [Back to Index](./README.md) | [Next: Requests and Validation ->](./10-requests-and-validation.md)
