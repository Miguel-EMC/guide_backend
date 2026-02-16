# 19 - WebSockets with Laravel Reverb

Laravel Reverb is the first‑party WebSocket server for broadcasting events in real time. It uses the Pusher protocol, so it integrates cleanly with Laravel Echo.

## Goals

- Install Reverb and broadcasting
- Broadcast events from Laravel
- Connect a real‑time client

## 1. Install Broadcasting + Reverb

```bash
php artisan install:broadcasting --reverb
```

This command configures broadcasting, publishes config, and sets Reverb as the driver.

Start the server:

```bash
php artisan reverb:start
```

## 2. Configure Environment

```env
BROADCAST_CONNECTION=reverb
REVERB_APP_ID=app-id
REVERB_APP_KEY=app-key
REVERB_APP_SECRET=app-secret
REVERB_HOST=localhost
REVERB_PORT=8080
REVERB_SCHEME=http
```

## 3. Define Channels

```php
// routes/channels.php
use App\Models\Order;
use Illuminate\Support\Facades\Broadcast;

Broadcast::channel('orders.{orderId}', function ($user, $orderId) {
    return (int) $user->id === Order::findOrFail($orderId)->user_id;
});
```

## 4. Broadcast an Event

```bash
php artisan make:event OrderStatusUpdated
```

```php
namespace App\Events;

use App\Models\Order;
use Illuminate\Broadcasting\Channel;
use Illuminate\Contracts\Broadcasting\ShouldBroadcast;

class OrderStatusUpdated implements ShouldBroadcast
{
    public function __construct(public Order $order) {}

    public function broadcastOn(): Channel
    {
        return new Channel('orders.' . $this->order->id);
    }

    public function broadcastAs(): string
    {
        return 'order.status.updated';
    }

    public function broadcastWith(): array
    {
        return [
            'id' => $this->order->id,
            'status' => $this->order->status,
        ];
    }
}
```

Dispatch the event as usual:

```php
use App\Events\OrderStatusUpdated;

OrderStatusUpdated::dispatch($order);
```

Broadcast events are queued. Ensure a queue worker is running.

## 5. Front‑End Client (Laravel Echo)

```bash
npm install --save-dev laravel-echo pusher-js
```

```js
import Echo from 'laravel-echo';
import Pusher from 'pusher-js';

window.Pusher = Pusher;

window.Echo = new Echo({
  broadcaster: 'reverb',
  key: import.meta.env.VITE_REVERB_APP_KEY,
  wsHost: import.meta.env.VITE_REVERB_HOST,
  wsPort: import.meta.env.VITE_REVERB_PORT ?? 8080,
  wssPort: import.meta.env.VITE_REVERB_PORT ?? 8080,
  forceTLS: false,
  enabledTransports: ['ws', 'wss'],
});
```

Listen to events:

```js
window.Echo.channel(`orders.${orderId}`)
  .listen('.order.status.updated', (e) => {
    console.log('Order updated', e);
  });
```

## 6. Production Notes

- Run Reverb behind a load balancer with sticky sessions.
- Use Redis for scaling and pub/sub coordination.
- Monitor memory and connection counts.

## Tips

- Keep payloads small and structured.
- Use private or presence channels for user data.
- Add auth guards for channel access.

---

[Previous: Events and Listeners](./18-events-and-listeners.md) | [Back to Index](./README.md) | [Next: Package Development ->](./20-package-development.md)
