# 22 - Microservices Introduction

Microservices split a system into independent services that can be deployed and scaled separately. This architecture provides flexibility but introduces operational complexity that requires mature tooling and practices.

## Goals

- Understand when microservices make sense
- Design service boundaries correctly
- Plan a safe migration path from monolith

## 1. When to Use Microservices

### Good Fits

| Scenario | Why Microservices Help |
| --- | --- |
| Multiple teams working independently | Teams can deploy without coordinating |
| Different scaling needs per domain | Scale checkout separately from catalog |
| Clear domain boundaries | Natural service separation |
| Polyglot requirements | Use best language/framework per service |
| Regulatory isolation | Keep PCI-compliant code separate |

### Avoid Microservices When

| Scenario | Why to Avoid |
| --- | --- |
| Early-stage product | Requirements change too fast |
| Small team (< 5 developers) | Coordination overhead exceeds benefits |
| Unstable domain model | Wrong boundaries cause constant refactoring |
| Limited DevOps maturity | Complex deployments without automation |
| Shared data across features | Distributed transactions add complexity |

### Decision Framework

```php
// Questions to ask before adopting microservices

class MicroservicesReadinessCheck
{
    public function evaluate(): array
    {
        return [
            'team_size' => $this->hasMultipleIndependentTeams(),
            'domain_stability' => $this->hasClearDomainBoundaries(),
            'deployment_automation' => $this->hasCI_CDPipelines(),
            'observability' => $this->hasLoggingTracingMetrics(),
            'scaling_needs' => $this->hasDifferentScalingRequirements(),
        ];
    }

    // If < 3 are true, consider staying with a modular monolith
}
```

## 2. Benefits

### Independent Deployment

```yaml
# Each service has its own deployment pipeline
# services/orders/deploy.yml
name: Deploy Orders Service

on:
  push:
    paths:
      - 'services/orders/**'
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: ./deploy.sh orders
```

### Isolated Failures

```php
// Circuit breaker prevents cascade failures
use App\Services\CircuitBreaker;

class OrderService
{
    public function __construct(
        private CircuitBreaker $inventoryBreaker,
        private CircuitBreaker $paymentBreaker,
    ) {}

    public function createOrder(array $data): Order
    {
        // If inventory service is down, order creation fails gracefully
        $inventory = $this->inventoryBreaker->call(
            fn () => $this->checkInventory($data['items']),
            fallback: fn () => $this->getCachedInventory($data['items'])
        );

        // Continue with payment...
    }
}
```

### Technology Choice Per Service

```plaintext
services/
├── orders/          # Laravel (PHP) - Complex business logic
├── notifications/   # Node.js - Real-time, event-driven
├── analytics/       # Python - Data processing, ML
├── search/          # Go - High-performance queries
└── gateway/         # Laravel/Nginx - API aggregation
```

### Independent Scaling

```yaml
# Kubernetes HPA per service
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: checkout-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: checkout-service
  minReplicas: 5    # Checkout needs more capacity
  maxReplicas: 50
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 60
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: catalog-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: catalog-service
  minReplicas: 2    # Catalog is mostly reads, needs less
  maxReplicas: 10
```

## 3. Costs and Challenges

### Distributed System Complexity

```php
// What was simple becomes complex

// Monolith: Simple function call
$user = User::find($id);
$orders = $user->orders;

// Microservices: Network call with failure handling
class UserService
{
    public function getUserWithOrders(int $userId): array
    {
        $user = $this->userClient->get("/users/{$userId}");

        if (!$user) {
            throw new ServiceUnavailableException('User service unreachable');
        }

        try {
            $orders = $this->orderClient->get("/users/{$userId}/orders");
        } catch (TimeoutException $e) {
            Log::warning("Orders service timeout for user {$userId}");
            $orders = []; // Graceful degradation
        }

        return [
            'user' => $user,
            'orders' => $orders,
        ];
    }
}
```

### Debugging Challenges

```php
// Distributed tracing is essential
class TracingMiddleware
{
    public function handle(Request $request, Closure $next)
    {
        $traceId = $request->header('X-Trace-ID') ?? Str::uuid()->toString();
        $spanId = Str::uuid()->toString();

        // Propagate trace context
        Context::set('trace_id', $traceId);
        Context::set('span_id', $spanId);
        Context::set('parent_span_id', $request->header('X-Span-ID'));

        Log::withContext([
            'trace_id' => $traceId,
            'span_id' => $spanId,
            'service' => config('app.service_name'),
        ]);

        $response = $next($request);

        $response->headers->set('X-Trace-ID', $traceId);

        return $response;
    }
}
```

### Network Latency

```php
// Monolith: ~0ms internal call
// Microservices: 1-10ms per service call

// Problem: API requires data from 5 services = 50ms+ latency
// Solution: Parallel requests

use Illuminate\Http\Client\Pool;
use Illuminate\Support\Facades\Http;

class ProductAggregator
{
    public function getProductDetails(int $productId): array
    {
        $responses = Http::pool(fn (Pool $pool) => [
            $pool->as('product')->get("http://catalog/products/{$productId}"),
            $pool->as('inventory')->get("http://inventory/products/{$productId}"),
            $pool->as('pricing')->get("http://pricing/products/{$productId}"),
            $pool->as('reviews')->get("http://reviews/products/{$productId}"),
        ]);

        return [
            'product' => $responses['product']->json(),
            'inventory' => $responses['inventory']->json(),
            'pricing' => $responses['pricing']->json(),
            'reviews' => $responses['reviews']->json(),
        ];
    }
}
```

### Data Consistency

```php
// ACID transactions don't work across services
// Use eventual consistency with sagas

class CreateOrderSaga
{
    private array $completedSteps = [];

    public function execute(array $orderData): void
    {
        try {
            // Step 1: Reserve inventory
            $this->reserveInventory($orderData['items']);
            $this->completedSteps[] = 'inventory';

            // Step 2: Process payment
            $this->processPayment($orderData['payment']);
            $this->completedSteps[] = 'payment';

            // Step 3: Create order
            $this->createOrder($orderData);
            $this->completedSteps[] = 'order';

            // Step 4: Send notifications
            $this->sendNotifications($orderData);
        } catch (Exception $e) {
            $this->compensate();
            throw $e;
        }
    }

    private function compensate(): void
    {
        foreach (array_reverse($this->completedSteps) as $step) {
            match ($step) {
                'payment' => $this->refundPayment(),
                'inventory' => $this->releaseInventory(),
                'order' => $this->cancelOrder(),
                default => null,
            };
        }
    }
}
```

## 4. Modular Monolith First

A modular monolith provides clear internal boundaries without distributed system complexity.

### Module Structure

```plaintext
app/
├── Modules/
│   ├── User/
│   │   ├── Actions/
│   │   ├── Models/
│   │   ├── Events/
│   │   ├── Http/
│   │   │   ├── Controllers/
│   │   │   └── Requests/
│   │   ├── Contracts/
│   │   │   └── UserRepositoryInterface.php
│   │   ├── Repositories/
│   │   └── UserServiceProvider.php
│   │
│   ├── Order/
│   │   ├── Actions/
│   │   ├── Models/
│   │   ├── Events/
│   │   ├── Http/
│   │   ├── Contracts/
│   │   ├── Repositories/
│   │   └── OrderServiceProvider.php
│   │
│   └── Inventory/
│       ├── Actions/
│       ├── Models/
│       ├── Events/
│       ├── Listeners/
│       └── InventoryServiceProvider.php
```

### Module Contracts

```php
// app/Modules/User/Contracts/UserServiceInterface.php
namespace App\Modules\User\Contracts;

interface UserServiceInterface
{
    public function find(int $id): ?UserDTO;
    public function create(array $data): UserDTO;
    public function updateEmail(int $id, string $email): void;
}

// app/Modules/User/Services/UserService.php
namespace App\Modules\User\Services;

use App\Modules\User\Contracts\UserServiceInterface;
use App\Modules\User\Models\User;

class UserService implements UserServiceInterface
{
    public function find(int $id): ?UserDTO
    {
        $user = User::find($id);
        return $user ? UserDTO::fromModel($user) : null;
    }
}
```

### Cross-Module Communication

```php
// Modules communicate through events, not direct calls

// app/Modules/Order/Actions/CreateOrderAction.php
namespace App\Modules\Order\Actions;

use App\Modules\Order\Events\OrderCreated;

class CreateOrderAction
{
    public function execute(array $data): Order
    {
        $order = Order::create($data);

        // Other modules listen to this event
        OrderCreated::dispatch($order);

        return $order;
    }
}

// app/Modules/Inventory/Listeners/ReserveInventoryOnOrderCreated.php
namespace App\Modules\Inventory\Listeners;

use App\Modules\Order\Events\OrderCreated;

class ReserveInventoryOnOrderCreated
{
    public function handle(OrderCreated $event): void
    {
        foreach ($event->order->items as $item) {
            $this->inventoryService->reserve(
                $item->product_id,
                $item->quantity
            );
        }
    }
}
```

### Extracting to Microservices

When a module needs to become a service:

```php
// 1. Create HTTP client that implements the same interface
namespace App\Modules\User\Clients;

use App\Modules\User\Contracts\UserServiceInterface;
use Illuminate\Support\Facades\Http;

class UserServiceClient implements UserServiceInterface
{
    public function __construct(
        private string $baseUrl = 'http://user-service',
    ) {}

    public function find(int $id): ?UserDTO
    {
        $response = Http::timeout(5)
            ->retry(3, 100)
            ->get("{$this->baseUrl}/users/{$id}");

        if ($response->failed()) {
            return null;
        }

        return UserDTO::fromArray($response->json());
    }
}

// 2. Swap binding in service provider
// app/Modules/User/UserServiceProvider.php
public function register(): void
{
    if (config('services.user.external')) {
        $this->app->bind(
            UserServiceInterface::class,
            UserServiceClient::class
        );
    } else {
        $this->app->bind(
            UserServiceInterface::class,
            UserService::class
        );
    }
}
```

## 5. Service Boundaries

### Domain-Driven Design Boundaries

```php
// Bounded contexts define service boundaries

// User Context - Owns user identity and authentication
namespace UserService\Models;
class User { /* id, email, password_hash, profile */ }

// Order Context - Owns order lifecycle
namespace OrderService\Models;
class Order { /* id, user_id, status, items, total */ }
// user_id is a reference, not a foreign key

// Customer Context (for CRM) - Different view of user
namespace CustomerService\Models;
class Customer { /* id, external_user_id, lifetime_value, segment */ }
```

### Anti-Corruption Layer

```php
// Translate between service contexts
namespace App\Services\AntiCorruption;

class LegacyUserAdapter
{
    public function __construct(
        private LegacyUserClient $legacyClient,
    ) {}

    public function getUser(int $userId): UserDTO
    {
        $legacyUser = $this->legacyClient->fetchUser($userId);

        // Translate legacy format to our domain model
        return new UserDTO(
            id: $legacyUser['user_id'],
            email: $legacyUser['email_address'],
            name: $legacyUser['first_name'] . ' ' . $legacyUser['last_name'],
            status: $this->translateStatus($legacyUser['active']),
        );
    }

    private function translateStatus(int $active): string
    {
        return $active === 1 ? 'active' : 'inactive';
    }
}
```

## 6. Data Ownership

### Each Service Owns Its Data

```yaml
# Database per service
services:
  user-service:
    database: user_db
    tables: [users, profiles, sessions]

  order-service:
    database: order_db
    tables: [orders, order_items, order_history]

  inventory-service:
    database: inventory_db
    tables: [products, stock_levels, reservations]
```

### Data Synchronization Patterns

```php
// Event-driven data synchronization
namespace App\Listeners;

class SyncUserToOrderService
{
    public function handle(UserUpdated $event): void
    {
        // Publish event to message broker
        $this->publisher->publish('user.updated', [
            'user_id' => $event->user->id,
            'email' => $event->user->email,
            'name' => $event->user->name,
            'updated_at' => now()->toIso8601String(),
        ]);
    }
}

// Order service subscribes and maintains local copy
class UserEventHandler
{
    public function handleUserUpdated(array $payload): void
    {
        UserCache::updateOrCreate(
            ['external_id' => $payload['user_id']],
            [
                'email' => $payload['email'],
                'name' => $payload['name'],
                'synced_at' => now(),
            ]
        );
    }
}
```

### Avoiding Distributed Joins

```php
// Bad: Trying to join across services
// SELECT o.*, u.name FROM orders o JOIN users u ON o.user_id = u.id

// Good: Denormalize or aggregate at API level
class OrderWithUserDTO
{
    public static function fromOrderAndUser(Order $order, UserDTO $user): self
    {
        return new self(
            orderId: $order->id,
            orderTotal: $order->total,
            userName: $user->name,
            userEmail: $user->email,
        );
    }
}

// Or embed necessary user data in order events
class OrderCreated
{
    public function __construct(
        public Order $order,
        public string $userName,
        public string $userEmail,
    ) {}
}
```

## 7. Team Structure (Conway's Law)

Align teams with service boundaries:

```plaintext
Team Structure:
├── Platform Team
│   ├── Infrastructure (K8s, CI/CD)
│   ├── Observability (Logging, Metrics, Tracing)
│   └── Developer Experience
│
├── User Team
│   ├── User Service
│   ├── Authentication Service
│   └── Profile Service
│
├── Commerce Team
│   ├── Order Service
│   ├── Payment Service
│   └── Shipping Service
│
└── Catalog Team
    ├── Product Service
    ├── Inventory Service
    └── Search Service
```

### Team Autonomy

```yaml
# Each team owns their deployment pipeline
# teams/commerce/services.yml
services:
  - name: order-service
    owner: commerce-team
    on_call: commerce-oncall@company.com
    slo:
      availability: 99.9%
      latency_p99: 200ms

  - name: payment-service
    owner: commerce-team
    on_call: commerce-oncall@company.com
    pci_compliant: true
    slo:
      availability: 99.99%
      latency_p99: 500ms
```

## 8. Migration Strategy

### Strangler Fig Pattern

```php
// Gradually route traffic from monolith to new service

// routes/api.php
Route::middleware(['feature:new-user-service'])->group(function () {
    // New service handles these routes
    Route::get('/users/{id}', [NewUserController::class, 'show']);
});

Route::middleware(['feature:!new-user-service'])->group(function () {
    // Legacy monolith handles these routes
    Route::get('/users/{id}', [LegacyUserController::class, 'show']);
});

// Feature flag controls traffic percentage
class FeatureMiddleware
{
    public function handle(Request $request, Closure $next, string $feature)
    {
        $invert = str_starts_with($feature, '!');
        $featureName = ltrim($feature, '!');

        $enabled = $this->isEnabled($featureName, $request);

        if ($invert) {
            $enabled = !$enabled;
        }

        if (!$enabled) {
            abort(404);
        }

        return $next($request);
    }

    private function isEnabled(string $feature, Request $request): bool
    {
        // Percentage rollout
        $percentage = config("features.{$feature}.percentage", 0);
        $userId = $request->user()?->id ?? 0;

        return ($userId % 100) < $percentage;
    }
}
```

### Parallel Run

```php
// Run both implementations and compare results
class ParallelUserService implements UserServiceInterface
{
    public function __construct(
        private UserService $legacy,
        private UserServiceClient $newService,
    ) {}

    public function find(int $id): ?UserDTO
    {
        $legacyResult = $this->legacy->find($id);

        // Async call to new service for comparison
        dispatch(function () use ($id, $legacyResult) {
            try {
                $newResult = $this->newService->find($id);

                if (!$this->resultsMatch($legacyResult, $newResult)) {
                    Log::warning('User service mismatch', [
                        'user_id' => $id,
                        'legacy' => $legacyResult,
                        'new' => $newResult,
                    ]);
                }
            } catch (Exception $e) {
                Log::error('New user service failed', [
                    'user_id' => $id,
                    'error' => $e->getMessage(),
                ]);
            }
        });

        // Always return legacy result during migration
        return $legacyResult;
    }
}
```

## 9. Observability Foundation

Build observability before microservices:

### Structured Logging

```php
// config/logging.php
'channels' => [
    'structured' => [
        'driver' => 'monolog',
        'handler' => StreamHandler::class,
        'formatter' => JsonFormatter::class,
        'with' => [
            'stream' => 'php://stdout',
        ],
        'tap' => [AddServiceContext::class],
    ],
],

// app/Logging/AddServiceContext.php
class AddServiceContext
{
    public function __invoke($logger): void
    {
        $logger->pushProcessor(function ($record) {
            $record['extra']['service'] = config('app.service_name');
            $record['extra']['environment'] = config('app.env');
            $record['extra']['trace_id'] = Context::get('trace_id');
            return $record;
        });
    }
}
```

### Metrics

```php
// app/Providers/MetricsServiceProvider.php
namespace App\Providers;

use Prometheus\CollectorRegistry;

class MetricsServiceProvider extends ServiceProvider
{
    public function boot(CollectorRegistry $registry): void
    {
        // Request duration histogram
        $histogram = $registry->getOrRegisterHistogram(
            'http',
            'request_duration_seconds',
            'HTTP request duration',
            ['method', 'route', 'status']
        );

        // Service call counter
        $counter = $registry->getOrRegisterCounter(
            'service',
            'calls_total',
            'Service calls',
            ['target_service', 'method', 'status']
        );
    }
}
```

### Distributed Tracing

```php
// Propagate trace context in all service calls
class TracedHttpClient
{
    public function get(string $url, array $options = []): Response
    {
        return Http::withHeaders([
            'X-Trace-ID' => Context::get('trace_id'),
            'X-Span-ID' => Str::uuid()->toString(),
            'X-Parent-Span-ID' => Context::get('span_id'),
        ])->get($url, $options);
    }
}
```

## 10. Checklist Before Microservices

```markdown
## Pre-Microservices Checklist

### Prerequisites
- [ ] CI/CD pipeline for all deployments
- [ ] Centralized logging with search
- [ ] Metrics collection and dashboards
- [ ] Distributed tracing setup
- [ ] Alerting and on-call rotation

### Domain Understanding
- [ ] Clear bounded contexts identified
- [ ] Data ownership defined per context
- [ ] API contracts documented
- [ ] Event schemas versioned

### Team Readiness
- [ ] Teams aligned with service boundaries
- [ ] Each team has deployment autonomy
- [ ] On-call responsibilities assigned
- [ ] Incident response procedures documented

### Infrastructure
- [ ] Container orchestration (Kubernetes)
- [ ] Service mesh or API gateway
- [ ] Secrets management
- [ ] Database per service strategy

### First Service Candidates
- [ ] Identify lowest-risk extraction target
- [ ] Clear API boundary exists
- [ ] Minimal cross-service transactions
- [ ] Team ready to own it end-to-end
```

## Tips

- Build observability first (logs, metrics, tracing).
- Start with a modular monolith and extract services only when needed.
- Keep contracts versioned and documented.
- Avoid distributed transactions; embrace eventual consistency.
- Align team structure with service boundaries.
- Extract services one at a time, validating each migration.

---

[Previous: Deployment](./21-deployment.md) | [Back to Index](./README.md) | [Next: Lumen and Octane ->](./23-lumen-and-octane.md)
