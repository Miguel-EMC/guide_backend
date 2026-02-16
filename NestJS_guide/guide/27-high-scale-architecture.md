# High-Scale Architecture

Scaling a NestJS system often requires more than vertical scaling. This chapter focuses on architectural patterns for high traffic and high reliability, with practical implementations of CQRS, event sourcing, and distributed patterns.

## Goals

- Decompose the system safely
- Improve read and write scalability
- Implement CQRS with NestJS
- Use the outbox pattern for reliable events
- Design event-driven architectures

## Install Dependencies

```bash
npm install @nestjs/cqrs
npm install @nestjs/event-emitter
```

## Read Scaling

- Use read replicas for heavy read workloads.
- Add caches at the service and edge layers.
- Introduce search engines for complex queries.

### Read Replica Configuration

```typescript
// src/database/database.module.ts
import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ConfigService } from '@nestjs/config';

@Module({
  imports: [
    // Primary (write) connection
    TypeOrmModule.forRootAsync({
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        type: 'postgres',
        host: config.get('DB_PRIMARY_HOST'),
        port: config.get('DB_PORT'),
        username: config.get('DB_USERNAME'),
        password: config.get('DB_PASSWORD'),
        database: config.get('DB_NAME'),
        entities: [__dirname + '/../**/*.entity{.ts,.js}'],
        synchronize: false,
      }),
    }),
    // Read replica connection
    TypeOrmModule.forRootAsync({
      name: 'replica',
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        type: 'postgres',
        host: config.get('DB_REPLICA_HOST'),
        port: config.get('DB_PORT'),
        username: config.get('DB_USERNAME'),
        password: config.get('DB_PASSWORD'),
        database: config.get('DB_NAME'),
        entities: [__dirname + '/../**/*.entity{.ts,.js}'],
        synchronize: false,
      }),
    }),
  ],
})
export class DatabaseModule {}
```

### Using Replicas

```typescript
// src/posts/posts.service.ts
import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Post } from './entities/post.entity';

@Injectable()
export class PostsService {
  constructor(
    @InjectRepository(Post)
    private readonly postRepository: Repository<Post>,
    @InjectRepository(Post, 'replica')
    private readonly postReplicaRepository: Repository<Post>,
  ) {}

  // Writes go to primary
  async create(data: CreatePostDto) {
    return this.postRepository.save(data);
  }

  // Reads go to replica
  async findAll() {
    return this.postReplicaRepository.find();
  }

  async findOne(id: number) {
    return this.postReplicaRepository.findOne({ where: { id } });
  }
}
```

## Write Scaling

- Partition data by tenant or region.
- Use queues to offload slow writes.
- Apply idempotency for retried requests.

### Idempotency Implementation

```typescript
// src/common/decorators/idempotent.decorator.ts
import { SetMetadata } from '@nestjs/common';

export const IDEMPOTENCY_KEY = 'idempotency';
export const Idempotent = () => SetMetadata(IDEMPOTENCY_KEY, true);
```

```typescript
// src/common/interceptors/idempotency.interceptor.ts
import {
  Injectable,
  NestInterceptor,
  ExecutionContext,
  CallHandler,
  ConflictException,
} from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { Observable, of } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Redis } from 'ioredis';
import { IDEMPOTENCY_KEY } from '../decorators/idempotent.decorator';

@Injectable()
export class IdempotencyInterceptor implements NestInterceptor {
  constructor(
    private readonly reflector: Reflector,
    private readonly redis: Redis,
  ) {}

  async intercept(
    context: ExecutionContext,
    next: CallHandler,
  ): Promise<Observable<any>> {
    const isIdempotent = this.reflector.get<boolean>(
      IDEMPOTENCY_KEY,
      context.getHandler(),
    );

    if (!isIdempotent) {
      return next.handle();
    }

    const request = context.switchToHttp().getRequest();
    const idempotencyKey = request.headers['idempotency-key'];

    if (!idempotencyKey) {
      return next.handle();
    }

    const cacheKey = `idempotency:${idempotencyKey}`;
    const cached = await this.redis.get(cacheKey);

    if (cached) {
      const result = JSON.parse(cached);
      if (result.status === 'processing') {
        throw new ConflictException('Request is being processed');
      }
      return of(result.data);
    }

    // Mark as processing
    await this.redis.set(cacheKey, JSON.stringify({ status: 'processing' }), 'EX', 300);

    return next.handle().pipe(
      tap(async (data) => {
        await this.redis.set(
          cacheKey,
          JSON.stringify({ status: 'completed', data }),
          'EX', 86400, // 24 hours
        );
      }),
    );
  }
}
```

## CQRS (Command Query Responsibility Segregation)

Separate read models from write models for independent scaling and optimized query performance.

### Module Setup

```typescript
// src/orders/orders.module.ts
import { Module } from '@nestjs/common';
import { CqrsModule } from '@nestjs/cqrs';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Order } from './entities/order.entity';
import { OrderReadModel } from './entities/order-read-model.entity';
import { OrdersController } from './orders.controller';
import { CreateOrderHandler } from './commands/handlers/create-order.handler';
import { GetOrderHandler } from './queries/handlers/get-order.handler';
import { OrderCreatedHandler } from './events/handlers/order-created.handler';

const CommandHandlers = [CreateOrderHandler];
const QueryHandlers = [GetOrderHandler];
const EventHandlers = [OrderCreatedHandler];

@Module({
  imports: [
    CqrsModule,
    TypeOrmModule.forFeature([Order, OrderReadModel]),
  ],
  controllers: [OrdersController],
  providers: [...CommandHandlers, ...QueryHandlers, ...EventHandlers],
})
export class OrdersModule {}
```

### Commands

```typescript
// src/orders/commands/impl/create-order.command.ts
export class CreateOrderCommand {
  constructor(
    public readonly userId: number,
    public readonly items: { productId: number; quantity: number }[],
    public readonly shippingAddress: string,
  ) {}
}
```

```typescript
// src/orders/commands/handlers/create-order.handler.ts
import { CommandHandler, ICommandHandler, EventBus } from '@nestjs/cqrs';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { CreateOrderCommand } from '../impl/create-order.command';
import { Order, OrderStatus } from '../../entities/order.entity';
import { OrderCreatedEvent } from '../../events/impl/order-created.event';

@CommandHandler(CreateOrderCommand)
export class CreateOrderHandler implements ICommandHandler<CreateOrderCommand> {
  constructor(
    @InjectRepository(Order)
    private readonly orderRepository: Repository<Order>,
    private readonly eventBus: EventBus,
  ) {}

  async execute(command: CreateOrderCommand): Promise<Order> {
    const { userId, items, shippingAddress } = command;

    // Calculate total
    const total = await this.calculateTotal(items);

    // Create order
    const order = this.orderRepository.create({
      userId,
      items,
      shippingAddress,
      total,
      status: OrderStatus.PENDING,
    });

    const savedOrder = await this.orderRepository.save(order);

    // Publish event
    this.eventBus.publish(
      new OrderCreatedEvent(
        savedOrder.id,
        savedOrder.userId,
        savedOrder.total,
        savedOrder.items,
      ),
    );

    return savedOrder;
  }

  private async calculateTotal(items: { productId: number; quantity: number }[]) {
    // Calculate total from product prices
    return items.reduce((sum, item) => sum + item.quantity * 100, 0);
  }
}
```

### Queries

```typescript
// src/orders/queries/impl/get-order.query.ts
export class GetOrderQuery {
  constructor(public readonly orderId: number) {}
}
```

```typescript
// src/orders/queries/impl/get-orders-by-user.query.ts
export class GetOrdersByUserQuery {
  constructor(
    public readonly userId: number,
    public readonly page: number = 1,
    public readonly limit: number = 10,
  ) {}
}
```

```typescript
// src/orders/queries/handlers/get-order.handler.ts
import { IQueryHandler, QueryHandler } from '@nestjs/cqrs';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { GetOrderQuery } from '../impl/get-order.query';
import { OrderReadModel } from '../../entities/order-read-model.entity';

@QueryHandler(GetOrderQuery)
export class GetOrderHandler implements IQueryHandler<GetOrderQuery> {
  constructor(
    @InjectRepository(OrderReadModel)
    private readonly orderReadRepository: Repository<OrderReadModel>,
  ) {}

  async execute(query: GetOrderQuery): Promise<OrderReadModel | null> {
    return this.orderReadRepository.findOne({
      where: { id: query.orderId },
    });
  }
}
```

### Events

```typescript
// src/orders/events/impl/order-created.event.ts
export class OrderCreatedEvent {
  constructor(
    public readonly orderId: number,
    public readonly userId: number,
    public readonly total: number,
    public readonly items: { productId: number; quantity: number }[],
  ) {}
}
```

```typescript
// src/orders/events/handlers/order-created.handler.ts
import { EventsHandler, IEventHandler } from '@nestjs/cqrs';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { OrderCreatedEvent } from '../impl/order-created.event';
import { OrderReadModel } from '../../entities/order-read-model.entity';

@EventsHandler(OrderCreatedEvent)
export class OrderCreatedHandler implements IEventHandler<OrderCreatedEvent> {
  constructor(
    @InjectRepository(OrderReadModel)
    private readonly orderReadRepository: Repository<OrderReadModel>,
  ) {}

  async handle(event: OrderCreatedEvent) {
    // Update read model (denormalized for fast queries)
    const readModel = this.orderReadRepository.create({
      id: event.orderId,
      userId: event.userId,
      total: event.total,
      itemCount: event.items.reduce((sum, i) => sum + i.quantity, 0),
      status: 'pending',
      createdAt: new Date(),
    });

    await this.orderReadRepository.save(readModel);
  }
}
```

### Read Model Entity

```typescript
// src/orders/entities/order-read-model.entity.ts
import { Entity, Column, PrimaryColumn, Index } from 'typeorm';

@Entity('order_read_models')
export class OrderReadModel {
  @PrimaryColumn()
  id: number;

  @Column()
  @Index()
  userId: number;

  @Column('decimal', { precision: 10, scale: 2 })
  total: number;

  @Column()
  itemCount: number;

  @Column()
  status: string;

  @Column({ nullable: true })
  customerName?: string;

  @Column({ nullable: true })
  customerEmail?: string;

  @Column()
  createdAt: Date;

  @Column({ nullable: true })
  completedAt?: Date;
}
```

### Controller with CQRS

```typescript
// src/orders/orders.controller.ts
import { Controller, Get, Post, Body, Param, Query } from '@nestjs/common';
import { CommandBus, QueryBus } from '@nestjs/cqrs';
import { CreateOrderCommand } from './commands/impl/create-order.command';
import { GetOrderQuery } from './queries/impl/get-order.query';
import { GetOrdersByUserQuery } from './queries/impl/get-orders-by-user.query';
import { CreateOrderDto } from './dto/create-order.dto';

@Controller('orders')
export class OrdersController {
  constructor(
    private readonly commandBus: CommandBus,
    private readonly queryBus: QueryBus,
  ) {}

  @Post()
  async create(@Body() dto: CreateOrderDto) {
    return this.commandBus.execute(
      new CreateOrderCommand(dto.userId, dto.items, dto.shippingAddress),
    );
  }

  @Get(':id')
  async findOne(@Param('id') id: string) {
    return this.queryBus.execute(new GetOrderQuery(+id));
  }

  @Get()
  async findByUser(
    @Query('userId') userId: string,
    @Query('page') page = '1',
    @Query('limit') limit = '10',
  ) {
    return this.queryBus.execute(
      new GetOrdersByUserQuery(+userId, +page, +limit),
    );
  }
}
```

## Outbox Pattern

Guarantee event delivery with transactional outbox.

### Outbox Entity

```typescript
// src/common/entities/outbox.entity.ts
import { Entity, Column, PrimaryGeneratedColumn, CreateDateColumn, Index } from 'typeorm';

@Entity('outbox')
export class OutboxMessage {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column()
  @Index()
  aggregateType: string;

  @Column()
  aggregateId: string;

  @Column()
  eventType: string;

  @Column('jsonb')
  payload: Record<string, any>;

  @CreateDateColumn()
  createdAt: Date;

  @Column({ default: false })
  @Index()
  processed: boolean;

  @Column({ nullable: true })
  processedAt?: Date;

  @Column({ default: 0 })
  retryCount: number;
}
```

### Outbox Service

```typescript
// src/common/outbox/outbox.service.ts
import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, DataSource } from 'typeorm';
import { OutboxMessage } from '../entities/outbox.entity';

@Injectable()
export class OutboxService {
  constructor(
    @InjectRepository(OutboxMessage)
    private readonly outboxRepository: Repository<OutboxMessage>,
    private readonly dataSource: DataSource,
  ) {}

  async executeWithOutbox<T>(
    operation: (queryRunner: any) => Promise<T>,
    event: {
      aggregateType: string;
      aggregateId: string;
      eventType: string;
      payload: Record<string, any>;
    },
  ): Promise<T> {
    const queryRunner = this.dataSource.createQueryRunner();
    await queryRunner.connect();
    await queryRunner.startTransaction();

    try {
      // Execute business logic
      const result = await operation(queryRunner);

      // Insert outbox message in same transaction
      const outboxMessage = queryRunner.manager.create(OutboxMessage, {
        aggregateType: event.aggregateType,
        aggregateId: event.aggregateId,
        eventType: event.eventType,
        payload: event.payload,
      });
      await queryRunner.manager.save(outboxMessage);

      await queryRunner.commitTransaction();
      return result;
    } catch (error) {
      await queryRunner.rollbackTransaction();
      throw error;
    } finally {
      await queryRunner.release();
    }
  }
}
```

### Outbox Processor

```typescript
// src/common/outbox/outbox.processor.ts
import { Injectable, Logger } from '@nestjs/common';
import { Cron, CronExpression } from '@nestjs/schedule';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, LessThan } from 'typeorm';
import { OutboxMessage } from '../entities/outbox.entity';
import { EventPublisher } from './event-publisher.service';

@Injectable()
export class OutboxProcessor {
  private readonly logger = new Logger(OutboxProcessor.name);
  private isProcessing = false;

  constructor(
    @InjectRepository(OutboxMessage)
    private readonly outboxRepository: Repository<OutboxMessage>,
    private readonly eventPublisher: EventPublisher,
  ) {}

  @Cron(CronExpression.EVERY_5_SECONDS)
  async processOutbox() {
    if (this.isProcessing) return;
    this.isProcessing = true;

    try {
      const messages = await this.outboxRepository.find({
        where: {
          processed: false,
          retryCount: LessThan(5),
        },
        order: { createdAt: 'ASC' },
        take: 100,
      });

      for (const message of messages) {
        try {
          await this.eventPublisher.publish(message.eventType, message.payload);

          message.processed = true;
          message.processedAt = new Date();
          await this.outboxRepository.save(message);

          this.logger.log(`Processed outbox message ${message.id}`);
        } catch (error) {
          message.retryCount += 1;
          await this.outboxRepository.save(message);
          this.logger.error(`Failed to process ${message.id}: ${error.message}`);
        }
      }
    } finally {
      this.isProcessing = false;
    }
  }
}
```

### Using Outbox in Service

```typescript
// src/orders/orders.service.ts
@Injectable()
export class OrdersService {
  constructor(private readonly outboxService: OutboxService) {}

  async createOrder(dto: CreateOrderDto) {
    return this.outboxService.executeWithOutbox(
      async (queryRunner) => {
        const order = queryRunner.manager.create(Order, {
          userId: dto.userId,
          items: dto.items,
          status: 'pending',
        });
        return queryRunner.manager.save(order);
      },
      {
        aggregateType: 'Order',
        aggregateId: 'new', // Will be updated after save
        eventType: 'OrderCreated',
        payload: { userId: dto.userId, items: dto.items },
      },
    );
  }
}
```

## Event Sourcing (Basic)

Store all state changes as events.

### Event Store Entity

```typescript
// src/common/event-store/event-store.entity.ts
import { Entity, Column, PrimaryGeneratedColumn, CreateDateColumn, Index } from 'typeorm';

@Entity('event_store')
export class StoredEvent {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column()
  @Index()
  aggregateId: string;

  @Column()
  @Index()
  aggregateType: string;

  @Column()
  eventType: string;

  @Column('jsonb')
  payload: Record<string, any>;

  @Column()
  version: number;

  @CreateDateColumn()
  occurredAt: Date;

  @Column({ nullable: true })
  metadata?: string;
}
```

### Event Store Service

```typescript
// src/common/event-store/event-store.service.ts
import { Injectable, ConflictException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { StoredEvent } from './event-store.entity';

@Injectable()
export class EventStoreService {
  constructor(
    @InjectRepository(StoredEvent)
    private readonly eventRepository: Repository<StoredEvent>,
  ) {}

  async appendEvents(
    aggregateId: string,
    aggregateType: string,
    events: { eventType: string; payload: Record<string, any> }[],
    expectedVersion: number,
  ): Promise<void> {
    // Optimistic concurrency check
    const currentVersion = await this.getAggregateVersion(aggregateId);
    if (currentVersion !== expectedVersion) {
      throw new ConflictException(
        `Concurrency conflict: expected version ${expectedVersion}, got ${currentVersion}`,
      );
    }

    const storedEvents = events.map((event, index) =>
      this.eventRepository.create({
        aggregateId,
        aggregateType,
        eventType: event.eventType,
        payload: event.payload,
        version: expectedVersion + index + 1,
      }),
    );

    await this.eventRepository.save(storedEvents);
  }

  async getEvents(aggregateId: string): Promise<StoredEvent[]> {
    return this.eventRepository.find({
      where: { aggregateId },
      order: { version: 'ASC' },
    });
  }

  async getAggregateVersion(aggregateId: string): Promise<number> {
    const result = await this.eventRepository
      .createQueryBuilder('event')
      .select('MAX(event.version)', 'version')
      .where('event.aggregateId = :aggregateId', { aggregateId })
      .getRawOne();

    return result?.version ?? 0;
  }
}
```

### Aggregate Base

```typescript
// src/common/event-store/aggregate.base.ts
export abstract class AggregateRoot {
  private _version = 0;
  private _uncommittedEvents: { eventType: string; payload: any }[] = [];

  get version(): number {
    return this._version;
  }

  get uncommittedEvents() {
    return [...this._uncommittedEvents];
  }

  protected apply(eventType: string, payload: any) {
    this.when(eventType, payload);
    this._uncommittedEvents.push({ eventType, payload });
  }

  loadFromHistory(events: { eventType: string; payload: any; version: number }[]) {
    for (const event of events) {
      this.when(event.eventType, event.payload);
      this._version = event.version;
    }
  }

  clearUncommittedEvents() {
    this._uncommittedEvents = [];
  }

  protected abstract when(eventType: string, payload: any): void;
}
```

## Service Boundaries

- Keep each service focused on a single domain.
- Avoid shared databases across services.
- Define stable APIs and event contracts.

### Anti-Corruption Layer

```typescript
// src/common/acl/external-payment.adapter.ts
import { Injectable } from '@nestjs/common';
import { HttpService } from '@nestjs/axios';
import { firstValueFrom } from 'rxjs';

interface ExternalPaymentResponse {
  transaction_id: string;
  status_code: string;
  amount_cents: number;
}

interface PaymentResult {
  transactionId: string;
  status: 'success' | 'failed' | 'pending';
  amount: number;
}

@Injectable()
export class PaymentAdapter {
  constructor(private readonly httpService: HttpService) {}

  async processPayment(orderId: string, amount: number): Promise<PaymentResult> {
    const response = await firstValueFrom(
      this.httpService.post<ExternalPaymentResponse>('/external/payments', {
        order_reference: orderId,
        amount_in_cents: Math.round(amount * 100),
      }),
    );

    // Translate external format to internal domain model
    return {
      transactionId: response.data.transaction_id,
      status: this.mapStatus(response.data.status_code),
      amount: response.data.amount_cents / 100,
    };
  }

  private mapStatus(externalStatus: string): 'success' | 'failed' | 'pending' {
    const statusMap: Record<string, 'success' | 'failed' | 'pending'> = {
      'COMPLETED': 'success',
      'APPROVED': 'success',
      'DECLINED': 'failed',
      'ERROR': 'failed',
      'PROCESSING': 'pending',
      'PENDING': 'pending',
    };
    return statusMap[externalStatus] ?? 'pending';
  }
}
```

## Tips

- Measure before you split services.
- Start with a modular monolith and extract services when necessary.
- Always define ownership of data and events.
- Use eventual consistency where appropriate.
- Implement idempotency for all write operations.
- Consider saga pattern for distributed transactions.
- Monitor event processing lag and failures.

---

[Previous: Security Hardening](./26-security-hardening.md) | [Back to Index](./README.md) | [Next: Scaling and Cost Optimization ->](./28-scaling-cost-optimization.md)
