# Architecture and Diagrams

This chapter adds a professional architecture layer: project layout, request flow, and system boundaries.

## Step 1: Standard Project Layout

```
project/
├── config/
│   ├── settings.py
│   ├── urls.py
│   ├── wsgi.py
│   └── asgi.py
├── core/
│   ├── models.py
│   ├── serializers.py
│   ├── viewsets.py
│   ├── services.py
│   ├── selectors.py
│   └── urls.py
├── tests/
└── manage.py
```

## Step 2: Request Lifecycle

```mermaid
graph TD
  Client -->|HTTP| Router
  Router --> Viewset
  Viewset --> Serializer
  Viewset --> Service
  Service --> ORM
  ORM --> Database
  Viewset --> Response
  Response --> Client
```

## Step 3: Service and Selector Pattern

Use `services.py` for writes and `selectors.py` for reads.

```python
# core/services.py
from .models import Doctor


def create_doctor(data):
    return Doctor.objects.create(**data)
```

```python
# core/selectors.py
from .models import Doctor


def get_active_doctors():
    return Doctor.objects.filter(is_active=True)
```

## Step 4: Auth Flow (Token)

```mermaid
sequenceDiagram
  participant Client
  participant API
  participant DB

  Client->>API: POST /api/token
  API->>DB: validate credentials
  DB-->>API: ok
  API-->>Client: token
  Client->>API: GET /api/doctors (Authorization: Token <token>)
  API-->>Client: 200 OK
```

## Step 5: Background Jobs

```mermaid
graph LR
  API -->|enqueue| Celery
  Celery --> Worker
  Worker --> DB
  Worker --> Email
```

## Step 6: Deployment Topology

```mermaid
graph TD
  Client --> CDN
  CDN --> Nginx
  Nginx --> App
  App --> DB
  App --> Redis
  App --> Celery
```

## Tips

- Keep views thin and move logic to services.
- Add a single place for query logic (selectors).
- Use diagrams to onboard new engineers quickly.

## Next Steps

- [Observability](./28-observability.md)
- [Performance](./29-performance.md)

---

[Previous: Django 6.0 Notes](./26-django-6-features.md) | [Back to Index](./README.md) | [Next: Observability](./28-observability.md)
