# Services and Providers

Providers are the backbone of NestJS. Services, repositories, factories, and helpers are all providers that can be injected through the DI container.

## Goals

- Understand how DI works in NestJS
- Learn provider registration patterns
- Build reusable, testable services

## Basic Service

```typescript
// src/users/users.service.ts
import { Injectable } from '@nestjs/common';

@Injectable()
export class UsersService {
  private users: Array<{ id: number; email: string }> = [];

  list() {
    return this.users;
  }

  create(email: string) {
    const user = { id: this.users.length + 1, email };
    this.users.push(user);
    return user;
  }
}
```

## Registering Providers

```typescript
// src/users/users.module.ts
import { Module } from '@nestjs/common';
import { UsersService } from './users.service';

@Module({
  providers: [UsersService],
  exports: [UsersService],
})
export class UsersModule {}
```

## Dependency Injection

```typescript
// src/users/users.controller.ts
import { Controller, Get } from '@nestjs/common';
import { UsersService } from './users.service';

@Controller('users')
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  @Get()
  list() {
    return this.usersService.list();
  }
}
```

## Custom Providers

### Value Provider

```typescript
@Module({
  providers: [
    { provide: 'API_KEY', useValue: process.env.API_KEY ?? 'dev-key' },
  ],
  exports: ['API_KEY'],
})
export class ConfigModule {}
```

### Class Provider

```typescript
@Module({
  providers: [
    { provide: UsersService, useClass: UsersService },
  ],
})
export class UsersModule {}
```

### Existing Provider (Alias)

```typescript
@Module({
  providers: [
    UsersService,
    { provide: 'IUsersService', useExisting: UsersService },
  ],
  exports: ['IUsersService'],
})
export class UsersModule {}
```

### Factory Provider

```typescript
@Module({
  providers: [
    {
      provide: 'REDIS_CLIENT',
      useFactory: async () => {
        const { createClient } = await import('redis');
        const client = createClient({ url: process.env.REDIS_URL });
        await client.connect();
        return client;
      },
    },
  ],
  exports: ['REDIS_CLIENT'],
})
export class CacheModule {}
```

## Async Module Configuration

Use `forRootAsync` when config depends on other providers.

```typescript
// src/database/database.module.ts
import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ConfigModule, ConfigService } from '@nestjs/config';

@Module({
  imports: [
    TypeOrmModule.forRootAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        type: 'postgres',
        host: config.get('DATABASE_HOST'),
        port: config.get<number>('DATABASE_PORT'),
        username: config.get('DATABASE_USER'),
        password: config.get('DATABASE_PASSWORD'),
        database: config.get('DATABASE_NAME'),
        autoLoadEntities: true,
        synchronize: false,
      }),
    }),
  ],
})
export class DatabaseModule {}
```

## Provider Scopes

| Scope | Description | Use Case |
| --- | --- | --- |
| `DEFAULT` | Singleton for entire app | Most providers |
| `REQUEST` | New instance per request | Request scoped cache, audit info |
| `TRANSIENT` | New instance per injection | Stateless helpers |

```typescript
import { Injectable, Scope } from '@nestjs/common';

@Injectable({ scope: Scope.REQUEST })
export class RequestContextService {
  requestId = crypto.randomUUID();
}
```

Request-scoped providers are slower and can complicate testing. Use them only when needed.

## Lifecycle Hooks

```typescript
import { Injectable, OnModuleInit, OnApplicationShutdown } from '@nestjs/common';

@Injectable()
export class MetricsService implements OnModuleInit, OnApplicationShutdown {
  onModuleInit() {
    // connect to metrics backend
  }

  onApplicationShutdown(signal?: string) {
    // cleanup
  }
}
```

## Injecting Tokens

```typescript
import { Injectable, Inject } from '@nestjs/common';

@Injectable()
export class ApiClient {
  constructor(@Inject('API_KEY') private readonly apiKey: string) {}
}
```

## Tips

- Keep providers stateless when possible.
- Export only what other modules must use.
- Avoid circular dependencies. Use `forwardRef` or restructure modules.

---

[Previous: Modules and Controllers](./02-modules-controllers.md) | [Back to Index](./README.md) | [Next: Data Validation ->](./04-data-validation.md)
