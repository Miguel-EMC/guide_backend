# 05 - Hello World with Express

This chapter creates a minimal Express 5 server with JSON responses and proper app separation.

## 1. Basic Server

```typescript
// src/app.ts
import express from 'express';

export const app = express();

app.get('/', (_req, res) => {
  res.json({ message: 'Hello Express 5' });
});
```

```typescript
// src/server.ts
import { app } from './app.js';
import 'dotenv/config';

const port = Number(process.env.PORT ?? 3000);
const server = app.listen(port, () => {
  console.log(`Listening on ${port}`);
});

// Graceful shutdown
process.on('SIGTERM', () => server.close());
process.on('SIGINT', () => server.close());
```

## 2. JSON Body Parsing

```typescript
app.use(express.json());
```

Without this middleware, `req.body` will be `undefined` for JSON requests.

## 3. Health Endpoint

```typescript
app.get('/health', (_req, res) => {
  res.json({ status: 'ok', ts: new Date().toISOString() });
});
```

## 4. Basic Middleware Stack

```typescript
import helmet from 'helmet';
import cors from 'cors';

app.use(helmet());
app.use(cors({ origin: '*' }));
```

## 5. Try It

```bash
curl http://localhost:3000
curl http://localhost:3000/health
```

---

[Previous: Node Modules and Event Loop](./04-nodejs-modules-and-event-loop.md) | [Back to Index](./README.md) | [Next: Routing ->](./06-routing-in-express.md)
