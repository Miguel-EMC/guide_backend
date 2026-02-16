# 08 - Microservices Patterns

This chapter covers essential patterns for building resilient, scalable microservices architectures.

---

## Communication Patterns

### API Gateway

**Intent**: Provide a single entry point for all clients, routing requests to appropriate microservices.

```
┌─────────────────────────────────────────────────────────────────┐
│                          CLIENTS                                 │
│         Web App        Mobile App        Third-party             │
└─────────────────────────────────────┬───────────────────────────┘
                                      │
                    ┌─────────────────▼───────────────────┐
                    │           API GATEWAY               │
                    │  - Authentication                   │
                    │  - Rate Limiting                    │
                    │  - Request Routing                  │
                    │  - Response Aggregation             │
                    │  - SSL Termination                  │
                    └────┬────────────┬────────────┬──────┘
                         │            │            │
           ┌─────────────▼──┐  ┌──────▼─────┐  ┌───▼──────────┐
           │  User Service  │  │Order Service│  │Product Service│
           └────────────────┘  └────────────┘  └──────────────┘
```

```typescript
// api-gateway/routes.ts
class ApiGateway {
  private services: Map<string, ServiceConfig> = new Map();

  registerService(path: string, config: ServiceConfig): void {
    this.services.set(path, config);
  }

  async handleRequest(req: Request): Promise<Response> {
    // Authentication
    const user = await this.authenticate(req);

    // Rate limiting
    await this.checkRateLimit(user.id);

    // Route to service
    const service = this.findService(req.path);
    if (!service) {
      return new Response(404, { error: 'Not found' });
    }

    // Forward request
    const response = await this.forwardRequest(service, req);

    // Transform response if needed
    return this.transformResponse(response);
  }

  private async forwardRequest(service: ServiceConfig, req: Request): Promise<Response> {
    const instance = await this.loadBalancer.getHealthyInstance(service.name);
    return fetch(`${instance.url}${req.path}`, {
      method: req.method,
      headers: req.headers,
      body: req.body,
    });
  }
}
```

---

### Circuit Breaker

**Intent**: Prevent cascading failures by stopping requests to a failing service and allowing it time to recover.

```
        CLOSED                    OPEN                     HALF-OPEN
    ┌─────────────┐          ┌─────────────┐          ┌─────────────┐
    │   Normal    │  Failure │   Failing   │  Timeout │   Testing   │
    │  Operation  │ Threshold│   Fast Fail │  Expired │   Service   │
    │             │─────────▶│             │─────────▶│             │
    │ Track fails │          │Return error │          │ Allow some  │
    └──────┬──────┘          └─────────────┘          └──────┬──────┘
           │                        ▲                        │
           │                        │ Failure                │ Success
           └────────────────────────┴────────────────────────┘
```

```typescript
enum CircuitState {
  CLOSED = 'CLOSED',
  OPEN = 'OPEN',
  HALF_OPEN = 'HALF_OPEN',
}

interface CircuitBreakerConfig {
  failureThreshold: number;      // Number of failures to open circuit
  successThreshold: number;      // Number of successes to close circuit
  timeout: number;               // Time in ms before trying half-open
}

class CircuitBreaker {
  private state: CircuitState = CircuitState.CLOSED;
  private failureCount: number = 0;
  private successCount: number = 0;
  private lastFailureTime: number = 0;

  constructor(
    private readonly name: string,
    private readonly config: CircuitBreakerConfig,
  ) {}

  async execute<T>(fn: () => Promise<T>): Promise<T> {
    if (this.state === CircuitState.OPEN) {
      if (Date.now() - this.lastFailureTime >= this.config.timeout) {
        this.state = CircuitState.HALF_OPEN;
        console.log(`[CircuitBreaker:${this.name}] Transitioning to HALF_OPEN`);
      } else {
        throw new CircuitOpenError(`Circuit ${this.name} is OPEN`);
      }
    }

    try {
      const result = await fn();
      this.onSuccess();
      return result;
    } catch (error) {
      this.onFailure();
      throw error;
    }
  }

  private onSuccess(): void {
    if (this.state === CircuitState.HALF_OPEN) {
      this.successCount++;
      if (this.successCount >= this.config.successThreshold) {
        this.state = CircuitState.CLOSED;
        this.failureCount = 0;
        this.successCount = 0;
        console.log(`[CircuitBreaker:${this.name}] Circuit CLOSED`);
      }
    } else {
      this.failureCount = 0;
    }
  }

  private onFailure(): void {
    this.failureCount++;
    this.lastFailureTime = Date.now();
    this.successCount = 0;

    if (this.failureCount >= this.config.failureThreshold) {
      this.state = CircuitState.OPEN;
      console.log(`[CircuitBreaker:${this.name}] Circuit OPENED`);
    }
  }

  getState(): CircuitState {
    return this.state;
  }
}

// Usage
const paymentCircuit = new CircuitBreaker('payment-service', {
  failureThreshold: 5,
  successThreshold: 3,
  timeout: 30000,
});

async function processPayment(orderId: string): Promise<PaymentResult> {
  return paymentCircuit.execute(async () => {
    return await paymentService.charge(orderId);
  });
}
```

---

### Retry Pattern with Exponential Backoff

**Intent**: Automatically retry failed operations with increasing delays between attempts.

```typescript
interface RetryConfig {
  maxRetries: number;
  baseDelay: number;      // ms
  maxDelay: number;       // ms
  exponentialBase: number;
  retryableErrors?: string[];
}

class RetryHandler {
  constructor(private config: RetryConfig) {}

  async execute<T>(fn: () => Promise<T>): Promise<T> {
    let lastError: Error;

    for (let attempt = 0; attempt <= this.config.maxRetries; attempt++) {
      try {
        return await fn();
      } catch (error) {
        lastError = error;

        if (!this.isRetryable(error)) {
          throw error;
        }

        if (attempt === this.config.maxRetries) {
          break;
        }

        const delay = this.calculateDelay(attempt);
        console.log(`Retry attempt ${attempt + 1} after ${delay}ms`);
        await this.sleep(delay);
      }
    }

    throw new MaxRetriesExceededError(
      `Max retries (${this.config.maxRetries}) exceeded`,
      lastError,
    );
  }

  private calculateDelay(attempt: number): number {
    // Exponential backoff with jitter
    const exponentialDelay = this.config.baseDelay *
      Math.pow(this.config.exponentialBase, attempt);
    const jitter = Math.random() * 0.3 * exponentialDelay; // 0-30% jitter
    const delay = Math.min(exponentialDelay + jitter, this.config.maxDelay);
    return Math.round(delay);
  }

  private isRetryable(error: Error): boolean {
    if (!this.config.retryableErrors) {
      // Default: retry on network and timeout errors
      return error.name === 'NetworkError' ||
             error.name === 'TimeoutError' ||
             error.message.includes('ECONNRESET');
    }
    return this.config.retryableErrors.includes(error.name);
  }

  private sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}

// Usage
const retryHandler = new RetryHandler({
  maxRetries: 3,
  baseDelay: 1000,
  maxDelay: 10000,
  exponentialBase: 2,
});

const result = await retryHandler.execute(async () => {
  return await externalApi.fetchData();
});
```

---

### Bulkhead Pattern

**Intent**: Isolate elements of an application into pools so that if one fails, the others continue to function.

```typescript
class Bulkhead {
  private currentCount: number = 0;
  private queue: Array<{
    resolve: (value: boolean) => void;
    reject: (error: Error) => void;
  }> = [];

  constructor(
    private readonly name: string,
    private readonly maxConcurrent: number,
    private readonly maxQueue: number,
  ) {}

  async execute<T>(fn: () => Promise<T>): Promise<T> {
    const acquired = await this.acquire();
    if (!acquired) {
      throw new BulkheadFullError(`Bulkhead ${this.name} is full`);
    }

    try {
      return await fn();
    } finally {
      this.release();
    }
  }

  private async acquire(): Promise<boolean> {
    if (this.currentCount < this.maxConcurrent) {
      this.currentCount++;
      return true;
    }

    if (this.queue.length >= this.maxQueue) {
      return false;
    }

    return new Promise((resolve, reject) => {
      this.queue.push({ resolve, reject });
    });
  }

  private release(): void {
    this.currentCount--;

    if (this.queue.length > 0) {
      const next = this.queue.shift()!;
      this.currentCount++;
      next.resolve(true);
    }
  }

  getStats(): { current: number; queued: number } {
    return {
      current: this.currentCount,
      queued: this.queue.length,
    };
  }
}

// Usage: Separate bulkheads for different service calls
const paymentBulkhead = new Bulkhead('payment', 10, 20);
const inventoryBulkhead = new Bulkhead('inventory', 5, 10);
const notificationBulkhead = new Bulkhead('notification', 20, 50);

async function processOrder(order: Order): Promise<void> {
  // Each service has isolated resources
  const payment = await paymentBulkhead.execute(() =>
    paymentService.charge(order.total)
  );

  const inventory = await inventoryBulkhead.execute(() =>
    inventoryService.reserve(order.items)
  );

  // Notification is fire-and-forget, separate pool
  notificationBulkhead.execute(() =>
    notificationService.sendOrderConfirmation(order)
  ).catch(console.error);
}
```

---

## Data Management Patterns

### Saga Pattern

**Intent**: Manage distributed transactions across multiple services using a sequence of local transactions with compensating actions.

```
┌──────────────────────────────────────────────────────────────────────┐
│                         SAGA: Place Order                             │
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  Step 1: Create Order    ──▶  Compensate: Cancel Order               │
│           │                                  ▲                       │
│           ▼                                  │                       │
│  Step 2: Reserve Inventory ──▶ Compensate: Release Inventory         │
│           │                                  ▲                       │
│           ▼                                  │                       │
│  Step 3: Process Payment ──▶  Compensate: Refund Payment            │
│           │                                  ▲                       │
│           ▼                                  │                       │
│  Step 4: Ship Order      ──▶  Compensate: Cancel Shipment           │
│           │                                                          │
│           ▼                                                          │
│       SUCCESS                                                        │
│                                                                       │
│  If any step fails, execute compensating transactions in reverse     │
└──────────────────────────────────────────────────────────────────────┘
```

```typescript
// Saga step definition
interface SagaStep<T> {
  name: string;
  execute(context: T): Promise<void>;
  compensate(context: T): Promise<void>;
}

// Saga orchestrator
class SagaOrchestrator<T> {
  private steps: SagaStep<T>[] = [];

  addStep(step: SagaStep<T>): this {
    this.steps.push(step);
    return this;
  }

  async execute(context: T): Promise<void> {
    const executedSteps: SagaStep<T>[] = [];

    try {
      for (const step of this.steps) {
        console.log(`Executing step: ${step.name}`);
        await step.execute(context);
        executedSteps.push(step);
      }
      console.log('Saga completed successfully');
    } catch (error) {
      console.error(`Saga failed at step: ${executedSteps[executedSteps.length - 1]?.name}`);
      console.log('Starting compensation...');

      // Compensate in reverse order
      for (let i = executedSteps.length - 1; i >= 0; i--) {
        try {
          console.log(`Compensating step: ${executedSteps[i].name}`);
          await executedSteps[i].compensate(context);
        } catch (compensateError) {
          console.error(`Compensation failed for ${executedSteps[i].name}`, compensateError);
          // Log for manual intervention
        }
      }

      throw new SagaFailedError('Saga failed and compensated', error);
    }
  }
}

// Define saga steps
interface OrderContext {
  orderId: string;
  customerId: string;
  items: OrderItem[];
  total: number;
  paymentId?: string;
  shipmentId?: string;
}

const createOrderStep: SagaStep<OrderContext> = {
  name: 'CreateOrder',
  async execute(ctx) {
    const order = await orderService.create({
      customerId: ctx.customerId,
      items: ctx.items,
      total: ctx.total,
    });
    ctx.orderId = order.id;
  },
  async compensate(ctx) {
    await orderService.cancel(ctx.orderId);
  },
};

const reserveInventoryStep: SagaStep<OrderContext> = {
  name: 'ReserveInventory',
  async execute(ctx) {
    await inventoryService.reserve(ctx.orderId, ctx.items);
  },
  async compensate(ctx) {
    await inventoryService.release(ctx.orderId, ctx.items);
  },
};

const processPaymentStep: SagaStep<OrderContext> = {
  name: 'ProcessPayment',
  async execute(ctx) {
    const payment = await paymentService.charge(ctx.customerId, ctx.total);
    ctx.paymentId = payment.id;
  },
  async compensate(ctx) {
    if (ctx.paymentId) {
      await paymentService.refund(ctx.paymentId);
    }
  },
};

const shipOrderStep: SagaStep<OrderContext> = {
  name: 'ShipOrder',
  async execute(ctx) {
    const shipment = await shippingService.createShipment(ctx.orderId);
    ctx.shipmentId = shipment.id;
  },
  async compensate(ctx) {
    if (ctx.shipmentId) {
      await shippingService.cancelShipment(ctx.shipmentId);
    }
  },
};

// Build and execute saga
const placeOrderSaga = new SagaOrchestrator<OrderContext>()
  .addStep(createOrderStep)
  .addStep(reserveInventoryStep)
  .addStep(processPaymentStep)
  .addStep(shipOrderStep);

await placeOrderSaga.execute({
  customerId: 'cust-123',
  items: [{ productId: 'prod-1', quantity: 2 }],
  total: 99.99,
});
```

---

### Event Sourcing

**Intent**: Store state as a sequence of events rather than current state. The current state is derived by replaying events.

```typescript
// Event base
interface DomainEvent {
  eventId: string;
  aggregateId: string;
  aggregateType: string;
  eventType: string;
  payload: any;
  timestamp: Date;
  version: number;
}

// Events for Order aggregate
class OrderCreated implements DomainEvent {
  eventType = 'OrderCreated';
  constructor(
    public eventId: string,
    public aggregateId: string,
    public aggregateType: string,
    public payload: { customerId: string; items: OrderItem[] },
    public timestamp: Date,
    public version: number,
  ) {}
}

class OrderConfirmed implements DomainEvent {
  eventType = 'OrderConfirmed';
  constructor(
    public eventId: string,
    public aggregateId: string,
    public aggregateType: string,
    public payload: { confirmedAt: Date },
    public timestamp: Date,
    public version: number,
  ) {}
}

class OrderShipped implements DomainEvent {
  eventType = 'OrderShipped';
  constructor(
    public eventId: string,
    public aggregateId: string,
    public aggregateType: string,
    public payload: { trackingNumber: string; carrier: string },
    public timestamp: Date,
    public version: number,
  ) {}
}

// Event Store
interface EventStore {
  append(events: DomainEvent[]): Promise<void>;
  getEvents(aggregateId: string): Promise<DomainEvent[]>;
  getEventsAfterVersion(aggregateId: string, version: number): Promise<DomainEvent[]>;
}

// Aggregate that rebuilds from events
class Order {
  private id: string;
  private customerId: string;
  private items: OrderItem[] = [];
  private status: OrderStatus = OrderStatus.DRAFT;
  private version: number = 0;
  private uncommittedEvents: DomainEvent[] = [];

  private constructor() {}

  static create(customerId: string, items: OrderItem[]): Order {
    const order = new Order();
    order.apply(new OrderCreated(
      uuid(),
      uuid(),
      'Order',
      { customerId, items },
      new Date(),
      1,
    ));
    return order;
  }

  static fromEvents(events: DomainEvent[]): Order {
    const order = new Order();
    for (const event of events) {
      order.applyEvent(event);
      order.version = event.version;
    }
    return order;
  }

  confirm(): void {
    if (this.status !== OrderStatus.DRAFT) {
      throw new Error('Only draft orders can be confirmed');
    }
    this.apply(new OrderConfirmed(
      uuid(),
      this.id,
      'Order',
      { confirmedAt: new Date() },
      new Date(),
      this.version + 1,
    ));
  }

  ship(trackingNumber: string, carrier: string): void {
    if (this.status !== OrderStatus.CONFIRMED) {
      throw new Error('Only confirmed orders can be shipped');
    }
    this.apply(new OrderShipped(
      uuid(),
      this.id,
      'Order',
      { trackingNumber, carrier },
      new Date(),
      this.version + 1,
    ));
  }

  private apply(event: DomainEvent): void {
    this.applyEvent(event);
    this.uncommittedEvents.push(event);
    this.version = event.version;
  }

  private applyEvent(event: DomainEvent): void {
    switch (event.eventType) {
      case 'OrderCreated':
        this.id = event.aggregateId;
        this.customerId = event.payload.customerId;
        this.items = event.payload.items;
        this.status = OrderStatus.DRAFT;
        break;
      case 'OrderConfirmed':
        this.status = OrderStatus.CONFIRMED;
        break;
      case 'OrderShipped':
        this.status = OrderStatus.SHIPPED;
        break;
    }
  }

  getUncommittedEvents(): DomainEvent[] {
    return [...this.uncommittedEvents];
  }

  clearUncommittedEvents(): void {
    this.uncommittedEvents = [];
  }
}

// Repository using Event Store
class EventSourcedOrderRepository {
  constructor(private eventStore: EventStore) {}

  async save(order: Order): Promise<void> {
    const events = order.getUncommittedEvents();
    await this.eventStore.append(events);
    order.clearUncommittedEvents();
  }

  async findById(orderId: string): Promise<Order | null> {
    const events = await this.eventStore.getEvents(orderId);
    if (events.length === 0) return null;
    return Order.fromEvents(events);
  }
}
```

---

### CQRS (Command Query Responsibility Segregation)

**Intent**: Separate read and write models for different optimization strategies.

```
                    ┌─────────────────────────────────────┐
                    │              CLIENT                  │
                    └───────────────┬─────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │                               │
            ┌───────▼───────┐               ┌───────▼───────┐
            │   COMMANDS    │               │    QUERIES    │
            │ (Write Model) │               │ (Read Model)  │
            └───────┬───────┘               └───────┬───────┘
                    │                               │
            ┌───────▼───────┐               ┌───────▼───────┐
            │Command Handler│               │ Query Handler │
            └───────┬───────┘               └───────┬───────┘
                    │                               │
            ┌───────▼───────┐               ┌───────▼───────┐
            │    Domain     │──── Events ──▶│  Read Store   │
            │   (Write DB)  │               │  (Optimized)  │
            └───────────────┘               └───────────────┘
```

```typescript
// Commands
interface Command {
  type: string;
}

class CreateOrderCommand implements Command {
  type = 'CreateOrder';
  constructor(
    public customerId: string,
    public items: { productId: string; quantity: number }[],
  ) {}
}

class ConfirmOrderCommand implements Command {
  type = 'ConfirmOrder';
  constructor(public orderId: string) {}
}

// Command Handlers
class CreateOrderHandler {
  constructor(
    private orderRepository: OrderRepository,
    private eventBus: EventBus,
  ) {}

  async handle(command: CreateOrderCommand): Promise<string> {
    const order = Order.create(command.customerId, command.items);
    await this.orderRepository.save(order);

    // Publish events for read model update
    await this.eventBus.publish(new OrderCreatedEvent(
      order.getId(),
      order.getCustomerId(),
      order.getItems(),
    ));

    return order.getId();
  }
}

// Queries
interface Query {
  type: string;
}

class GetOrderQuery implements Query {
  type = 'GetOrder';
  constructor(public orderId: string) {}
}

class GetCustomerOrdersQuery implements Query {
  type = 'GetCustomerOrders';
  constructor(
    public customerId: string,
    public status?: string,
    public page: number = 1,
    public limit: number = 10,
  ) {}
}

// Query Handlers (use optimized read store)
class GetOrderQueryHandler {
  constructor(private readStore: OrderReadStore) {}

  async handle(query: GetOrderQuery): Promise<OrderView | null> {
    return this.readStore.findById(query.orderId);
  }
}

class GetCustomerOrdersQueryHandler {
  constructor(private readStore: OrderReadStore) {}

  async handle(query: GetCustomerOrdersQuery): Promise<PaginatedResult<OrderSummary>> {
    return this.readStore.findByCustomer(
      query.customerId,
      query.status,
      query.page,
      query.limit,
    );
  }
}

// Read Store (denormalized for fast queries)
interface OrderView {
  orderId: string;
  customerId: string;
  customerName: string;  // Denormalized
  items: Array<{
    productId: string;
    productName: string;  // Denormalized
    quantity: number;
    unitPrice: number;
  }>;
  totalAmount: number;
  status: string;
  createdAt: Date;
}

// Event Handler to update read model
class OrderCreatedEventHandler {
  constructor(
    private readStore: OrderReadStore,
    private customerService: CustomerService,
    private productService: ProductService,
  ) {}

  async handle(event: OrderCreatedEvent): Promise<void> {
    // Denormalize data for read model
    const customer = await this.customerService.getById(event.customerId);
    const enrichedItems = await Promise.all(
      event.items.map(async item => {
        const product = await this.productService.getById(item.productId);
        return {
          productId: item.productId,
          productName: product.name,
          quantity: item.quantity,
          unitPrice: product.price,
        };
      })
    );

    const orderView: OrderView = {
      orderId: event.orderId,
      customerId: event.customerId,
      customerName: customer.name,
      items: enrichedItems,
      totalAmount: enrichedItems.reduce((sum, i) => sum + i.unitPrice * i.quantity, 0),
      status: 'draft',
      createdAt: event.timestamp,
    };

    await this.readStore.save(orderView);
  }
}
```

---

## Service Discovery

**Intent**: Automatically detect and register service instances for dynamic routing.

```typescript
interface ServiceInstance {
  id: string;
  name: string;
  host: string;
  port: number;
  metadata?: Record<string, string>;
  healthCheckUrl?: string;
}

interface ServiceRegistry {
  register(instance: ServiceInstance): Promise<void>;
  deregister(instanceId: string): Promise<void>;
  getInstances(serviceName: string): Promise<ServiceInstance[]>;
  getHealthyInstances(serviceName: string): Promise<ServiceInstance[]>;
}

// Client-side service discovery
class ServiceDiscoveryClient {
  private cache: Map<string, ServiceInstance[]> = new Map();
  private roundRobinIndex: Map<string, number> = new Map();

  constructor(
    private registry: ServiceRegistry,
    private refreshIntervalMs: number = 30000,
  ) {
    this.startRefreshing();
  }

  async getNextInstance(serviceName: string): Promise<ServiceInstance> {
    let instances = this.cache.get(serviceName);

    if (!instances || instances.length === 0) {
      instances = await this.registry.getHealthyInstances(serviceName);
      this.cache.set(serviceName, instances);
    }

    if (instances.length === 0) {
      throw new NoAvailableInstanceError(serviceName);
    }

    // Round-robin load balancing
    const index = this.roundRobinIndex.get(serviceName) || 0;
    const instance = instances[index % instances.length];
    this.roundRobinIndex.set(serviceName, index + 1);

    return instance;
  }

  private startRefreshing(): void {
    setInterval(async () => {
      for (const serviceName of this.cache.keys()) {
        const instances = await this.registry.getHealthyInstances(serviceName);
        this.cache.set(serviceName, instances);
      }
    }, this.refreshIntervalMs);
  }
}

// Usage
const discovery = new ServiceDiscoveryClient(consulRegistry);
const paymentInstance = await discovery.getNextInstance('payment-service');
const response = await fetch(`http://${paymentInstance.host}:${paymentInstance.port}/charge`);
```

---

## Summary Table

| Pattern | Problem Solved | When to Use |
|---------|---------------|-------------|
| **API Gateway** | Multiple entry points, cross-cutting concerns | Always in microservices |
| **Circuit Breaker** | Cascading failures | External service calls |
| **Retry with Backoff** | Transient failures | Network operations |
| **Bulkhead** | Resource exhaustion | Isolate critical services |
| **Saga** | Distributed transactions | Multi-service workflows |
| **Event Sourcing** | Audit, temporal queries | Complex domains, audit requirements |
| **CQRS** | Read/write optimization | Different read/write patterns |
| **Service Discovery** | Dynamic service location | Dynamic environments |
