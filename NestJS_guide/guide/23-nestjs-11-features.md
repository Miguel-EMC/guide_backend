# NestJS 11 Advanced Features & Express v5

This chapter covers the most significant features introduced in NestJS 11, including Express v5 integration, performance improvements, and modern development patterns for 2026.

## Express v5 Integration

NestJS 11 features native Express v5 support, bringing significant performance improvements and modern routing capabilities.

### Express v5 Breaking Changes

| Change | Express v4 | Express v5 |
|--------|-------------|-------------|
| **Wildcard Routes** | `/*` | `/*splat` (named required) |
| **Promise Support** | Basic | Full native support |
| **Async Handlers** | Via wrapper | Native async/await |
| **Error Handling** | Basic | Enhanced context |
| **Performance** | Good | ~20% faster |

### Migrating to Express v5

```typescript
// OLD Express v4 style (deprecated in NestJS 11)
@Controller('api')
export class LegacyController {
  @Get('*')
  wildcardHandler() {
    return { message: 'Wildcard matched' };
  }
}

// NEW Express v5 style (NestJS 11)
@Controller('api')
export class ModernController {
  @Get('*splat')  // Named wildcard required
  wildcardHandler(@Param('splat') splat: string) {
    return { 
      message: 'Wildcard matched',
      path: splat 
    };
  }

  @Get('async')
  async asyncHandler() {
    // Native async handler
    const result = await this.externalService.getData();
    return { data: result };
  }
}
```

### Custom Express v5 Middleware

```typescript
// middleware/express-v5.middleware.ts
import { Request, Response, NextFunction } from 'express';

export function expressV5Middleware(
  req: Request, 
  res: Response, 
  next: NextFunction
) {
  // Enhanced error handling in Express v5
  res.setHeader('X-Express-Version', '5.0');
  
  // Better async error handling
  try {
    // Your middleware logic
    next();
  } catch (error) {
    // Express v5 handles async errors better
    next(error);
  }
}

// In module
@Module({
  providers: [
    {
      provide: 'APP_FILTER',
      useFactory: () => expressV5Middleware,
      inject: [],
    },
  ],
})
export class AppModule {}
```

## Performance Optimizations

NestJS 11 introduces significant performance improvements.

### Dependency Injection Enhancements

```typescript
// Enhanced DI with better memory management
@Injectable()
export class OptimizedService {
  private cache = new Map<string, any>();
  
  constructor(
    private readonly httpService: HttpService,
    private readonly configService: ConfigService,
  ) {
    // DI is now 15-20% faster
  }

  @Cache('results', 300) // 5 minutes
  async getOptimizedData(id: string): Promise<any> {
    // Check cache first
    if (this.cache.has(id)) {
      return this.cache.get(id);
    }

    const data = await this.httpService
      .get(`https://api.example.com/data/${id}`)
      .toPromise();

    // Cache result
    this.cache.set(id, data);
    return data;
  }
}
```

### Lazy Loading Modules

```typescript
// Lazy module loading for better startup time
@Module({
  imports: [
    // Eager loading (default)
    CoreModule,
    
    // Lazy loading (new in NestJS 11)
    {
      module: HeavyModule,
      lazy: true, // Only load when needed
    },
  ],
})
export class AppModule {}

// Dynamic module loading
@Controller('dynamic')
export class DynamicController {
  constructor(
    private readonly moduleRef: ModuleRef,
  ) {}

  @Post('load-feature')
  async loadFeature(@Body() body: { feature: string }) {
    // Dynamically load module at runtime
    switch (body.feature) {
      case 'analytics':
        await this.moduleRef.create(AnalyticsModule);
        break;
      case 'reports':
        await this.moduleRef.create(ReportsModule);
        break;
    }
    
    return { message: `${body.feature} loaded` };
  }
}
```

## Enhanced CLI & Development Experience

NestJS 11 CLI features AI-assisted code generation and improved developer tools.

### AI-Powered Code Generation

```bash
# Generate controller with AI assistance
nest g controller users --ai --crud

# Generate service with best practices
nest g service analytics --ai --optimizable

# Generate module with dependency recommendations
nest g module notifications --ai --suggest-deps

# AI code review
nest review --module=users --ai-analysis

# Performance optimization suggestions
nest analyze --performance --ai-recommendations
```

### Enhanced Testing Commands

```bash
# Generate comprehensive test suite
nest test --generate --coverage --ai

# Performance testing
nest test:performance --load-test --report

# Integration testing with mock generation
nest test:integration --auto-mocks --ai
```

## Modern Error Handling

Enhanced error handling with better context and debugging information.

### Structured Error Handling

```typescript
// Enhanced error classes
export class ValidationError extends HttpException {
  constructor(
    message: string,
    public readonly details: Record<string, any>,
    public readonly context?: any,
  ) {
    super(message, HttpStatus.BAD_REQUEST);
    
    // Enhanced error metadata
    this.name = 'ValidationError';
    this.timestamp = new Date();
    this.traceId = context?.traceId;
  }
}

// Global exception filter with Express v5 support
@Catch()
export class GlobalExceptionFilter implements ExceptionFilter {
  catch(exception: unknown, host: ArgumentsHost): void {
    const ctx = host.switchToHttp();
    const response = ctx.getResponse<Response>();
    const request = ctx.getRequest<Request>();

    // Enhanced error context in Express v5
    const error = {
      timestamp: new Date().toISOString(),
      path: request.url,
      method: request.method,
      traceId: request.headers['x-trace-id'],
      
      // Detailed error information
      error: {
        name: exception?.constructor.name,
        message: exception?.message,
        details: exception instanceof ValidationError 
          ? exception.details 
          : null,
        stack: process.env.NODE_ENV === 'development' 
          ? exception?.stack 
          : undefined,
      },
    };

    response
      .status(exception instanceof HttpException 
        ? exception.getStatus() 
        : HttpStatus.INTERNAL_SERVER_ERROR)
      .json(error);
  }
}
```

## Advanced Dependency Injection

### Circular Dependency Resolution

```typescript
// ForwardRef for circular dependencies (improved in v11)
@Injectable()
export class UserService {
  constructor(
    @Inject(forwardRef(() => NotificationService))
    private readonly notificationService: NotificationService,
  ) {}

  async notifyUser(userId: string, message: string) {
    // Circular dependency resolved better in NestJS 11
    await this.notificationService.send(userId, message);
  }
}

@Injectable()
export class NotificationService {
  constructor(
    @Inject(forwardRef(() => UserService))
    private readonly userService: UserService,
  ) {}

  async getUserPreferences(userId: string) {
    // Better circular dependency handling
    return await this.userService.getPreferences(userId);
  }
}
```

### Scoped Dependencies

```typescript
// Request-scoped dependencies (enhanced)
@Injectable({ scope: Scope.REQUEST })
export class RequestContextService {
  constructor(@Inject(REQUEST) private readonly request: Request) {}

  // Automatic cleanup at request end
  getTraceId(): string {
    return this.request.headers['x-trace-id'] || 'unknown';
  }

  getUserAgent(): string {
    return this.request.headers['user-agent'];
  }
}

// Transient dependencies (new feature)
@Injectable({ scope: Scope.TRANSIENT })
export class TransientService {
  // New instance for each injection
  private readonly id = Math.random();

  getId(): string {
    return this.id.toString();
  }
}
```

## Real-time Features with Enhanced WebSockets

### WebSocket Gateway with Express v5

```typescript
@WebSocketGateway({
  cors: {
    origin: process.env.ALLOWED_ORIGINS?.split(','),
    credentials: true,
  },
  transports: ['websocket', 'polling'],
})
export class RealtimeGateway implements OnGatewayConnection, OnGatewayDisconnect {
  @WebSocketServer()
  server: Server;

  private readonly connectedClients = new Map<string, Socket>();

  constructor(
    private readonly userService: UserService,
    private readonly notificationService: NotificationService,
  ) {}

  async handleConnection(client: Socket, ...args: any[]): Promise<void> {
    // Enhanced connection handling
    const token = client.handshake.auth.token;
    
    try {
      const user = await this.userService.validateToken(token);
      
      // Store connection metadata
      this.connectedClients.set(client.id, {
        client,
        user,
        connectedAt: new Date(),
        lastActivity: new Date(),
      });

      client.emit('connected', { 
        userId: user.id,
        timestamp: new Date().toISOString(),
      });

      // Join user-specific rooms
      await client.join(`user:${user.id}`);
      await client.join(`role:${user.role}`);

    } catch (error) {
      client.emit('error', { message: 'Authentication failed' });
      client.disconnect();
    }
  }

  @SubscribeMessage('join-room')
  async handleJoinRoom(
    client: Socket, 
    payload: { room: string }
  ): Promise<void> {
    const connection = this.connectedClients.get(client.id);
    
    if (!connection) {
      throw new WsException('Not authenticated');
    }

    // Enhanced room management
    await client.join(payload.room);
    
    client.emit('joined-room', {
      room: payload.room,
      timestamp: new Date().toISOString(),
    });

    // Notify other room members
    client.to(payload.room).emit('user-joined', {
      user: connection.user,
      timestamp: new Date().toISOString(),
    });
  }

  @SubscribeMessage('send-message')
  async handleMessage(
    client: Socket, 
    payload: { room: string; content: string }
  ): Promise<void> {
    const connection = this.connectedClients.get(client.id);
    
    // AI-powered message processing
    const processedMessage = await this.processMessageWithAI(
      payload.content,
      connection.user
    );

    // Broadcast to room
    this.server.to(payload.room).emit('message', {
      id: generateId(),
      user: connection.user,
      content: processedMessage,
      room: payload.room,
      timestamp: new Date().toISOString(),
    });

    // Update last activity
    connection.lastActivity = new Date();
  }

  handleDisconnect(client: Socket): void {
    const connection = this.connectedClients.get(client.id);
    
    if (connection) {
      // Leave all rooms
      client.rooms.forEach(room => {
        if (room !== client.id) {
          client.leave(room);
        }
      });

      // Notify disconnection
      this.server.emit('user-disconnected', {
        user: connection.user,
        timestamp: new Date().toISOString(),
      });

      this.connectedClients.delete(client.id);
    }
  }

  private async processMessageWithAI(
    content: string, 
    user: User
  ): Promise<string> {
    // AI content moderation/enhancement
    if (process.env.AI_MESSAGE_PROCESSING === 'enabled') {
      const result = await this.aiService.processMessage(content, {
        userRole: user.role,
        userPermissions: user.permissions,
        context: 'chat',
      });
      
      return result.processedContent;
    }
    
    return content;
  }
}
```

## AI Integration Patterns

### AI-Powered Controllers

```typescript
// AI-enhanced controller with Express v5
@Controller('ai-assistant')
export class AIAssistantController {
  constructor(
    private readonly aiService: AIService,
    private readonly userService: UserService,
  ) {}

  @Post('analyze')
  async analyzeContent(@Body() body: { content: string; context?: string }) {
    const analysis = await this.aiService.analyze(body.content, {
      context: body.context,
      type: 'content-analysis',
      depth: 'comprehensive',
    });

    return {
      original: body.content,
      analysis: analysis,
      timestamp: new Date().toISOString(),
    };
  }

  @Post('generate')
  async generateContent(@Body() body: { 
    prompt: string; 
    type: 'text' | 'code' | 'email';
    tone?: 'professional' | 'casual' | 'technical';
  }) {
    const user = await this.userService.getCurrentUser();
    
    // Context-aware content generation
    const context = {
      userType: user.role,
      preferences: user.preferences,
      history: user.recentActivity,
    };

    const generated = await this.aiService.generate(body.prompt, {
      type: body.type,
      tone: body.tone || 'professional',
      context: context,
      maxTokens: 2000,
      temperature: 0.7,
    });

    return {
      content: generated.content,
      usage: generated.usage,
      timestamp: new Date().toISOString(),
    };
  }

  @Post('optimize')
  async optimizeCode(@Body() body: { 
    code: string; 
    language: string;
    target: 'performance' | 'readability' | 'security';
  }) {
    const optimization = await this.aiService.optimizeCode(body.code, {
      language: body.language,
      target: body.target,
      preserveFunctionality: true,
      explainChanges: true,
    });

    return {
      original: body.code,
      optimized: optimization.optimizedCode,
      improvements: optimization.improvements,
      explanation: optimization.explanation,
      confidence: optimization.confidence,
    };
  }
}
```

## Performance Monitoring & Analytics

### Built-in Performance Tracking

```typescript
// Performance interceptor (enhanced in NestJS 11)
@Injectable()
export class PerformanceInterceptor implements NestInterceptor {
  constructor(
    private readonly metricsService: MetricsService,
  ) {}

  intercept(context: ExecutionContext, next: CallHandler): Observable<any> {
    const start = Date.now();
    const request = context.switchToHttp().getRequest<Request>();
    
    const traceId = request.headers['x-trace-id'] || generateId();
    
    return next.handle().pipe(
      tap(() => {
        const duration = Date.now() - start;
        
        // Enhanced performance tracking
        this.metricsService.recordRequest({
          method: request.method,
          url: request.url,
          statusCode: context.switchToHttp().getResponse().statusCode,
          duration,
          traceId,
          timestamp: new Date(),
          
          // Additional Express v5 metrics
          userAgent: request.headers['user-agent'],
          contentLength: request.headers['content-length'],
          responseTime: duration,
          
          // Performance categories
          category: this.categorizePerformance(duration),
        });
      }),
      
      // AI-powered performance analysis
      catchError((error) => {
        const duration = Date.now() - start;
        
        this.metricsService.recordError({
          error: error.message,
          stack: error.stack,
          duration,
          traceId,
          context: context.getClass().name,
          aiAnalysis: process.env.AI_ERROR_ANALYSIS === 'enabled'
            ? this.aiService.analyzeError(error)
            : null,
        });
        
        throw error;
      }),
    );
  }

  private categorizePerformance(duration: number): string {
    if (duration < 100) return 'fast';
    if (duration < 500) return 'normal';
    if (duration < 2000) return 'slow';
    return 'very-slow';
  }
}
```

## Testing Modern Features

### Testing AI Integration

```typescript
describe('AIAssistantController', () => {
  let controller: AIAssistantController;
  let aiService: jest.Mocked<AIService>;

  beforeEach(async () => {
    const module = await Test.createTestingModule({
      controllers: [AIAssistantController],
      providers: [
        {
          provide: AIService,
          useValue: {
            analyze: jest.fn(),
            generate: jest.fn(),
            optimizeCode: jest.fn(),
          },
        },
      ],
    }).compile();

    controller = module.get<AIAssistantController>(AIAssistantController);
    aiService = module.get(AIService);
  });

  describe('generateContent', () => {
    it('should generate content with AI assistance', async () => {
      const mockResponse = {
        content: 'Generated professional email content',
        usage: { tokens: 150, cost: 0.003 },
      };

      aiService.generate.mockResolvedValue(mockResponse);

      const result = await controller.generateContent({
        prompt: 'Write a welcome email',
        type: 'email',
        tone: 'professional',
      });

      expect(result).toEqual({
        content: 'Generated professional email content',
        usage: { tokens: 150, cost: 0.003 },
        timestamp: expect.any(String),
      });

      expect(aiService.generate).toHaveBeenCalledWith(
        'Write a welcome email',
        expect.objectContaining({
          type: 'email',
          tone: 'professional',
          maxTokens: 2000,
          temperature: 0.7,
        })
      );
    });
  });
});
```

### Performance Testing

```typescript
describe('PerformanceInterceptor', () => {
  let interceptor: PerformanceInterceptor;
  let metricsService: jest.Mocked<MetricsService>;

  beforeEach(() => {
    metricsService = {
      recordRequest: jest.fn(),
      recordError: jest.fn(),
    } as any;

    interceptor = new PerformanceInterceptor(metricsService);
  });

  it('should record performance metrics', async () => {
    const context = createMock<ExecutionContext>();
    const handler = {
      handle: jest.fn().mockReturnValue(of('response')),
    };

    await interceptor.intercept(context, handler).toPromise();

    expect(metricsService.recordRequest).toHaveBeenCalledWith(
      expect.objectContaining({
        method: expect.any(String),
        url: expect.any(String),
        duration: expect.any(Number),
        timestamp: expect.any(Date),
        category: expect.any(String),
      })
    );
  });
});
```

## Deployment Strategies

### Optimized Production Configuration

```typescript
// main.ts (production optimized)
import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { ExpressAdapter } from '@nestjs/platform-express';
import * as express from 'express';

async function bootstrap() {
  const server = express();
  
  // Express v5 optimizations
  server.set('trust proxy', 1);
  server.set('strict routing', true);
  
  const app = await NestFactory.create(
    AppModule,
    new ExpressAdapter(server),
    {
      // NestJS 11 optimizations
      snapshot: false,
      abortOnError: false,
      
      // Performance settings
      logger: ['error', 'warn'], // Production logging
      bufferLogs: true,
    }
  );

  // Global validation
  app.useGlobalPipes(
    new ValidationPipe({
      transform: true,
      whitelist: true,
      forbidNonWhitelisted: true,
      transformOptions: {
        enableImplicitConversion: true,
      },
    })
  );

  // Enable CORS for frontend
  app.enableCors({
    origin: process.env.ALLOWED_ORIGINS?.split(','),
    credentials: true,
  });

  // Start server
  await app.listen(process.env.PORT || 3000);
  
  console.log(`Application running on port ${process.env.PORT || 3000}`);
}

// Handle graceful shutdown
process.on('SIGTERM', async () => {
  console.log('SIGTERM received, shutting down gracefully');
  process.exit(0);
});

process.on('SIGINT', async () => {
  console.log('SIGINT received, shutting down gracefully');
  process.exit(0);
});

bootstrap().catch(error => {
  console.error('Application failed to start:', error);
  process.exit(1);
});
```

### Docker with Express v5

```dockerfile
# Dockerfile for NestJS 11 + Express v5
FROM node:22-alpine AS builder

WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production && npm cache clean --force

COPY . .
RUN npm run build

FROM node:22-alpine AS runtime

WORKDIR /app
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/package.json ./package.json

# Health check for Express v5
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:${PORT:-3000}/health || exit 1

EXPOSE 3000
USER node

CMD ["node", "dist/main"]
```

## Migration Guide for NestJS 11

### Breaking Changes

1. **Express v5 Default**
   ```typescript
   // Old: Express v4 style
   @Get('*')
   handler() { /* ... */ }

   // New: Express v5 style  
   @Get('*splat')
   handler(@Param('splat') splat: string) { /* ... */ }
   ```

2. **TypeScript 5.x Compatibility**
   ```bash
   # Update TypeScript
   npm install typescript@^5.0
   npm install @types/node@^20.0
   ```

3. **Enhanced Performance Mode**
   ```typescript
   // Enable new performance features
   await NestFactory.create(AppModule, null, {
     snapshot: false,  // Better startup time
     abortOnError: false,  // Enhanced error handling
   });
   ```

### Upgrade Steps

1. **Update Dependencies**
   ```bash
   npm update @nestjs/core @nestjs/common @nestjs/platform-express
   npm install typescript@^5.0
   ```

2. **Fix Wildcard Routes**
   - Rename `*` to `*splat` in all route handlers
   - Update parameter decorators accordingly

3. **Update TypeScript Config**
   ```json
   {
     "compilerOptions": {
       "target": "ES2022",
       "module": "commonjs",
       "strict": true,
       "esModuleInterop": true,
       "skipLibCheck": true
     }
   }
   ```

4. **Test Performance Improvements**
   - Verify application startup time
   - Monitor memory usage
   - Test concurrent request handling

## Best Practices for NestJS 11

### Do's

```typescript
// ✅ Use Express v5 named wildcards
@Get('*splat')
wildcardHandler(@Param('splat') path: string) { /* ... */ }

// ✅ Leverage performance optimizations
await NestFactory.create(AppModule, null, {
  snapshot: false,
  abortOnError: false,
});

// ✅ Use enhanced error handling
@Catch()
export class GlobalExceptionFilter implements ExceptionFilter {
  // Enhanced error context
}

// ✅ Implement AI integration patterns
@Controller('ai')
export class AIController {
  // AI-powered features
}
```

### Don'ts

```typescript
// ❌ Don't use old wildcard syntax
@Get('*')  // Express v4 syntax

// ❌ Don't ignore performance settings
// Always enable optimizations in production

// ❌ Don't skip TypeScript 5.x features
// Leverage modern type system

// ❌ Don't ignore AI integration opportunities
// Build intelligent features
```

## Next Steps

- [Testing](./11-testing.md) - Advanced testing strategies
- [Deployment](./12-deployment.md) - Production deployment strategies
- [Common Patterns](./22-common-patterns.md) - Enterprise architecture patterns

---

[Back to Index](./README.md) | [Next: Testing](./11-testing.md)