# 07 - Advanced Design Patterns

This chapter covers additional design patterns commonly used in backend development that weren't covered in the overview.

---

## Creational Patterns

### Builder Pattern

**Intent**: Separate the construction of a complex object from its representation, allowing the same construction process to create different representations.

**Use Case**: Creating objects with many optional parameters, like HTTP requests, queries, or configuration objects.

```typescript
// Bad: Constructor with many parameters
const request = new HttpRequest(
  'POST',
  '/api/users',
  { 'Content-Type': 'application/json' },
  JSON.stringify({ name: 'John' }),
  5000,
  true,
  3,
);

// Good: Builder pattern
const request = new HttpRequestBuilder()
  .method('POST')
  .url('/api/users')
  .header('Content-Type', 'application/json')
  .body({ name: 'John' })
  .timeout(5000)
  .retry(3)
  .build();
```

**Implementation:**

```typescript
// HttpRequestBuilder.ts
class HttpRequest {
  constructor(
    public readonly method: string,
    public readonly url: string,
    public readonly headers: Record<string, string>,
    public readonly body: any,
    public readonly timeout: number,
    public readonly retries: number,
  ) {}
}

class HttpRequestBuilder {
  private _method: string = 'GET';
  private _url: string = '';
  private _headers: Record<string, string> = {};
  private _body: any = null;
  private _timeout: number = 30000;
  private _retries: number = 0;

  method(method: string): this {
    this._method = method;
    return this;
  }

  url(url: string): this {
    this._url = url;
    return this;
  }

  header(key: string, value: string): this {
    this._headers[key] = value;
    return this;
  }

  headers(headers: Record<string, string>): this {
    this._headers = { ...this._headers, ...headers };
    return this;
  }

  body(body: any): this {
    this._body = body;
    return this;
  }

  timeout(ms: number): this {
    this._timeout = ms;
    return this;
  }

  retry(count: number): this {
    this._retries = count;
    return this;
  }

  build(): HttpRequest {
    if (!this._url) {
      throw new Error('URL is required');
    }
    return new HttpRequest(
      this._method,
      this._url,
      this._headers,
      this._body,
      this._timeout,
      this._retries,
    );
  }
}
```

**Query Builder Example:**

```typescript
class QueryBuilder {
  private _select: string[] = ['*'];
  private _from: string = '';
  private _where: string[] = [];
  private _orderBy: string[] = [];
  private _limit: number | null = null;
  private _offset: number | null = null;
  private _params: any[] = [];

  select(...columns: string[]): this {
    this._select = columns;
    return this;
  }

  from(table: string): this {
    this._from = table;
    return this;
  }

  where(condition: string, ...params: any[]): this {
    this._where.push(condition);
    this._params.push(...params);
    return this;
  }

  orderBy(column: string, direction: 'ASC' | 'DESC' = 'ASC'): this {
    this._orderBy.push(`${column} ${direction}`);
    return this;
  }

  limit(count: number): this {
    this._limit = count;
    return this;
  }

  offset(count: number): this {
    this._offset = count;
    return this;
  }

  build(): { sql: string; params: any[] } {
    let sql = `SELECT ${this._select.join(', ')} FROM ${this._from}`;

    if (this._where.length > 0) {
      sql += ` WHERE ${this._where.join(' AND ')}`;
    }

    if (this._orderBy.length > 0) {
      sql += ` ORDER BY ${this._orderBy.join(', ')}`;
    }

    if (this._limit !== null) {
      sql += ` LIMIT ${this._limit}`;
    }

    if (this._offset !== null) {
      sql += ` OFFSET ${this._offset}`;
    }

    return { sql, params: this._params };
  }
}

// Usage
const query = new QueryBuilder()
  .select('id', 'name', 'email')
  .from('users')
  .where('status = $1', 'active')
  .where('role = $2', 'admin')
  .orderBy('created_at', 'DESC')
  .limit(10)
  .offset(20)
  .build();
```

---

### Prototype Pattern

**Intent**: Create new objects by copying an existing object (prototype).

**Use Case**: Creating objects that are expensive to create from scratch, or when you need variations of an object.

```typescript
interface Cloneable<T> {
  clone(): T;
}

class ReportTemplate implements Cloneable<ReportTemplate> {
  constructor(
    public title: string,
    public headers: string[],
    public styling: Record<string, string>,
    public footer: string,
  ) {}

  clone(): ReportTemplate {
    return new ReportTemplate(
      this.title,
      [...this.headers],
      { ...this.styling },
      this.footer,
    );
  }
}

// Usage
const baseTemplate = new ReportTemplate(
  'Monthly Report',
  ['Date', 'Revenue', 'Expenses'],
  { fontSize: '12px', color: '#333' },
  'Company Confidential',
);

const salesReport = baseTemplate.clone();
salesReport.title = 'Sales Report';
salesReport.headers = ['Date', 'Product', 'Quantity', 'Revenue'];

const expenseReport = baseTemplate.clone();
expenseReport.title = 'Expense Report';
expenseReport.headers = ['Date', 'Category', 'Amount', 'Vendor'];
```

---

## Structural Patterns

### Facade Pattern

**Intent**: Provide a unified interface to a set of interfaces in a subsystem. Facade defines a higher-level interface that makes the subsystem easier to use.

**Use Case**: Simplifying complex APIs, hiding library complexity, or providing a simple interface to a complex system.

```typescript
// Complex subsystem classes
class VideoConverter {
  convert(filename: string, format: string): Buffer {
    console.log(`Converting ${filename} to ${format}`);
    return Buffer.from('converted video');
  }
}

class AudioExtractor {
  extract(videoBuffer: Buffer): Buffer {
    console.log('Extracting audio');
    return Buffer.from('audio');
  }
}

class ThumbnailGenerator {
  generate(videoBuffer: Buffer, timestamp: number): Buffer {
    console.log(`Generating thumbnail at ${timestamp}s`);
    return Buffer.from('thumbnail');
  }
}

class StorageService {
  upload(buffer: Buffer, path: string): string {
    console.log(`Uploading to ${path}`);
    return `https://storage.example.com/${path}`;
  }
}

// Facade
class VideoProcessingFacade {
  private videoConverter = new VideoConverter();
  private audioExtractor = new AudioExtractor();
  private thumbnailGenerator = new ThumbnailGenerator();
  private storageService = new StorageService();

  async processVideo(filename: string): Promise<ProcessedVideo> {
    // Complex orchestration hidden behind simple method
    const videoBuffer = this.videoConverter.convert(filename, 'mp4');
    const audioBuffer = this.audioExtractor.extract(videoBuffer);
    const thumbnail = this.thumbnailGenerator.generate(videoBuffer, 5);

    const videoUrl = this.storageService.upload(videoBuffer, `videos/${filename}.mp4`);
    const audioUrl = this.storageService.upload(audioBuffer, `audio/${filename}.mp3`);
    const thumbnailUrl = this.storageService.upload(thumbnail, `thumbnails/${filename}.jpg`);

    return {
      videoUrl,
      audioUrl,
      thumbnailUrl,
    };
  }
}

// Usage - Simple interface
const facade = new VideoProcessingFacade();
const result = await facade.processVideo('my-video');
```

---

### Proxy Pattern

**Intent**: Provide a surrogate or placeholder for another object to control access to it.

**Use Case**: Lazy loading, access control, logging, caching.

```typescript
// Interface
interface DatabaseConnection {
  query(sql: string): Promise<any[]>;
  close(): void;
}

// Real implementation
class RealDatabaseConnection implements DatabaseConnection {
  constructor(private connectionString: string) {
    console.log('Opening database connection...');
    // Expensive connection setup
  }

  async query(sql: string): Promise<any[]> {
    console.log(`Executing: ${sql}`);
    return [{ id: 1, name: 'Test' }];
  }

  close(): void {
    console.log('Closing connection');
  }
}

// Lazy Loading Proxy
class LazyDatabaseProxy implements DatabaseConnection {
  private realConnection: RealDatabaseConnection | null = null;

  constructor(private connectionString: string) {}

  private getConnection(): RealDatabaseConnection {
    if (!this.realConnection) {
      this.realConnection = new RealDatabaseConnection(this.connectionString);
    }
    return this.realConnection;
  }

  async query(sql: string): Promise<any[]> {
    return this.getConnection().query(sql);
  }

  close(): void {
    if (this.realConnection) {
      this.realConnection.close();
      this.realConnection = null;
    }
  }
}

// Logging Proxy
class LoggingDatabaseProxy implements DatabaseConnection {
  constructor(private connection: DatabaseConnection) {}

  async query(sql: string): Promise<any[]> {
    const start = Date.now();
    console.log(`[DB] Executing query: ${sql}`);

    try {
      const result = await this.connection.query(sql);
      console.log(`[DB] Query completed in ${Date.now() - start}ms, rows: ${result.length}`);
      return result;
    } catch (error) {
      console.error(`[DB] Query failed: ${error.message}`);
      throw error;
    }
  }

  close(): void {
    this.connection.close();
  }
}

// Caching Proxy
class CachingDatabaseProxy implements DatabaseConnection {
  private cache = new Map<string, { data: any[]; expiry: number }>();

  constructor(
    private connection: DatabaseConnection,
    private ttlMs: number = 60000,
  ) {}

  async query(sql: string): Promise<any[]> {
    const cached = this.cache.get(sql);
    if (cached && cached.expiry > Date.now()) {
      console.log('[Cache] Hit');
      return cached.data;
    }

    console.log('[Cache] Miss');
    const result = await this.connection.query(sql);
    this.cache.set(sql, { data: result, expiry: Date.now() + this.ttlMs });
    return result;
  }

  close(): void {
    this.cache.clear();
    this.connection.close();
  }
}

// Usage - Stack proxies
const realDb = new RealDatabaseConnection('postgres://...');
const cachedDb = new CachingDatabaseProxy(realDb, 30000);
const loggedDb = new LoggingDatabaseProxy(cachedDb);

await loggedDb.query('SELECT * FROM users');
```

---

### Composite Pattern

**Intent**: Compose objects into tree structures to represent part-whole hierarchies. Composite lets clients treat individual objects and compositions uniformly.

**Use Case**: File systems, organization hierarchies, menu systems, permission trees.

```typescript
// Component interface
interface Permission {
  getName(): string;
  hasAccess(resource: string): boolean;
}

// Leaf
class SimplePermission implements Permission {
  constructor(
    private name: string,
    private allowedResources: string[],
  ) {}

  getName(): string {
    return this.name;
  }

  hasAccess(resource: string): boolean {
    return this.allowedResources.some(r =>
      resource.startsWith(r) || r === '*'
    );
  }
}

// Composite
class PermissionGroup implements Permission {
  private permissions: Permission[] = [];

  constructor(private name: string) {}

  add(permission: Permission): void {
    this.permissions.push(permission);
  }

  remove(permission: Permission): void {
    const index = this.permissions.indexOf(permission);
    if (index > -1) {
      this.permissions.splice(index, 1);
    }
  }

  getName(): string {
    return this.name;
  }

  hasAccess(resource: string): boolean {
    return this.permissions.some(p => p.hasAccess(resource));
  }
}

// Usage
const readUsers = new SimplePermission('read:users', ['/api/users']);
const writeUsers = new SimplePermission('write:users', ['/api/users']);
const readOrders = new SimplePermission('read:orders', ['/api/orders']);
const adminAll = new SimplePermission('admin:all', ['*']);

const viewerRole = new PermissionGroup('viewer');
viewerRole.add(readUsers);
viewerRole.add(readOrders);

const editorRole = new PermissionGroup('editor');
editorRole.add(viewerRole);  // Inherits viewer permissions
editorRole.add(writeUsers);

const adminRole = new PermissionGroup('admin');
adminRole.add(adminAll);

// Check permissions uniformly
console.log(viewerRole.hasAccess('/api/users'));   // true
console.log(viewerRole.hasAccess('/api/orders'));  // true
console.log(viewerRole.hasAccess('/api/admin'));   // false
console.log(adminRole.hasAccess('/api/anything')); // true
```

---

## Behavioral Patterns

### Command Pattern

**Intent**: Encapsulate a request as an object, thereby letting you parameterize clients with different requests, queue or log requests, and support undoable operations.

**Use Case**: Task queues, undo/redo functionality, macro recording, transaction scripts.

```typescript
// Command interface
interface Command {
  execute(): Promise<void>;
  undo?(): Promise<void>;
}

// Concrete commands
class CreateUserCommand implements Command {
  private createdUserId?: string;

  constructor(
    private userRepository: UserRepository,
    private userData: CreateUserDto,
  ) {}

  async execute(): Promise<void> {
    const user = await this.userRepository.save(this.userData);
    this.createdUserId = user.id;
  }

  async undo(): Promise<void> {
    if (this.createdUserId) {
      await this.userRepository.delete(this.createdUserId);
    }
  }
}

class SendEmailCommand implements Command {
  constructor(
    private emailService: EmailService,
    private email: EmailDto,
  ) {}

  async execute(): Promise<void> {
    await this.emailService.send(this.email);
  }
}

class TransferMoneyCommand implements Command {
  constructor(
    private accountService: AccountService,
    private fromAccountId: string,
    private toAccountId: string,
    private amount: number,
  ) {}

  async execute(): Promise<void> {
    await this.accountService.withdraw(this.fromAccountId, this.amount);
    await this.accountService.deposit(this.toAccountId, this.amount);
  }

  async undo(): Promise<void> {
    await this.accountService.withdraw(this.toAccountId, this.amount);
    await this.accountService.deposit(this.fromAccountId, this.amount);
  }
}

// Command invoker / queue
class CommandQueue {
  private queue: Command[] = [];
  private history: Command[] = [];

  enqueue(command: Command): void {
    this.queue.push(command);
  }

  async processAll(): Promise<void> {
    while (this.queue.length > 0) {
      const command = this.queue.shift()!;
      try {
        await command.execute();
        this.history.push(command);
      } catch (error) {
        console.error('Command failed:', error);
        // Could implement retry logic here
        throw error;
      }
    }
  }

  async undoLast(): Promise<void> {
    const command = this.history.pop();
    if (command?.undo) {
      await command.undo();
    }
  }
}

// Usage
const queue = new CommandQueue();
queue.enqueue(new CreateUserCommand(userRepo, { name: 'John' }));
queue.enqueue(new SendEmailCommand(emailService, { to: 'john@example.com' }));
await queue.processAll();
```

---

### Chain of Responsibility Pattern

**Intent**: Avoid coupling the sender of a request to its receiver by giving more than one object a chance to handle the request. Chain the receiving objects and pass the request along the chain until an object handles it.

**Use Case**: Middleware chains, validation pipelines, logging chains, approval workflows.

```typescript
// Handler interface
interface Handler<T> {
  setNext(handler: Handler<T>): Handler<T>;
  handle(request: T): Promise<T>;
}

// Abstract base handler
abstract class AbstractHandler<T> implements Handler<T> {
  private nextHandler: Handler<T> | null = null;

  setNext(handler: Handler<T>): Handler<T> {
    this.nextHandler = handler;
    return handler;
  }

  async handle(request: T): Promise<T> {
    if (this.nextHandler) {
      return this.nextHandler.handle(request);
    }
    return request;
  }
}

// Request type
interface HttpRequest {
  headers: Record<string, string>;
  body: any;
  user?: User;
  validated?: boolean;
  rateLimit?: { remaining: number };
}

// Concrete handlers
class AuthenticationHandler extends AbstractHandler<HttpRequest> {
  constructor(private authService: AuthService) {
    super();
  }

  async handle(request: HttpRequest): Promise<HttpRequest> {
    const token = request.headers['authorization']?.replace('Bearer ', '');

    if (!token) {
      throw new UnauthorizedError('No token provided');
    }

    const user = await this.authService.validateToken(token);
    if (!user) {
      throw new UnauthorizedError('Invalid token');
    }

    request.user = user;
    return super.handle(request);
  }
}

class RateLimitHandler extends AbstractHandler<HttpRequest> {
  constructor(private rateLimiter: RateLimiter) {
    super();
  }

  async handle(request: HttpRequest): Promise<HttpRequest> {
    const clientId = request.user?.id || request.headers['x-client-id'];
    const result = await this.rateLimiter.check(clientId);

    if (!result.allowed) {
      throw new TooManyRequestsError('Rate limit exceeded');
    }

    request.rateLimit = { remaining: result.remaining };
    return super.handle(request);
  }
}

class ValidationHandler extends AbstractHandler<HttpRequest> {
  constructor(private validator: Validator) {
    super();
  }

  async handle(request: HttpRequest): Promise<HttpRequest> {
    const errors = this.validator.validate(request.body);

    if (errors.length > 0) {
      throw new ValidationError(errors);
    }

    request.validated = true;
    return super.handle(request);
  }
}

class LoggingHandler extends AbstractHandler<HttpRequest> {
  async handle(request: HttpRequest): Promise<HttpRequest> {
    console.log(`[${new Date().toISOString()}] Request from user: ${request.user?.id}`);
    const result = await super.handle(request);
    console.log(`[${new Date().toISOString()}] Request processed`);
    return result;
  }
}

// Build the chain
const auth = new AuthenticationHandler(authService);
const rateLimit = new RateLimitHandler(rateLimiter);
const validation = new ValidationHandler(validator);
const logging = new LoggingHandler();

auth.setNext(rateLimit).setNext(validation).setNext(logging);

// Process request through chain
const processedRequest = await auth.handle(incomingRequest);
```

---

### State Pattern

**Intent**: Allow an object to alter its behavior when its internal state changes. The object will appear to change its class.

**Use Case**: Order processing, document workflows, game states, connection handling.

```typescript
// State interface
interface OrderState {
  confirm(order: Order): void;
  ship(order: Order): void;
  deliver(order: Order): void;
  cancel(order: Order): void;
}

// Concrete states
class PendingState implements OrderState {
  confirm(order: Order): void {
    console.log('Order confirmed');
    order.setState(new ConfirmedState());
  }

  ship(order: Order): void {
    throw new Error('Cannot ship pending order');
  }

  deliver(order: Order): void {
    throw new Error('Cannot deliver pending order');
  }

  cancel(order: Order): void {
    console.log('Order cancelled');
    order.setState(new CancelledState());
  }
}

class ConfirmedState implements OrderState {
  confirm(order: Order): void {
    throw new Error('Order already confirmed');
  }

  ship(order: Order): void {
    console.log('Order shipped');
    order.setState(new ShippedState());
  }

  deliver(order: Order): void {
    throw new Error('Cannot deliver unshipped order');
  }

  cancel(order: Order): void {
    console.log('Order cancelled, refund initiated');
    order.setState(new CancelledState());
  }
}

class ShippedState implements OrderState {
  confirm(order: Order): void {
    throw new Error('Order already confirmed and shipped');
  }

  ship(order: Order): void {
    throw new Error('Order already shipped');
  }

  deliver(order: Order): void {
    console.log('Order delivered');
    order.setState(new DeliveredState());
  }

  cancel(order: Order): void {
    throw new Error('Cannot cancel shipped order');
  }
}

class DeliveredState implements OrderState {
  confirm(order: Order): void {
    throw new Error('Order already completed');
  }

  ship(order: Order): void {
    throw new Error('Order already completed');
  }

  deliver(order: Order): void {
    throw new Error('Order already delivered');
  }

  cancel(order: Order): void {
    throw new Error('Cannot cancel delivered order');
  }
}

class CancelledState implements OrderState {
  confirm(order: Order): void {
    throw new Error('Cannot modify cancelled order');
  }

  ship(order: Order): void {
    throw new Error('Cannot modify cancelled order');
  }

  deliver(order: Order): void {
    throw new Error('Cannot modify cancelled order');
  }

  cancel(order: Order): void {
    throw new Error('Order already cancelled');
  }
}

// Context
class Order {
  private state: OrderState = new PendingState();

  setState(state: OrderState): void {
    this.state = state;
  }

  confirm(): void {
    this.state.confirm(this);
  }

  ship(): void {
    this.state.ship(this);
  }

  deliver(): void {
    this.state.deliver(this);
  }

  cancel(): void {
    this.state.cancel(this);
  }
}

// Usage
const order = new Order();
order.confirm();  // Works
order.ship();     // Works
order.cancel();   // Throws: Cannot cancel shipped order
```

---

### Template Method Pattern

**Intent**: Define the skeleton of an algorithm in an operation, deferring some steps to subclasses. Template Method lets subclasses redefine certain steps of an algorithm without changing the algorithm's structure.

**Use Case**: Report generation, data processing pipelines, test frameworks.

```typescript
// Abstract class with template method
abstract class DataProcessor {
  // Template method - defines the algorithm structure
  async process(): Promise<ProcessingResult> {
    const data = await this.fetchData();
    const validated = this.validate(data);
    const transformed = this.transform(validated);
    const result = await this.save(transformed);
    this.notify(result);
    return result;
  }

  // Abstract methods - must be implemented by subclasses
  protected abstract fetchData(): Promise<any[]>;
  protected abstract transform(data: any[]): any[];
  protected abstract save(data: any[]): Promise<ProcessingResult>;

  // Default implementations - can be overridden
  protected validate(data: any[]): any[] {
    return data.filter(item => item !== null && item !== undefined);
  }

  protected notify(result: ProcessingResult): void {
    console.log(`Processed ${result.count} items`);
  }
}

// Concrete implementation for CSV
class CsvDataProcessor extends DataProcessor {
  constructor(private filePath: string) {
    super();
  }

  protected async fetchData(): Promise<any[]> {
    const content = await fs.readFile(this.filePath, 'utf-8');
    return this.parseCsv(content);
  }

  protected transform(data: any[]): any[] {
    return data.map(row => ({
      ...row,
      processedAt: new Date(),
    }));
  }

  protected async save(data: any[]): Promise<ProcessingResult> {
    await this.database.insertMany('csv_data', data);
    return { count: data.length, source: 'csv' };
  }

  private parseCsv(content: string): any[] {
    // CSV parsing logic
  }
}

// Concrete implementation for API
class ApiDataProcessor extends DataProcessor {
  constructor(private apiUrl: string) {
    super();
  }

  protected async fetchData(): Promise<any[]> {
    const response = await fetch(this.apiUrl);
    return response.json();
  }

  protected validate(data: any[]): any[] {
    // Custom validation for API data
    return data.filter(item =>
      item.id && item.timestamp && new Date(item.timestamp) > this.getLastProcessed()
    );
  }

  protected transform(data: any[]): any[] {
    return data.map(item => ({
      externalId: item.id,
      data: item,
      fetchedAt: new Date(),
    }));
  }

  protected async save(data: any[]): Promise<ProcessingResult> {
    await this.database.upsertMany('api_data', data, 'externalId');
    return { count: data.length, source: 'api' };
  }

  protected notify(result: ProcessingResult): void {
    super.notify(result);
    this.metrics.increment('api_data_processed', result.count);
  }
}

// Usage
const csvProcessor = new CsvDataProcessor('/data/import.csv');
await csvProcessor.process();

const apiProcessor = new ApiDataProcessor('https://api.example.com/data');
await apiProcessor.process();
```

---

## Summary Table

| Pattern | Category | Intent | Common Use Cases |
|---------|----------|--------|------------------|
| **Builder** | Creational | Construct complex objects step by step | HTTP requests, queries, configs |
| **Prototype** | Creational | Clone existing objects | Templates, configurations |
| **Facade** | Structural | Simplify complex subsystems | APIs, libraries, services |
| **Proxy** | Structural | Control access to objects | Caching, logging, lazy loading |
| **Composite** | Structural | Tree structures | Permissions, menus, file systems |
| **Command** | Behavioral | Encapsulate requests as objects | Queues, undo/redo, transactions |
| **Chain of Responsibility** | Behavioral | Pass requests through handlers | Middleware, validation, workflows |
| **State** | Behavioral | Behavior based on internal state | Order status, connections, games |
| **Template Method** | Behavioral | Define algorithm skeleton | Data processing, reports |
