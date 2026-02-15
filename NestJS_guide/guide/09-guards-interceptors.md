# Guards, Interceptors, and Pipes

Guards decide if a request can proceed. Interceptors wrap request and response flows. Pipes validate and transform inputs. This chapter uses these tools to enforce policy and standardize API behavior.

## Goals

- Apply authorization with guards
- Shape responses with interceptors
- Validate inputs with pipes

## Guards

### Roles Decorator

```typescript
// src/common/decorators/roles.decorator.ts
import { SetMetadata } from '@nestjs/common';

export const ROLES_KEY = 'roles';
export const Roles = (...roles: string[]) => SetMetadata(ROLES_KEY, roles);
```

### Roles Guard

```typescript
// src/common/guards/roles.guard.ts
import { CanActivate, ExecutionContext, Injectable } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { ROLES_KEY } from '../decorators/roles.decorator';

@Injectable()
export class RolesGuard implements CanActivate {
  constructor(private readonly reflector: Reflector) {}

  canActivate(context: ExecutionContext): boolean {
    const roles = this.reflector.getAllAndOverride<string[]>(ROLES_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);
    if (!roles) return true;

    const request = context.switchToHttp().getRequest();
    const user = request.user;
    return roles.some((role) => user?.roles?.includes(role));
  }
}
```

### Apply Guards

```typescript
import { Controller, Get, UseGuards } from '@nestjs/common';
import { Roles } from '../common/decorators/roles.decorator';
import { RolesGuard } from '../common/guards/roles.guard';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';

@Controller('admin')
@UseGuards(JwtAuthGuard, RolesGuard)
export class AdminController {
  @Get('users')
  @Roles('admin')
  listUsers() {
    return [];
  }
}
```

## Interceptors

### Response Transform Interceptor

```typescript
// src/common/interceptors/transform.interceptor.ts
import {
  CallHandler,
  ExecutionContext,
  Injectable,
  NestInterceptor,
} from '@nestjs/common';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable()
export class TransformInterceptor<T>
  implements NestInterceptor<T, { data: T; ts: string }>
{
  intercept(context: ExecutionContext, next: CallHandler): Observable<any> {
    return next.handle().pipe(
      map((data) => ({ data, ts: new Date().toISOString() })),
    );
  }
}
```

### Timeout Interceptor

```typescript
// src/common/interceptors/timeout.interceptor.ts
import {
  CallHandler,
  ExecutionContext,
  Injectable,
  NestInterceptor,
  RequestTimeoutException,
} from '@nestjs/common';
import { Observable, timeout, catchError, throwError } from 'rxjs';

@Injectable()
export class TimeoutInterceptor implements NestInterceptor {
  intercept(context: ExecutionContext, next: CallHandler): Observable<any> {
    return next.handle().pipe(
      timeout(5000),
      catchError(() => throwError(() => new RequestTimeoutException())),
    );
  }
}
```

### Apply Interceptors

```typescript
import { UseInterceptors } from '@nestjs/common';
import { TransformInterceptor } from '../common/interceptors/transform.interceptor';

@UseInterceptors(TransformInterceptor)
@Get()
list() {
  return [];
}
```

### Global Interceptors

```typescript
import { APP_INTERCEPTOR } from '@nestjs/core';

@Module({
  providers: [
    { provide: APP_INTERCEPTOR, useClass: TransformInterceptor },
  ],
})
export class AppModule {}
```

## Pipes

Use pipes to validate and transform inputs. The most common pipe is `ValidationPipe`.

```typescript
import { ValidationPipe } from '@nestjs/common';

app.useGlobalPipes(
  new ValidationPipe({
    whitelist: true,
    forbidNonWhitelisted: true,
    transform: true,
  }),
);
```

## Tips

- Keep guards simple and focused on authorization.
- Avoid heavy logic inside interceptors.
- Use global pipes for consistent validation.

---

[Previous: Authentication](./08-authentication.md) | [Back to Index](./README.md) | [Next: Swagger Documentation ->](./10-swagger-documentation.md)
