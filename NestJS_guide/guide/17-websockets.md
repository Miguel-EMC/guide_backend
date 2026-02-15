# WebSockets

NestJS provides WebSocket support via gateways. You can use Socket.IO (default) or the native WebSocket adapter.

## Goals

- Build a real-time gateway
- Broadcast events to clients
- Validate incoming messages

## Install (Socket.IO)

```bash
npm install @nestjs/websockets @nestjs/platform-socket.io
```

## Basic Gateway

```typescript
// src/chat/chat.gateway.ts
import {
  WebSocketGateway,
  SubscribeMessage,
  MessageBody,
  ConnectedSocket,
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { Logger } from '@nestjs/common';

@WebSocketGateway({
  namespace: '/chat',
  cors: { origin: '*' },
})
export class ChatGateway {
  private readonly logger = new Logger(ChatGateway.name);

  @SubscribeMessage('message')
  handleMessage(
    @MessageBody() payload: { text: string },
    @ConnectedSocket() client: Socket,
  ) {
    this.logger.log(`Message from ${client.id}`);
    return { ok: true, received: payload.text };
  }
}
```

## Emitting Events

```typescript
import { WebSocketServer } from '@nestjs/websockets';

@WebSocketServer()
server: Server;

broadcastUpdate() {
  this.server.emit('post.updated', { id: 1 });
}
```

## Validating Messages

Reuse DTOs and validation pipes for incoming messages.

```typescript
import { UsePipes, ValidationPipe } from '@nestjs/common';

@UsePipes(new ValidationPipe({ whitelist: true, transform: true }))
@SubscribeMessage('message')
handleMessage(@MessageBody() dto: { text: string }) {
  return dto;
}
```

## Namespaces and Rooms

```typescript
handleJoin(@ConnectedSocket() client: Socket) {
  client.join('room:alpha');
  client.to('room:alpha').emit('system', 'user joined');
}
```

## Tips

- Use guards to authorize WebSocket connections.
- Consider a dedicated gateway module for large apps.
- If you scale horizontally, back Socket.IO with a shared adapter.

---

[Previous: File Uploads](./16-file-uploads.md) | [Back to Index](./README.md) | [Next: Queues and Jobs ->](./18-queues-jobs.md)
