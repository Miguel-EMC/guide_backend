# Error Handling

NestJS provides a structured exceptions layer. You can throw HTTP exceptions in services or controllers, and use exception filters to standardize error responses.

## Goals

- Use built-in HTTP exceptions consistently
- Provide a stable error response shape
- Map database errors to user-safe messages

## Built-in HTTP Exceptions

NestJS exposes common HTTP exceptions through `@nestjs/common`.

```typescript
import {
  BadRequestException,
  ConflictException,
  ForbiddenException,
  NotFoundException,
  UnauthorizedException,
} from '@nestjs/common';

throw new NotFoundException('User not found');
throw new UnauthorizedException('Invalid credentials');
throw new ConflictException('Email already exists');
throw new BadRequestException('Invalid input');
throw new ForbiddenException('Insufficient permissions');
```

## Custom Business Exceptions

```typescript
import { HttpException, HttpStatus } from '@nestjs/common';

export class BusinessException extends HttpException {
  constructor(code: string, message: string, status = HttpStatus.BAD_REQUEST) {
    super(
      {
        statusCode: status,
        code,
        message,
        timestamp: new Date().toISOString(),
      },
      status,
    );
  }
}
```

## Global Exception Filter

A global filter lets you control the error format across the entire app.

```typescript
// src/common/filters/all-exceptions.filter.ts
import {
  ArgumentsHost,
  Catch,
  ExceptionFilter,
  HttpException,
  HttpStatus,
} from '@nestjs/common';
import { QueryFailedError } from 'typeorm';
import type { Request, Response } from 'express';

@Catch()
export class AllExceptionsFilter implements ExceptionFilter {
  catch(exception: unknown, host: ArgumentsHost) {
    const ctx = host.switchToHttp();
    const response = ctx.getResponse<Response>();
    const request = ctx.getRequest<Request>();

    let status = HttpStatus.INTERNAL_SERVER_ERROR;
    let message = 'internal_error';
    let details: unknown = undefined;

    if (exception instanceof HttpException) {
      status = exception.getStatus();
      const payload = exception.getResponse();

      if (typeof payload === 'string') {
        message = payload;
      } else if (typeof payload === 'object' && payload) {
        const payloadObj = payload as Record<string, unknown>;
        if (typeof payloadObj.message === 'string') {
          message = payloadObj.message;
        } else if (Array.isArray(payloadObj.message)) {
          message = 'validation_error';
          details = payloadObj.message;
        }
      }
    }

    if (exception instanceof QueryFailedError) {
      status = HttpStatus.BAD_REQUEST;
      message = 'database_error';
    }

    response.status(status).json({
      statusCode: status,
      message,
      details,
      path: request.url,
      timestamp: new Date().toISOString(),
    });
  }
}
```

Register the filter globally.

```typescript
// src/app.module.ts
import { APP_FILTER } from '@nestjs/core';
import { AllExceptionsFilter } from './common/filters/all-exceptions.filter';

@Module({
  providers: [
    {
      provide: APP_FILTER,
      useClass: AllExceptionsFilter,
    },
  ],
})
export class AppModule {}
```

## Avoid Leaking Internal Errors

In production, do not return stack traces or database error strings. Map them to a generic message and log the full error server-side.

## Tips

- Throw exceptions from services, not from repositories.
- Keep error messages stable for clients.
- Log full errors with a request ID for debugging.

---

[Previous: Entities and Relationships](./06-entities-relationships.md) | [Back to Index](./README.md) | [Next: Authentication ->](./08-authentication.md)
