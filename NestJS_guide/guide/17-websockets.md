# WebSockets

NestJS provides WebSocket support via gateways. You can use Socket.IO (default) or the native WebSocket adapter. This chapter covers real-time communication, authentication, scaling, and production patterns.

## Goals

- Build a real-time gateway
- Broadcast events to clients
- Validate incoming messages
- Authenticate WebSocket connections
- Scale horizontally with Redis adapter

## Install (Socket.IO)

```bash
npm install @nestjs/websockets @nestjs/platform-socket.io
npm install -D @types/socket.io
```

For Redis adapter (horizontal scaling):

```bash
npm install @socket.io/redis-adapter redis
```

## Basic Gateway

```typescript
// src/chat/chat.gateway.ts
import {
  WebSocketGateway,
  SubscribeMessage,
  MessageBody,
  ConnectedSocket,
  OnGatewayConnection,
  OnGatewayDisconnect,
  OnGatewayInit,
  WebSocketServer,
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { Logger } from '@nestjs/common';

@WebSocketGateway({
  namespace: '/chat',
  cors: {
    origin: process.env.FRONTEND_URL ?? 'http://localhost:3000',
    credentials: true,
  },
})
export class ChatGateway
  implements OnGatewayInit, OnGatewayConnection, OnGatewayDisconnect
{
  private readonly logger = new Logger(ChatGateway.name);

  @WebSocketServer()
  server: Server;

  afterInit(server: Server) {
    this.logger.log('WebSocket Gateway initialized');
  }

  handleConnection(client: Socket) {
    this.logger.log(`Client connected: ${client.id}`);
  }

  handleDisconnect(client: Socket) {
    this.logger.log(`Client disconnected: ${client.id}`);
  }

  @SubscribeMessage('message')
  handleMessage(
    @MessageBody() payload: { text: string },
    @ConnectedSocket() client: Socket,
  ) {
    this.logger.log(`Message from ${client.id}: ${payload.text}`);
    return { event: 'message', data: { ok: true, received: payload.text } };
  }
}
```

## Emitting Events

```typescript
// Emit to all connected clients
broadcastUpdate() {
  this.server.emit('post.updated', { id: 1 });
}

// Emit to specific client
emitToClient(clientId: string, event: string, data: any) {
  this.server.to(clientId).emit(event, data);
}

// Emit to all except sender
broadcastExcludingSender(client: Socket, event: string, data: any) {
  client.broadcast.emit(event, data);
}
```

## Validating Messages

Reuse DTOs and validation pipes for incoming messages.

```typescript
// src/chat/dto/send-message.dto.ts
import { IsString, IsNotEmpty, MaxLength, IsOptional } from 'class-validator';

export class SendMessageDto {
  @IsString()
  @IsNotEmpty()
  @MaxLength(1000)
  text: string;

  @IsString()
  @IsOptional()
  roomId?: string;
}
```

```typescript
// src/chat/chat.gateway.ts
import { UsePipes, ValidationPipe } from '@nestjs/common';
import { SendMessageDto } from './dto/send-message.dto';

@UsePipes(new ValidationPipe({ whitelist: true, transform: true }))
@SubscribeMessage('message')
handleMessage(
  @MessageBody() dto: SendMessageDto,
  @ConnectedSocket() client: Socket,
) {
  if (dto.roomId) {
    client.to(dto.roomId).emit('message', {
      text: dto.text,
      senderId: client.id,
      timestamp: new Date().toISOString(),
    });
  }
  return { ok: true };
}
```

## Namespaces and Rooms

```typescript
// Join a room
@SubscribeMessage('join_room')
handleJoinRoom(
  @MessageBody() data: { roomId: string },
  @ConnectedSocket() client: Socket,
) {
  client.join(data.roomId);
  client.to(data.roomId).emit('user_joined', {
    userId: client.id,
    roomId: data.roomId,
  });
  return { joined: data.roomId };
}

// Leave a room
@SubscribeMessage('leave_room')
handleLeaveRoom(
  @MessageBody() data: { roomId: string },
  @ConnectedSocket() client: Socket,
) {
  client.leave(data.roomId);
  client.to(data.roomId).emit('user_left', {
    userId: client.id,
    roomId: data.roomId,
  });
  return { left: data.roomId };
}

// Send to room
@SubscribeMessage('room_message')
handleRoomMessage(
  @MessageBody() data: { roomId: string; text: string },
  @ConnectedSocket() client: Socket,
) {
  this.server.to(data.roomId).emit('message', {
    text: data.text,
    senderId: client.id,
    roomId: data.roomId,
  });
}
```

## Authentication

### JWT Authentication Guard

```typescript
// src/auth/guards/ws-jwt.guard.ts
import { CanActivate, ExecutionContext, Injectable } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { WsException } from '@nestjs/websockets';
import { Socket } from 'socket.io';

@Injectable()
export class WsJwtGuard implements CanActivate {
  constructor(private readonly jwtService: JwtService) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const client: Socket = context.switchToWs().getClient();
    const token = this.extractToken(client);

    if (!token) {
      throw new WsException('Unauthorized: No token provided');
    }

    try {
      const payload = await this.jwtService.verifyAsync(token);
      client.data.user = payload;
      return true;
    } catch {
      throw new WsException('Unauthorized: Invalid token');
    }
  }

  private extractToken(client: Socket): string | null {
    // From handshake auth
    const authToken = client.handshake.auth?.token;
    if (authToken) return authToken;

    // From query params
    const queryToken = client.handshake.query?.token as string;
    if (queryToken) return queryToken;

    // From headers
    const authHeader = client.handshake.headers?.authorization;
    if (authHeader?.startsWith('Bearer ')) {
      return authHeader.slice(7);
    }

    return null;
  }
}
```

### Using the Guard

```typescript
// src/chat/chat.gateway.ts
import { UseGuards } from '@nestjs/common';
import { WsJwtGuard } from '../auth/guards/ws-jwt.guard';

@WebSocketGateway({ namespace: '/chat' })
@UseGuards(WsJwtGuard)
export class ChatGateway {
  @SubscribeMessage('message')
  handleMessage(
    @MessageBody() dto: SendMessageDto,
    @ConnectedSocket() client: Socket,
  ) {
    const user = client.data.user;
    this.logger.log(`Message from user ${user.sub}: ${dto.text}`);
    return { ok: true, userId: user.sub };
  }
}
```

### Authentication on Connection

```typescript
// src/chat/chat.gateway.ts
import { JwtService } from '@nestjs/jwt';

@WebSocketGateway({ namespace: '/chat' })
export class ChatGateway implements OnGatewayConnection {
  constructor(private readonly jwtService: JwtService) {}

  async handleConnection(client: Socket) {
    try {
      const token =
        client.handshake.auth?.token ||
        client.handshake.query?.token;

      if (!token) {
        client.emit('error', { message: 'Authentication required' });
        client.disconnect();
        return;
      }

      const payload = await this.jwtService.verifyAsync(token);
      client.data.user = payload;

      // Join user-specific room
      client.join(`user:${payload.sub}`);

      this.logger.log(`User ${payload.sub} connected`);
    } catch (error) {
      client.emit('error', { message: 'Invalid token' });
      client.disconnect();
    }
  }
}
```

## Redis Adapter (Horizontal Scaling)

When running multiple server instances, use Redis adapter for cross-instance communication.

```typescript
// src/websocket/websocket.module.ts
import { Module } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createAdapter } from '@socket.io/redis-adapter';
import { createClient } from 'redis';

@Module({
  providers: [
    {
      provide: 'REDIS_ADAPTER',
      useFactory: async (configService: ConfigService) => {
        const pubClient = createClient({
          url: configService.get('REDIS_URL'),
        });
        const subClient = pubClient.duplicate();

        await Promise.all([pubClient.connect(), subClient.connect()]);

        return createAdapter(pubClient, subClient);
      },
      inject: [ConfigService],
    },
  ],
  exports: ['REDIS_ADAPTER'],
})
export class WebsocketModule {}
```

### Configure in Main

```typescript
// src/main.ts
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { IoAdapter } from '@nestjs/platform-socket.io';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  const redisAdapter = app.get('REDIS_ADAPTER');
  const ioAdapter = new IoAdapter(app);

  // Apply Redis adapter
  app.useWebSocketAdapter({
    ...ioAdapter,
    createIOServer: (port, options) => {
      const server = ioAdapter.createIOServer(port, options);
      server.adapter(redisAdapter);
      return server;
    },
  });

  await app.listen(3000);
}
bootstrap();
```

### Custom Redis Adapter

```typescript
// src/websocket/redis-io.adapter.ts
import { IoAdapter } from '@nestjs/platform-socket.io';
import { ServerOptions } from 'socket.io';
import { createAdapter } from '@socket.io/redis-adapter';
import { createClient } from 'redis';
import { INestApplication } from '@nestjs/common';

export class RedisIoAdapter extends IoAdapter {
  private adapterConstructor: ReturnType<typeof createAdapter>;

  constructor(app: INestApplication) {
    super(app);
  }

  async connectToRedis(redisUrl: string): Promise<void> {
    const pubClient = createClient({ url: redisUrl });
    const subClient = pubClient.duplicate();

    await Promise.all([pubClient.connect(), subClient.connect()]);

    this.adapterConstructor = createAdapter(pubClient, subClient);
  }

  createIOServer(port: number, options?: ServerOptions) {
    const server = super.createIOServer(port, options);
    server.adapter(this.adapterConstructor);
    return server;
  }
}
```

```typescript
// src/main.ts
async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  const configService = app.get(ConfigService);

  const redisIoAdapter = new RedisIoAdapter(app);
  await redisIoAdapter.connectToRedis(configService.get('REDIS_URL'));

  app.useWebSocketAdapter(redisIoAdapter);

  await app.listen(3000);
}
```

## Reconnection Handling

### Client-Side (JavaScript)

```typescript
import { io, Socket } from 'socket.io-client';

const socket: Socket = io('http://localhost:3000/chat', {
  auth: { token: 'your-jwt-token' },
  reconnection: true,
  reconnectionAttempts: 5,
  reconnectionDelay: 1000,
  reconnectionDelayMax: 5000,
});

socket.on('connect', () => {
  console.log('Connected:', socket.id);
});

socket.on('disconnect', (reason) => {
  console.log('Disconnected:', reason);
  if (reason === 'io server disconnect') {
    // Server initiated disconnect, need to manually reconnect
    socket.connect();
  }
});

socket.on('connect_error', (error) => {
  console.error('Connection error:', error.message);
  if (error.message === 'Invalid token') {
    // Refresh token and reconnect
    refreshToken().then((newToken) => {
      socket.auth = { token: newToken };
      socket.connect();
    });
  }
});

socket.io.on('reconnect', (attempt) => {
  console.log('Reconnected after', attempt, 'attempts');
});

socket.io.on('reconnect_attempt', (attempt) => {
  console.log('Reconnection attempt', attempt);
});

socket.io.on('reconnect_failed', () => {
  console.log('Reconnection failed');
});
```

### Server-Side Connection State

```typescript
// src/chat/chat.gateway.ts
@WebSocketGateway({ namespace: '/chat' })
export class ChatGateway {
  private connectedUsers = new Map<string, { socketId: string; userId: number }>();

  handleConnection(client: Socket) {
    const userId = client.data.user?.sub;
    if (userId) {
      this.connectedUsers.set(client.id, { socketId: client.id, userId });
    }
  }

  handleDisconnect(client: Socket) {
    this.connectedUsers.delete(client.id);
  }

  isUserOnline(userId: number): boolean {
    return [...this.connectedUsers.values()].some((u) => u.userId === userId);
  }

  getUserSockets(userId: number): string[] {
    return [...this.connectedUsers.entries()]
      .filter(([, data]) => data.userId === userId)
      .map(([socketId]) => socketId);
  }
}
```

## Complete Chat Example

```typescript
// src/chat/chat.gateway.ts
import {
  WebSocketGateway,
  SubscribeMessage,
  MessageBody,
  ConnectedSocket,
  OnGatewayConnection,
  OnGatewayDisconnect,
  WebSocketServer,
  WsException,
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { Logger, UseGuards, UsePipes, ValidationPipe } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { WsJwtGuard } from '../auth/guards/ws-jwt.guard';
import { ChatService } from './chat.service';
import { SendMessageDto } from './dto/send-message.dto';
import { JoinRoomDto } from './dto/join-room.dto';

interface ConnectedUser {
  socketId: string;
  userId: number;
  username: string;
  rooms: Set<string>;
}

@WebSocketGateway({
  namespace: '/chat',
  cors: {
    origin: process.env.FRONTEND_URL,
    credentials: true,
  },
})
@UseGuards(WsJwtGuard)
@UsePipes(new ValidationPipe({ whitelist: true, transform: true }))
export class ChatGateway implements OnGatewayConnection, OnGatewayDisconnect {
  @WebSocketServer()
  server: Server;

  private readonly logger = new Logger(ChatGateway.name);
  private connectedUsers = new Map<string, ConnectedUser>();

  constructor(
    private readonly jwtService: JwtService,
    private readonly chatService: ChatService,
  ) {}

  async handleConnection(client: Socket) {
    try {
      const token = client.handshake.auth?.token;
      if (!token) {
        client.disconnect();
        return;
      }

      const payload = await this.jwtService.verifyAsync(token);
      client.data.user = payload;

      const user: ConnectedUser = {
        socketId: client.id,
        userId: payload.sub,
        username: payload.username,
        rooms: new Set(),
      };
      this.connectedUsers.set(client.id, user);

      // Join personal room for direct messages
      client.join(`user:${payload.sub}`);

      this.logger.log(`User ${payload.username} connected`);

      // Notify others
      this.server.emit('user_online', {
        userId: payload.sub,
        username: payload.username,
      });
    } catch {
      client.disconnect();
    }
  }

  handleDisconnect(client: Socket) {
    const user = this.connectedUsers.get(client.id);
    if (user) {
      this.connectedUsers.delete(client.id);

      // Check if user has no other connections
      const stillConnected = [...this.connectedUsers.values()].some(
        (u) => u.userId === user.userId,
      );

      if (!stillConnected) {
        this.server.emit('user_offline', {
          userId: user.userId,
          username: user.username,
        });
      }

      this.logger.log(`User ${user.username} disconnected`);
    }
  }

  @SubscribeMessage('join_room')
  async handleJoinRoom(
    @MessageBody() dto: JoinRoomDto,
    @ConnectedSocket() client: Socket,
  ) {
    const user = this.connectedUsers.get(client.id);
    if (!user) throw new WsException('User not found');

    // Verify room access
    const canJoin = await this.chatService.canUserJoinRoom(
      user.userId,
      dto.roomId,
    );
    if (!canJoin) {
      throw new WsException('Access denied to this room');
    }

    client.join(dto.roomId);
    user.rooms.add(dto.roomId);

    // Load recent messages
    const messages = await this.chatService.getRoomMessages(dto.roomId, 50);

    // Notify room members
    client.to(dto.roomId).emit('user_joined_room', {
      roomId: dto.roomId,
      userId: user.userId,
      username: user.username,
    });

    return { joined: dto.roomId, messages };
  }

  @SubscribeMessage('leave_room')
  handleLeaveRoom(
    @MessageBody() dto: { roomId: string },
    @ConnectedSocket() client: Socket,
  ) {
    const user = this.connectedUsers.get(client.id);
    if (!user) throw new WsException('User not found');

    client.leave(dto.roomId);
    user.rooms.delete(dto.roomId);

    client.to(dto.roomId).emit('user_left_room', {
      roomId: dto.roomId,
      userId: user.userId,
      username: user.username,
    });

    return { left: dto.roomId };
  }

  @SubscribeMessage('send_message')
  async handleSendMessage(
    @MessageBody() dto: SendMessageDto,
    @ConnectedSocket() client: Socket,
  ) {
    const user = this.connectedUsers.get(client.id);
    if (!user) throw new WsException('User not found');

    // Save message to database
    const message = await this.chatService.saveMessage({
      text: dto.text,
      roomId: dto.roomId,
      userId: user.userId,
    });

    const payload = {
      id: message.id,
      text: message.text,
      roomId: dto.roomId,
      userId: user.userId,
      username: user.username,
      timestamp: message.createdAt,
    };

    // Broadcast to room
    this.server.to(dto.roomId).emit('new_message', payload);

    return { sent: true, messageId: message.id };
  }

  @SubscribeMessage('typing')
  handleTyping(
    @MessageBody() dto: { roomId: string; isTyping: boolean },
    @ConnectedSocket() client: Socket,
  ) {
    const user = this.connectedUsers.get(client.id);
    if (!user) return;

    client.to(dto.roomId).emit('user_typing', {
      roomId: dto.roomId,
      userId: user.userId,
      username: user.username,
      isTyping: dto.isTyping,
    });
  }

  @SubscribeMessage('direct_message')
  async handleDirectMessage(
    @MessageBody() dto: { recipientId: number; text: string },
    @ConnectedSocket() client: Socket,
  ) {
    const user = this.connectedUsers.get(client.id);
    if (!user) throw new WsException('User not found');

    const message = await this.chatService.saveDirectMessage({
      text: dto.text,
      senderId: user.userId,
      recipientId: dto.recipientId,
    });

    const payload = {
      id: message.id,
      text: message.text,
      senderId: user.userId,
      senderUsername: user.username,
      timestamp: message.createdAt,
    };

    // Send to recipient's personal room
    this.server.to(`user:${dto.recipientId}`).emit('direct_message', payload);

    return { sent: true, messageId: message.id };
  }

  // Method to send notifications from other services
  sendToUser(userId: number, event: string, data: any) {
    this.server.to(`user:${userId}`).emit(event, data);
  }

  sendToRoom(roomId: string, event: string, data: any) {
    this.server.to(roomId).emit(event, data);
  }

  getOnlineUsers(): { userId: number; username: string }[] {
    const uniqueUsers = new Map<number, string>();
    for (const user of this.connectedUsers.values()) {
      uniqueUsers.set(user.userId, user.username);
    }
    return [...uniqueUsers.entries()].map(([userId, username]) => ({
      userId,
      username,
    }));
  }
}
```

## Exception Handling

```typescript
// src/websocket/filters/ws-exception.filter.ts
import { Catch, ArgumentsHost } from '@nestjs/common';
import { BaseWsExceptionFilter, WsException } from '@nestjs/websockets';
import { Socket } from 'socket.io';

@Catch()
export class WsExceptionFilter extends BaseWsExceptionFilter {
  catch(exception: unknown, host: ArgumentsHost) {
    const client: Socket = host.switchToWs().getClient();

    let error = { message: 'Internal error', code: 'INTERNAL_ERROR' };

    if (exception instanceof WsException) {
      const wsError = exception.getError();
      error =
        typeof wsError === 'string'
          ? { message: wsError, code: 'WS_ERROR' }
          : (wsError as any);
    }

    client.emit('error', error);
  }
}
```

```typescript
// Apply globally
@WebSocketGateway({ namespace: '/chat' })
@UseFilters(new WsExceptionFilter())
export class ChatGateway {}
```

## Tips

- Always authenticate WebSocket connections.
- Use Redis adapter for horizontal scaling.
- Implement heartbeat/ping-pong for connection health.
- Handle reconnection gracefully on both client and server.
- Use rooms for efficient broadcasting.
- Validate all incoming messages with DTOs.
- Consider message queues for high-volume scenarios.
- Monitor WebSocket connections in production.

---

[Previous: File Uploads](./16-file-uploads.md) | [Back to Index](./README.md) | [Next: Queues and Jobs ->](./18-queues-jobs.md)
