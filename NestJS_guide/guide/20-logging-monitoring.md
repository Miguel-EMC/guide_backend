# Logging and Monitoring

Logging is critical for debugging and production observability. NestJS ships with a built-in logger you can extend or replace.

## Goals

- Use structured logs consistently
- Add request-level context
- Prepare for production monitoring

## Built-in Logger

```typescript
import { Logger } from '@nestjs/common';

const logger = new Logger('Bootstrap');
logger.log('Starting app');
```

## Custom Logger

```typescript
// src/common/logger/app.logger.ts
import { LoggerService } from '@nestjs/common';

export class AppLogger implements LoggerService {
  log(message: string) {
    console.log(JSON.stringify({ level: 'info', message }));
  }
  error(message: string, trace?: string) {
    console.error(JSON.stringify({ level: 'error', message, trace }));
  }
  warn(message: string) {
    console.warn(JSON.stringify({ level: 'warn', message }));
  }
}
```

```typescript
// src/main.ts
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { AppLogger } from './common/logger/app.logger';

async function bootstrap() {
  const app = await NestFactory.create(AppModule, {
    logger: new AppLogger(),
  });
  await app.listen(3000);
}
bootstrap();
```

## Request Logging (Interceptor)

```typescript
// src/common/interceptors/logging.interceptor.ts
import {
  CallHandler,
  ExecutionContext,
  Injectable,
  NestInterceptor,
  Logger,
} from '@nestjs/common';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

@Injectable()
export class LoggingInterceptor implements NestInterceptor {
  private readonly logger = new Logger('HTTP');

  intercept(context: ExecutionContext, next: CallHandler): Observable<any> {
    const req = context.switchToHttp().getRequest();
    const start = Date.now();

    return next.handle().pipe(
      tap(() => {
        const ms = Date.now() - start;
        this.logger.log(`${req.method} ${req.url} ${ms}ms`);
      }),
    );
  }
}
```

## Correlation IDs

Attach a request ID so you can trace logs across services.

```typescript
// src/common/middleware/request-id.middleware.ts
import { NextFunction, Request, Response } from 'express';
import { randomUUID } from 'crypto';

export function requestId(req: Request, _res: Response, next: NextFunction) {
  req.headers['x-request-id'] = req.headers['x-request-id'] ?? randomUUID();
  next();
}
```

## Tips

- Prefer structured JSON logs for production.
- Log errors with context and user identifiers (not secrets).
- Add health checks and metrics once you deploy.

---

[Previous: RBAC and Permissions](./19-rbac-permissions.md) | [Back to Index](./README.md) | [Next: API Versioning ->](./21-api-versioning.md)
