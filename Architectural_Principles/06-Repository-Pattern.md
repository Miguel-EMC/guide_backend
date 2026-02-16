# 06 - Repository Pattern

The Repository Pattern mediates between the domain and data mapping layers, acting like an in-memory collection of domain objects. It provides a more object-oriented view of the persistence layer.

---

## Core Concept

A Repository encapsulates the logic required to access data sources. It centralizes common data access functionality, providing better maintainability and decoupling the infrastructure from the domain layer.

```
┌─────────────────────────────────────────────────────────────────┐
│                      APPLICATION LAYER                          │
│                                                                 │
│    ┌─────────────────┐         ┌─────────────────┐              │
│    │   Use Case      │         │   Use Case      │              │
│    │   (Service)     │         │   (Service)     │              │
│    └────────┬────────┘         └────────┬────────┘              │
│             │                           │                       │
└─────────────┼───────────────────────────┼───────────────────────┘
              │                           │
              ▼                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    REPOSITORY INTERFACE                          │
│                    (Domain Layer)                                │
│                                                                 │
│    ┌─────────────────────────────────────────────────────────┐  │
│    │  interface UserRepository {                              │  │
│    │    save(user: User): Promise<void>                       │  │
│    │    findById(id: UserId): Promise<User | null>            │  │
│    │    findByEmail(email: Email): Promise<User | null>       │  │
│    │  }                                                       │  │
│    └─────────────────────────────────────────────────────────┘  │
│                                                                 │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                  REPOSITORY IMPLEMENTATION                       │
│                  (Infrastructure Layer)                          │
│                                                                 │
│  ┌──────────────────┐  ┌──────────────────┐  ┌────────────────┐ │
│  │ PostgresUserRepo │  │  MongoUserRepo   │  │ InMemoryUserRepo│ │
│  └──────────────────┘  └──────────────────┘  └────────────────┘ │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                        DATA SOURCES                              │
│     PostgreSQL          MongoDB           In-Memory (Tests)      │
└─────────────────────────────────────────────────────────────────┘
```

---

## Repository Interface (Domain Layer)

The interface belongs to the domain layer. It defines what operations are available without specifying how they're implemented.

```typescript
// domain/repositories/UserRepository.ts
import { User } from '../entities/User';
import { UserId } from '../value-objects/UserId';
import { Email } from '../value-objects/Email';

export interface UserRepository {
  // Persistence
  save(user: User): Promise<void>;
  delete(id: UserId): Promise<void>;

  // Retrieval
  findById(id: UserId): Promise<User | null>;
  findByEmail(email: Email): Promise<User | null>;
  findAll(): Promise<User[]>;

  // Queries
  findActiveUsers(): Promise<User[]>;
  findByRole(role: UserRole): Promise<User[]>;

  // Identity
  nextId(): UserId;

  // Existence checks
  exists(id: UserId): Promise<boolean>;
  existsByEmail(email: Email): Promise<boolean>;
}
```

```typescript
// domain/repositories/OrderRepository.ts
import { Order } from '../entities/Order';
import { OrderId } from '../value-objects/OrderId';
import { CustomerId } from '../value-objects/CustomerId';
import { DateRange } from '../value-objects/DateRange';

export interface OrderRepository {
  save(order: Order): Promise<void>;
  findById(id: OrderId): Promise<Order | null>;
  findByCustomerId(customerId: CustomerId): Promise<Order[]>;
  findByStatus(status: OrderStatus): Promise<Order[]>;
  findByDateRange(range: DateRange): Promise<Order[]>;
  delete(id: OrderId): Promise<void>;
  nextId(): OrderId;
}
```

---

## Generic Repository Interface

For common operations, you can define a generic base interface:

```typescript
// domain/repositories/Repository.ts
export interface Repository<T, ID> {
  save(entity: T): Promise<void>;
  findById(id: ID): Promise<T | null>;
  findAll(): Promise<T[]>;
  delete(id: ID): Promise<void>;
  exists(id: ID): Promise<boolean>;
}

// Usage
export interface UserRepository extends Repository<User, UserId> {
  findByEmail(email: Email): Promise<User | null>;
  findActiveUsers(): Promise<User[]>;
}
```

---

## Repository Implementations

### PostgreSQL Implementation

```typescript
// infrastructure/repositories/PostgresUserRepository.ts
import { Pool } from 'pg';
import { UserRepository } from '../../domain/repositories/UserRepository';
import { User } from '../../domain/entities/User';
import { UserId } from '../../domain/value-objects/UserId';
import { Email } from '../../domain/value-objects/Email';

export class PostgresUserRepository implements UserRepository {
  constructor(private readonly pool: Pool) {}

  async save(user: User): Promise<void> {
    const query = `
      INSERT INTO users (id, email, password_hash, name, role, created_at, updated_at)
      VALUES ($1, $2, $3, $4, $5, $6, $7)
      ON CONFLICT (id) DO UPDATE SET
        email = $2,
        password_hash = $3,
        name = $4,
        role = $5,
        updated_at = $7
    `;

    await this.pool.query(query, [
      user.getId().getValue(),
      user.getEmail().getValue(),
      user.getPasswordHash().getValue(),
      user.getName(),
      user.getRole(),
      user.getCreatedAt(),
      new Date(),
    ]);
  }

  async findById(id: UserId): Promise<User | null> {
    const result = await this.pool.query(
      'SELECT * FROM users WHERE id = $1',
      [id.getValue()]
    );

    if (result.rows.length === 0) return null;
    return this.toDomain(result.rows[0]);
  }

  async findByEmail(email: Email): Promise<User | null> {
    const result = await this.pool.query(
      'SELECT * FROM users WHERE email = $1',
      [email.getValue()]
    );

    if (result.rows.length === 0) return null;
    return this.toDomain(result.rows[0]);
  }

  async findAll(): Promise<User[]> {
    const result = await this.pool.query('SELECT * FROM users ORDER BY created_at DESC');
    return result.rows.map(row => this.toDomain(row));
  }

  async findActiveUsers(): Promise<User[]> {
    const result = await this.pool.query(
      'SELECT * FROM users WHERE status = $1 ORDER BY created_at DESC',
      ['active']
    );
    return result.rows.map(row => this.toDomain(row));
  }

  async findByRole(role: UserRole): Promise<User[]> {
    const result = await this.pool.query(
      'SELECT * FROM users WHERE role = $1',
      [role]
    );
    return result.rows.map(row => this.toDomain(row));
  }

  async delete(id: UserId): Promise<void> {
    await this.pool.query('DELETE FROM users WHERE id = $1', [id.getValue()]);
  }

  async exists(id: UserId): Promise<boolean> {
    const result = await this.pool.query(
      'SELECT EXISTS(SELECT 1 FROM users WHERE id = $1)',
      [id.getValue()]
    );
    return result.rows[0].exists;
  }

  async existsByEmail(email: Email): Promise<boolean> {
    const result = await this.pool.query(
      'SELECT EXISTS(SELECT 1 FROM users WHERE email = $1)',
      [email.getValue()]
    );
    return result.rows[0].exists;
  }

  nextId(): UserId {
    return UserId.generate();
  }

  private toDomain(row: any): User {
    return User.reconstitute(
      UserId.fromString(row.id),
      new Email(row.email),
      new HashedPassword(row.password_hash),
      row.name,
      row.role as UserRole,
      row.created_at,
    );
  }
}
```

### MongoDB Implementation

```typescript
// infrastructure/repositories/MongoUserRepository.ts
import { Collection, Db } from 'mongodb';
import { UserRepository } from '../../domain/repositories/UserRepository';
import { User } from '../../domain/entities/User';
import { UserId } from '../../domain/value-objects/UserId';
import { Email } from '../../domain/value-objects/Email';

export class MongoUserRepository implements UserRepository {
  private collection: Collection;

  constructor(db: Db) {
    this.collection = db.collection('users');
  }

  async save(user: User): Promise<void> {
    const document = this.toDocument(user);
    await this.collection.updateOne(
      { _id: document._id },
      { $set: document },
      { upsert: true }
    );
  }

  async findById(id: UserId): Promise<User | null> {
    const doc = await this.collection.findOne({ _id: id.getValue() });
    return doc ? this.toDomain(doc) : null;
  }

  async findByEmail(email: Email): Promise<User | null> {
    const doc = await this.collection.findOne({ email: email.getValue() });
    return doc ? this.toDomain(doc) : null;
  }

  async findAll(): Promise<User[]> {
    const docs = await this.collection.find().sort({ createdAt: -1 }).toArray();
    return docs.map(doc => this.toDomain(doc));
  }

  async findActiveUsers(): Promise<User[]> {
    const docs = await this.collection.find({ status: 'active' }).toArray();
    return docs.map(doc => this.toDomain(doc));
  }

  async findByRole(role: UserRole): Promise<User[]> {
    const docs = await this.collection.find({ role }).toArray();
    return docs.map(doc => this.toDomain(doc));
  }

  async delete(id: UserId): Promise<void> {
    await this.collection.deleteOne({ _id: id.getValue() });
  }

  async exists(id: UserId): Promise<boolean> {
    const count = await this.collection.countDocuments({ _id: id.getValue() });
    return count > 0;
  }

  async existsByEmail(email: Email): Promise<boolean> {
    const count = await this.collection.countDocuments({ email: email.getValue() });
    return count > 0;
  }

  nextId(): UserId {
    return UserId.generate();
  }

  private toDocument(user: User): any {
    return {
      _id: user.getId().getValue(),
      email: user.getEmail().getValue(),
      passwordHash: user.getPasswordHash().getValue(),
      name: user.getName(),
      role: user.getRole(),
      createdAt: user.getCreatedAt(),
    };
  }

  private toDomain(doc: any): User {
    return User.reconstitute(
      UserId.fromString(doc._id),
      new Email(doc.email),
      new HashedPassword(doc.passwordHash),
      doc.name,
      doc.role as UserRole,
      doc.createdAt,
    );
  }
}
```

### In-Memory Implementation (Testing)

```typescript
// infrastructure/repositories/InMemoryUserRepository.ts
export class InMemoryUserRepository implements UserRepository {
  private users: Map<string, User> = new Map();
  private idCounter = 0;

  async save(user: User): Promise<void> {
    this.users.set(user.getId().getValue(), user);
  }

  async findById(id: UserId): Promise<User | null> {
    return this.users.get(id.getValue()) ?? null;
  }

  async findByEmail(email: Email): Promise<User | null> {
    return Array.from(this.users.values()).find(
      user => user.getEmail().equals(email)
    ) ?? null;
  }

  async findAll(): Promise<User[]> {
    return Array.from(this.users.values());
  }

  async findActiveUsers(): Promise<User[]> {
    return Array.from(this.users.values()).filter(
      user => user.getStatus() === 'active'
    );
  }

  async findByRole(role: UserRole): Promise<User[]> {
    return Array.from(this.users.values()).filter(
      user => user.getRole() === role
    );
  }

  async delete(id: UserId): Promise<void> {
    this.users.delete(id.getValue());
  }

  async exists(id: UserId): Promise<boolean> {
    return this.users.has(id.getValue());
  }

  async existsByEmail(email: Email): Promise<boolean> {
    return Array.from(this.users.values()).some(
      user => user.getEmail().equals(email)
    );
  }

  nextId(): UserId {
    this.idCounter++;
    return UserId.fromString(`user-${this.idCounter}`);
  }

  // Test helpers
  clear(): void {
    this.users.clear();
    this.idCounter = 0;
  }

  count(): number {
    return this.users.size;
  }
}
```

---

## Unit of Work Pattern

The Unit of Work pattern maintains a list of objects affected by a business transaction and coordinates the writing of changes.

```typescript
// domain/UnitOfWork.ts
export interface UnitOfWork {
  userRepository: UserRepository;
  orderRepository: OrderRepository;
  productRepository: ProductRepository;

  begin(): Promise<void>;
  commit(): Promise<void>;
  rollback(): Promise<void>;
}
```

```typescript
// infrastructure/PostgresUnitOfWork.ts
import { Pool, PoolClient } from 'pg';

export class PostgresUnitOfWork implements UnitOfWork {
  private client: PoolClient | null = null;

  public userRepository: UserRepository;
  public orderRepository: OrderRepository;
  public productRepository: ProductRepository;

  constructor(private readonly pool: Pool) {}

  async begin(): Promise<void> {
    this.client = await this.pool.connect();
    await this.client.query('BEGIN');

    // Create repositories with shared client
    this.userRepository = new PostgresUserRepository(this.client);
    this.orderRepository = new PostgresOrderRepository(this.client);
    this.productRepository = new PostgresProductRepository(this.client);
  }

  async commit(): Promise<void> {
    if (!this.client) throw new Error('Transaction not started');
    await this.client.query('COMMIT');
    this.client.release();
    this.client = null;
  }

  async rollback(): Promise<void> {
    if (!this.client) throw new Error('Transaction not started');
    await this.client.query('ROLLBACK');
    this.client.release();
    this.client = null;
  }
}
```

**Using Unit of Work:**

```typescript
// application/use-cases/PlaceOrder.ts
export class PlaceOrderUseCase {
  constructor(private readonly unitOfWork: UnitOfWork) {}

  async execute(command: PlaceOrderCommand): Promise<OrderDto> {
    await this.unitOfWork.begin();

    try {
      // All operations use the same transaction
      const customer = await this.unitOfWork.userRepository.findById(command.customerId);
      if (!customer) throw new CustomerNotFoundError();

      const order = Order.create(customer.getId(), command.items);

      // Update inventory
      for (const item of command.items) {
        const product = await this.unitOfWork.productRepository.findById(item.productId);
        product.decreaseStock(item.quantity);
        await this.unitOfWork.productRepository.save(product);
      }

      await this.unitOfWork.orderRepository.save(order);

      await this.unitOfWork.commit();

      return OrderMapper.toDto(order);
    } catch (error) {
      await this.unitOfWork.rollback();
      throw error;
    }
  }
}
```

---

## Specification Pattern

Use specifications for complex queries:

```typescript
// domain/specifications/Specification.ts
export interface Specification<T> {
  isSatisfiedBy(entity: T): boolean;
  toSqlWhere(): { sql: string; params: any[] };
}

export abstract class CompositeSpecification<T> implements Specification<T> {
  abstract isSatisfiedBy(entity: T): boolean;
  abstract toSqlWhere(): { sql: string; params: any[] };

  and(other: Specification<T>): Specification<T> {
    return new AndSpecification(this, other);
  }

  or(other: Specification<T>): Specification<T> {
    return new OrSpecification(this, other);
  }

  not(): Specification<T> {
    return new NotSpecification(this);
  }
}
```

```typescript
// domain/specifications/user/ActiveUserSpecification.ts
export class ActiveUserSpecification extends CompositeSpecification<User> {
  isSatisfiedBy(user: User): boolean {
    return user.getStatus() === UserStatus.ACTIVE;
  }

  toSqlWhere(): { sql: string; params: any[] } {
    return { sql: 'status = $1', params: ['active'] };
  }
}

// domain/specifications/user/AdminUserSpecification.ts
export class AdminUserSpecification extends CompositeSpecification<User> {
  isSatisfiedBy(user: User): boolean {
    return user.getRole() === UserRole.ADMIN;
  }

  toSqlWhere(): { sql: string; params: any[] } {
    return { sql: 'role = $1', params: ['admin'] };
  }
}
```

```typescript
// Extended Repository Interface
export interface UserRepository {
  // ... other methods
  findBySpecification(spec: Specification<User>): Promise<User[]>;
}

// Implementation
async findBySpecification(spec: Specification<User>): Promise<User[]> {
  const { sql, params } = spec.toSqlWhere();
  const result = await this.pool.query(
    `SELECT * FROM users WHERE ${sql}`,
    params
  );
  return result.rows.map(row => this.toDomain(row));
}

// Usage
const activeAdmins = await userRepository.findBySpecification(
  new ActiveUserSpecification().and(new AdminUserSpecification())
);
```

---

## Best Practices

### 1. One Repository Per Aggregate

```typescript
// ✅ Good: Repository for aggregate root
interface OrderRepository {
  save(order: Order): Promise<void>;  // Saves Order with its OrderItems
  findById(id: OrderId): Promise<Order | null>;
}

// ❌ Bad: Separate repository for aggregate parts
interface OrderItemRepository {
  save(item: OrderItem): Promise<void>;
}
```

### 2. Return Domain Objects, Not Primitives

```typescript
// ✅ Good
findById(id: UserId): Promise<User | null>;

// ❌ Bad
findById(id: string): Promise<any>;
```

### 3. Define Interfaces in Domain Layer

```typescript
// domain/repositories/UserRepository.ts  ✅
export interface UserRepository { ... }

// infrastructure/repositories/UserRepository.ts  ❌
```

### 4. Keep Repository Methods Focused

```typescript
// ✅ Good: Focused methods
findActiveByRole(role: UserRole): Promise<User[]>;

// ❌ Bad: Generic with too many options
findAll(options: {
  status?: string;
  role?: string;
  sort?: string;
  page?: number;
  limit?: number;
}): Promise<User[]>;
```

---

## Summary

| Aspect | Description |
|--------|-------------|
| **Purpose** | Abstract data access from domain logic |
| **Interface Location** | Domain layer |
| **Implementation Location** | Infrastructure layer |
| **Returns** | Domain objects (entities, aggregates) |
| **Responsibility** | CRUD + domain-specific queries |
| **Testing** | Use in-memory implementations |
| **Transactions** | Use Unit of Work for multi-aggregate operations |
