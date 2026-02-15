# API Schema Generation

This chapter covers OpenAPI schema generation with drf-spectacular, including customization, documentation, and CI integration.

## Why OpenAPI?

| Benefit | Description |
|---------|-------------|
| Documentation | Auto-generated, always up-to-date docs |
| Client Generation | Generate SDKs in any language |
| Testing | Import into Postman, Insomnia |
| Validation | Contract-first development |

## Installation

```bash
uv add drf-spectacular
```

## Basic Configuration

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
    "DESCRIPTION": "API for managing doctors, appointments, and patients",
    "VERSION": "1.0.0",
    "SERVE_INCLUDE_SCHEMA": False,

    # Contact info
    "CONTACT": {
        "name": "API Support",
        "email": "api@example.com",
    },

    # License
    "LICENSE": {
        "name": "MIT",
    },

    # Servers
    "SERVERS": [
        {"url": "https://api.example.com", "description": "Production"},
        {"url": "https://staging-api.example.com", "description": "Staging"},
        {"url": "http://localhost:8000", "description": "Development"},
    ],
}
```

## URL Configuration

```python
# config/urls.py
from drf_spectacular.views import (
    SpectacularAPIView,
    SpectacularRedocView,
    SpectacularSwaggerView,
)

urlpatterns = [
    # OpenAPI schema
    path("api/schema/", SpectacularAPIView.as_view(), name="schema"),

    # Swagger UI
    path("api/docs/", SpectacularSwaggerView.as_view(url_name="schema"), name="swagger-ui"),

    # ReDoc (alternative docs)
    path("api/redoc/", SpectacularRedocView.as_view(url_name="schema"), name="redoc"),

    # Your API endpoints
    path("api/", include("doctors.urls")),
]
```

## Annotating Endpoints

### Using @extend_schema

```python
# doctors/views.py
from drf_spectacular.utils import extend_schema, OpenApiParameter, OpenApiExample
from drf_spectacular.types import OpenApiTypes


class DoctorViewSet(viewsets.ModelViewSet):
    queryset = Doctor.objects.all()
    serializer_class = DoctorSerializer

    @extend_schema(
        summary="List all doctors",
        description="Returns a paginated list of all doctors in the system.",
        parameters=[
            OpenApiParameter(
                name="specialty",
                type=str,
                location=OpenApiParameter.QUERY,
                description="Filter by medical specialty",
                examples=[
                    OpenApiExample("Cardiology", value="cardiology"),
                    OpenApiExample("Neurology", value="neurology"),
                ],
            ),
            OpenApiParameter(
                name="is_active",
                type=bool,
                location=OpenApiParameter.QUERY,
                description="Filter by active status",
            ),
        ],
        responses={
            200: DoctorSerializer(many=True),
            401: OpenApiTypes.OBJECT,
        },
        tags=["Doctors"],
    )
    def list(self, request, *args, **kwargs):
        return super().list(request, *args, **kwargs)

    @extend_schema(
        summary="Create a new doctor",
        description="Creates a new doctor record. Requires admin permissions.",
        request=DoctorCreateSerializer,
        responses={
            201: DoctorSerializer,
            400: OpenApiTypes.OBJECT,
            403: OpenApiTypes.OBJECT,
        },
        tags=["Doctors"],
    )
    def create(self, request, *args, **kwargs):
        return super().create(request, *args, **kwargs)
```

### Different Serializers for Request/Response

```python
# doctors/views.py
from drf_spectacular.utils import extend_schema


class DoctorViewSet(viewsets.ModelViewSet):
    @extend_schema(
        request=DoctorCreateSerializer,
        responses={201: DoctorDetailSerializer},
    )
    def create(self, request, *args, **kwargs):
        return super().create(request, *args, **kwargs)

    @extend_schema(
        request=DoctorUpdateSerializer,
        responses={200: DoctorDetailSerializer},
    )
    def update(self, request, *args, **kwargs):
        return super().update(request, *args, **kwargs)
```

### Documenting Custom Actions

```python
# doctors/views.py
from rest_framework.decorators import action
from drf_spectacular.utils import extend_schema


class DoctorViewSet(viewsets.ModelViewSet):
    @extend_schema(
        summary="Get doctor schedule",
        description="Returns the weekly schedule for a specific doctor",
        responses={200: ScheduleSerializer},
        tags=["Doctor Schedule"],
    )
    @action(detail=True, methods=["get"])
    def schedule(self, request, pk=None):
        doctor = self.get_object()
        schedule = doctor.get_weekly_schedule()
        serializer = ScheduleSerializer(schedule)
        return Response(serializer.data)

    @extend_schema(
        summary="Deactivate doctor",
        description="Marks a doctor as inactive. Requires admin permissions.",
        request=None,
        responses={200: {"type": "object", "properties": {"status": {"type": "string"}}}},
        tags=["Doctor Management"],
    )
    @action(detail=True, methods=["post"])
    def deactivate(self, request, pk=None):
        doctor = self.get_object()
        doctor.is_active = False
        doctor.save()
        return Response({"status": "deactivated"})
```

## Serializer Documentation

### Field Documentation

```python
# doctors/serializers.py
from rest_framework import serializers


class DoctorSerializer(serializers.ModelSerializer):
    first_name = serializers.CharField(
        max_length=100,
        help_text="Doctor's first name",
    )
    last_name = serializers.CharField(
        max_length=100,
        help_text="Doctor's last name",
    )
    email = serializers.EmailField(
        help_text="Contact email address",
    )
    specialty = serializers.ChoiceField(
        choices=Doctor.SPECIALTY_CHOICES,
        help_text="Medical specialty",
    )

    class Meta:
        model = Doctor
        fields = ["id", "first_name", "last_name", "email", "specialty"]
```

### Inline Serializer

```python
from drf_spectacular.utils import extend_schema, inline_serializer
from rest_framework import serializers


@extend_schema(
    request=inline_serializer(
        name="LoginRequest",
        fields={
            "email": serializers.EmailField(),
            "password": serializers.CharField(),
        },
    ),
    responses={
        200: inline_serializer(
            name="LoginResponse",
            fields={
                "access_token": serializers.CharField(),
                "refresh_token": serializers.CharField(),
                "expires_in": serializers.IntegerField(),
            },
        ),
    },
)
def login(request):
    pass
```

## Authentication Documentation

```python
# config/settings.py
SPECTACULAR_SETTINGS = {
    # ...
    "SECURITY": [
        {"BearerAuth": []},
    ],
    "COMPONENTS": {
        "securitySchemes": {
            "BearerAuth": {
                "type": "http",
                "scheme": "bearer",
                "bearerFormat": "JWT",
            },
            "ApiKeyAuth": {
                "type": "apiKey",
                "in": "header",
                "name": "X-API-Key",
            },
        },
    },
}
```

### Per-Endpoint Security

```python
from drf_spectacular.utils import extend_schema


@extend_schema(
    security=[{"BearerAuth": []}],
)
def protected_endpoint(request):
    pass


@extend_schema(
    security=[],  # No auth required
)
def public_endpoint(request):
    pass
```

## Error Response Documentation

```python
# core/schemas.py
from drf_spectacular.utils import extend_schema, OpenApiResponse


def error_responses():
    """Common error responses for all endpoints."""
    return {
        400: OpenApiResponse(description="Bad Request - Invalid input"),
        401: OpenApiResponse(description="Unauthorized - Authentication required"),
        403: OpenApiResponse(description="Forbidden - Insufficient permissions"),
        404: OpenApiResponse(description="Not Found - Resource does not exist"),
        500: OpenApiResponse(description="Internal Server Error"),
    }


# Usage
@extend_schema(
    responses={
        200: DoctorSerializer,
        **error_responses(),
    },
)
def get_doctor(request, pk):
    pass
```

## Tags and Grouping

```python
# config/settings.py
SPECTACULAR_SETTINGS = {
    # ...
    "TAGS": [
        {"name": "Doctors", "description": "Doctor management endpoints"},
        {"name": "Appointments", "description": "Appointment scheduling"},
        {"name": "Patients", "description": "Patient management"},
        {"name": "Authentication", "description": "Login, logout, token refresh"},
    ],
}
```

```python
# doctors/views.py
from drf_spectacular.utils import extend_schema_view, extend_schema


@extend_schema_view(
    list=extend_schema(tags=["Doctors"]),
    create=extend_schema(tags=["Doctors"]),
    retrieve=extend_schema(tags=["Doctors"]),
    update=extend_schema(tags=["Doctors"]),
    destroy=extend_schema(tags=["Doctors"]),
)
class DoctorViewSet(viewsets.ModelViewSet):
    queryset = Doctor.objects.all()
    serializer_class = DoctorSerializer
```

## API Versioning

```python
# config/settings.py
SPECTACULAR_SETTINGS = {
    "VERSION": "2.0.0",
    "PREPROCESSING_HOOKS": [
        "core.schema.filter_endpoints_by_version",
    ],
}
```

```python
# core/schema.py
def filter_endpoints_by_version(endpoints):
    """Filter endpoints based on API version."""
    filtered = []
    for path, path_regex, method, callback in endpoints:
        # Include only v2 endpoints
        if path.startswith("/api/v2/"):
            filtered.append((path, path_regex, method, callback))
    return filtered
```

## Generating Schema Files

```bash
# Generate OpenAPI schema
uv run python manage.py spectacular --file schema.yaml

# Generate JSON format
uv run python manage.py spectacular --format openapi-json --file schema.json

# Validate schema
uv run python manage.py spectacular --validate
```

## CI Integration

### GitHub Actions

```yaml
# .github/workflows/openapi.yml
name: OpenAPI Schema

on:
  push:
    branches: [main]
  pull_request:

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: "3.12"

      - name: Install uv
        uses: astral-sh/setup-uv@v4

      - name: Install dependencies
        run: uv sync --frozen

      - name: Validate OpenAPI schema
        run: uv run python manage.py spectacular --validate --fail-on-warn

      - name: Generate schema
        run: uv run python manage.py spectacular --file schema.yaml

      - name: Check for schema changes
        run: |
          git diff --exit-code schema.yaml || \
            echo "::warning::OpenAPI schema has changed"

      - name: Upload schema artifact
        uses: actions/upload-artifact@v4
        with:
          name: openapi-schema
          path: schema.yaml
```

### Breaking Change Detection

```bash
# Install oasdiff
go install github.com/tufin/oasdiff@latest

# Compare schemas
oasdiff breaking base-schema.yaml new-schema.yaml
```

## Client Generation

### TypeScript Client

```bash
# Using openapi-generator
npx openapi-generator-cli generate \
  -i http://localhost:8000/api/schema/ \
  -g typescript-axios \
  -o ./frontend/src/api

# Using openapi-typescript
npx openapi-typescript http://localhost:8000/api/schema/ \
  --output ./frontend/src/api/schema.d.ts
```

### Python Client

```bash
npx openapi-generator-cli generate \
  -i http://localhost:8000/api/schema/ \
  -g python \
  -o ./clients/python
```

## Customization

### Custom Schema Class

```python
# core/schema.py
from drf_spectacular.openapi import AutoSchema


class CustomAutoSchema(AutoSchema):
    def get_operation_id(self):
        """Custom operation ID format."""
        action = self.method.lower()
        if hasattr(self.view, "action"):
            action = self.view.action
        return f"{self.view.__class__.__name__}_{action}"

    def get_tags(self):
        """Auto-generate tags from view module."""
        tags = super().get_tags()
        if not tags:
            module = self.view.__class__.__module__
            app_name = module.split(".")[0]
            tags = [app_name.replace("_", " ").title()]
        return tags
```

```python
# config/settings.py
REST_FRAMEWORK = {
    "DEFAULT_SCHEMA_CLASS": "core.schema.CustomAutoSchema",
}
```

### Exclude Endpoints

```python
from drf_spectacular.utils import extend_schema


@extend_schema(exclude=True)
def internal_endpoint(request):
    """This won't appear in the schema."""
    pass
```

### Add Examples

```python
from drf_spectacular.utils import extend_schema, OpenApiExample


@extend_schema(
    examples=[
        OpenApiExample(
            "Create Doctor",
            description="Example request to create a new doctor",
            value={
                "first_name": "John",
                "last_name": "Smith",
                "email": "john.smith@hospital.com",
                "specialty": "cardiology",
            },
            request_only=True,
        ),
        OpenApiExample(
            "Doctor Response",
            description="Example doctor response",
            value={
                "id": 1,
                "first_name": "John",
                "last_name": "Smith",
                "email": "john.smith@hospital.com",
                "specialty": "cardiology",
                "is_active": True,
            },
            response_only=True,
        ),
    ],
)
def create_doctor(request):
    pass
```

## Best Practices

1. **Keep schemas in sync** - Generate in CI, fail on changes
2. **Document all parameters** - Use `help_text` on serializer fields
3. **Use consistent tags** - Group related endpoints
4. **Include examples** - Make docs actionable
5. **Version your API** - Use URL or header versioning
6. **Validate regularly** - `--validate --fail-on-warn` in CI
7. **Generate clients** - Keep frontend/mobile SDKs updated

## References

- [drf-spectacular Documentation](https://drf-spectacular.readthedocs.io/)
- [OpenAPI Specification](https://spec.openapis.org/oas/latest.html)
- [OpenAPI Generator](https://openapi-generator.tech/)

## Next Steps

- [Frontend Integration](./25-frontend-integration.md)
- [Django Version Features](./26-django-6-features.md)

---

[Previous: Parsers and Renderers](./23-parsers-renderers.md) | [Back to Index](./README.md) | [Next: Frontend Integration](./25-frontend-integration.md)
