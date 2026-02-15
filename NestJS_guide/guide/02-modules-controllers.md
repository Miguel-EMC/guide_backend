# Modules and Controllers

NestJS uses a modular architecture. Modules organize code, controllers handle incoming requests, and providers implement the business logic. This chapter focuses on module boundaries and routing.

## Goals

- Design clean feature modules
- Understand imports, exports, and global modules
- Build controllers with typed request handling

## Modules

A module is a class annotated with `@Module()` that groups providers and controllers.

```typescript
// src/users/users.module.ts
import { Module } from '@nestjs/common';
import { UsersController } from './users.controller';
import { UsersService } from './users.service';

@Module({
  controllers: [UsersController],
  providers: [UsersService],
  exports: [UsersService],
})
export class UsersModule {}
```

### Imports and Exports

- `imports` pulls in providers exported by other modules.
- `exports` makes providers available to consumers.

```typescript
// src/app.module.ts
import { Module } from '@nestjs/common';
import { UsersModule } from './users/users.module';

@Module({
  imports: [UsersModule],
})
export class AppModule {}
```

### Shared and Global Modules

```typescript
// src/common/common.module.ts
import { Global, Module } from '@nestjs/common';
import { ConfigService } from './config.service';

@Global()
@Module({
  providers: [ConfigService],
  exports: [ConfigService],
})
export class CommonModule {}
```

Use `@Global()` sparingly. It is helpful for configuration or logging, but can hide dependencies.

### Dynamic Modules

Dynamic modules let you pass configuration when importing.

```typescript
// src/feature/feature.module.ts
import { DynamicModule, Module } from '@nestjs/common';
import { FeatureService } from './feature.service';

@Module({})
export class FeatureModule {
  static forRoot(options: { apiKey: string }): DynamicModule {
    return {
      module: FeatureModule,
      providers: [
        FeatureService,
        { provide: 'FEATURE_OPTIONS', useValue: options },
      ],
      exports: [FeatureService],
    };
  }
}
```

### Circular Dependencies

If two modules depend on each other, use `forwardRef`.

```typescript
import { Module, forwardRef } from '@nestjs/common';

@Module({
  imports: [forwardRef(() => UsersModule)],
})
export class AuthModule {}
```

## Controllers

Controllers define routes and map HTTP requests to handlers.

```typescript
// src/users/users.controller.ts
import {
  Controller,
  Get,
  Post,
  Body,
  Param,
  Query,
  HttpCode,
  ParseIntPipe,
} from '@nestjs/common';
import { UsersService } from './users.service';
import { CreateUserDto } from './dto/create-user.dto';
import { ListUsersQueryDto } from './dto/list-users-query.dto';

@Controller('users')
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  @Get()
  list(@Query() query: ListUsersQueryDto) {
    return this.usersService.list(query);
  }

  @Get(':id')
  get(@Param('id', ParseIntPipe) id: number) {
    return this.usersService.get(id);
  }

  @Post()
  @HttpCode(201)
  create(@Body() dto: CreateUserDto) {
    return this.usersService.create(dto);
  }
}
```

### Common Parameter Decorators

| Decorator | Purpose |
| --- | --- |
| `@Param()` | Route params (`/users/:id`) |
| `@Query()` | Query string params |
| `@Body()` | Request body |
| `@Headers()` | Request headers |
| `@Req()` | Raw request object |
| `@Res()` | Raw response object |

If you use `@Res()`, you must send the response manually. Prefer returning values so Nest can handle serialization and interceptors.

### HTTP Metadata

```typescript
import { Controller, Get, Header, Redirect } from '@nestjs/common';

@Controller('meta')
export class MetaController {
  @Get('ping')
  @Header('Cache-Control', 'no-store')
  ping() {
    return { ok: true };
  }

  @Get('docs')
  @Redirect('https://example.com', 302)
  docs() {}
}
```

## Tips

- Keep modules focused on a single domain.
- Export only what other modules need.
- Avoid calling database or HTTP clients inside controllers. Put logic in services.

---

[Previous: Introduction](./01-introduction.md) | [Back to Index](./README.md) | [Next: Services and Providers ->](./03-services-providers.md)
