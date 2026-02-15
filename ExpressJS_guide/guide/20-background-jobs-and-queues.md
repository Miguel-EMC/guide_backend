# 20 - Background Jobs and Queues

Background jobs keep API latency low by moving heavy work out of the request path.

## Goals

- Offload slow or CPU-heavy tasks
- Ensure retries and dead-letter handling
- Monitor job health and throughput

## 1. Choose a Queue

For Node.js, Redis-backed queues are common. BullMQ is a solid default.

```bash
npm install bullmq ioredis
```

## 2. Create a Queue

```typescript
import { Queue } from 'bullmq';
import Redis from 'ioredis';

const connection = new Redis(process.env.REDIS_URL as string);
export const emailQueue = new Queue('emails', { connection });
```

## 3. Enqueue Jobs

```typescript
await emailQueue.add(
  'welcome',
  { userId: 123 },
  {
    attempts: 5,
    backoff: { type: 'exponential', delay: 1000 },
    removeOnComplete: 1000,
  },
);
```

## 4. Process Jobs with a Worker

```typescript
import { Worker } from 'bullmq';
import { sendWelcomeEmail } from './mailers/send-welcome.js';

export const emailWorker = new Worker(
  'emails',
  async (job) => {
    await sendWelcomeEmail(job.data.userId);
  },
  { connection },
);
```

## 5. Idempotency and Deduplication

Use a deterministic job ID to avoid duplicate work.

```typescript
await emailQueue.add('welcome', { userId }, { jobId: `welcome:${userId}` });
```

## 6. Scheduled and Delayed Jobs

```typescript
await emailQueue.add(
  'daily-report',
  { tenantId: 'acme' },
  { repeat: { cron: '0 2 * * *' } },
);
```

## 7. Error Handling and Dead Letters

- Use retries with exponential backoff.
- Capture failed jobs and alert on spikes.
- Move unrecoverable jobs to a dead-letter queue.

## 8. Monitoring

Add a dashboard for queue metrics and failures.

```bash
npm install @bull-board/api @bull-board/express
```

```typescript
import { createBullBoard } from '@bull-board/api';
import { BullMQAdapter } from '@bull-board/api/bullMQAdapter.js';
import { ExpressAdapter } from '@bull-board/express';

const serverAdapter = new ExpressAdapter();
createBullBoard({
  queues: [new BullMQAdapter(emailQueue)],
  serverAdapter,
});

serverAdapter.setBasePath('/admin/queues');
app.use('/admin/queues', serverAdapter.getRouter());
```

## Tips

- Keep jobs small and idempotent.
- Use a separate worker deployment from the API.
- Use a queue for all external side effects (emails, webhooks, files).

---

[Previous: Performance and Scaling](./19-performance-and-scaling.md) | [Back to Index](./README.md) | [Next: CI/CD ->](./21-ci-cd.md)
