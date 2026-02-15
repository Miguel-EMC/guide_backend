# API Documentation with drf-spectacular

This chapter shows how to generate OpenAPI schemas and serve Swagger UI / ReDoc using drf-spectacular.

## Step 1: Install

```bash
uv add drf-spectacular
# or
pip install drf-spectacular
```

## Step 2: Configure Settings

```python
# config/settings.py
INSTALLED_APPS = [
    # ...
    "drf_spectacular",
]

REST_FRAMEWORK = {
    "DEFAULT_SCHEMA_CLASS": "drf_spectacular.openapi.AutoSchema",
}

SPECTACULAR_SETTINGS = {
    "TITLE": "Doctor API",
    "DESCRIPTION": "Healthcare API",
    "VERSION": "1.0.0",
    "SERVE_INCLUDE_SCHEMA": False,
    "COMPONENT_SPLIT_REQUEST": True,
}
```

## Step 3: Add Schema URLs

```python
# config/urls.py
from django.urls import path
from drf_spectacular.views import (
    SpectacularAPIView,
    SpectacularSwaggerView,
    SpectacularRedocView,
)

urlpatterns = [
    path("api/schema/", SpectacularAPIView.as_view(), name="schema"),
    path("api/docs/", SpectacularSwaggerView.as_view(url_name="schema"), name="swagger-ui"),
    path("api/redoc/", SpectacularRedocView.as_view(url_name="schema"), name="redoc"),
]
```

## Step 4: Document Views

### extend_schema

```python
from drf_spectacular.utils import extend_schema, OpenApiParameter
from rest_framework import generics


class DoctorListView(generics.ListCreateAPIView):
    queryset = Doctor.objects.all()
    serializer_class = DoctorReadSerializer

    @extend_schema(
        summary="List doctors",
        parameters=[
            OpenApiParameter(name="specialty", type=str, location=OpenApiParameter.QUERY),
        ],
    )
    def get(self, request, *args, **kwargs):
        return super().get(request, *args, **kwargs)
```

### ViewSet annotations

```python
from drf_spectacular.utils import extend_schema_view, extend_schema


@extend_schema_view(
    list=extend_schema(summary="List doctors"),
    retrieve=extend_schema(summary="Doctor details"),
)
class DoctorViewSet(viewsets.ModelViewSet):
    queryset = Doctor.objects.all()
    serializer_class = DoctorReadSerializer
```

## Step 5: Generate Schema File

```bash
uv run python manage.py spectacular --file schema.yml
```

## Compatibility Note

drf-spectacular documents support for Django up to 5.2 and DRF 3.16. If you use Django 6.0, validate compatibility in your project.

## References

- [drf-spectacular Documentation](https://drf-spectacular.readthedocs.io/)

## Next Steps

- [Testing](./09-testing.md) - Unit and API tests
- [Project: Doctor API](./10-project-doctor-api.md) - Complete example

---

[Previous: Validation](./07-validation.md) | [Back to Index](./README.md) | [Next: Testing](./09-testing.md)
