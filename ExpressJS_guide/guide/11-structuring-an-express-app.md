# 11 - Structuring an Express App

A clean architecture makes large Express projects maintainable and testable.

## Recommended Structure

```
src/
  app.ts
  server.ts
  config/
    env.ts
  db/
    prisma.ts
  modules/
    users/
      users.controller.ts
      users.service.ts
      users.routes.ts
      users.types.ts
    posts/
      posts.controller.ts
      posts.service.ts
      posts.routes.ts
  middleware/
    error-handler.ts
    request-id.ts
  utils/
```

## App vs Server

- `app.ts` wires middleware and routes.
- `server.ts` starts the HTTP listener.

```typescript
// src/app.ts
import express from 'express';
import { usersRouter } from './modules/users/users.routes.js';
import { errorHandler } from './middleware/error-handler.js';

export const app = express();
app.use(express.json());

app.use('/users', usersRouter);
app.use(errorHandler);
```

## Route/Controller/Service Pattern

```typescript
// src/modules/users/users.routes.ts
import { Router } from 'express';
import { listUsers } from './users.controller.js';

export const usersRouter = Router();
usersRouter.get('/', listUsers);
```

```typescript
// src/modules/users/users.controller.ts
import type { Request, Response } from 'express';
import { findAllUsers } from './users.service.js';

export async function listUsers(_req: Request, res: Response) {
  const users = await findAllUsers();
  res.json(users);
}
```

```typescript
// src/modules/users/users.service.ts
import { prisma } from '../../db/prisma.js';

export function findAllUsers() {
  return prisma.user.findMany();
}
```

## Environment Config

Keep environment configuration centralized.

```typescript
// src/config/env.ts
import 'dotenv/config';

export const env = {
  port: Number(process.env.PORT ?? 3000),
  nodeEnv: process.env.NODE_ENV ?? 'development',
  databaseUrl: process.env.DATABASE_URL as string,
};
```

## Repository Pattern (Optional)

Use repositories to isolate database logic.

```typescript
// src/modules/users/users.repo.ts
import { prisma } from '../../db/prisma.js';

export function findUserByEmail(email: string) {
  return prisma.user.findUnique({ where: { email } });
}
```

## Service Composition

Services can be composed for complex workflows.

```typescript
// src/modules/auth/auth.service.ts
import { findUserByEmail } from '../users/users.repo.js';

export async function login(email: string, password: string) {
  const user = await findUserByEmail(email);
  if (!user) return null;
  return user;
}
```

## Tips

- Keep controllers thin and move logic to services.
- Keep DB access in service or repository layers.
- Use a consistent naming convention across modules.

---

[Previous: Database with Prisma](./10-connecting-to-a-database-prisma.md) | [Back to Index](./README.md) | [Next: Building a CRUD API ->](./12-building-a-crud-api.md)
