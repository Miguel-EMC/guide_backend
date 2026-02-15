# 27 - Datastores and Caching

This chapter covers common datastores and caching options: PostgreSQL, MongoDB, Firebase, Redis, and PostgREST.

## Goals

- Choose the right datastore for each workload
- Understand tradeoffs between SQL and NoSQL
- Add caching where it matters

## 1. PostgreSQL (Relational)

Good for transactional data, strong consistency, and complex queries.

```typescript
// Using Prisma (see chapter 10)
const posts = await prisma.post.findMany();
```

Production tips:

- Add indexes for hot queries.
- Use read replicas for heavy read traffic.
- Use migrations with clear rollbacks.

## 2. MongoDB (Document)

Good for flexible schemas and nested data.

```bash
npm install mongodb
```

```typescript
import { MongoClient } from 'mongodb';

const client = new MongoClient(process.env.MONGO_URL as string);
await client.connect();
const db = client.db('app');
const users = db.collection('users');
```

Production tips:

- Define schema validation rules.
- Add indexes for filter fields.
- Avoid unbounded document growth.

## 3. Firebase (Auth + Firestore)

Useful for rapid prototyping and mobile-first stacks.

```bash
npm install firebase-admin
```

```typescript
import admin from 'firebase-admin';

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
});

const firestore = admin.firestore();
```

Production tips:

- Use service accounts in server environments.
- Enforce security rules and least privilege.

## 4. Redis (Cache + Rate Limit + Queues)

Redis is ideal for caching and rate limiting.

```typescript
import Redis from 'ioredis';
const redis = new Redis(process.env.REDIS_URL as string);

await redis.set('key', 'value', 'EX', 60);
```

Production tips:

- Set TTLs for all cache keys.
- Use key prefixes by service and environment.
- Monitor memory and eviction rates.

## 5. PostgREST

PostgREST auto-generates a REST API from a PostgreSQL schema. Use it for internal services or rapid prototyping.

## 6. Cache Patterns

Common patterns:

- Read-through caching
- Write-through caching
- Cache-aside (most common)
- Refresh-ahead for hot keys

## 7. Cache-Aside Example

```typescript
const key = `user:${id}`;
const cached = await redis.get(key);
if (cached) return JSON.parse(cached);

const user = await prisma.user.findUnique({ where: { id } });
if (user) await redis.set(key, JSON.stringify(user), 'EX', 60);
return user;
```

## 8. Cache Stampede Protection

Use short locks or request coalescing for hot keys.

```typescript
const lockKey = `lock:user:${id}`;
const lock = await redis.set(lockKey, '1', 'NX', 'EX', 5);
if (lock) {
  const user = await prisma.user.findUnique({ where: { id } });
  if (user) await redis.set(key, JSON.stringify(user), 'EX', 60);
  await redis.del(lockKey);
  return user;
}

const retry = await redis.get(key);
return retry ? JSON.parse(retry) : null;
```

## 9. HTTP Cache Headers

Even with Redis, add HTTP cache headers for public data.

```typescript
res.setHeader('Cache-Control', 'public, max-age=60, stale-while-revalidate=300');
```

## Tips

- Use Postgres for most transactional workloads.
- Use Redis for hot data, rate limits, and queues.
- Use MongoDB when flexibility is more important than strict schemas.

---

[Previous: Cost Optimization](./26-cost-optimization.md) | [Back to Index](./README.md)
