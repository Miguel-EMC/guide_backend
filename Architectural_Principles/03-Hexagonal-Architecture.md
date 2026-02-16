# 03 - Hexagonal Architecture (Ports & Adapters)

Hexagonal Architecture, also known as Ports and Adapters, was introduced by Alistair Cockburn. It aims to create loosely coupled application components that can be easily connected to their software environment through ports and adapters. This makes the application highly testable and independent of external systems.

---

## Core Concept

The fundamental idea is to isolate the application's core business logic from external concerns like databases, web frameworks, and third-party services.

```
                    ┌─────────────────────────────────────┐
                    │           EXTERNAL WORLD            │
                    │  (Web, CLI, Tests, Message Queues)  │
                    └─────────────────┬───────────────────┘
                                      │
                    ┌─────────────────▼───────────────────┐
                    │         DRIVING ADAPTERS            │
                    │   (Controllers, CLI Commands)       │
                    └─────────────────┬───────────────────┘
                                      │
                    ┌─────────────────▼───────────────────┐
                    │           INPUT PORTS               │
                    │      (Use Case Interfaces)          │
                    └─────────────────┬───────────────────┘
                                      │
                    ┌─────────────────▼───────────────────┐
                    │                                     │
                    │         APPLICATION CORE            │
                    │                                     │
                    │    ┌───────────────────────────┐    │
                    │    │      DOMAIN MODEL         │    │
                    │    │  (Entities, Value Objects)│    │
                    │    └───────────────────────────┘    │
                    │                                     │
                    └─────────────────┬───────────────────┘
                                      │
                    ┌─────────────────▼───────────────────┐
                    │          OUTPUT PORTS               │
                    │    (Repository Interfaces)          │
                    └─────────────────┬───────────────────┘
                                      │
                    ┌─────────────────▼───────────────────┐
                    │         DRIVEN ADAPTERS             │
                    │  (Database, External APIs, Cache)   │
                    └─────────────────┬───────────────────┘
                                      │
                    ┌─────────────────▼───────────────────┐
                    │           EXTERNAL WORLD            │
                    │   (PostgreSQL, Redis, Stripe API)   │
                    └─────────────────────────────────────┘
```

---

## Key Components

### 1. Domain (Core)

The innermost layer containing pure business logic. It has no dependencies on external frameworks or libraries.

```typescript
// domain/entities/Order.ts
export class Order {
  private readonly id: string;
  private items: OrderItem[];
  private status: OrderStatus;

  constructor(id: string, items: OrderItem[]) {
    if (items.length === 0) {
      throw new Error('Order must have at least one item');
    }
    this.id = id;
    this.items = items;
    this.status = OrderStatus.PENDING;
  }

  getTotalAmount(): number {
    return this.items.reduce((sum, item) => sum + item.getSubtotal(), 0);
  }

  confirm(): void {
    if (this.status !== OrderStatus.PENDING) {
      throw new Error('Only pending orders can be confirmed');
    }
    this.status = OrderStatus.CONFIRMED;
  }

  cancel(): void {
    if (this.status === OrderStatus.SHIPPED) {
      throw new Error('Cannot cancel shipped orders');
    }
    this.status = OrderStatus.CANCELLED;
  }
}
```

```typescript
// domain/value-objects/Money.ts
export class Money {
  constructor(
    private readonly amount: number,
    private readonly currency: string
  ) {
    if (amount < 0) {
      throw new Error('Amount cannot be negative');
    }
  }

  add(other: Money): Money {
    this.ensureSameCurrency(other);
    return new Money(this.amount + other.amount, this.currency);
  }

  multiply(factor: number): Money {
    return new Money(this.amount * factor, this.currency);
  }

  private ensureSameCurrency(other: Money): void {
    if (this.currency !== other.currency) {
      throw new Error('Cannot operate on different currencies');
    }
  }
}
```

### 2. Ports (Interfaces)

Ports define how the application interacts with the outside world. There are two types:

#### Input Ports (Driving Ports)

Define what the application can do. They are implemented by use cases.

```typescript
// application/ports/input/CreateOrderUseCase.ts
export interface CreateOrderUseCase {
  execute(command: CreateOrderCommand): Promise<OrderDto>;
}

export interface CreateOrderCommand {
  customerId: string;
  items: { productId: string; quantity: number }[];
  shippingAddress: Address;
}
```

```typescript
// application/ports/input/GetOrderUseCase.ts
export interface GetOrderUseCase {
  execute(orderId: string): Promise<OrderDto | null>;
}
```

#### Output Ports (Driven Ports)

Define what the application needs from external systems. They are implemented by adapters.

```typescript
// application/ports/output/OrderRepository.ts
export interface OrderRepository {
  save(order: Order): Promise<void>;
  findById(id: string): Promise<Order | null>;
  findByCustomerId(customerId: string): Promise<Order[]>;
  delete(id: string): Promise<void>;
}
```

```typescript
// application/ports/output/PaymentGateway.ts
export interface PaymentGateway {
  charge(amount: Money, paymentMethodId: string): Promise<PaymentResult>;
  refund(transactionId: string): Promise<RefundResult>;
}
```

```typescript
// application/ports/output/NotificationService.ts
export interface NotificationService {
  sendOrderConfirmation(order: Order, customerEmail: string): Promise<void>;
  sendShippingNotification(order: Order, trackingNumber: string): Promise<void>;
}
```

### 3. Application Layer (Use Cases)

Implements the input ports and orchestrates the domain logic.

```typescript
// application/use-cases/CreateOrderUseCaseImpl.ts
export class CreateOrderUseCaseImpl implements CreateOrderUseCase {
  constructor(
    private readonly orderRepository: OrderRepository,
    private readonly productRepository: ProductRepository,
    private readonly paymentGateway: PaymentGateway,
    private readonly notificationService: NotificationService,
  ) {}

  async execute(command: CreateOrderCommand): Promise<OrderDto> {
    // 1. Validate products exist and have stock
    const orderItems = await this.buildOrderItems(command.items);

    // 2. Create domain entity
    const order = new Order(
      this.generateOrderId(),
      orderItems,
      command.shippingAddress
    );

    // 3. Process payment
    const paymentResult = await this.paymentGateway.charge(
      order.getTotalAmount(),
      command.paymentMethodId
    );

    if (!paymentResult.success) {
      throw new PaymentFailedException(paymentResult.error);
    }

    // 4. Confirm order
    order.confirm();

    // 5. Persist
    await this.orderRepository.save(order);

    // 6. Send notification
    await this.notificationService.sendOrderConfirmation(
      order,
      command.customerEmail
    );

    return OrderMapper.toDto(order);
  }

  private async buildOrderItems(items: CreateOrderCommand['items']): Promise<OrderItem[]> {
    // Validate and build order items
  }

  private generateOrderId(): string {
    return `ORD-${Date.now()}`;
  }
}
```

### 4. Adapters

Implement the ports to connect with external systems.

#### Driving Adapters (Primary)

```typescript
// adapters/input/http/OrderController.ts
@Controller('orders')
export class OrderController {
  constructor(
    @Inject('CreateOrderUseCase')
    private readonly createOrderUseCase: CreateOrderUseCase,
    @Inject('GetOrderUseCase')
    private readonly getOrderUseCase: GetOrderUseCase,
  ) {}

  @Post()
  async createOrder(@Body() dto: CreateOrderDto): Promise<OrderDto> {
    return this.createOrderUseCase.execute({
      customerId: dto.customerId,
      items: dto.items,
      shippingAddress: dto.shippingAddress,
    });
  }

  @Get(':id')
  async getOrder(@Param('id') id: string): Promise<OrderDto> {
    const order = await this.getOrderUseCase.execute(id);
    if (!order) {
      throw new NotFoundException('Order not found');
    }
    return order;
  }
}
```

```typescript
// adapters/input/cli/ProcessOrdersCommand.ts
@Command({ name: 'orders:process' })
export class ProcessOrdersCommand {
  constructor(
    @Inject('ProcessPendingOrdersUseCase')
    private readonly processOrdersUseCase: ProcessPendingOrdersUseCase,
  ) {}

  async run(): Promise<void> {
    const processed = await this.processOrdersUseCase.execute();
    console.log(`Processed ${processed} orders`);
  }
}
```

#### Driven Adapters (Secondary)

```typescript
// adapters/output/persistence/PostgresOrderRepository.ts
export class PostgresOrderRepository implements OrderRepository {
  constructor(
    @InjectRepository(OrderEntity)
    private readonly repository: Repository<OrderEntity>,
  ) {}

  async save(order: Order): Promise<void> {
    const entity = OrderMapper.toEntity(order);
    await this.repository.save(entity);
  }

  async findById(id: string): Promise<Order | null> {
    const entity = await this.repository.findOne({ where: { id } });
    return entity ? OrderMapper.toDomain(entity) : null;
  }

  async findByCustomerId(customerId: string): Promise<Order[]> {
    const entities = await this.repository.find({ where: { customerId } });
    return entities.map(OrderMapper.toDomain);
  }

  async delete(id: string): Promise<void> {
    await this.repository.delete(id);
  }
}
```

```typescript
// adapters/output/payment/StripePaymentGateway.ts
export class StripePaymentGateway implements PaymentGateway {
  private stripe: Stripe;

  constructor(apiKey: string) {
    this.stripe = new Stripe(apiKey);
  }

  async charge(amount: Money, paymentMethodId: string): Promise<PaymentResult> {
    try {
      const paymentIntent = await this.stripe.paymentIntents.create({
        amount: amount.getAmountInCents(),
        currency: amount.getCurrency().toLowerCase(),
        payment_method: paymentMethodId,
        confirm: true,
      });

      return {
        success: true,
        transactionId: paymentIntent.id,
      };
    } catch (error) {
      return {
        success: false,
        error: error.message,
      };
    }
  }

  async refund(transactionId: string): Promise<RefundResult> {
    const refund = await this.stripe.refunds.create({
      payment_intent: transactionId,
    });
    return { success: refund.status === 'succeeded' };
  }
}
```

```typescript
// adapters/output/notification/SendGridNotificationService.ts
export class SendGridNotificationService implements NotificationService {
  constructor(private readonly sendgrid: SendGridClient) {}

  async sendOrderConfirmation(order: Order, customerEmail: string): Promise<void> {
    await this.sendgrid.send({
      to: customerEmail,
      from: 'orders@example.com',
      templateId: 'order-confirmation',
      dynamicTemplateData: {
        orderId: order.getId(),
        total: order.getTotalAmount().toString(),
      },
    });
  }

  async sendShippingNotification(order: Order, trackingNumber: string): Promise<void> {
    // Implementation
  }
}
```

---

## Project Structure

```
src/
├── domain/                          # Core business logic
│   ├── entities/
│   │   ├── Order.ts
│   │   ├── OrderItem.ts
│   │   └── Customer.ts
│   ├── value-objects/
│   │   ├── Money.ts
│   │   ├── Address.ts
│   │   └── OrderStatus.ts
│   ├── events/
│   │   ├── OrderCreated.ts
│   │   └── OrderShipped.ts
│   └── exceptions/
│       ├── InsufficientStockException.ts
│       └── InvalidOrderStateException.ts
│
├── application/                     # Use cases and ports
│   ├── ports/
│   │   ├── input/                   # Driving ports (use cases)
│   │   │   ├── CreateOrderUseCase.ts
│   │   │   ├── CancelOrderUseCase.ts
│   │   │   └── GetOrderUseCase.ts
│   │   └── output/                  # Driven ports (repositories, services)
│   │       ├── OrderRepository.ts
│   │       ├── PaymentGateway.ts
│   │       └── NotificationService.ts
│   ├── use-cases/
│   │   ├── CreateOrderUseCaseImpl.ts
│   │   ├── CancelOrderUseCaseImpl.ts
│   │   └── GetOrderUseCaseImpl.ts
│   ├── dto/
│   │   ├── OrderDto.ts
│   │   └── CreateOrderCommand.ts
│   └── mappers/
│       └── OrderMapper.ts
│
├── adapters/                        # External world connections
│   ├── input/                       # Driving adapters
│   │   ├── http/
│   │   │   ├── OrderController.ts
│   │   │   └── CustomerController.ts
│   │   ├── graphql/
│   │   │   └── OrderResolver.ts
│   │   └── cli/
│   │       └── ProcessOrdersCommand.ts
│   └── output/                      # Driven adapters
│       ├── persistence/
│       │   ├── PostgresOrderRepository.ts
│       │   └── entities/
│       │       └── OrderEntity.ts
│       ├── payment/
│       │   ├── StripePaymentGateway.ts
│       │   └── PayPalPaymentGateway.ts
│       ├── notification/
│       │   └── SendGridNotificationService.ts
│       └── cache/
│           └── RedisOrderCache.ts
│
└── infrastructure/                  # Framework configuration
    ├── config/
    │   └── database.config.ts
    ├── modules/
    │   └── OrderModule.ts
    └── main.ts
```

---

## Dependency Injection Setup

```typescript
// infrastructure/modules/OrderModule.ts
@Module({
  imports: [TypeOrmModule.forFeature([OrderEntity])],
  controllers: [OrderController],
  providers: [
    // Use Cases (Input Ports Implementation)
    {
      provide: 'CreateOrderUseCase',
      useClass: CreateOrderUseCaseImpl,
    },
    {
      provide: 'GetOrderUseCase',
      useClass: GetOrderUseCaseImpl,
    },
    // Output Ports Implementation (Adapters)
    {
      provide: 'OrderRepository',
      useClass: PostgresOrderRepository,
    },
    {
      provide: 'PaymentGateway',
      useFactory: (config: ConfigService) => {
        return new StripePaymentGateway(config.get('STRIPE_API_KEY'));
      },
      inject: [ConfigService],
    },
    {
      provide: 'NotificationService',
      useClass: SendGridNotificationService,
    },
  ],
})
export class OrderModule {}
```

---

## Testing Benefits

The architecture makes testing straightforward by allowing you to swap adapters with test doubles.

```typescript
// Unit test with mock adapters
describe('CreateOrderUseCase', () => {
  let useCase: CreateOrderUseCase;
  let orderRepository: jest.Mocked<OrderRepository>;
  let paymentGateway: jest.Mocked<PaymentGateway>;

  beforeEach(() => {
    orderRepository = {
      save: jest.fn(),
      findById: jest.fn(),
    };
    paymentGateway = {
      charge: jest.fn().mockResolvedValue({ success: true, transactionId: 'tx-123' }),
    };

    useCase = new CreateOrderUseCaseImpl(
      orderRepository,
      paymentGateway,
      // ... other mocks
    );
  });

  it('should create order and process payment', async () => {
    const command = {
      customerId: 'cust-1',
      items: [{ productId: 'prod-1', quantity: 2 }],
    };

    const result = await useCase.execute(command);

    expect(paymentGateway.charge).toHaveBeenCalled();
    expect(orderRepository.save).toHaveBeenCalled();
    expect(result.status).toBe('CONFIRMED');
  });
});
```

---

## Benefits

| Benefit | Description |
|---------|-------------|
| **Testability** | Core logic can be tested without databases or external services |
| **Flexibility** | Easy to swap implementations (change from PostgreSQL to MongoDB) |
| **Maintainability** | Changes in external systems don't affect business logic |
| **Independence** | Domain is framework-agnostic |
| **Parallel Development** | Teams can work on different adapters simultaneously |

---

## When to Use

- Medium to large applications with complex business logic
- Applications that need to support multiple interfaces (REST, GraphQL, CLI)
- Systems that may change databases or external services
- Projects where testability is a priority

## When NOT to Use

- Simple CRUD applications
- Prototypes or MVPs where speed is critical
- Small scripts or utilities
