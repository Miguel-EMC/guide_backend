# 10 - Connecting to a Database with Prisma

This chapter uses Prisma with PostgreSQL. The same approach works for MySQL and SQLite.

## 1. Install Prisma

```bash
npm install prisma @prisma/client
npx prisma init
```

## 2. Configure Database URL

```env
# .env
DATABASE_URL="postgresql://postgres:postgres@localhost:5432/express_db"
```

## 3. Define the Schema

```prisma
// prisma/schema.prisma
generator client {
  provider = "prisma-client-js"
}

datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}

model User {
  id        Int      @id @default(autoincrement())
  email     String   @unique
  password  String
  posts     Post[]
  createdAt DateTime @default(now())
  updatedAt DateTime @updatedAt
}

model Post {
  id        Int      @id @default(autoincrement())
  title     String
  content   String
  authorId  Int
  author    User     @relation(fields: [authorId], references: [id])
  createdAt DateTime @default(now())
  updatedAt DateTime @updatedAt
}
```

## 4. Migrate and Generate

```bash
npx prisma migrate dev --name init
npx prisma generate
```

Use `prisma migrate deploy` in production CI/CD.

## 5. Create a Prisma Client Singleton

```typescript
// src/db/prisma.ts
import { PrismaClient } from '@prisma/client';

const globalForPrisma = globalThis as unknown as { prisma?: PrismaClient };

export const prisma =
  globalForPrisma.prisma ??
  new PrismaClient({
    log: ['error', 'warn'],
  });

if (process.env.NODE_ENV !== 'production') globalForPrisma.prisma = prisma;
```

## 6. Use Prisma in a Service

```typescript
// src/modules/posts/posts.service.ts
import { prisma } from '../../db/prisma.js';

export async function listPosts() {
  return prisma.post.findMany({ include: { author: true } });
}
```

## 7. Transactions

```typescript
await prisma.$transaction(async (tx) => {
  await tx.user.update({ where: { id: 1 }, data: { email: 'x@y.com' } });
  await tx.post.create({ data: { title: 'New', content: '...', authorId: 1 } });
});
```

## 8. Seeding

```typescript
// prisma/seed.ts
import { prisma } from '../src/db/prisma.js';

await prisma.user.create({ data: { email: 'admin@x.com', password: 'hash' } });
```

Run with:

```bash
node --loader tsx prisma/seed.ts
```

## 9. Connection Pooling

Use pooled connections in production. Many hosted Postgres providers require a pooler.

```env
DATABASE_URL="postgresql://user:pass@host:5432/db?pgbouncer=true"
```

## 10. Production Checklist

- Use `migrate deploy` in CI/CD.
- Keep Prisma Client in a singleton.
- Add indexes for hot queries.
- Avoid long transactions in request handlers.

## Tips

- Use `migrate dev` in development and `migrate deploy` in production.
- Add indexes for hot queries.
- Keep Prisma queries in service or repository layers.

---

[Previous: Error Handling](./09-error-handling.md) | [Back to Index](./README.md) | [Next: Structuring an Express App ->](./11-structuring-an-express-app.md)
