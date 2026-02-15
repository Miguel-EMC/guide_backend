# Frontend Integration

This chapter covers connecting frontend applications (React, Vue, Next.js) to your DRF API, including authentication strategies, error handling, real-time updates, and type-safe API clients.

## Overview

| Integration Pattern | Use Case |
|--------------------|----------|
| REST API + Token | SPAs, Mobile apps |
| REST API + Session | Traditional web apps |
| REST API + JWT | Microservices, Third-party apps |
| WebSocket | Real-time features |
| Server-Side Rendering | SEO-critical pages |

## CORS Configuration

### Installation

```bash
uv add django-cors-headers
```

### Basic Setup

```python
# config/settings.py
INSTALLED_APPS = [
    "corsheaders",
    # ...
]

MIDDLEWARE = [
    "corsheaders.middleware.CorsMiddleware",  # Must be high in the list
    "django.middleware.common.CommonMiddleware",
    # ...
]
```

### Development Configuration

```python
# config/settings.py
if DEBUG:
    CORS_ALLOW_ALL_ORIGINS = True
else:
    CORS_ALLOWED_ORIGINS = [
        "https://app.example.com",
        "https://admin.example.com",
    ]
```

### Production Configuration

```python
# config/settings.py
CORS_ALLOWED_ORIGINS = [
    "https://app.example.com",
    "https://admin.example.com",
]

# Allow credentials (cookies, auth headers)
CORS_ALLOW_CREDENTIALS = True

# Allowed headers
CORS_ALLOW_HEADERS = [
    "accept",
    "accept-encoding",
    "authorization",
    "content-type",
    "dnt",
    "origin",
    "user-agent",
    "x-csrftoken",
    "x-requested-with",
]

# Allowed methods
CORS_ALLOW_METHODS = [
    "DELETE",
    "GET",
    "OPTIONS",
    "PATCH",
    "POST",
    "PUT",
]

# Cache preflight requests
CORS_PREFLIGHT_MAX_AGE = 86400

# Expose headers to frontend
CORS_EXPOSE_HEADERS = [
    "Content-Disposition",
    "X-Request-ID",
    "X-RateLimit-Limit",
    "X-RateLimit-Remaining",
]
```

### Per-Path Configuration

```python
# config/settings.py
CORS_URLS_REGEX = r"^/api/.*$"  # Only allow CORS on /api/ paths
```

## Authentication Strategies

### Token Authentication

```python
# config/settings.py
INSTALLED_APPS = [
    "rest_framework.authtoken",
    # ...
]

REST_FRAMEWORK = {
    "DEFAULT_AUTHENTICATION_CLASSES": [
        "rest_framework.authentication.TokenAuthentication",
    ],
}
```

```python
# auth/views.py
from rest_framework.authtoken.views import ObtainAuthToken
from rest_framework.authtoken.models import Token
from rest_framework.response import Response


class LoginView(ObtainAuthToken):
    def post(self, request, *args, **kwargs):
        serializer = self.serializer_class(
            data=request.data,
            context={"request": request},
        )
        serializer.is_valid(raise_exception=True)
        user = serializer.validated_data["user"]
        token, created = Token.objects.get_or_create(user=user)

        return Response({
            "token": token.key,
            "user_id": user.pk,
            "email": user.email,
        })
```

### JWT Authentication

```bash
uv add djangorestframework-simplejwt
```

```python
# config/settings.py
from datetime import timedelta

REST_FRAMEWORK = {
    "DEFAULT_AUTHENTICATION_CLASSES": [
        "rest_framework_simplejwt.authentication.JWTAuthentication",
    ],
}

SIMPLE_JWT = {
    "ACCESS_TOKEN_LIFETIME": timedelta(minutes=15),
    "REFRESH_TOKEN_LIFETIME": timedelta(days=7),
    "ROTATE_REFRESH_TOKENS": True,
    "BLACKLIST_AFTER_ROTATION": True,
    "AUTH_HEADER_TYPES": ("Bearer",),
}
```

```python
# config/urls.py
from rest_framework_simplejwt.views import (
    TokenObtainPairView,
    TokenRefreshView,
)

urlpatterns = [
    path("api/auth/token/", TokenObtainPairView.as_view(), name="token_obtain_pair"),
    path("api/auth/token/refresh/", TokenRefreshView.as_view(), name="token_refresh"),
]
```

### Session Authentication with CSRF

```python
# config/settings.py
REST_FRAMEWORK = {
    "DEFAULT_AUTHENTICATION_CLASSES": [
        "rest_framework.authentication.SessionAuthentication",
    ],
}

# CSRF settings for API
CSRF_COOKIE_HTTPONLY = False  # Allow JS to read CSRF token
CSRF_COOKIE_SAMESITE = "Lax"
CSRF_TRUSTED_ORIGINS = [
    "https://app.example.com",
]
```

## React Integration

### API Client Setup

```typescript
// src/api/client.ts
const API_BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8000";

interface ApiError {
  detail?: string;
  [key: string]: unknown;
}

class ApiClient {
  private baseUrl: string;
  private token: string | null = null;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
    this.token = localStorage.getItem("token");
  }

  setToken(token: string | null) {
    this.token = token;
    if (token) {
      localStorage.setItem("token", token);
    } else {
      localStorage.removeItem("token");
    }
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${this.baseUrl}${endpoint}`;

    const headers: HeadersInit = {
      "Content-Type": "application/json",
      ...options.headers,
    };

    if (this.token) {
      headers["Authorization"] = `Bearer ${this.token}`;
    }

    const response = await fetch(url, {
      ...options,
      headers,
    });

    if (!response.ok) {
      const error: ApiError = await response.json().catch(() => ({}));

      if (response.status === 401) {
        this.setToken(null);
        window.location.href = "/login";
      }

      throw new Error(error.detail || `HTTP ${response.status}`);
    }

    if (response.status === 204) {
      return undefined as T;
    }

    return response.json();
  }

  get<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: "GET" });
  }

  post<T>(endpoint: string, data: unknown): Promise<T> {
    return this.request<T>(endpoint, {
      method: "POST",
      body: JSON.stringify(data),
    });
  }

  put<T>(endpoint: string, data: unknown): Promise<T> {
    return this.request<T>(endpoint, {
      method: "PUT",
      body: JSON.stringify(data),
    });
  }

  patch<T>(endpoint: string, data: unknown): Promise<T> {
    return this.request<T>(endpoint, {
      method: "PATCH",
      body: JSON.stringify(data),
    });
  }

  delete<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: "DELETE" });
  }
}

export const apiClient = new ApiClient(API_BASE_URL);
```

### Type Definitions

```typescript
// src/api/types.ts
export interface Doctor {
  id: number;
  first_name: string;
  last_name: string;
  email: string;
  specialty: string;
  is_active: boolean;
  created_at: string;
}

export interface PaginatedResponse<T> {
  count: number;
  next: string | null;
  previous: string | null;
  results: T[];
}

export interface CreateDoctorRequest {
  first_name: string;
  last_name: string;
  email: string;
  specialty: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  access: string;
  refresh: string;
}
```

### API Functions

```typescript
// src/api/doctors.ts
import { apiClient } from "./client";
import type { Doctor, PaginatedResponse, CreateDoctorRequest } from "./types";

export const doctorsApi = {
  list: (params?: { page?: number; specialty?: string }) => {
    const searchParams = new URLSearchParams();
    if (params?.page) searchParams.set("page", String(params.page));
    if (params?.specialty) searchParams.set("specialty", params.specialty);

    const query = searchParams.toString();
    return apiClient.get<PaginatedResponse<Doctor>>(
      `/api/doctors/${query ? `?${query}` : ""}`
    );
  },

  get: (id: number) => {
    return apiClient.get<Doctor>(`/api/doctors/${id}/`);
  },

  create: (data: CreateDoctorRequest) => {
    return apiClient.post<Doctor>("/api/doctors/", data);
  },

  update: (id: number, data: Partial<CreateDoctorRequest>) => {
    return apiClient.patch<Doctor>(`/api/doctors/${id}/`, data);
  },

  delete: (id: number) => {
    return apiClient.delete<void>(`/api/doctors/${id}/`);
  },
};
```

### React Query Integration

```bash
npm install @tanstack/react-query
```

```typescript
// src/hooks/useDoctors.ts
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { doctorsApi } from "../api/doctors";
import type { CreateDoctorRequest } from "../api/types";

export function useDoctors(params?: { page?: number; specialty?: string }) {
  return useQuery({
    queryKey: ["doctors", params],
    queryFn: () => doctorsApi.list(params),
  });
}

export function useDoctor(id: number) {
  return useQuery({
    queryKey: ["doctors", id],
    queryFn: () => doctorsApi.get(id),
    enabled: !!id,
  });
}

export function useCreateDoctor() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateDoctorRequest) => doctorsApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["doctors"] });
    },
  });
}

export function useUpdateDoctor() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<CreateDoctorRequest> }) =>
      doctorsApi.update(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ["doctors"] });
      queryClient.invalidateQueries({ queryKey: ["doctors", variables.id] });
    },
  });
}

export function useDeleteDoctor() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => doctorsApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["doctors"] });
    },
  });
}
```

### Component Example

```tsx
// src/components/DoctorList.tsx
import { useDoctors, useDeleteDoctor } from "../hooks/useDoctors";

export function DoctorList() {
  const [page, setPage] = useState(1);
  const { data, isLoading, error } = useDoctors({ page });
  const deleteDoctor = useDeleteDoctor();

  if (isLoading) return <div>Loading...</div>;
  if (error) return <div>Error: {error.message}</div>;

  return (
    <div>
      <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>Email</th>
            <th>Specialty</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {data?.results.map((doctor) => (
            <tr key={doctor.id}>
              <td>{doctor.first_name} {doctor.last_name}</td>
              <td>{doctor.email}</td>
              <td>{doctor.specialty}</td>
              <td>
                <button
                  onClick={() => deleteDoctor.mutate(doctor.id)}
                  disabled={deleteDoctor.isPending}
                >
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <div>
        <button
          disabled={!data?.previous}
          onClick={() => setPage((p) => p - 1)}
        >
          Previous
        </button>
        <span>Page {page}</span>
        <button
          disabled={!data?.next}
          onClick={() => setPage((p) => p + 1)}
        >
          Next
        </button>
      </div>
    </div>
  );
}
```

## Vue Integration

### Composable API Client

```typescript
// src/composables/useApi.ts
import { ref } from "vue";

const API_BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8000";

export function useApi<T>() {
  const data = ref<T | null>(null);
  const error = ref<Error | null>(null);
  const loading = ref(false);

  const token = localStorage.getItem("token");

  const headers: HeadersInit = {
    "Content-Type": "application/json",
  };

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const execute = async (endpoint: string, options: RequestInit = {}) => {
    loading.value = true;
    error.value = null;

    try {
      const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        ...options,
        headers: { ...headers, ...options.headers },
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      data.value = await response.json();
    } catch (e) {
      error.value = e as Error;
    } finally {
      loading.value = false;
    }
  };

  return { data, error, loading, execute };
}
```

### Pinia Store

```typescript
// src/stores/doctors.ts
import { defineStore } from "pinia";
import { ref } from "vue";
import type { Doctor, PaginatedResponse } from "../types";

export const useDoctorsStore = defineStore("doctors", () => {
  const doctors = ref<Doctor[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const pagination = ref({
    count: 0,
    next: null as string | null,
    previous: null as string | null,
  });

  const fetchDoctors = async (page = 1) => {
    loading.value = true;
    error.value = null;

    try {
      const response = await fetch(
        `${import.meta.env.VITE_API_URL}/api/doctors/?page=${page}`,
        {
          headers: {
            Authorization: `Bearer ${localStorage.getItem("token")}`,
          },
        }
      );

      if (!response.ok) throw new Error("Failed to fetch");

      const data: PaginatedResponse<Doctor> = await response.json();
      doctors.value = data.results;
      pagination.value = {
        count: data.count,
        next: data.next,
        previous: data.previous,
      };
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      loading.value = false;
    }
  };

  const createDoctor = async (doctorData: Partial<Doctor>) => {
    const response = await fetch(
      `${import.meta.env.VITE_API_URL}/api/doctors/`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${localStorage.getItem("token")}`,
        },
        body: JSON.stringify(doctorData),
      }
    );

    if (!response.ok) throw new Error("Failed to create");

    await fetchDoctors();
  };

  return {
    doctors,
    loading,
    error,
    pagination,
    fetchDoctors,
    createDoctor,
  };
});
```

## Session Authentication with CSRF

### Django Configuration

```python
# config/settings.py
CSRF_COOKIE_HTTPONLY = False  # JS needs to read the cookie
CSRF_COOKIE_SAMESITE = "Lax"
SESSION_COOKIE_SAMESITE = "Lax"
SESSION_COOKIE_HTTPONLY = True

# For cross-origin requests
CSRF_TRUSTED_ORIGINS = [
    "https://app.example.com",
]
```

### Frontend CSRF Handler

```typescript
// src/api/csrf.ts
function getCookie(name: string): string | null {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) {
    return parts.pop()?.split(";").shift() || null;
  }
  return null;
}

export async function csrfFetch(
  url: string,
  options: RequestInit = {}
): Promise<Response> {
  const csrftoken = getCookie("csrftoken");

  return fetch(url, {
    ...options,
    credentials: "include", // Include cookies
    headers: {
      "Content-Type": "application/json",
      "X-CSRFToken": csrftoken || "",
      ...options.headers,
    },
  });
}
```

### Get CSRF Token Endpoint

```python
# auth/views.py
from django.middleware.csrf import get_token
from django.http import JsonResponse


def get_csrf_token(request):
    """Return CSRF token for frontend."""
    return JsonResponse({"csrfToken": get_token(request)})
```

```python
# config/urls.py
urlpatterns = [
    path("api/auth/csrf/", get_csrf_token, name="csrf"),
]
```

## Error Handling

### Standardized Error Response

```python
# core/exceptions.py
from rest_framework.views import exception_handler
from rest_framework.response import Response


def custom_exception_handler(exc, context):
    response = exception_handler(exc, context)

    if response is not None:
        error_data = {
            "error": {
                "code": response.status_code,
                "message": get_error_message(response.data),
                "details": response.data if isinstance(response.data, dict) else None,
            }
        }
        response.data = error_data

    return response


def get_error_message(data):
    if isinstance(data, dict):
        if "detail" in data:
            return str(data["detail"])
        if "non_field_errors" in data:
            return str(data["non_field_errors"][0])
        # Return first field error
        for key, value in data.items():
            if isinstance(value, list):
                return f"{key}: {value[0]}"
    return "An error occurred"
```

```python
# config/settings.py
REST_FRAMEWORK = {
    "EXCEPTION_HANDLER": "core.exceptions.custom_exception_handler",
}
```

### Frontend Error Handler

```typescript
// src/api/errors.ts
export interface ApiError {
  error: {
    code: number;
    message: string;
    details?: Record<string, string[]>;
  };
}

export function parseApiError(error: unknown): string {
  if (error instanceof Response) {
    return `HTTP ${error.status}`;
  }

  if (typeof error === "object" && error !== null) {
    const apiError = error as ApiError;
    if (apiError.error?.message) {
      return apiError.error.message;
    }
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "An unexpected error occurred";
}

export function getFieldErrors(error: ApiError): Record<string, string> {
  const fieldErrors: Record<string, string> = {};

  if (error.error?.details) {
    for (const [field, errors] of Object.entries(error.error.details)) {
      if (Array.isArray(errors) && errors.length > 0) {
        fieldErrors[field] = errors[0];
      }
    }
  }

  return fieldErrors;
}
```

## File Uploads

### Django View

```python
# doctors/views.py
from rest_framework.parsers import MultiPartParser, FormParser


class DoctorPhotoUploadView(generics.UpdateAPIView):
    queryset = Doctor.objects.all()
    parser_classes = [MultiPartParser, FormParser]

    def update(self, request, *args, **kwargs):
        doctor = self.get_object()
        photo = request.FILES.get("photo")

        if not photo:
            return Response(
                {"error": "No photo provided"},
                status=400,
            )

        doctor.photo = photo
        doctor.save()

        return Response({"photo_url": doctor.photo.url})
```

### Frontend Upload

```typescript
// src/api/upload.ts
export async function uploadDoctorPhoto(
  doctorId: number,
  file: File
): Promise<{ photo_url: string }> {
  const formData = new FormData();
  formData.append("photo", file);

  const response = await fetch(
    `${API_BASE_URL}/api/doctors/${doctorId}/photo/`,
    {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${localStorage.getItem("token")}`,
        // Don't set Content-Type - browser sets it with boundary
      },
      body: formData,
    }
  );

  if (!response.ok) {
    throw new Error("Upload failed");
  }

  return response.json();
}
```

### React Upload Component

```tsx
// src/components/PhotoUpload.tsx
import { useState, useCallback } from "react";
import { uploadDoctorPhoto } from "../api/upload";

interface Props {
  doctorId: number;
  onSuccess: (url: string) => void;
}

export function PhotoUpload({ doctorId, onSuccess }: Props) {
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleFileChange = useCallback(
    async (event: React.ChangeEvent<HTMLInputElement>) => {
      const file = event.target.files?.[0];
      if (!file) return;

      // Validate file
      if (!file.type.startsWith("image/")) {
        setError("Please select an image file");
        return;
      }

      if (file.size > 5 * 1024 * 1024) {
        setError("File size must be less than 5MB");
        return;
      }

      setUploading(true);
      setError(null);

      try {
        const result = await uploadDoctorPhoto(doctorId, file);
        onSuccess(result.photo_url);
      } catch (e) {
        setError((e as Error).message);
      } finally {
        setUploading(false);
      }
    },
    [doctorId, onSuccess]
  );

  return (
    <div>
      <input
        type="file"
        accept="image/*"
        onChange={handleFileChange}
        disabled={uploading}
      />
      {uploading && <span>Uploading...</span>}
      {error && <span style={{ color: "red" }}>{error}</span>}
    </div>
  );
}
```

## Type-Safe Client Generation

### Using OpenAPI Generator

```bash
# Generate TypeScript client from schema
npx openapi-generator-cli generate \
  -i http://localhost:8000/api/schema/ \
  -g typescript-fetch \
  -o ./src/api/generated

# Or with axios
npx openapi-generator-cli generate \
  -i http://localhost:8000/api/schema/ \
  -g typescript-axios \
  -o ./src/api/generated
```

### Using openapi-typescript

```bash
npm install openapi-typescript
npx openapi-typescript http://localhost:8000/api/schema/ --output src/api/schema.d.ts
```

```typescript
// src/api/client.ts
import type { paths } from "./schema";

type DoctorsResponse = paths["/api/doctors/"]["get"]["responses"]["200"]["content"]["application/json"];
type Doctor = DoctorsResponse["results"][number];
```

## Real-Time with WebSockets

### Django Channels Setup

```bash
uv add channels channels-redis
```

```python
# config/asgi.py
import os
from django.core.asgi import get_asgi_application
from channels.routing import ProtocolTypeRouter, URLRouter
from channels.auth import AuthMiddlewareStack
from doctors.routing import websocket_urlpatterns

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "config.settings")

application = ProtocolTypeRouter({
    "http": get_asgi_application(),
    "websocket": AuthMiddlewareStack(
        URLRouter(websocket_urlpatterns)
    ),
})
```

```python
# doctors/consumers.py
import json
from channels.generic.websocket import AsyncWebsocketConsumer


class DoctorUpdatesConsumer(AsyncWebsocketConsumer):
    async def connect(self):
        self.room_group_name = "doctor_updates"

        await self.channel_layer.group_add(
            self.room_group_name,
            self.channel_name,
        )

        await self.accept()

    async def disconnect(self, close_code):
        await self.channel_layer.group_discard(
            self.room_group_name,
            self.channel_name,
        )

    async def doctor_update(self, event):
        await self.send(text_data=json.dumps(event["message"]))
```

### Frontend WebSocket Hook

```typescript
// src/hooks/useWebSocket.ts
import { useEffect, useRef, useCallback } from "react";

interface UseWebSocketOptions {
  url: string;
  onMessage: (data: unknown) => void;
  onError?: (error: Event) => void;
  reconnect?: boolean;
}

export function useWebSocket({
  url,
  onMessage,
  onError,
  reconnect = true,
}: UseWebSocketOptions) {
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<number>();

  const connect = useCallback(() => {
    const token = localStorage.getItem("token");
    const wsUrl = `${url}?token=${token}`;

    const ws = new WebSocket(wsUrl);

    ws.onopen = () => {
      console.log("WebSocket connected");
    };

    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      onMessage(data);
    };

    ws.onerror = (error) => {
      console.error("WebSocket error:", error);
      onError?.(error);
    };

    ws.onclose = () => {
      console.log("WebSocket closed");
      if (reconnect) {
        reconnectTimeoutRef.current = window.setTimeout(connect, 3000);
      }
    };

    wsRef.current = ws;
  }, [url, onMessage, onError, reconnect]);

  useEffect(() => {
    connect();

    return () => {
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
      wsRef.current?.close();
    };
  }, [connect]);

  const send = useCallback((data: unknown) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(data));
    }
  }, []);

  return { send };
}
```

## Best Practices

1. **Use HTTPS in production** - Never send tokens over HTTP
2. **Store tokens securely** - Memory for SPAs, httpOnly cookies for sessions
3. **Handle token refresh** - Implement automatic refresh before expiry
4. **Validate CORS strictly** - Don't use `CORS_ALLOW_ALL_ORIGINS` in production
5. **Type your API** - Generate types from OpenAPI schema
6. **Handle errors gracefully** - Show user-friendly messages
7. **Implement retry logic** - For transient network failures
8. **Use React Query/SWR** - For caching and state management

## References

- [django-cors-headers](https://github.com/adamchainz/django-cors-headers)
- [DRF Authentication](https://www.django-rest-framework.org/api-guide/authentication/)
- [React Query](https://tanstack.com/query/latest)
- [OpenAPI Generator](https://openapi-generator.tech/)

## Next Steps

- [Django Version Features](./26-django-6-features.md)
- [Architecture and Diagrams](./27-architecture-diagrams.md)

---

[Previous: API Schema Generation](./24-api-schema-generation.md) | [Back to Index](./README.md) | [Next: Django Version Features](./26-django-6-features.md)
