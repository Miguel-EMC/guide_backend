# OpenAPI Customization and Docs

This chapter shows how to customize OpenAPI metadata, tags, and schema generation in FastAPI to produce professional API docs and client SDKs.

## App Metadata

```python
from fastapi import FastAPI

app = FastAPI(
    title="Payments API",
    description="Production API for payments",
    version="1.0.0",
    terms_of_service="https://example.com/terms",
    contact={"name": "API Support", "email": "api@example.com"},
    license_info={"name": "Apache 2.0"},
)
```

## Docs and OpenAPI URLs

Disable or move docs in production if needed.

```python
app = FastAPI(
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json",
)
```

## Tag Metadata

```python
tags_metadata = [
    {"name": "users", "description": "User management"},
    {"name": "billing", "description": "Billing and invoices"},
]

app = FastAPI(openapi_tags=tags_metadata)
```

## Route-Level OpenAPI Extras

```python
from fastapi import APIRouter

router = APIRouter()


@router.get(
    "/status",
    openapi_extra={"x-internal": True},
    tags=["internal"],
)
async def status():
    return {"ok": True}
```

## Custom OpenAPI Schema

Override schema generation to inject custom fields or change defaults.

```python
from fastapi import FastAPI
from fastapi.openapi.utils import get_openapi

app = FastAPI()


def custom_openapi():
    if app.openapi_schema:
        return app.openapi_schema
    schema = get_openapi(
        title="My API",
        version="1.0.0",
        description="Custom schema",
        routes=app.routes,
    )
    schema["info"]["x-logo"] = {
        "url": "https://example.com/logo.png"
    }
    app.openapi_schema = schema
    return app.openapi_schema


app.openapi = custom_openapi
```

## Client SDKs

Once OpenAPI is stable, generate SDKs using tools like OpenAPI Generator or custom codegen pipelines.

## References

- [FastAPI OpenAPI Metadata](https://fastapi.tiangolo.com/tutorial/metadata/)
- [FastAPI Extending OpenAPI](https://fastapi.tiangolo.com/how-to/extending-openapi/)

## Next Steps

- [CI/CD](./24-ci-cd.md) - Automation pipelines
- [Production Checklist](./25-production-checklist.md) - Launch readiness

---

[Previous: WebSockets](./22-websockets.md) | [Back to Index](./README.md) | [Next: CI/CD](./24-ci-cd.md)
