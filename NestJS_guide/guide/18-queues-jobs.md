# Queues and Background Jobs

Queues let you move slow or unreliable work out of the request path. NestJS integrates with BullMQ through `@nestjs/bullmq`.

## Goals

- Configure a queue with Redis
- Publish jobs from services
- Process jobs in a worker

## Install

```bash
npm install @nestjs/bullmq bullmq
```

## Configure the Queue

```typescript
// src/app.module.ts
import { Module } from '@nestjs/common';
import { BullModule } from '@nestjs/bullmq';

@Module({
  imports: [
    BullModule.forRoot({
      connection: {
        host: 'localhost',
        port: 6379,
      },
    }),
    BullModule.registerQueue({
      name: 'email',
    }),
  ],
})
export class AppModule {}
```

## Publish Jobs

```typescript
// src/notifications/notifications.service.ts
import { InjectQueue } from '@nestjs/bullmq';
import { Queue } from 'bullmq';

export class NotificationsService {
  constructor(@InjectQueue('email') private readonly queue: Queue) {}

  async sendWelcomeEmail(userId: number) {
    await this.queue.add('welcome', { userId });
  }
}
```

## Process Jobs

```typescript
// src/notifications/email.processor.ts
import { Processor, WorkerHost } from '@nestjs/bullmq';
import { Job } from 'bullmq';

@Processor('email')
export class EmailProcessor extends WorkerHost {
  async process(job: Job<{ userId: number }>) {
    if (job.name === 'welcome') {
      // send email
    }
  }
}
```

## Handling Failures and Retries

```typescript
await this.queue.add('welcome', { userId }, {
  attempts: 3,
  backoff: { type: 'exponential', delay: 5000 },
});
```

## Separate Worker Process

For large workloads, run workers separately from the HTTP app. You can start a worker-only Nest app that registers processors and exits when not needed.

## Tips

- Avoid putting secrets in job payloads.
- Set sensible retry limits.
- Use a shared Redis instance for multiple API nodes.

---

[Previous: WebSockets](./17-websockets.md) | [Back to Index](./README.md) | [Next: RBAC and Permissions ->](./19-rbac-permissions.md)
