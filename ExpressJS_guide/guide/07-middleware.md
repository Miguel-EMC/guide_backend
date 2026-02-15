# 07 - Middleware

Middleware functions run in sequence and can read or modify the request and response objects.

## 1. Built-in Middleware

```typescript
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
```

`express.urlencoded()` defaults to `extended: false` in Express 5.

## 2. Third-Party Middleware

```typescript
import cors from 'cors';
import helmet from 'helmet';

app.use(helmet());
app.use(cors({ origin: '*' }));
```

## 3. Custom Middleware

```typescript
import type { Request, Response, NextFunction } from 'express';

function requestLogger(req: Request, _res: Response, next: NextFunction) {
  console.log(`${req.method} ${req.path}`);
  next();
}

app.use(requestLogger);
```

## 4. Async Handler Helper

Express 5 handles thrown errors in async handlers, but it is still useful to wrap promise handlers for consistency.

```typescript
export const asyncHandler = (fn: any) => (req: any, res: any, next: any) => {
  Promise.resolve(fn(req, res, next)).catch(next);
};
```

## 5. Middleware Order Matters

Order determines which middleware runs first. Parse input and apply security headers before your routes.

```typescript
app.use(helmet());
app.use(express.json());
app.use('/api', apiRouter);
```

## 6. Error Middleware

```typescript
import type { Request, Response, NextFunction } from 'express';

export function errorHandler(
  err: unknown,
  _req: Request,
  res: Response,
  _next: NextFunction,
) {
  console.error(err);
  res.status(500).json({ message: 'internal_error' });
}
```

Register after all routes.

--- 

[Previous: Routing](./06-routing-in-express.md) | [Back to Index](./README.md) | [Next: Request and Response ->](./08-request-and-response.md)
