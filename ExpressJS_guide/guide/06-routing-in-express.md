# 06 - Routing in Express

Express routes map HTTP requests to handlers. Express 5 uses an updated path matching engine.

## 1. Basic Routes

```typescript
app.get('/users', (_req, res) => {
  res.json([]);
});

app.post('/users', (req, res) => {
  res.status(201).json(req.body);
});
```

## 2. Route Parameters

```typescript
app.get('/users/:id', (req, res) => {
  res.json({ id: req.params.id });
});
```

## 3. Query Parameters

```typescript
app.get('/search', (req, res) => {
  res.json({ q: req.query.q });
});
```

## 4. Named Wildcards (Express 5)

Wildcard routes now require a name.

```typescript
app.get('/files/*splat', (req, res) => {
  res.json({ path: req.params.splat });
});
```

## 5. Optional Segments

Express 5 uses updated path syntax. If you relied on optional segments, use the new pattern style.

```typescript
// Example from the migration guide
app.get('/:file{.:ext}', (req, res) => {
  res.json({ file: req.params.file, ext: req.params.ext });
});
```

If you used complex patterns, review the Express 5 migration guide.

## 6. Routers

Split routes into modules using `express.Router()`.

```typescript
// src/routes/users.routes.ts
import { Router } from 'express';

export const usersRouter = Router();

usersRouter.get('/', (_req, res) => res.json([]));
usersRouter.get('/:id', (req, res) => res.json({ id: req.params.id }));
```

```typescript
// src/app.ts
import { usersRouter } from './routes/users.routes.js';
app.use('/users', usersRouter);
```

## 7. Router-Level Middleware

```typescript
usersRouter.use((req, _res, next) => {
  console.log('users route');
  next();
});
```

## 8. Route Order

Routes are matched in the order they are defined. Define more specific routes first.

## 9. Versioned Routers

```typescript
app.use('/v1/users', usersRouter);
```

--- 

[Previous: Hello World](./05-hello-world-with-express.md) | [Back to Index](./README.md) | [Next: Middleware ->](./07-middleware.md)
