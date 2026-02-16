# 17 - Queues and Jobs

Queues let you run slow or heavy work outside the request cycle: emails, webhooks, report generation, media processing, and more.

## Goals

- Configure a queue driver
- Create robust jobs with retries
- Monitor and handle failures

## 1. Configure the Queue Driver

Set your connection in `.env`:

```env
QUEUE_CONNECTION=redis
```

Common drivers: `sync`, `database`, `redis`, `sqs`.

## 2. Create a Job

```bash
php artisan make:job SendWelcomeEmail
```

```php
namespace App\Jobs;

use App\Mail\WelcomeMail;
use App\Models\User;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Queue\SerializesModels;
use Illuminate\Support\Facades\Mail;

class SendWelcomeEmail implements ShouldQueue
{
    use Dispatchable, InteractsWithQueue, Queueable, SerializesModels;

    public function __construct(public int $userId) {}

    public function handle(): void
    {
        $user = User::findOrFail($this->userId);
        Mail::to($user->email)->send(new WelcomeMail($user));
    }
}
```

## 3. Dispatch Jobs

```php
use App\Jobs\SendWelcomeEmail;

SendWelcomeEmail::dispatch($user->id)->onQueue('emails');
```

## 4. Queue Workers

```bash
php artisan queue:work --queue=emails,default --tries=3
```

Use Supervisor or systemd to keep workers running in production.

## 5. Retries, Backoff, and Timeouts

```php
public int $tries = 5;
public int $timeout = 120;

public function backoff(): array
{
    return [10, 60, 300];
}
```

## 6. Failed Jobs

```bash
php artisan queue:failed-table
php artisan migrate
```

Inspect and retry:

```bash
php artisan queue:failed
php artisan queue:retry all
```

## 7. Job Batching

```php
use App\Jobs\ProcessCsvRow;
use Illuminate\Support\Facades\Bus;
use Throwable;

Bus::batch([
    new ProcessCsvRow($row1),
    new ProcessCsvRow($row2),
])->then(function () {
    // All jobs completed
})->catch(function (Throwable $e) {
    // First failure
})->dispatch();
```

## 8. Job Middleware

Throttle, prevent overlap, or limit exceptions:

```php
use Illuminate\Queue\Middleware\RateLimited;
use Illuminate\Queue\Middleware\WithoutOverlapping;

public function middleware(): array
{
    return [
        new WithoutOverlapping($this->userId),
        new RateLimited('email-sends'),
    ];
}
```

## 9. Unique Jobs

```php
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Contracts\Queue\ShouldBeUnique;

class ImportFeed implements ShouldQueue, ShouldBeUnique
{
    public int $uniqueFor = 3600;
}
```

## 10. Horizon (Optional)

For Redis queues, Laravel Horizon provides dashboards, metrics, and worker management.

## Tips

- Keep jobs idempotent.
- Store job payloads small.
- Use queues for every external side effect.

---

[Previous: Testing with Pest and PHPUnit](./16-testing-pest-phpunit.md) | [Back to Index](./README.md) | [Next: Events and Listeners ->](./18-events-and-listeners.md)
