# WebSockets and Realtime Patterns

This chapter covers WebSocket endpoints, connection management, authentication, and scaling patterns.

## Basic WebSocket Endpoint

```python
from fastapi import FastAPI, WebSocket, WebSocketDisconnect

app = FastAPI()


@app.websocket("/ws")
async def websocket_endpoint(ws: WebSocket):
    await ws.accept()
    try:
        while True:
            data = await ws.receive_text()
            await ws.send_text(f"echo: {data}")
    except WebSocketDisconnect:
        pass
```

## Connection Manager

```python
from typing import List
from fastapi import WebSocket


class ConnectionManager:
    def __init__(self) -> None:
        self.active: List[WebSocket] = []

    async def connect(self, ws: WebSocket) -> None:
        await ws.accept()
        self.active.append(ws)

    def disconnect(self, ws: WebSocket) -> None:
        if ws in self.active:
            self.active.remove(ws)

    async def broadcast(self, message: str) -> None:
        for ws in self.active:
            await ws.send_text(message)


manager = ConnectionManager()


@app.websocket("/ws/room")
async def room(ws: WebSocket):
    await manager.connect(ws)
    try:
        while True:
            msg = await ws.receive_text()
            await manager.broadcast(msg)
    except WebSocketDisconnect:
        manager.disconnect(ws)
```

## Authentication

Validate credentials before accepting the connection.

```python
from fastapi import WebSocket


async def authenticate(ws: WebSocket) -> bool:
    token = ws.headers.get("authorization")
    return token == "Bearer secret"


@app.websocket("/ws/private")
async def private_ws(ws: WebSocket):
    if not await authenticate(ws):
        await ws.close(code=1008)
        return
    await ws.accept()
    await ws.send_text("ok")
```

## Scaling WebSockets

WebSockets are stateful. If you run multiple workers or containers, use a shared message broker like Redis PubSub to broadcast messages across instances.

## Best Practices

- Enforce auth before `accept`
- Limit message size and rate
- Handle disconnects gracefully
- Use heartbeats or ping messages for long-lived connections

## References

- [FastAPI WebSockets](https://fastapi.tiangolo.com/reference/websockets/)
- [Starlette WebSockets](https://www.starlette.io/websockets/)

## Next Steps

- [OpenAPI Customization](./23-openapi-customization.md) - Better docs and clients
- [CI/CD](./24-ci-cd.md) - Automation pipelines

---

[Previous: Background Jobs](./21-background-jobs.md) | [Back to Index](./README.md) | [Next: OpenAPI Customization](./23-openapi-customization.md)
