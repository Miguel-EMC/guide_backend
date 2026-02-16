# Microservices

NestJS provides first-class support for microservices architecture through the `@nestjs/microservices` package. This chapter covers transport layers, messaging patterns, and service communication.

## Goals

- Understand microservices in NestJS
- Implement different transport layers
- Build event-driven communication
- Create hybrid applications

## Install Dependencies

```bash
npm install @nestjs/microservices
```

For specific transports:

```bash
# Redis
npm install ioredis

# RabbitMQ
npm install amqplib amqp-connection-manager

# Kafka
npm install kafkajs

# gRPC
npm install @grpc/grpc-js @grpc/proto-loader

# NATS
npm install nats
```

## Transport Layers Overview

| Transport | Use Case | Protocol |
|-----------|----------|----------|
| TCP | Internal services, low latency | Binary |
| Redis | Pub/sub, simple queues | Redis protocol |
| RabbitMQ | Complex routing, reliability | AMQP |
| Kafka | High throughput, event streaming | Kafka protocol |
| gRPC | Typed contracts, streaming | HTTP/2 + Protobuf |
| NATS | Lightweight, cloud-native | NATS protocol |

## TCP Transport (Default)

### Microservice Setup

```typescript
// apps/orders-service/src/main.ts
import { NestFactory } from '@nestjs/core';
import { MicroserviceOptions, Transport } from '@nestjs/microservices';
import { OrdersModule } from './orders.module';

async function bootstrap() {
  const app = await NestFactory.createMicroservice<MicroserviceOptions>(
    OrdersModule,
    {
      transport: Transport.TCP,
      options: {
        host: '0.0.0.0',
        port: 3001,
      },
    },
  );
  await app.listen();
  console.log('Orders microservice is running on port 3001');
}
bootstrap();
```

### Message Patterns

```typescript
// apps/orders-service/src/orders.controller.ts
import { Controller } from '@nestjs/common';
import { MessagePattern, Payload, EventPattern } from '@nestjs/microservices';
import { OrdersService } from './orders.service';
import { CreateOrderDto } from './dto/create-order.dto';

@Controller()
export class OrdersController {
  constructor(private readonly ordersService: OrdersService) {}

  // Request-Response pattern
  @MessagePattern({ cmd: 'create_order' })
  async createOrder(@Payload() data: CreateOrderDto) {
    return this.ordersService.create(data);
  }

  @MessagePattern({ cmd: 'get_order' })
  async getOrder(@Payload() data: { id: number }) {
    return this.ordersService.findOne(data.id);
  }

  @MessagePattern({ cmd: 'list_orders' })
  async listOrders(@Payload() data: { userId: number }) {
    return this.ordersService.findByUser(data.userId);
  }

  // Event-based pattern (fire and forget)
  @EventPattern('order_paid')
  async handleOrderPaid(@Payload() data: { orderId: number; amount: number }) {
    await this.ordersService.markAsPaid(data.orderId);
  }
}
```

### Client Connection

```typescript
// apps/api-gateway/src/orders/orders.module.ts
import { Module } from '@nestjs/common';
import { ClientsModule, Transport } from '@nestjs/microservices';
import { OrdersController } from './orders.controller';

@Module({
  imports: [
    ClientsModule.register([
      {
        name: 'ORDERS_SERVICE',
        transport: Transport.TCP,
        options: {
          host: process.env.ORDERS_SERVICE_HOST ?? 'localhost',
          port: parseInt(process.env.ORDERS_SERVICE_PORT ?? '3001'),
        },
      },
    ]),
  ],
  controllers: [OrdersController],
})
export class OrdersModule {}
```

### Calling the Microservice

```typescript
// apps/api-gateway/src/orders/orders.controller.ts
import { Controller, Get, Post, Body, Param, Inject } from '@nestjs/common';
import { ClientProxy } from '@nestjs/microservices';
import { firstValueFrom } from 'rxjs';
import { CreateOrderDto } from './dto/create-order.dto';

@Controller('orders')
export class OrdersController {
  constructor(
    @Inject('ORDERS_SERVICE') private readonly ordersClient: ClientProxy,
  ) {}

  @Post()
  async create(@Body() dto: CreateOrderDto) {
    // Request-response: wait for reply
    return firstValueFrom(
      this.ordersClient.send({ cmd: 'create_order' }, dto),
    );
  }

  @Get(':id')
  async findOne(@Param('id') id: string) {
    return firstValueFrom(
      this.ordersClient.send({ cmd: 'get_order' }, { id: +id }),
    );
  }

  @Post(':id/pay')
  async pay(@Param('id') id: string, @Body() body: { amount: number }) {
    // Event: fire and forget
    this.ordersClient.emit('order_paid', { orderId: +id, amount: body.amount });
    return { status: 'payment_initiated' };
  }
}
```

## Redis Transport

Redis provides pub/sub messaging with simple setup.

### Microservice

```typescript
// main.ts
import { NestFactory } from '@nestjs/core';
import { MicroserviceOptions, Transport } from '@nestjs/microservices';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.createMicroservice<MicroserviceOptions>(
    AppModule,
    {
      transport: Transport.REDIS,
      options: {
        host: process.env.REDIS_HOST ?? 'localhost',
        port: parseInt(process.env.REDIS_PORT ?? '6379'),
        password: process.env.REDIS_PASSWORD,
      },
    },
  );
  await app.listen();
}
bootstrap();
```

### Client

```typescript
ClientsModule.register([
  {
    name: 'NOTIFICATIONS_SERVICE',
    transport: Transport.REDIS,
    options: {
      host: process.env.REDIS_HOST ?? 'localhost',
      port: parseInt(process.env.REDIS_PORT ?? '6379'),
      password: process.env.REDIS_PASSWORD,
    },
  },
]),
```

## RabbitMQ Transport

RabbitMQ provides reliable messaging with advanced routing.

### Microservice

```typescript
import { NestFactory } from '@nestjs/core';
import { MicroserviceOptions, Transport } from '@nestjs/microservices';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.createMicroservice<MicroserviceOptions>(
    AppModule,
    {
      transport: Transport.RMQ,
      options: {
        urls: [process.env.RABBITMQ_URL ?? 'amqp://localhost:5672'],
        queue: 'orders_queue',
        queueOptions: {
          durable: true,
        },
        prefetchCount: 10,
        noAck: false,
      },
    },
  );
  await app.listen();
}
bootstrap();
```

### Acknowledging Messages

```typescript
import { Controller } from '@nestjs/common';
import { Ctx, MessagePattern, Payload, RmqContext } from '@nestjs/microservices';

@Controller()
export class OrdersController {
  @MessagePattern('process_order')
  async processOrder(
    @Payload() data: { orderId: number },
    @Ctx() context: RmqContext,
  ) {
    const channel = context.getChannelRef();
    const originalMsg = context.getMessage();

    try {
      await this.ordersService.process(data.orderId);
      // Acknowledge success
      channel.ack(originalMsg);
      return { success: true };
    } catch (error) {
      // Reject and requeue
      channel.nack(originalMsg, false, true);
      throw error;
    }
  }
}
```

### Client

```typescript
ClientsModule.register([
  {
    name: 'ORDERS_SERVICE',
    transport: Transport.RMQ,
    options: {
      urls: [process.env.RABBITMQ_URL ?? 'amqp://localhost:5672'],
      queue: 'orders_queue',
      queueOptions: {
        durable: true,
      },
    },
  },
]),
```

## Kafka Transport

Kafka is ideal for high-throughput event streaming.

### Microservice

```typescript
import { NestFactory } from '@nestjs/core';
import { MicroserviceOptions, Transport } from '@nestjs/microservices';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.createMicroservice<MicroserviceOptions>(
    AppModule,
    {
      transport: Transport.KAFKA,
      options: {
        client: {
          clientId: 'orders-service',
          brokers: (process.env.KAFKA_BROKERS ?? 'localhost:9092').split(','),
        },
        consumer: {
          groupId: 'orders-consumer-group',
        },
      },
    },
  );
  await app.listen();
}
bootstrap();
```

### Kafka Controller

```typescript
import { Controller } from '@nestjs/common';
import { MessagePattern, Payload, Ctx, KafkaContext } from '@nestjs/microservices';

@Controller()
export class OrdersController {
  @MessagePattern('orders.created')
  async handleOrderCreated(
    @Payload() data: { orderId: number; userId: number },
    @Ctx() context: KafkaContext,
  ) {
    const topic = context.getTopic();
    const partition = context.getPartition();
    const offset = context.getMessage().offset;

    console.log(`Processing ${topic}[${partition}] offset ${offset}`);

    return this.ordersService.process(data);
  }
}
```

### Kafka Client

```typescript
ClientsModule.register([
  {
    name: 'ORDERS_SERVICE',
    transport: Transport.KAFKA,
    options: {
      client: {
        clientId: 'api-gateway',
        brokers: (process.env.KAFKA_BROKERS ?? 'localhost:9092').split(','),
      },
      producer: {
        allowAutoTopicCreation: true,
      },
    },
  },
]),
```

## gRPC Transport

gRPC provides strongly-typed contracts with Protocol Buffers.

### Proto Definition

```protobuf
// proto/orders.proto
syntax = "proto3";

package orders;

service OrdersService {
  rpc CreateOrder (CreateOrderRequest) returns (Order);
  rpc GetOrder (GetOrderRequest) returns (Order);
  rpc ListOrders (ListOrdersRequest) returns (OrderList);
  rpc StreamOrders (ListOrdersRequest) returns (stream Order);
}

message CreateOrderRequest {
  int32 user_id = 1;
  repeated OrderItem items = 2;
}

message OrderItem {
  int32 product_id = 1;
  int32 quantity = 2;
}

message GetOrderRequest {
  int32 id = 1;
}

message ListOrdersRequest {
  int32 user_id = 1;
}

message Order {
  int32 id = 1;
  int32 user_id = 2;
  string status = 3;
  double total = 4;
  string created_at = 5;
}

message OrderList {
  repeated Order orders = 1;
}
```

### gRPC Microservice

```typescript
// main.ts
import { NestFactory } from '@nestjs/core';
import { MicroserviceOptions, Transport } from '@nestjs/microservices';
import { join } from 'path';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.createMicroservice<MicroserviceOptions>(
    AppModule,
    {
      transport: Transport.GRPC,
      options: {
        package: 'orders',
        protoPath: join(__dirname, './proto/orders.proto'),
        url: '0.0.0.0:5001',
      },
    },
  );
  await app.listen();
}
bootstrap();
```

### gRPC Controller

```typescript
import { Controller } from '@nestjs/common';
import { GrpcMethod, GrpcStreamMethod } from '@nestjs/microservices';
import { Observable, Subject } from 'rxjs';

interface Order {
  id: number;
  userId: number;
  status: string;
  total: number;
  createdAt: string;
}

@Controller()
export class OrdersController {
  @GrpcMethod('OrdersService', 'CreateOrder')
  createOrder(data: { userId: number; items: any[] }): Order {
    return this.ordersService.create(data);
  }

  @GrpcMethod('OrdersService', 'GetOrder')
  getOrder(data: { id: number }): Order {
    return this.ordersService.findOne(data.id);
  }

  @GrpcStreamMethod('OrdersService', 'StreamOrders')
  streamOrders(data: { userId: number }): Observable<Order> {
    const subject = new Subject<Order>();

    // Stream orders one by one
    this.ordersService.findByUser(data.userId).then((orders) => {
      for (const order of orders) {
        subject.next(order);
      }
      subject.complete();
    });

    return subject.asObservable();
  }
}
```

### gRPC Client

```typescript
// orders.module.ts
import { Module } from '@nestjs/common';
import { ClientsModule, Transport } from '@nestjs/microservices';
import { join } from 'path';

@Module({
  imports: [
    ClientsModule.register([
      {
        name: 'ORDERS_PACKAGE',
        transport: Transport.GRPC,
        options: {
          package: 'orders',
          protoPath: join(__dirname, './proto/orders.proto'),
          url: process.env.ORDERS_GRPC_URL ?? 'localhost:5001',
        },
      },
    ]),
  ],
})
export class OrdersModule {}
```

### Using gRPC Client

```typescript
import { Controller, OnModuleInit, Inject, Get, Param } from '@nestjs/common';
import { ClientGrpc } from '@nestjs/microservices';
import { Observable } from 'rxjs';

interface OrdersGrpcService {
  getOrder(data: { id: number }): Observable<Order>;
  listOrders(data: { userId: number }): Observable<OrderList>;
}

@Controller('orders')
export class OrdersController implements OnModuleInit {
  private ordersService: OrdersGrpcService;

  constructor(@Inject('ORDERS_PACKAGE') private client: ClientGrpc) {}

  onModuleInit() {
    this.ordersService = this.client.getService<OrdersGrpcService>('OrdersService');
  }

  @Get(':id')
  getOrder(@Param('id') id: string): Observable<Order> {
    return this.ordersService.getOrder({ id: +id });
  }
}
```

## Hybrid Applications

Combine HTTP API with microservices in a single application.

```typescript
// main.ts
import { NestFactory } from '@nestjs/core';
import { MicroserviceOptions, Transport } from '@nestjs/microservices';
import { AppModule } from './app.module';

async function bootstrap() {
  // Create HTTP application
  const app = await NestFactory.create(AppModule);

  // Connect microservice transport
  app.connectMicroservice<MicroserviceOptions>({
    transport: Transport.TCP,
    options: { port: 3001 },
  });

  // Connect another transport
  app.connectMicroservice<MicroserviceOptions>({
    transport: Transport.REDIS,
    options: {
      host: 'localhost',
      port: 6379,
    },
  });

  // Start all transports
  await app.startAllMicroservices();

  // Start HTTP server
  await app.listen(3000);
  console.log('Hybrid app running: HTTP on 3000, TCP on 3001, Redis pub/sub');
}
bootstrap();
```

## Async Client Registration

Use `registerAsync` for dynamic configuration.

```typescript
ClientsModule.registerAsync([
  {
    name: 'ORDERS_SERVICE',
    imports: [ConfigModule],
    inject: [ConfigService],
    useFactory: (config: ConfigService) => ({
      transport: Transport.TCP,
      options: {
        host: config.get('ORDERS_SERVICE_HOST'),
        port: config.get('ORDERS_SERVICE_PORT'),
      },
    }),
  },
]),
```

## Error Handling

### Client-Side Timeout

```typescript
import { timeout, catchError } from 'rxjs/operators';
import { TimeoutError, throwError } from 'rxjs';

@Get(':id')
async findOne(@Param('id') id: string) {
  return firstValueFrom(
    this.ordersClient.send({ cmd: 'get_order' }, { id: +id }).pipe(
      timeout(5000),
      catchError((err) => {
        if (err instanceof TimeoutError) {
          throw new ServiceUnavailableException('Orders service timeout');
        }
        throw err;
      }),
    ),
  );
}
```

### Service-Side Exceptions

```typescript
import { RpcException } from '@nestjs/microservices';

@MessagePattern({ cmd: 'get_order' })
async getOrder(@Payload() data: { id: number }) {
  const order = await this.ordersService.findOne(data.id);
  if (!order) {
    throw new RpcException({
      code: 'ORDER_NOT_FOUND',
      message: `Order ${data.id} not found`,
    });
  }
  return order;
}
```

### Global Exception Filter

```typescript
// rpc-exception.filter.ts
import { Catch, RpcExceptionFilter, ArgumentsHost } from '@nestjs/common';
import { RpcException } from '@nestjs/microservices';
import { Observable, throwError } from 'rxjs';

@Catch(RpcException)
export class AllRpcExceptionsFilter implements RpcExceptionFilter<RpcException> {
  catch(exception: RpcException, host: ArgumentsHost): Observable<any> {
    const error = exception.getError();
    return throwError(() => error);
  }
}
```

## Health Checks

```typescript
// health/health.controller.ts
import { Controller, Get } from '@nestjs/common';
import {
  HealthCheck,
  HealthCheckService,
  MicroserviceHealthIndicator,
} from '@nestjs/terminus';
import { Transport } from '@nestjs/microservices';

@Controller('health')
export class HealthController {
  constructor(
    private health: HealthCheckService,
    private microservice: MicroserviceHealthIndicator,
  ) {}

  @Get()
  @HealthCheck()
  check() {
    return this.health.check([
      () =>
        this.microservice.pingCheck('orders-tcp', {
          transport: Transport.TCP,
          options: { host: 'localhost', port: 3001 },
        }),
      () =>
        this.microservice.pingCheck('redis', {
          transport: Transport.REDIS,
          options: { host: 'localhost', port: 6379 },
        }),
    ]);
  }
}
```

## Project Structure for Microservices

```
project/
├── apps/
│   ├── api-gateway/
│   │   ├── src/
│   │   │   ├── orders/
│   │   │   ├── users/
│   │   │   └── main.ts
│   │   └── tsconfig.app.json
│   ├── orders-service/
│   │   ├── src/
│   │   │   ├── orders.module.ts
│   │   │   ├── orders.controller.ts
│   │   │   ├── orders.service.ts
│   │   │   └── main.ts
│   │   └── tsconfig.app.json
│   └── users-service/
│       └── ...
├── libs/
│   └── shared/
│       └── src/
│           ├── dto/
│           └── interfaces/
├── nest-cli.json
└── package.json
```

### nest-cli.json for Monorepo

```json
{
  "$schema": "https://json.schemastore.org/nest-cli",
  "collection": "@nestjs/schematics",
  "sourceRoot": "apps/api-gateway/src",
  "monorepo": true,
  "root": "apps/api-gateway",
  "compilerOptions": {
    "webpack": true,
    "tsConfigPath": "apps/api-gateway/tsconfig.app.json"
  },
  "projects": {
    "api-gateway": {
      "type": "application",
      "root": "apps/api-gateway",
      "entryFile": "main",
      "sourceRoot": "apps/api-gateway/src",
      "compilerOptions": {
        "tsConfigPath": "apps/api-gateway/tsconfig.app.json"
      }
    },
    "orders-service": {
      "type": "application",
      "root": "apps/orders-service",
      "entryFile": "main",
      "sourceRoot": "apps/orders-service/src",
      "compilerOptions": {
        "tsConfigPath": "apps/orders-service/tsconfig.app.json"
      }
    },
    "shared": {
      "type": "library",
      "root": "libs/shared",
      "entryFile": "index",
      "sourceRoot": "libs/shared/src",
      "compilerOptions": {
        "tsConfigPath": "libs/shared/tsconfig.lib.json"
      }
    }
  }
}
```

## Tips

- Start with a modular monolith and extract services when needed.
- Use shared libraries for DTOs and interfaces.
- Implement circuit breakers for resilience.
- Add correlation IDs for distributed tracing.
- Use health checks to monitor service connectivity.
- Consider message serialization carefully (JSON vs Protobuf).

---

[Previous: Scaling and Cost Optimization](./28-scaling-cost-optimization.md) | [Back to Index](./README.md) | [Next: GraphQL ->](./30-graphql.md)
