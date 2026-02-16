# OpenTelemetry and Distributed Tracing

OpenTelemetry provides a unified observability framework for traces, metrics, and logs. This chapter covers instrumenting NestJS applications for production-grade observability.

## Goals

- Instrument NestJS with OpenTelemetry
- Implement distributed tracing
- Export traces to Jaeger/Zipkin
- Add custom spans and metrics
- Correlate logs with traces

## Install Dependencies

```bash
# Core OpenTelemetry
npm install @opentelemetry/api
npm install @opentelemetry/sdk-node
npm install @opentelemetry/auto-instrumentations-node

# Exporters
npm install @opentelemetry/exporter-trace-otlp-http
npm install @opentelemetry/exporter-metrics-otlp-http

# Specific instrumentations
npm install @opentelemetry/instrumentation-http
npm install @opentelemetry/instrumentation-express
npm install @opentelemetry/instrumentation-nestjs-core
npm install @opentelemetry/instrumentation-pg
npm install @opentelemetry/instrumentation-redis-4
```

## Tracing Setup

Create the tracing configuration file. This must be loaded before the NestJS app.

```typescript
// src/tracing.ts
import { NodeSDK } from '@opentelemetry/sdk-node';
import { getNodeAutoInstrumentations } from '@opentelemetry/auto-instrumentations-node';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http';
import { OTLPMetricExporter } from '@opentelemetry/exporter-metrics-otlp-http';
import { PeriodicExportingMetricReader } from '@opentelemetry/sdk-metrics';
import { Resource } from '@opentelemetry/resources';
import {
  ATTR_SERVICE_NAME,
  ATTR_SERVICE_VERSION,
  ATTR_DEPLOYMENT_ENVIRONMENT_NAME,
} from '@opentelemetry/semantic-conventions';

const resource = new Resource({
  [ATTR_SERVICE_NAME]: process.env.OTEL_SERVICE_NAME ?? 'nestjs-api',
  [ATTR_SERVICE_VERSION]: process.env.npm_package_version ?? '1.0.0',
  [ATTR_DEPLOYMENT_ENVIRONMENT_NAME]: process.env.NODE_ENV ?? 'development',
});

const traceExporter = new OTLPTraceExporter({
  url: process.env.OTEL_EXPORTER_OTLP_ENDPOINT ?? 'http://localhost:4318/v1/traces',
});

const metricExporter = new OTLPMetricExporter({
  url: process.env.OTEL_EXPORTER_OTLP_ENDPOINT ?? 'http://localhost:4318/v1/metrics',
});

const sdk = new NodeSDK({
  resource,
  traceExporter,
  metricReader: new PeriodicExportingMetricReader({
    exporter: metricExporter,
    exportIntervalMillis: 10000,
  }),
  instrumentations: [
    getNodeAutoInstrumentations({
      '@opentelemetry/instrumentation-fs': { enabled: false },
      '@opentelemetry/instrumentation-dns': { enabled: false },
    }),
  ],
});

sdk.start();

process.on('SIGTERM', () => {
  sdk.shutdown()
    .then(() => console.log('Tracing terminated'))
    .catch((error) => console.error('Error terminating tracing', error))
    .finally(() => process.exit(0));
});

export default sdk;
```

## Bootstrap with Tracing

```typescript
// src/main.ts
import './tracing'; // Must be first import
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  await app.listen(3000);
}
bootstrap();
```

## Environment Variables

```env
# OpenTelemetry
OTEL_SERVICE_NAME=my-nestjs-api
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318

# For Jaeger
OTEL_EXPORTER_JAEGER_ENDPOINT=http://localhost:14268/api/traces
```

## Custom Spans

Add manual instrumentation for business logic.

```typescript
// src/common/decorators/trace.decorator.ts
import { trace, SpanStatusCode, Span } from '@opentelemetry/api';

const tracer = trace.getTracer('nestjs-app');

export function Trace(spanName?: string) {
  return function (
    target: any,
    propertyKey: string,
    descriptor: PropertyDescriptor,
  ) {
    const originalMethod = descriptor.value;
    const name = spanName ?? `${target.constructor.name}.${propertyKey}`;

    descriptor.value = async function (...args: any[]) {
      return tracer.startActiveSpan(name, async (span: Span) => {
        try {
          const result = await originalMethod.apply(this, args);
          span.setStatus({ code: SpanStatusCode.OK });
          return result;
        } catch (error) {
          span.setStatus({
            code: SpanStatusCode.ERROR,
            message: error.message,
          });
          span.recordException(error);
          throw error;
        } finally {
          span.end();
        }
      });
    };

    return descriptor;
  };
}
```

### Using the Decorator

```typescript
// src/orders/orders.service.ts
import { Injectable } from '@nestjs/common';
import { Trace } from '../common/decorators/trace.decorator';

@Injectable()
export class OrdersService {
  @Trace()
  async createOrder(data: CreateOrderDto) {
    // Business logic is automatically traced
    return this.orderRepository.save(data);
  }

  @Trace('orders.process-payment')
  async processPayment(orderId: number) {
    // Custom span name
  }
}
```

## Adding Span Attributes

```typescript
import { trace, context } from '@opentelemetry/api';

@Injectable()
export class OrdersService {
  async createOrder(data: CreateOrderDto) {
    const span = trace.getActiveSpan();

    if (span) {
      span.setAttribute('order.user_id', data.userId);
      span.setAttribute('order.items_count', data.items.length);
      span.setAttribute('order.total', data.total);
    }

    const order = await this.orderRepository.save(data);

    if (span) {
      span.setAttribute('order.id', order.id);
    }

    return order;
  }
}
```

## Trace Context Propagation

### HTTP Client

```typescript
// src/common/http/traced-http.service.ts
import { Injectable, HttpService } from '@nestjs/common';
import { context, propagation, trace } from '@opentelemetry/api';

@Injectable()
export class TracedHttpService {
  constructor(private readonly httpService: HttpService) {}

  async get<T>(url: string, config?: any): Promise<T> {
    const headers = config?.headers ?? {};

    // Inject trace context into headers
    propagation.inject(context.active(), headers);

    const response = await this.httpService.axiosRef.get(url, {
      ...config,
      headers,
    });

    return response.data;
  }
}
```

### Extracting Context (Incoming Requests)

Auto-instrumentation handles this, but for custom scenarios:

```typescript
import { propagation, context, trace } from '@opentelemetry/api';

function extractTraceContext(headers: Record<string, string>) {
  return propagation.extract(context.active(), headers);
}
```

## Trace ID in Logs

### Winston Integration

```typescript
// src/common/logger/traced-logger.ts
import { LoggerService, Injectable, Scope } from '@nestjs/common';
import { trace, context } from '@opentelemetry/api';
import * as winston from 'winston';

@Injectable({ scope: Scope.TRANSIENT })
export class TracedLogger implements LoggerService {
  private logger: winston.Logger;
  private context?: string;

  constructor() {
    this.logger = winston.createLogger({
      format: winston.format.combine(
        winston.format.timestamp(),
        winston.format.json(),
      ),
      transports: [new winston.transports.Console()],
    });
  }

  setContext(context: string) {
    this.context = context;
  }

  private getTraceInfo() {
    const span = trace.getActiveSpan();
    if (!span) return {};

    const spanContext = span.spanContext();
    return {
      traceId: spanContext.traceId,
      spanId: spanContext.spanId,
    };
  }

  log(message: string, ...meta: any[]) {
    this.logger.info(message, {
      context: this.context,
      ...this.getTraceInfo(),
      meta,
    });
  }

  error(message: string, trace?: string, ...meta: any[]) {
    this.logger.error(message, {
      context: this.context,
      stack: trace,
      ...this.getTraceInfo(),
      meta,
    });
  }

  warn(message: string, ...meta: any[]) {
    this.logger.warn(message, {
      context: this.context,
      ...this.getTraceInfo(),
      meta,
    });
  }

  debug(message: string, ...meta: any[]) {
    this.logger.debug(message, {
      context: this.context,
      ...this.getTraceInfo(),
      meta,
    });
  }

  verbose(message: string, ...meta: any[]) {
    this.logger.verbose(message, {
      context: this.context,
      ...this.getTraceInfo(),
      meta,
    });
  }
}
```

## Custom Metrics

```typescript
// src/common/metrics/metrics.service.ts
import { Injectable, OnModuleInit } from '@nestjs/common';
import { metrics, Counter, Histogram } from '@opentelemetry/api';

@Injectable()
export class MetricsService implements OnModuleInit {
  private httpRequestsTotal: Counter;
  private httpRequestDuration: Histogram;
  private ordersCreated: Counter;
  private orderValue: Histogram;

  onModuleInit() {
    const meter = metrics.getMeter('nestjs-app');

    this.httpRequestsTotal = meter.createCounter('http_requests_total', {
      description: 'Total number of HTTP requests',
    });

    this.httpRequestDuration = meter.createHistogram('http_request_duration_ms', {
      description: 'HTTP request duration in milliseconds',
      unit: 'ms',
    });

    this.ordersCreated = meter.createCounter('orders_created_total', {
      description: 'Total number of orders created',
    });

    this.orderValue = meter.createHistogram('order_value', {
      description: 'Order value distribution',
      unit: 'USD',
    });
  }

  recordHttpRequest(method: string, path: string, statusCode: number, duration: number) {
    this.httpRequestsTotal.add(1, {
      method,
      path,
      status_code: statusCode.toString(),
    });

    this.httpRequestDuration.record(duration, {
      method,
      path,
    });
  }

  recordOrderCreated(value: number) {
    this.ordersCreated.add(1);
    this.orderValue.record(value);
  }
}
```

### Metrics Interceptor

```typescript
// src/common/interceptors/metrics.interceptor.ts
import {
  Injectable,
  NestInterceptor,
  ExecutionContext,
  CallHandler,
} from '@nestjs/common';
import { Observable, tap } from 'rxjs';
import { MetricsService } from '../metrics/metrics.service';

@Injectable()
export class MetricsInterceptor implements NestInterceptor {
  constructor(private readonly metricsService: MetricsService) {}

  intercept(context: ExecutionContext, next: CallHandler): Observable<any> {
    const request = context.switchToHttp().getRequest();
    const { method, route } = request;
    const path = route?.path ?? request.url;
    const start = Date.now();

    return next.handle().pipe(
      tap({
        next: () => {
          const response = context.switchToHttp().getResponse();
          const duration = Date.now() - start;
          this.metricsService.recordHttpRequest(method, path, response.statusCode, duration);
        },
        error: (error) => {
          const duration = Date.now() - start;
          const statusCode = error.status ?? 500;
          this.metricsService.recordHttpRequest(method, path, statusCode, duration);
        },
      }),
    );
  }
}
```

## Baggage for Context

Pass business context across services.

```typescript
import { propagation, context, ROOT_CONTEXT } from '@opentelemetry/api';

// Set baggage
const baggage = propagation.createBaggage({
  'user.id': { value: '12345' },
  'tenant.id': { value: 'acme-corp' },
});

const ctx = propagation.setBaggage(context.active(), baggage);

// Read baggage
const currentBaggage = propagation.getBaggage(context.active());
const userId = currentBaggage?.getEntry('user.id')?.value;
```

## Jaeger Setup

### docker-compose.yml

```yaml
version: '3.8'

services:
  jaeger:
    image: jaegertracing/all-in-one:1.54
    ports:
      - '16686:16686'  # UI
      - '14268:14268'  # Collector HTTP
      - '4317:4317'    # OTLP gRPC
      - '4318:4318'    # OTLP HTTP
    environment:
      COLLECTOR_OTLP_ENABLED: true

  app:
    build: .
    ports:
      - '3000:3000'
    environment:
      OTEL_SERVICE_NAME: nestjs-api
      OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318
    depends_on:
      - jaeger
```

## Grafana Tempo Setup

For production with Grafana stack:

```yaml
# docker-compose.yml
version: '3.8'

services:
  tempo:
    image: grafana/tempo:2.3.1
    command: ['-config.file=/etc/tempo.yaml']
    volumes:
      - ./tempo.yaml:/etc/tempo.yaml
    ports:
      - '4317:4317'
      - '4318:4318'

  grafana:
    image: grafana/grafana:10.2.3
    ports:
      - '3001:3000'
    environment:
      GF_AUTH_ANONYMOUS_ENABLED: true
      GF_AUTH_ANONYMOUS_ORG_ROLE: Admin
    volumes:
      - ./grafana-datasources.yaml:/etc/grafana/provisioning/datasources/datasources.yaml
```

```yaml
# tempo.yaml
server:
  http_listen_port: 3200

distributor:
  receivers:
    otlp:
      protocols:
        http:
        grpc:

storage:
  trace:
    backend: local
    local:
      path: /var/tempo/traces
```

## Sampling Strategies

### Head-Based Sampling

```typescript
import { TraceIdRatioBasedSampler } from '@opentelemetry/sdk-trace-base';

const sdk = new NodeSDK({
  sampler: new TraceIdRatioBasedSampler(0.1), // Sample 10%
  // ...
});
```

### Parent-Based Sampling

```typescript
import {
  ParentBasedSampler,
  TraceIdRatioBasedSampler,
  AlwaysOnSampler,
} from '@opentelemetry/sdk-trace-base';

const sdk = new NodeSDK({
  sampler: new ParentBasedSampler({
    root: new TraceIdRatioBasedSampler(0.1),
    remoteParentSampled: new AlwaysOnSampler(),
    remoteParentNotSampled: new TraceIdRatioBasedSampler(0.01),
  }),
});
```

### Environment-Based

```typescript
const samplingRate = process.env.NODE_ENV === 'production' ? 0.1 : 1.0;

const sdk = new NodeSDK({
  sampler: new TraceIdRatioBasedSampler(samplingRate),
});
```

## Database Instrumentation

PostgreSQL traces are automatic with `@opentelemetry/instrumentation-pg`:

```typescript
// Automatic spans for queries
const users = await this.userRepository.find();
// Creates span: pg.query SELECT * FROM users
```

Add custom attributes:

```typescript
import { trace } from '@opentelemetry/api';

async findUsers(filters: UserFilters) {
  const span = trace.getActiveSpan();
  span?.setAttribute('db.filters', JSON.stringify(filters));

  return this.userRepository.find({ where: filters });
}
```

## Health Check with Tracing

```typescript
// src/health/health.controller.ts
import { Controller, Get } from '@nestjs/common';
import { HealthCheck, HealthCheckService } from '@nestjs/terminus';
import { trace } from '@opentelemetry/api';

@Controller('health')
export class HealthController {
  constructor(private health: HealthCheckService) {}

  @Get()
  @HealthCheck()
  check() {
    const span = trace.getActiveSpan();
    span?.setAttribute('health.check', true);

    return this.health.check([
      // health indicators
    ]);
  }
}
```

## Trace Response Header

Return trace ID to clients for debugging.

```typescript
// src/common/interceptors/trace-id.interceptor.ts
import {
  Injectable,
  NestInterceptor,
  ExecutionContext,
  CallHandler,
} from '@nestjs/common';
import { Observable } from 'rxjs';
import { trace } from '@opentelemetry/api';

@Injectable()
export class TraceIdInterceptor implements NestInterceptor {
  intercept(context: ExecutionContext, next: CallHandler): Observable<any> {
    const response = context.switchToHttp().getResponse();
    const span = trace.getActiveSpan();

    if (span) {
      const traceId = span.spanContext().traceId;
      response.setHeader('X-Trace-Id', traceId);
    }

    return next.handle();
  }
}
```

## Tips

- Initialize tracing before any other imports in main.ts.
- Use semantic conventions for attribute names.
- Sample traces in production to reduce costs.
- Correlate logs with trace IDs for debugging.
- Add business context as span attributes.
- Use baggage sparingly (it's propagated everywhere).
- Monitor exporter errors and queue sizes.
- Set appropriate export intervals for metrics.

---

[Previous: OAuth2 and Social Auth](./31-oauth2-social-auth.md) | [Back to Index](./README.md)
