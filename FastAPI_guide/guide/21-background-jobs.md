# Background Jobs and Task Queues

This chapter shows how to handle long-running or retryable work in FastAPI using `BackgroundTasks` and external worker queues.

## When to Use BackgroundTasks

Use `BackgroundTasks` for short, non-critical work that can run after the response is sent.

Examples:

- Send emails
- Push notifications
- Write audit logs

## BackgroundTasks Example

```python
from fastapi import BackgroundTasks, FastAPI

app = FastAPI()


def send_welcome_email(email: str) -> None:
    # Call your email provider
    pass


@app.post("/register")
async def register(email: str, background_tasks: BackgroundTasks):
    background_tasks.add_task(send_welcome_email, email)
    return {"ok": True}
```

## When to Use a Queue

Use a worker queue when you need:

- Retries and backoff
- Job persistence
- Scheduling
- Work that might take seconds or minutes

Common options include Celery, RQ, and Dramatiq.

## Queue Pattern (High Level)

```text
API -> enqueue job -> worker processes job -> store result
```

## Outbox Pattern

For reliability, write jobs to the database in the same transaction as the request, then have a worker poll and process them.

## Idempotency

Design jobs to be safe to retry. Store idempotency keys and check for duplicates.

## Best Practices

- Keep API endpoints fast and offload slow work
- Use a queue for anything that can fail or must be retried
- Track job status in a database
- Add metrics for job latency and failures

## References

- [FastAPI Background Tasks](https://fastapi.tiangolo.com/tutorial/background-tasks/)

## Next Steps

- [WebSockets](./22-websockets.md) - Realtime delivery
- [OpenAPI Customization](./23-openapi-customization.md) - Better docs and clients

---

[Previous: Performance](./20-performance.md) | [Back to Index](./README.md) | [Next: WebSockets](./22-websockets.md)
