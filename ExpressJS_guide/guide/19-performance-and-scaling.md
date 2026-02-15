# 19 - Performance and Scaling

This chapter covers practical steps to scale Express APIs without sacrificing reliability.

## Goals

- Measure before you optimize
- Reduce latency and resource usage
- Scale horizontally with confidence

## 1. Baseline Performance

Use a simple load test to capture current performance.

```bash
npm install -D autocannon
npx autocannon -c 50 -d 20 http://localhost:3000/health
```

## 2. Cache Hot Reads

```bash
npm install ioredis
```

```typescript
import Redis from 'ioredis';
const redis = new Redis(process.env.REDIS_URL as string);

app.get('/posts/:id', async (req, res) => {
  const key = `post:${req.params.id}`;
  const cached = await redis.get(key);
  if (cached) return res.json(JSON.parse(cached));

  const post = await postsService.getPost(req.params.id);
  await redis.set(key, JSON.stringify(post), 'EX', 60);
  return res.json(post);
});
```

## 3. HTTP Caching

```typescript
import etag from 'etag';

app.get('/public/posts', async (req, res) => {
  const payload = await postsService.listPublic();
  const tag = etag(JSON.stringify(payload));

  if (req.headers['if-none-match'] === tag) {
    return res.status(304).end();
  }

  res.setHeader('ETag', tag);
  res.setHeader('Cache-Control', 'public, max-age=60, stale-while-revalidate=300');
  return res.json(payload);
});
```

## 4. Database Performance

- Add indexes for hot queries.
- Use cursor pagination for large datasets.
- Avoid N+1 queries with joins or batched queries.
- Use connection pooling for high concurrency.

## 5. Avoid Event Loop Blocking

- Offload CPU-heavy work to worker threads or queues.
- Keep handlers small and async.

```typescript
import { Worker } from 'worker_threads';

export function runCpuTask(payload: unknown) {
  return new Promise((resolve, reject) => {
    const worker = new Worker(new URL('./cpu-task.js', import.meta.url), {
      workerData: payload,
    });
    worker.on('message', resolve);
    worker.on('error', reject);
  });
}
```

## 6. Compression and Response Size

```bash
npm install compression
```

```typescript
import compression from 'compression';
app.use(compression());
```

## 7. Horizontal Scaling

- Run multiple instances behind a load balancer.
- Keep services stateless and store shared state in Redis or a database.
- Use health checks so the balancer can drain unhealthy nodes.

## 8. Graceful Shutdown

```typescript
const server = app.listen(port, () => {
  console.log(`Server running on port ${port}`);
});

function shutdown() {
  server.close(() => process.exit(0));
}

process.on('SIGTERM', shutdown);
process.on('SIGINT', shutdown);
```

## Tips

- Measure p95 and p99 latency, not only averages.
- Reduce JSON payload size and remove unused fields.
- Add caching only where it helps and invalidate intentionally.

---

[Previous: Observability and Logging](./18-observability-logging.md) | [Back to Index](./README.md) | [Next: Background Jobs ->](./20-background-jobs-and-queues.md)
