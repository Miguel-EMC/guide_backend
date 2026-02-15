# 18 - Observability and Logging

Production APIs need structured logs, metrics, and traces. This chapter builds a practical observability stack for Express.

## Goals

- Capture reliable logs without leaking secrets
- Export metrics for latency, errors, and saturation
- Enable distributed tracing when services grow

## 1. Structured Logging with Pino

```bash
npm install pino pino-http
```

```typescript
import pino from 'pino';
import pinoHttp from 'pino-http';
import { randomUUID } from 'crypto';

export const logger = pino({
  level: process.env.LOG_LEVEL ?? 'info',
  redact: ['req.headers.authorization', 'req.headers.cookie'],
});

app.use(
  pinoHttp({
    logger,
    genReqId: (req) => (req.headers['x-request-id'] as string) ?? randomUUID(),
    customProps: (req) => ({ requestId: req.id }),
  }),
);
```

## 2. Request IDs and Correlation

```typescript
app.use((req, res, next) => {
  const id = (req as any).id as string;
  res.setHeader('x-request-id', id);
  next();
});
```

Forward the same ID to downstream services to correlate traces and logs.

## 3. Metrics with Prometheus

```bash
npm install prom-client
```

```typescript
import client from 'prom-client';

const register = new client.Registry();
client.collectDefaultMetrics({ register });

const httpDuration = new client.Histogram({
  name: 'http_request_duration_seconds',
  help: 'HTTP request duration in seconds',
  labelNames: ['method', 'route', 'status'],
  buckets: [0.01, 0.05, 0.1, 0.3, 0.5, 1, 2],
});
register.registerMetric(httpDuration);

app.use((req, res, next) => {
  const end = httpDuration.startTimer();
  res.on('finish', () => {
    end({
      method: req.method,
      route: req.route?.path ?? req.path,
      status: String(res.statusCode),
    });
  });
  next();
});

app.get('/metrics', async (_req, res) => {
  res.setHeader('Content-Type', register.contentType);
  res.end(await register.metrics());
});
```

## 4. Tracing with OpenTelemetry

```bash
npm install @opentelemetry/sdk-node @opentelemetry/auto-instrumentations-node
```

```typescript
import { NodeSDK } from '@opentelemetry/sdk-node';
import { getNodeAutoInstrumentations } from '@opentelemetry/auto-instrumentations-node';

const sdk = new NodeSDK({
  instrumentations: [getNodeAutoInstrumentations()],
});

sdk.start();
```

Configure an OTLP exporter to send traces to your collector.

## 5. Health and Readiness Endpoints

```typescript
app.get('/healthz', (_req, res) => {
  res.json({ status: 'ok', ts: new Date().toISOString() });
});

app.get('/readyz', async (_req, res) => {
  try {
    await prisma.$queryRaw`SELECT 1`;
    res.json({ status: 'ready' });
  } catch {
    res.status(503).json({ status: 'not_ready' });
  }
});
```

## 6. Error Reporting

Capture unhandled errors and forward them to a central system.

```typescript
app.use((err: unknown, _req, _res, next) => {
  logger.error({ err }, 'request_failed');
  next(err);
});
```

## 7. Log Levels and Sampling

- Use `debug` only in local dev.
- Sample noisy logs in high traffic routes.
- Never log secrets or raw tokens.

## Tips

- Track golden signals: latency, traffic, errors, saturation.
- Use dashboards per service and per endpoint.
- Add SLO alerts only after you have stable metrics.

---

[Previous: Security Hardening](./17-security-hardening.md) | [Back to Index](./README.md) | [Next: Performance and Scaling ->](./19-performance-and-scaling.md)
