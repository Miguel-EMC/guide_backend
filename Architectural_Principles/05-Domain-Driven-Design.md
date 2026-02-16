# 05 - Domain-Driven Design (DDD)

Domain-Driven Design is an approach to software development that centers the development on programming a domain model that has a rich understanding of the processes and rules of a domain. It was introduced by Eric Evans in his book "Domain-Driven Design: Tackling Complexity in the Heart of Software" (2003).

---

## Strategic Design

Strategic design deals with high-level design decisions about the structure of the domain.

### Ubiquitous Language

A shared language between developers and domain experts that is used everywhere: code, documentation, and conversations.

```
❌ Bad: "The user clicks the submit button and the system saves the form data"
✅ Good: "The customer places an order and the order is confirmed"
```

The code should reflect this language:

```typescript
// ❌ Bad naming
class FormHandler {
  submitData(formData: any) {
    this.database.insert(formData);
  }
}

// ✅ Good naming using ubiquitous language
class OrderService {
  placeOrder(orderDetails: OrderDetails): Order {
    const order = Order.create(orderDetails);
    order.confirm();
    return order;
  }
}
```

### Bounded Contexts

A bounded context is a boundary within which a particular domain model is defined and applicable. Different contexts can have different models of the same concept.

```
┌─────────────────────────────────────────────────────────────────────┐
│                         E-COMMERCE SYSTEM                           │
├─────────────────────┬─────────────────────┬─────────────────────────┤
│   SALES CONTEXT     │  SHIPPING CONTEXT   │   BILLING CONTEXT       │
│                     │                     │                         │
│  ┌───────────────┐  │  ┌───────────────┐  │  ┌───────────────────┐  │
│  │    Order      │  │  │   Shipment    │  │  │     Invoice       │  │
│  │  - items      │  │  │  - packages   │  │  │  - lineItems      │  │
│  │  - customer   │  │  │  - address    │  │  │  - customer       │  │
│  │  - total      │  │  │  - carrier    │  │  │  - paymentTerms   │  │
│  └───────────────┘  │  └───────────────┘  │  └───────────────────┘  │
│                     │                     │                         │
│  ┌───────────────┐  │  ┌───────────────┐  │  ┌───────────────────┐  │
│  │   Customer    │  │  │   Recipient   │  │  │   BillingCustomer │  │
│  │  - name       │  │  │  - name       │  │  │  - name           │  │
│  │  - email      │  │  │  - address    │  │  │  - taxId          │  │
│  │  - preferences│  │  │  - phone      │  │  │  - billingAddress │  │
│  └───────────────┘  │  └───────────────┘  │  └───────────────────┘  │
└─────────────────────┴─────────────────────┴─────────────────────────┘
```

### Context Mapping

Shows the relationships between bounded contexts.

```
┌──────────────┐         ┌──────────────┐
│    SALES     │ ──────▶ │   SHIPPING   │
│   CONTEXT    │  U/D    │   CONTEXT    │
└──────────────┘         └──────────────┘
       │                        │
       │ U/D                    │ U/D
       ▼                        ▼
┌──────────────┐         ┌──────────────┐
│   BILLING    │         │  INVENTORY   │
│   CONTEXT    │         │   CONTEXT    │
└──────────────┘         └──────────────┘

U/D = Upstream/Downstream
```

**Relationship Types:**
- **Partnership**: Two contexts work together
- **Customer-Supplier**: Downstream depends on upstream
- **Conformist**: Downstream conforms to upstream model
- **Anti-Corruption Layer**: Downstream translates upstream model
- **Shared Kernel**: Contexts share a subset of the model

---

## Tactical Design

Tactical design deals with the building blocks of the domain model.

### 1. Entities

Objects with a distinct identity that runs through time and different representations.

```typescript
// entities/Order.ts
export class Order {
  private readonly id: OrderId;
  private items: OrderItem[];
  private status: OrderStatus;
  private customerId: CustomerId;
  private placedAt: Date;
  private confirmedAt?: Date;

  private constructor(
    id: OrderId,
    customerId: CustomerId,
    items: OrderItem[],
  ) {
    this.id = id;
    this.customerId = customerId;
    this.items = items;
    this.status = OrderStatus.DRAFT;
    this.placedAt = new Date();
  }

  static create(customerId: CustomerId, items: OrderItem[]): Order {
    if (items.length === 0) {
      throw new EmptyOrderError();
    }
    return new Order(OrderId.generate(), customerId, items);
  }

  addItem(item: OrderItem): void {
    if (this.status !== OrderStatus.DRAFT) {
      throw new CannotModifyOrderError(this.status);
    }
    const existingItem = this.items.find(i => i.productId.equals(item.productId));
    if (existingItem) {
      existingItem.increaseQuantity(item.quantity);
    } else {
      this.items.push(item);
    }
  }

  removeItem(productId: ProductId): void {
    if (this.status !== OrderStatus.DRAFT) {
      throw new CannotModifyOrderError(this.status);
    }
    this.items = this.items.filter(i => !i.productId.equals(productId));
  }

  place(): void {
    if (this.status !== OrderStatus.DRAFT) {
      throw new InvalidOrderTransitionError(this.status, OrderStatus.PLACED);
    }
    if (this.items.length === 0) {
      throw new EmptyOrderError();
    }
    this.status = OrderStatus.PLACED;
  }

  confirm(): void {
    if (this.status !== OrderStatus.PLACED) {
      throw new InvalidOrderTransitionError(this.status, OrderStatus.CONFIRMED);
    }
    this.status = OrderStatus.CONFIRMED;
    this.confirmedAt = new Date();
  }

  cancel(): void {
    if (this.status === OrderStatus.SHIPPED || this.status === OrderStatus.DELIVERED) {
      throw new CannotCancelOrderError(this.status);
    }
    this.status = OrderStatus.CANCELLED;
  }

  getTotalAmount(): Money {
    return this.items.reduce(
      (total, item) => total.add(item.getSubtotal()),
      Money.zero('USD'),
    );
  }

  // Identity comparison
  equals(other: Order): boolean {
    return this.id.equals(other.id);
  }

  getId(): OrderId { return this.id; }
  getStatus(): OrderStatus { return this.status; }
  getItems(): ReadonlyArray<OrderItem> { return [...this.items]; }
}
```

### 2. Value Objects

Objects that describe some characteristic or attribute but have no conceptual identity. They are immutable.

```typescript
// value-objects/Money.ts
export class Money {
  private constructor(
    private readonly amount: number,
    private readonly currency: string,
  ) {
    if (amount < 0) {
      throw new NegativeAmountError(amount);
    }
  }

  static of(amount: number, currency: string): Money {
    return new Money(amount, currency);
  }

  static zero(currency: string): Money {
    return new Money(0, currency);
  }

  add(other: Money): Money {
    this.ensureSameCurrency(other);
    return new Money(this.amount + other.amount, this.currency);
  }

  subtract(other: Money): Money {
    this.ensureSameCurrency(other);
    return new Money(this.amount - other.amount, this.currency);
  }

  multiply(factor: number): Money {
    return new Money(this.amount * factor, this.currency);
  }

  isGreaterThan(other: Money): boolean {
    this.ensureSameCurrency(other);
    return this.amount > other.amount;
  }

  private ensureSameCurrency(other: Money): void {
    if (this.currency !== other.currency) {
      throw new CurrencyMismatchError(this.currency, other.currency);
    }
  }

  // Value equality
  equals(other: Money): boolean {
    return this.amount === other.amount && this.currency === other.currency;
  }

  getAmount(): number { return this.amount; }
  getCurrency(): string { return this.currency; }
  toString(): string { return `${this.currency} ${this.amount.toFixed(2)}`; }
}
```

```typescript
// value-objects/Address.ts
export class Address {
  private constructor(
    private readonly street: string,
    private readonly city: string,
    private readonly state: string,
    private readonly postalCode: string,
    private readonly country: string,
  ) {}

  static create(
    street: string,
    city: string,
    state: string,
    postalCode: string,
    country: string,
  ): Address {
    if (!street || !city || !country) {
      throw new InvalidAddressError();
    }
    return new Address(street, city, state, postalCode, country);
  }

  // Value equality
  equals(other: Address): boolean {
    return (
      this.street === other.street &&
      this.city === other.city &&
      this.state === other.state &&
      this.postalCode === other.postalCode &&
      this.country === other.country
    );
  }

  format(): string {
    return `${this.street}, ${this.city}, ${this.state} ${this.postalCode}, ${this.country}`;
  }
}
```

```typescript
// value-objects/OrderId.ts
export class OrderId {
  private constructor(private readonly value: string) {}

  static generate(): OrderId {
    return new OrderId(`ORD-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`);
  }

  static fromString(value: string): OrderId {
    if (!value.startsWith('ORD-')) {
      throw new InvalidOrderIdError(value);
    }
    return new OrderId(value);
  }

  equals(other: OrderId): boolean {
    return this.value === other.value;
  }

  getValue(): string { return this.value; }
  toString(): string { return this.value; }
}
```

### 3. Aggregates

A cluster of domain objects (entities and value objects) that can be treated as a single unit. Each aggregate has a root entity (Aggregate Root) that controls access to the aggregate.

```typescript
// aggregates/Order (Aggregate Root)
export class Order {
  private readonly id: OrderId;
  private items: OrderItem[];          // Part of aggregate
  private shippingAddress: Address;    // Part of aggregate
  private billingAddress: Address;     // Part of aggregate
  private status: OrderStatus;
  private readonly customerId: CustomerId;  // Reference to another aggregate

  // All access to OrderItems goes through Order (the aggregate root)
  addItem(productId: ProductId, quantity: number, price: Money): void {
    // Invariant enforcement
    if (this.status !== OrderStatus.DRAFT) {
      throw new CannotModifyOrderError();
    }

    const item = new OrderItem(productId, quantity, price);
    this.items.push(item);

    // Raise domain event
    this.addDomainEvent(new OrderItemAdded(this.id, productId, quantity));
  }

  // OrderItem cannot be accessed directly from outside
  // It must go through Order
}

// This is NOT an aggregate root - it's part of Order aggregate
export class OrderItem {
  constructor(
    private readonly productId: ProductId,
    private quantity: number,
    private readonly unitPrice: Money,
  ) {}

  getSubtotal(): Money {
    return this.unitPrice.multiply(this.quantity);
  }

  increaseQuantity(amount: number): void {
    this.quantity += amount;
  }
}
```

**Aggregate Rules:**

1. Reference aggregates by ID, not by object reference
2. Changes to the aggregate are made only through the root
3. Aggregates are consistency boundaries
4. Keep aggregates small

```typescript
// ❌ Bad: Direct reference to another aggregate
class Order {
  private customer: Customer;  // Wrong! Direct reference
}

// ✅ Good: Reference by ID
class Order {
  private customerId: CustomerId;  // Correct! Reference by ID
}
```

### 4. Domain Events

Something that happened in the domain that domain experts care about. Events are immutable and represent facts.

```typescript
// events/DomainEvent.ts
export abstract class DomainEvent {
  readonly occurredOn: Date;

  constructor() {
    this.occurredOn = new Date();
  }
}

// events/OrderPlaced.ts
export class OrderPlaced extends DomainEvent {
  constructor(
    public readonly orderId: OrderId,
    public readonly customerId: CustomerId,
    public readonly totalAmount: Money,
    public readonly items: ReadonlyArray<{
      productId: ProductId;
      quantity: number;
    }>,
  ) {
    super();
  }
}

// events/OrderShipped.ts
export class OrderShipped extends DomainEvent {
  constructor(
    public readonly orderId: OrderId,
    public readonly trackingNumber: string,
    public readonly carrier: string,
    public readonly estimatedDelivery: Date,
  ) {
    super();
  }
}

// events/PaymentReceived.ts
export class PaymentReceived extends DomainEvent {
  constructor(
    public readonly orderId: OrderId,
    public readonly amount: Money,
    public readonly paymentMethod: string,
    public readonly transactionId: string,
  ) {
    super();
  }
}
```

**Aggregate with Domain Events:**

```typescript
export abstract class AggregateRoot {
  private domainEvents: DomainEvent[] = [];

  protected addDomainEvent(event: DomainEvent): void {
    this.domainEvents.push(event);
  }

  pullDomainEvents(): DomainEvent[] {
    const events = [...this.domainEvents];
    this.domainEvents = [];
    return events;
  }
}

export class Order extends AggregateRoot {
  place(): void {
    // ... validation and state change
    this.status = OrderStatus.PLACED;

    // Raise event
    this.addDomainEvent(new OrderPlaced(
      this.id,
      this.customerId,
      this.getTotalAmount(),
      this.items.map(i => ({ productId: i.productId, quantity: i.quantity })),
    ));
  }
}
```

### 5. Domain Services

Operations that don't naturally fit within an entity or value object. They contain domain logic that spans multiple aggregates.

```typescript
// services/PricingService.ts
export class PricingService {
  constructor(
    private readonly discountRepository: DiscountRepository,
    private readonly taxCalculator: TaxCalculator,
  ) {}

  calculateOrderTotal(order: Order, customer: Customer): OrderPricing {
    const subtotal = order.getSubtotal();

    // Apply customer-specific discounts
    const discount = this.calculateDiscount(subtotal, customer);
    const afterDiscount = subtotal.subtract(discount);

    // Calculate tax based on shipping address
    const tax = this.taxCalculator.calculate(
      afterDiscount,
      order.getShippingAddress(),
    );

    const total = afterDiscount.add(tax);

    return new OrderPricing(subtotal, discount, tax, total);
  }

  private calculateDiscount(subtotal: Money, customer: Customer): Money {
    const discounts = this.discountRepository.findActiveForCustomer(customer.getId());
    // Apply discount rules
    return discounts.reduce(
      (total, discount) => total.add(discount.apply(subtotal)),
      Money.zero(subtotal.getCurrency()),
    );
  }
}
```

```typescript
// services/OrderFulfillmentService.ts
export class OrderFulfillmentService {
  constructor(
    private readonly orderRepository: OrderRepository,
    private readonly inventoryService: InventoryService,
    private readonly shippingService: ShippingService,
  ) {}

  async fulfillOrder(orderId: OrderId): Promise<void> {
    const order = await this.orderRepository.findById(orderId);
    if (!order) throw new OrderNotFoundError(orderId);

    // Reserve inventory
    for (const item of order.getItems()) {
      await this.inventoryService.reserve(item.productId, item.quantity);
    }

    // Create shipment
    const shipment = await this.shippingService.createShipment(order);

    // Update order
    order.ship(shipment.trackingNumber);
    await this.orderRepository.save(order);
  }
}
```

### 6. Repositories

Provide a collection-like interface for accessing domain objects. They abstract the persistence mechanism.

```typescript
// repositories/OrderRepository.ts
export interface OrderRepository {
  save(order: Order): Promise<void>;
  findById(id: OrderId): Promise<Order | null>;
  findByCustomerId(customerId: CustomerId): Promise<Order[]>;
  findPendingOrders(): Promise<Order[]>;
  nextId(): OrderId;
}
```

```typescript
// repositories/impl/PostgresOrderRepository.ts
export class PostgresOrderRepository implements OrderRepository {
  constructor(private readonly db: DatabaseConnection) {}

  async save(order: Order): Promise<void> {
    await this.db.transaction(async (tx) => {
      // Save order
      await tx.query(
        `INSERT INTO orders (id, customer_id, status, total, placed_at)
         VALUES ($1, $2, $3, $4, $5)
         ON CONFLICT (id) DO UPDATE SET status = $3, total = $4`,
        [order.getId().getValue(), order.getCustomerId().getValue(),
         order.getStatus(), order.getTotalAmount().getAmount(), order.getPlacedAt()]
      );

      // Save order items
      await tx.query('DELETE FROM order_items WHERE order_id = $1', [order.getId().getValue()]);
      for (const item of order.getItems()) {
        await tx.query(
          `INSERT INTO order_items (order_id, product_id, quantity, unit_price)
           VALUES ($1, $2, $3, $4)`,
          [order.getId().getValue(), item.productId.getValue(),
           item.quantity, item.unitPrice.getAmount()]
        );
      }
    });
  }

  async findById(id: OrderId): Promise<Order | null> {
    const orderRow = await this.db.query(
      'SELECT * FROM orders WHERE id = $1',
      [id.getValue()]
    );
    if (!orderRow) return null;

    const itemRows = await this.db.query(
      'SELECT * FROM order_items WHERE order_id = $1',
      [id.getValue()]
    );

    return this.reconstituteOrder(orderRow, itemRows);
  }

  private reconstituteOrder(orderRow: any, itemRows: any[]): Order {
    // Reconstitute the aggregate from persistence
    return Order.reconstitute(
      OrderId.fromString(orderRow.id),
      CustomerId.fromString(orderRow.customer_id),
      itemRows.map(row => new OrderItem(
        ProductId.fromString(row.product_id),
        row.quantity,
        Money.of(row.unit_price, 'USD'),
      )),
      orderRow.status as OrderStatus,
      orderRow.placed_at,
    );
  }
}
```

### 7. Factories

Encapsulate complex object creation logic.

```typescript
// factories/OrderFactory.ts
export class OrderFactory {
  constructor(
    private readonly productRepository: ProductRepository,
    private readonly pricingService: PricingService,
  ) {}

  async createOrder(
    customerId: CustomerId,
    items: Array<{ productId: string; quantity: number }>,
    shippingAddress: Address,
  ): Promise<Order> {
    // Validate and enrich items with current prices
    const orderItems: OrderItem[] = [];

    for (const item of items) {
      const product = await this.productRepository.findById(
        ProductId.fromString(item.productId)
      );

      if (!product) {
        throw new ProductNotFoundError(item.productId);
      }

      if (!product.isAvailable()) {
        throw new ProductUnavailableError(item.productId);
      }

      orderItems.push(new OrderItem(
        product.getId(),
        item.quantity,
        product.getPrice(),
      ));
    }

    return Order.create(customerId, orderItems, shippingAddress);
  }
}
```

---

## Project Structure with DDD

```
src/
├── domain/
│   ├── order/                       # Order Bounded Context
│   │   ├── entities/
│   │   │   ├── Order.ts             # Aggregate Root
│   │   │   └── OrderItem.ts
│   │   ├── value-objects/
│   │   │   ├── OrderId.ts
│   │   │   ├── OrderStatus.ts
│   │   │   └── Money.ts
│   │   ├── events/
│   │   │   ├── OrderPlaced.ts
│   │   │   ├── OrderConfirmed.ts
│   │   │   └── OrderShipped.ts
│   │   ├── services/
│   │   │   └── PricingService.ts
│   │   ├── repositories/
│   │   │   └── OrderRepository.ts   # Interface
│   │   ├── factories/
│   │   │   └── OrderFactory.ts
│   │   └── errors/
│   │       ├── EmptyOrderError.ts
│   │       └── InvalidOrderStateError.ts
│   │
│   ├── customer/                    # Customer Bounded Context
│   │   ├── entities/
│   │   │   └── Customer.ts
│   │   ├── value-objects/
│   │   │   ├── CustomerId.ts
│   │   │   └── Email.ts
│   │   └── repositories/
│   │       └── CustomerRepository.ts
│   │
│   └── shared/                      # Shared Kernel
│       ├── value-objects/
│       │   ├── Address.ts
│       │   └── Money.ts
│       └── events/
│           └── DomainEvent.ts
│
├── application/
│   ├── commands/
│   │   ├── PlaceOrderCommand.ts
│   │   └── PlaceOrderHandler.ts
│   ├── queries/
│   │   ├── GetOrderQuery.ts
│   │   └── GetOrderHandler.ts
│   └── event-handlers/
│       ├── OrderPlacedHandler.ts
│       └── PaymentReceivedHandler.ts
│
├── infrastructure/
│   ├── persistence/
│   │   ├── PostgresOrderRepository.ts
│   │   └── PostgresCustomerRepository.ts
│   ├── messaging/
│   │   └── RabbitMQEventPublisher.ts
│   └── services/
│       └── StripePaymentService.ts
│
└── interfaces/
    ├── http/
    │   └── OrderController.ts
    └── graphql/
        └── OrderResolver.ts
```

---

## Summary Table

| Building Block | Purpose | Identity | Mutability |
|---------------|---------|----------|------------|
| **Entity** | Objects with identity | Has unique ID | Mutable |
| **Value Object** | Descriptive objects | No identity | Immutable |
| **Aggregate** | Consistency boundary | Root has ID | Controlled |
| **Domain Event** | Record of something that happened | No identity | Immutable |
| **Domain Service** | Stateless operations | N/A | N/A |
| **Repository** | Collection abstraction | N/A | N/A |
| **Factory** | Complex object creation | N/A | N/A |

---

## When to Use DDD

**Good Fit:**
- Complex business domains
- Long-lived projects
- Teams with access to domain experts
- Projects where the domain is the competitive advantage

**Not a Good Fit:**
- Simple CRUD applications
- Technical/infrastructure projects
- Short-lived projects
- Lack of domain experts
