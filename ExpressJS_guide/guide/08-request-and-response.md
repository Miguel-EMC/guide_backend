# 08 - Request and Response Objects

Express provides rich `req` and `res` objects. Understanding them is key to building clean handlers.

## 1. Reading Requests

```typescript
app.get('/users/:id', (req, res) => {
  const id = req.params.id;
  const q = req.query.q;
  const auth = req.header('authorization');
  res.json({ id, q, auth });
});
```

In Express 5, `req.query` is a getter and no longer writable.

## 2. Parsing Bodies

```typescript
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
```

If you do not register these middleware, `req.body` will be `undefined`.

## 3. Sending Responses

```typescript
res.status(201).json({ id: 1 });
res.send('ok');
res.sendStatus(204);
```

## 4. Setting Headers

```typescript
res.set('Cache-Control', 'no-store');
res.setHeader('X-Request-Id', 'abc123');
```

## 5. Redirects and Files

```typescript
res.redirect(302, 'https://example.com');
res.download('/tmp/report.csv');
```

## 6. res.locals for Request Context

```typescript
app.use((req, res, next) => {
  res.locals.requestId = req.header('x-request-id');
  next();
});
```

## 7. Streaming Responses

For large payloads, stream instead of buffering in memory.

```typescript
import fs from 'fs';

app.get('/download', (_req, res) => {
  const stream = fs.createReadStream('./big-file.csv');
  stream.pipe(res);
});
```

--- 

[Previous: Middleware](./07-middleware.md) | [Back to Index](./README.md) | [Next: Error Handling ->](./09-error-handling.md)
