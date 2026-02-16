# 16 - Testing with Pest and PHPUnit

Laravel ships with PHPUnit and a powerful testing layer. Pest is a popular alternative with a cleaner syntax. You can use either, or mix both.

## Goals

- Write fast unit tests
- Build reliable feature tests for APIs
- Fake external services and time

## 1. Running Tests

```bash
php artisan test
```

## 2. Installing Pest (Optional)

```bash
composer require pestphp/pest --dev
./vendor/bin/pest --init
```

## 3. Database Testing

Use `RefreshDatabase` to migrate and wrap each test in a transaction.

```php
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class PostTest extends TestCase
{
    use RefreshDatabase;

    public function test_posts_can_be_created(): void
    {
        $response = $this->postJson('/api/posts', [
            'title' => 'New Post',
            'content' => 'Hello world',
        ]);

        $response->assertCreated();
        $this->assertDatabaseHas('posts', ['title' => 'New Post']);
    }
}
```

## 4. Feature Test Example

```php
use App\Models\Post;

public function test_user_can_list_posts(): void
{
    Post::factory()->count(3)->create();

    $response = $this->getJson('/api/posts');

    $response
        ->assertOk()
        ->assertJsonStructure([
            'data' => [['id', 'title', 'created_at']],
        ]);
}
```

## 5. Authentication in Tests

```php
use App\Models\User;
use Laravel\Sanctum\Sanctum;

public function test_protected_route_requires_auth(): void
{
    Sanctum::actingAs(User::factory()->create(), ['posts:read']);

    $this->getJson('/api/posts')
        ->assertOk();
}
```

## 6. Faking External Systems

```php
use App\Jobs\SendWelcomeEmail;
use App\Events\UserRegistered;
use Illuminate\Support\Facades\Mail;
use Illuminate\Support\Facades\Queue;
use Illuminate\Support\Facades\Event;
use Illuminate\Support\Facades\Http;

Mail::fake();
Queue::fake();
Event::fake();
Http::fake([
    'api.example.com/*' => Http::response(['ok' => true], 200),
]);
```

Assert dispatches:

```php
Queue::assertPushed(SendWelcomeEmail::class);
Event::assertDispatched(UserRegistered::class);
```

## 7. Testing Jobs

```php
use App\Jobs\ProcessInvoice;
use Illuminate\Support\Facades\Bus;

Bus::fake();

ProcessInvoice::dispatch($invoiceId);

Bus::assertDispatched(ProcessInvoice::class, function ($job) use ($invoiceId) {
    return $job->invoiceId === $invoiceId;
});
```

## 8. Time Travel

```php
$this->travel(2)->hours();

// ... execute logic that depends on time

$this->travelBack();
```

## 9. Debugging Tips

- Use `->withoutExceptionHandling()` to surface errors.
- Use `dd()` only during local runs.
- Keep assertions specific and stable.

## Tips

- Prefer small tests with clear intent.
- Use factories for consistent fixtures.
- Run tests in CI with a clean database.

---

[Previous: Error Handling and Logging](./15-error-handling-and-logging.md) | [Back to Index](./README.md) | [Next: Queues and Jobs ->](./17-queues-and-jobs.md)
