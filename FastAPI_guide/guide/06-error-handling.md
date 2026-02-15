# Error Handling

This guide covers HTTPException, validation errors, custom exception handlers, and best practices for consistent error responses.

## HTTPException

Use `HTTPException` to return controlled error responses.

```python
from fastapi import FastAPI, HTTPException, status

app = FastAPI()


@app.get("/items/{item_id}")
async def read_item(item_id: int):
    if item_id == 0:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Item not found",
        )
    return {"id": item_id}
```

### Custom Headers

```python
from fastapi import HTTPException

raise HTTPException(
    status_code=400,
    detail="Invalid ID",
    headers={"X-Error": "Invalid ID"},
)
```

### Structured Detail

FastAPI's `HTTPException` allows any JSON-serializable `detail` (Starlette's only accepts strings).

```python
raise HTTPException(
    status_code=404,
    detail={"error": "not_found", "resource": "item", "id": 123},
)
```

## Validation Errors

FastAPI raises `RequestValidationError` when request data is invalid. You can override it to change the response format.

```python
from fastapi import FastAPI, Request
from fastapi.encoders import jsonable_encoder
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

app = FastAPI()


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    return JSONResponse(
        status_code=422,
        content=jsonable_encoder({"detail": exc.errors(), "body": exc.body}),
    )
```

`exc.body` contains the invalid request body, which is useful for debugging.

## Custom Exceptions

Create domain-specific exceptions and register handlers for them.

```python
from fastapi import Request
from fastapi.responses import JSONResponse


class ResourceNotFound(Exception):
    def __init__(self, resource: str, resource_id: int):
        self.resource = resource
        self.resource_id = resource_id


@app.exception_handler(ResourceNotFound)
async def resource_not_found_handler(request: Request, exc: ResourceNotFound):
    return JSONResponse(
        status_code=404,
        content={
            "error": "not_found",
            "resource": exc.resource,
            "id": exc.resource_id,
        },
    )
```

## Override Default Exception Handlers

To override HTTP errors, register handlers for Starlette's `HTTPException` so all HTTP exceptions are captured.

```python
from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError
from fastapi.responses import PlainTextResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

app = FastAPI()


@app.exception_handler(StarletteHTTPException)
async def http_exception_handler(request, exc):
    return PlainTextResponse(str(exc.detail), status_code=exc.status_code)


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request, exc: RequestValidationError):
    message = "Validation errors:"
    for error in exc.errors():
        message += f"\nField: {error['loc']}, Error: {error['msg']}"
    return PlainTextResponse(message, status_code=400)
```

## Reuse FastAPI Default Handlers

If you only want to add logging or extra behavior, you can reuse the default handlers.

```python
from fastapi import FastAPI, HTTPException
from fastapi.exception_handlers import (
    http_exception_handler,
    request_validation_exception_handler,
)
from fastapi.exceptions import RequestValidationError
from starlette.exceptions import HTTPException as StarletteHTTPException

app = FastAPI()


@app.exception_handler(StarletteHTTPException)
async def custom_http_exception_handler(request, exc):
    # add logging here
    return await http_exception_handler(request, exc)


@app.exception_handler(RequestValidationError)
async def custom_validation_exception_handler(request, exc):
    # add logging here
    return await request_validation_exception_handler(request, exc)
```

## Catch-All Handler

Use a global handler for unexpected exceptions. Log the real error and return a safe response.

```python
import logging
from fastapi import Request
from fastapi.responses import JSONResponse

logger = logging.getLogger(__name__)


@app.exception_handler(Exception)
async def unhandled_exception_handler(request: Request, exc: Exception):
    logger.exception("Unhandled error")
    return JSONResponse(
        status_code=500,
        content={"error": "internal_error", "message": "Unexpected server error"},
    )
```

## Best Practices

- Use `HTTPException` for expected errors and domain exceptions for business rules.
- Do not leak internal stack traces or database errors to clients.
- Keep a consistent error response shape across the API.
- Prefer `lifespan` for startup/shutdown hooks (events are deprecated).

## References

- [Handling Errors](https://fastapi.tiangolo.com/tutorial/handling-errors/)
- [Lifespan Events](https://fastapi.tiangolo.com/advanced/events/)

## Next Steps

- [Database Setup](./07-database-setup.md) - Connect to databases
- [CRUD Operations](./08-crud-operations.md) - Database operations

---

[Previous: Response Models](./05-response-models.md) | [Back to Index](./README.md) | [Next: Database Setup](./07-database-setup.md)
