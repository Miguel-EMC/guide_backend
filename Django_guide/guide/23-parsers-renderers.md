# 23 - Parsers and Renderers

Django REST Framework (DRF) provides a flexible way to handle the incoming and outgoing data of your API through **Parsers** and **Renderers**.

-   **Parsers**: Determine how the API handles the format of incoming client requests (e.g., JSON, form data).
-   **Renderers**: Determine the format in which the API sends responses back to the client (e.g., JSON, HTML for the browsable API).

Understanding and configuring parsers and renderers is crucial for building robust and interoperable APIs.

---

## 1. Parsers

Parsers are used to parse the content of the request body, allowing DRF to handle various media types sent by clients.

### Default Parsers

By default, DRF includes these parsers:

-   `rest_framework.parsers.JSONParser`: Handles JSON-encoded request bodies.
-   `rest_framework.parsers.FormParser`: Handles standard HTML form content.
-   `rest_framework.parsers.MultiPartParser`: Handles HTML form content that supports file uploads.

### Configuring Parsers

You can configure parsers globally in your `settings.py` or on a per-view/viewset basis.

#### Global Configuration (settings.py)

To apply parsers across your entire project, add or modify the `DEFAULT_PARSER_CLASSES` setting in your `REST_FRAMEWORK` dictionary:

```python
# settings.py
REST_FRAMEWORK = {
    'DEFAULT_PARSER_CLASSES': [
        'rest_framework.parsers.JSONParser',
        'rest_framework.parsers.FormParser',
        'rest_framework.parsers.MultiPartParser', # For file uploads
    ]
}
```

#### Per-View Configuration

You can override the global settings for specific views or viewsets using the `parser_classes` attribute:

```python
# views.py
from rest_framework.parsers import JSONParser, MultiPartParser
from rest_framework.views import APIView
from rest_framework.response import Response

class FileUploadView(APIView):
    parser_classes = [MultiPartParser, JSONParser] # Allow both for this view

    def post(self, request, format=None):
        file_obj = request.data['file']
        # Do something with the file...
        return Response(status=204)
```

---

<h2>2. Renderers</h2>

Renderers are used to format the content of the response before it is sent back to the client. This allows clients to receive data in their preferred format.

<h3>Default Renderers</h3>

By default, DRF includes these renderers:

-   `rest_framework.renderers.JSONRenderer`: Renders response data into JSON. This is the most common renderer for APIs.
-   `rest_framework.renderers.BrowsableAPIRenderer`: Renders data into an HTML browsable API. This is what makes DRF's API so user-friendly for development.

<h3>Configuring Renderers</h3>

Similar to parsers, renderers can be configured globally or per-view/viewset.

<h4>Global Configuration (settings.py)</h4>

To set default renderers for your project:

```python
# settings.py
REST_FRAMEWORK = {
    'DEFAULT_RENDERER_CLASSES': [
        'rest_framework.renderers.JSONRenderer',
        'rest_framework.renderers.BrowsableAPIRenderer',
    ]
}
```

<h4>Per-View Configuration</h4>

You can specify renderers for individual views or viewsets using the `renderer_classes` attribute:

```python
# views.py
from rest_framework.renderers import JSONRenderer
from rest_framework.response import Response
from rest_framework.views import APIView

class OnlyJsonView(APIView):
    renderer_classes = [JSONRenderer] # Only allow JSON output

    def get(self, request, format=None):
        content = {'message': 'Hello, world! This is a JSON response.'}
        return Response(content)
```

---

<h2>3. Content Negotiation</h2>

DRF uses **content negotiation** to determine which parser/renderer to use for a given request/response. This process is driven primarily by the client's `Accept` and `Content-Type` headers.

-   **`Content-Type` header (Request)**: Informs the server about the format of the request body. DRF's parsers use this to process incoming data.
-   **`Accept` header (Response)**: Informs the server about the media types the client expects in the response. DRF's renderers use this to format the outgoing data.

If a client sends an `Accept: application/json` header, DRF will try to use `JSONRenderer`. If `Content-Type: application/json` is sent with a POST request, DRF will use `JSONParser`.

By leveraging parsers and renderers, you can create APIs that are highly flexible and compatible with a wide range of clients and data formats.