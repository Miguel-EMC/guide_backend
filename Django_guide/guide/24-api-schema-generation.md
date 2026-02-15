# API Schema Generation

This chapter focuses on generating OpenAPI schemas for your API using DRF and drf-spectacular.

## Step 1: Install drf-spectacular

```bash
uv add drf-spectacular
```

## Step 2: Configure

```python
# config/settings.py
INSTALLED_APPS = ["drf_spectacular", *INSTALLED_APPS]

REST_FRAMEWORK = {
    "DEFAULT_SCHEMA_CLASS": "drf_spectacular.openapi.AutoSchema",
}

SPECTACULAR_SETTINGS = {
    "TITLE": "Doctor API",
    "VERSION": "1.0.0",
    "DESCRIPTION": "API documentation",
}
```

## Step 3: Schema URLs

```python
from drf_spectacular.views import SpectacularAPIView, SpectacularSwaggerView

urlpatterns = [
    path("api/schema/", SpectacularAPIView.as_view(), name="schema"),
    path("api/docs/", SpectacularSwaggerView.as_view(url_name="schema"), name="swagger-ui"),
]
```

## Step 4: Annotate Endpoints

```python
from drf_spectacular.utils import extend_schema, OpenApiParameter


class DoctorListView(generics.ListAPIView):
    queryset = Doctor.objects.all()
    serializer_class = DoctorReadSerializer

    @extend_schema(
        summary="List doctors",
        parameters=[OpenApiParameter(name="specialty", type=str, location=OpenApiParameter.QUERY)],
    )
    def get(self, request, *args, **kwargs):
        return super().get(request, *args, **kwargs)
```

## Tips

- Keep schemas in CI to detect breaking changes.
- Document auth requirements and error responses.

## References

- [drf-spectacular](https://drf-spectacular.readthedocs.io/)

## Next Steps

- [Frontend Integration](./25-frontend-integration.md)
- [Django 6.0 Notes](./26-django-6-features.md)

---

[Previous: Parsers and Renderers](./23-parsers-renderers.md) | [Back to Index](./README.md) | [Next: Frontend Integration](./25-frontend-integration.md)
