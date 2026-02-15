# 09 - Error Handling

Express uses error-handling middleware to centralize failures and keep routes clean. Express 5 also handles async errors in route handlers.

## 1. Error Middleware

Error middleware has four arguments.

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

Register it after all routes:

```typescript
app.use(errorHandler);
```

## 2. Throwing Errors in Routes

```typescript
app.get('/boom', async (_req, _res) => {
  throw new Error('boom');
});
```

In Express 5, thrown errors inside async handlers propagate to the error middleware automatically.

## 3. Custom Errors

```typescript
export class AppError extends Error {
  constructor(public status: number, public code: string, message: string) {
    super(message);
  }
}

app.get('/users/:id', (req, res) => {
  if (!req.params.id) {
    throw new AppError(400, 'bad_request', 'Missing id');
  }
  res.json({ id: req.params.id });
});
```

Update the error handler to handle `AppError`:

```typescript
if (err instanceof AppError) {
  return res.status(err.status).json({ code: err.code, message: err.message });
}
```

## 4. 404 Handler

```typescript
app.use((_req, res) => {
  res.status(404).json({ message: 'not_found' });
});
```

## 5. Production Guidance

- Avoid leaking stack traces to clients.
- Log errors with request IDs.
- Map database errors to user-safe messages.

## 6. Error Response Shape

Use a consistent error shape so clients can rely on it.

```json
{
  "message": "validation_error",
  "details": [
    { "field": "email", "error": "Invalid email" }
  ]
}
```

## 7. Example: Validation Error Mapping

```typescript
export function validationErrorHandler(
  err: any,
  _req: Request,
  res: Response,
  next: NextFunction,
) {
  if (!err?.issues) return next(err);
  return res.status(400).json({
    message: 'validation_error',
    details: err.issues,
  });
}
```

--- 

[Previous: Request and Response](./08-request-and-response.md) | [Back to Index](./README.md) | [Next: Database with Prisma ->](./10-connecting-to-a-database-prisma.md)
