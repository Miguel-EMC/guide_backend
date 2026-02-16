# 18 - Events and Listeners

Events decouple your domain logic by allowing parts of your app to react to what happened without tight coupling.

## Goals

- Create events and listeners
- Queue listeners for async work
- Avoid side effects inside controllers

## 1. Create an Event

```bash
php artisan make:event OrderPlaced
```

```php
namespace App\Events;

use App\Models\Order;

class OrderPlaced
{
    public function __construct(public Order $order) {}
}
```

## 2. Create a Listener

```bash
php artisan make:listener SendOrderReceipt --event=OrderPlaced
```

```php
namespace App\Listeners;

use App\Events\OrderPlaced;
use App\Mail\ReceiptMail;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Support\Facades\Mail;

class SendOrderReceipt implements ShouldQueue
{
    public function handle(OrderPlaced $event): void
    {
        Mail::to($event->order->user->email)
            ->send(new ReceiptMail($event->order));
    }
}
```

## 3. Register the Listener

```php
// app/Providers/EventServiceProvider.php
namespace App\Providers;

use App\Events\OrderPlaced;
use App\Listeners\SendOrderReceipt;
use Illuminate\Foundation\Support\Providers\EventServiceProvider as ServiceProvider;

class EventServiceProvider extends ServiceProvider
{
    protected $listen = [
        OrderPlaced::class => [
            SendOrderReceipt::class,
        ],
    ];
}
```

## 4. Dispatch the Event

```php
use App\Events\OrderPlaced;

OrderPlaced::dispatch($order);
```

Use events after database commits when needed:

```php
use App\Events\OrderPlaced;

OrderPlaced::dispatch($order)->afterCommit();
```

## 5. Event Discovery (Optional)

Laravel can auto‑discover events and listeners based on your folder structure. Enable it in the event provider:

```php
public function shouldDiscoverEvents(): bool
{
    return true;
}
```

## 6. Model Events and Observers

```php
php artisan make:observer OrderObserver --model=Order
```

```php
namespace App\Observers;

use App\Events\OrderPlaced;
use App\Models\Order;

class OrderObserver
{
    public function created(Order $order): void
    {
        OrderPlaced::dispatch($order);
    }
}
```

Register in a service provider:

```php
namespace App\Providers;

use App\Models\Order;
use App\Observers\OrderObserver;
use Illuminate\Support\ServiceProvider;

class AppServiceProvider extends ServiceProvider
{
    public function boot(): void
    {
        Order::observe(OrderObserver::class);
    }
}
```

## 7. Testing Events

```php
use App\Events\OrderPlaced;
use Illuminate\Support\Facades\Event;

Event::fake();

OrderPlaced::dispatch($order);

Event::assertDispatched(OrderPlaced::class);
```

## Tips

- Keep events small and serializable.
- Queue listeners for slow tasks.
- Use `afterCommit()` for consistency when the database writes matter.

---

[Previous: Queues and Jobs](./17-queues-and-jobs.md) | [Back to Index](./README.md) | [Next: WebSockets with Reverb ->](./19-websockets-laravel-reverb.md)
