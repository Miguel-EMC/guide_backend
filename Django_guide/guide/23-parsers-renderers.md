# Parsers and Renderers

Parsers handle incoming request bodies; renderers control response formats.

## Step 1: Global Configuration

```python
# config/settings.py
REST_FRAMEWORK = {
    "DEFAULT_PARSER_CLASSES": [
        "rest_framework.parsers.JSONParser",
        "rest_framework.parsers.FormParser",
        "rest_framework.parsers.MultiPartParser",
    ],
    "DEFAULT_RENDERER_CLASSES": [
        "rest_framework.renderers.JSONRenderer",
        "rest_framework.renderers.BrowsableAPIRenderer",
    ],
}
```

## Step 2: Per-View Configuration

```python
from rest_framework.parsers import MultiPartParser
from rest_framework.renderers import JSONRenderer
from rest_framework.views import APIView
from rest_framework.response import Response


class FileUploadView(APIView):
    parser_classes = [MultiPartParser]
    renderer_classes = [JSONRenderer]

    def post(self, request):
        return Response({"ok": True})
```

## Step 3: Content Negotiation

DRF chooses parsers by `Content-Type` and renderers by `Accept` header.

## Tips

- Use JSON only for APIs in production.
- Keep Browsable API for dev environments.

## References

- [DRF Parsers](https://www.django-rest-framework.org/api-guide/parsers/)
- [DRF Renderers](https://www.django-rest-framework.org/api-guide/renderers/)

## Next Steps

- [API Schema Generation](./24-api-schema-generation.md)
- [Frontend Integration](./25-frontend-integration.md)

---

[Previous: Security](./22-security.md) | [Back to Index](./README.md) | [Next: API Schema Generation](./24-api-schema-generation.md)
