# Parsers and Renderers

Parsers deserialize incoming request data; renderers serialize outgoing response data. This chapter covers built-in and custom parsers/renderers for DRF APIs.

## Overview

| Component | Direction | Purpose |
|-----------|-----------|---------|
| Parser | Request → Python | Deserialize request body |
| Renderer | Python → Response | Serialize response data |

## Content Negotiation

DRF automatically selects parser/renderer based on headers:

- **Parser**: Selected by `Content-Type` header
- **Renderer**: Selected by `Accept` header

## Parsers

### Built-in Parsers

| Parser | Content-Type | Use Case |
|--------|--------------|----------|
| JSONParser | application/json | API requests |
| FormParser | application/x-www-form-urlencoded | HTML forms |
| MultiPartParser | multipart/form-data | File uploads |
| FileUploadParser | */* | Raw file uploads |

### Global Configuration

```python
# config/settings.py
REST_FRAMEWORK = {
    "DEFAULT_PARSER_CLASSES": [
        "rest_framework.parsers.JSONParser",
        "rest_framework.parsers.FormParser",
        "rest_framework.parsers.MultiPartParser",
    ],
}
```

### Per-View Configuration

```python
from rest_framework.parsers import JSONParser, MultiPartParser
from rest_framework.views import APIView


class UserProfileView(APIView):
    parser_classes = [JSONParser]

    def post(self, request):
        # request.data contains parsed JSON
        return Response({"received": request.data})


class FileUploadView(APIView):
    parser_classes = [MultiPartParser]

    def post(self, request):
        file = request.FILES.get("file")
        return Response({"filename": file.name})
```

### Custom Parser

```python
# core/parsers.py
import csv
import io
from rest_framework.parsers import BaseParser


class CSVParser(BaseParser):
    """Parse CSV data into list of dictionaries."""

    media_type = "text/csv"

    def parse(self, stream, media_type=None, parser_context=None):
        """
        Parse the incoming CSV data.
        Returns a list of dictionaries.
        """
        encoding = parser_context.get("encoding", "utf-8")
        decoded_stream = io.StringIO(stream.read().decode(encoding))

        reader = csv.DictReader(decoded_stream)
        return list(reader)
```

```python
# Usage
class BulkImportView(APIView):
    parser_classes = [CSVParser]

    def post(self, request):
        # request.data is a list of dicts from CSV
        for row in request.data:
            Doctor.objects.create(**row)
        return Response({"imported": len(request.data)})
```

### XML Parser

```bash
uv add djangorestframework-xml
```

```python
# config/settings.py
REST_FRAMEWORK = {
    "DEFAULT_PARSER_CLASSES": [
        "rest_framework.parsers.JSONParser",
        "rest_framework_xml.parsers.XMLParser",
    ],
}
```

### YAML Parser

```python
# core/parsers.py
import yaml
from rest_framework.parsers import BaseParser


class YAMLParser(BaseParser):
    """Parse YAML data."""

    media_type = "application/x-yaml"

    def parse(self, stream, media_type=None, parser_context=None):
        encoding = parser_context.get("encoding", "utf-8")
        data = stream.read().decode(encoding)
        return yaml.safe_load(data)
```

## Renderers

### Built-in Renderers

| Renderer | Content-Type | Use Case |
|----------|--------------|----------|
| JSONRenderer | application/json | API responses |
| BrowsableAPIRenderer | text/html | Development UI |
| HTMLFormRenderer | text/html | HTML forms |
| StaticHTMLRenderer | text/html | Static HTML |

### Global Configuration

```python
# config/settings.py
REST_FRAMEWORK = {
    "DEFAULT_RENDERER_CLASSES": [
        "rest_framework.renderers.JSONRenderer",
        "rest_framework.renderers.BrowsableAPIRenderer",  # Dev only
    ],
}
```

### Production Configuration

```python
# config/settings.py
import os

if os.environ.get("DEBUG", "").lower() == "true":
    DEFAULT_RENDERER_CLASSES = [
        "rest_framework.renderers.JSONRenderer",
        "rest_framework.renderers.BrowsableAPIRenderer",
    ]
else:
    DEFAULT_RENDERER_CLASSES = [
        "rest_framework.renderers.JSONRenderer",
    ]

REST_FRAMEWORK = {
    "DEFAULT_RENDERER_CLASSES": DEFAULT_RENDERER_CLASSES,
}
```

### Per-View Configuration

```python
from rest_framework.renderers import JSONRenderer
from rest_framework.views import APIView


class APIOnlyView(APIView):
    renderer_classes = [JSONRenderer]

    def get(self, request):
        return Response({"message": "JSON only"})
```

### Custom JSON Renderer

```python
# core/renderers.py
import json
from rest_framework.renderers import JSONRenderer


class PrettyJSONRenderer(JSONRenderer):
    """JSON renderer with indentation for readability."""

    def render(self, data, accepted_media_type=None, renderer_context=None):
        if data is None:
            return b""

        renderer_context = renderer_context or {}
        indent = 2

        return json.dumps(
            data,
            cls=self.encoder_class,
            indent=indent,
            ensure_ascii=self.ensure_ascii,
        ).encode("utf-8")


class CamelCaseJSONRenderer(JSONRenderer):
    """Convert snake_case keys to camelCase."""

    def render(self, data, accepted_media_type=None, renderer_context=None):
        camelized_data = self.camelize(data)
        return super().render(camelized_data, accepted_media_type, renderer_context)

    def camelize(self, data):
        if isinstance(data, dict):
            return {
                self.to_camel_case(key): self.camelize(value)
                for key, value in data.items()
            }
        if isinstance(data, list):
            return [self.camelize(item) for item in data]
        return data

    def to_camel_case(self, snake_str):
        components = snake_str.split("_")
        return components[0] + "".join(x.title() for x in components[1:])
```

### CSV Renderer

```python
# core/renderers.py
import csv
import io
from rest_framework.renderers import BaseRenderer


class CSVRenderer(BaseRenderer):
    """Render data as CSV."""

    media_type = "text/csv"
    format = "csv"

    def render(self, data, accepted_media_type=None, renderer_context=None):
        if data is None:
            return b""

        # Handle paginated results
        if isinstance(data, dict) and "results" in data:
            data = data["results"]

        if not data:
            return b""

        # Get fieldnames from first item
        if isinstance(data, list):
            fieldnames = list(data[0].keys())
        else:
            fieldnames = list(data.keys())
            data = [data]

        output = io.StringIO()
        writer = csv.DictWriter(output, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(data)

        return output.getvalue().encode("utf-8")
```

```python
# Usage
class DoctorExportView(generics.ListAPIView):
    queryset = Doctor.objects.all()
    serializer_class = DoctorSerializer
    renderer_classes = [CSVRenderer]

    def finalize_response(self, request, response, *args, **kwargs):
        response = super().finalize_response(request, response, *args, **kwargs)
        if isinstance(response.accepted_renderer, CSVRenderer):
            response["Content-Disposition"] = 'attachment; filename="doctors.csv"'
        return response
```

### XML Renderer

```bash
uv add djangorestframework-xml
```

```python
# config/settings.py
REST_FRAMEWORK = {
    "DEFAULT_RENDERER_CLASSES": [
        "rest_framework.renderers.JSONRenderer",
        "rest_framework_xml.renderers.XMLRenderer",
    ],
}
```

### PDF Renderer

```bash
uv add reportlab
```

```python
# core/renderers.py
from io import BytesIO
from rest_framework.renderers import BaseRenderer
from reportlab.lib.pagesizes import letter
from reportlab.pdfgen import canvas


class PDFRenderer(BaseRenderer):
    """Render data as PDF report."""

    media_type = "application/pdf"
    format = "pdf"

    def render(self, data, accepted_media_type=None, renderer_context=None):
        buffer = BytesIO()
        pdf = canvas.Canvas(buffer, pagesize=letter)

        # Add content
        pdf.drawString(100, 750, "Doctor Report")

        y_position = 700
        if isinstance(data, dict) and "results" in data:
            data = data["results"]

        for item in data[:20]:  # Limit items
            text = f"{item.get('first_name', '')} {item.get('last_name', '')}"
            pdf.drawString(100, y_position, text)
            y_position -= 20

        pdf.save()
        return buffer.getvalue()
```

## Content Negotiation Examples

### Request with Different Parsers

```bash
# JSON (default)
curl -X POST http://localhost:8000/api/doctors/ \
  -H "Content-Type: application/json" \
  -d '{"name": "Dr. Smith"}'

# Form data
curl -X POST http://localhost:8000/api/doctors/ \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "name=Dr.%20Smith"

# File upload
curl -X POST http://localhost:8000/api/upload/ \
  -H "Content-Type: multipart/form-data" \
  -F "file=@document.pdf"
```

### Request with Different Renderers

```bash
# JSON (default)
curl http://localhost:8000/api/doctors/

# CSV export
curl http://localhost:8000/api/doctors/ \
  -H "Accept: text/csv"

# XML
curl http://localhost:8000/api/doctors/ \
  -H "Accept: application/xml"
```

### Format Suffix

```python
# config/urls.py
from rest_framework.urlpatterns import format_suffix_patterns

urlpatterns = [
    path("api/doctors/", DoctorListView.as_view()),
]

urlpatterns = format_suffix_patterns(urlpatterns)
```

```bash
# Access with format suffix
curl http://localhost:8000/api/doctors.json
curl http://localhost:8000/api/doctors.csv
```

## Error Handling

### Custom Exception Renderer

```python
# core/renderers.py
from rest_framework.renderers import JSONRenderer


class ErrorJSONRenderer(JSONRenderer):
    """Consistent error response format."""

    def render(self, data, accepted_media_type=None, renderer_context=None):
        response = renderer_context.get("response")

        if response and response.status_code >= 400:
            # Wrap error in consistent format
            data = {
                "success": False,
                "error": data,
                "status_code": response.status_code,
            }

        return super().render(data, accepted_media_type, renderer_context)
```

## Streaming Responses

```python
# core/views.py
from django.http import StreamingHttpResponse
import csv


def stream_csv(request):
    """Stream large CSV file."""

    def generate():
        # Header
        yield "id,name,email\n"

        # Data rows
        for doctor in Doctor.objects.iterator():
            yield f"{doctor.id},{doctor.name},{doctor.email}\n"

    response = StreamingHttpResponse(
        generate(),
        content_type="text/csv",
    )
    response["Content-Disposition"] = 'attachment; filename="doctors.csv"'
    return response
```

## Best Practices

1. **Use JSON for APIs** - Standard, well-supported
2. **Disable BrowsableAPI in production** - Security and performance
3. **Support CSV export** - Common business requirement
4. **Use streaming for large data** - Memory efficient
5. **Validate content types** - Reject unexpected formats
6. **Set proper Content-Disposition** - For downloads

## References

- [DRF Parsers](https://www.django-rest-framework.org/api-guide/parsers/)
- [DRF Renderers](https://www.django-rest-framework.org/api-guide/renderers/)
- [Content Negotiation](https://www.django-rest-framework.org/api-guide/content-negotiation/)

## Next Steps

- [API Schema Generation](./24-api-schema-generation.md)
- [Frontend Integration](./25-frontend-integration.md)

---

[Previous: Security](./22-security.md) | [Back to Index](./README.md) | [Next: API Schema Generation](./24-api-schema-generation.md)
