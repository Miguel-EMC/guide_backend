# 09 - Database Migrations and Seeding

Migrations let you version your database schema. Seeders and factories help with test data.

## Create a Migration

```bash
php artisan make:migration create_posts_table
```

```php
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class {
    public function up(): void
    {
        Schema::create('posts', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->string('title');
            $table->text('content');
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('posts');
    }
};
```

## Run Migrations

```bash
php artisan migrate
```

## Create a Seeder

```bash
php artisan make:seeder DatabaseSeeder
```

```php
use App\Models\User;
use App\Models\Post;

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

## Factories

```php
use Illuminate\Database\Eloquent\Factories\Factory;

class PostFactory extends Factory
{
    public function definition(): array
    {
        return [
            'title' => $this->faker->sentence,
            'content' => $this->faker->paragraph,
        ];
    }
}
```

## Seed the Database

```bash
php artisan db:seed
```

## Tips

- Use factories for realistic data.
- Keep migrations small and focused.
- Avoid destructive changes in production without backups.

---

[Previous: Eloquent ORM and Models](./08-eloquent-orm-models.md) | [Back to Index](./README.md) | [Next: Requests and Validation ->](./10-requests-and-validation.md)
