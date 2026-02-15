# Introduction to Django and DRF (2026 Edition)

Django is a high-level Python web framework focused on clean architecture, security, and rapid development. Django REST Framework (DRF) adds powerful tools for building REST APIs.

## Version Guidance

| Stack | When to Use | Notes |
|-------|------------|-------|
| Django 5.2 LTS + DRF 3.16 | Default for DRF projects | Stable, supported by DRF |
| Django 6.0 + DRF | If you need the newest Django features | Validate DRF compatibility first |

### Python Support

- Django 6.0 supports Python 3.12-3.14.
- Django 5.2 LTS supports Python 3.10-3.14.

## Why DRF in 2026

- Mature ecosystem with batteries included
- Strong authentication/permissions model
- Browsable API for faster iteration
- Excellent integration with Django ORM and admin

## Installation

### Option A: uv (Recommended)

```bash
uv init django-project
cd django-project

# Recommended stack for DRF
uv add "django>=5.2,<6.0"
uv add "djangorestframework>=3.16"
uv add "drf-spectacular>=0.28"

uv run django-admin startproject config .
uv run python manage.py startapp api
```

### Option B: pip

```bash
pip install "django>=5.2,<6.0" "djangorestframework>=3.16" "drf-spectacular>=0.28"

django-admin startproject config .
python manage.py startapp api
```

## Async in Django (Reality Check)

Django supports async views under ASGI, but much of the ORM remains synchronous. Use `sync_to_async` when doing database work inside `async def`.

```python
from asgiref.sync import sync_to_async
from django.http import JsonResponse
from .models import Doctor


async def doctor_list(request):
    doctors = await sync_to_async(list)(Doctor.objects.all())
    data = [{"id": d.id, "name": d.name} for d in doctors]
    return JsonResponse({"doctors": data})
```

For WebSockets and long-lived connections, use Django Channels.

## Next Steps

- [Models](./02-models.md) - Define your data model
- [Serializers](./03-serializers.md) - Validate and transform data

---

[Previous: uv Setup](./00-uv-setup.md) | [Back to Index](./README.md) | [Next: Models](./02-models.md)
