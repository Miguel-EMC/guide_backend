# Authentication and Permissions

This chapter explains DRF authentication classes, permission policies, and practical patterns for production APIs.

## Step 1: Choose Authentication Strategy

DRF supports multiple authentication classes. Common built-ins:

| Class | Use Case | Notes |
|-------|----------|-------|
| `SessionAuthentication` | Browser + admin | Requires CSRF for unsafe methods |
| `BasicAuthentication` | Testing | Not recommended for production |
| `TokenAuthentication` | Simple token auth | Good for small APIs |

## Step 2: Configure Global Authentication

```python
# config/settings.py
REST_FRAMEWORK = {
    "DEFAULT_AUTHENTICATION_CLASSES": [
        "rest_framework.authentication.SessionAuthentication",
        "rest_framework.authentication.TokenAuthentication",
    ],
}
```

## Step 3: Token Authentication Setup

```python
# config/settings.py
INSTALLED_APPS = [
    # ...
    "rest_framework.authtoken",
]
```

```bash
uv run python manage.py migrate
```

Create a token endpoint:

```python
# config/urls.py
from django.urls import path
from rest_framework.authtoken.views import obtain_auth_token

urlpatterns = [
    path("api/token/", obtain_auth_token, name="api-token"),
]
```

Use it from a client:

```bash
curl -X POST http://localhost:8000/api/token/ \
  -d "username=admin&password=password"

curl http://localhost:8000/api/doctors/ \
  -H "Authorization: Token <token>"
```

## Step 4: Per-View Authentication

```python
from rest_framework.authentication import SessionAuthentication, TokenAuthentication
from rest_framework.permissions import IsAuthenticated


class DoctorListView(generics.ListAPIView):
    authentication_classes = [SessionAuthentication, TokenAuthentication]
    permission_classes = [IsAuthenticated]
    queryset = Doctor.objects.all()
    serializer_class = DoctorReadSerializer
```

## Step 5: Permissions

Permissions control access after authentication.

### Common Permission Classes

| Class | Purpose |
|-------|---------|
| `AllowAny` | Public endpoints |
| `IsAuthenticated` | Authenticated only |
| `IsAdminUser` | Admin only |
| `IsAuthenticatedOrReadOnly` | Safe methods public, write authenticated |
| `DjangoModelPermissions` | Uses Django perms |
| `DjangoObjectPermissions` | Object-level perms |

### Global Permissions

```python
REST_FRAMEWORK = {
    "DEFAULT_PERMISSION_CLASSES": [
        "rest_framework.permissions.IsAuthenticated",
    ],
}
```

### Per-View Permissions

```python
from rest_framework.permissions import AllowAny, IsAuthenticated


class PublicDoctorListView(generics.ListAPIView):
    permission_classes = [AllowAny]
    queryset = Doctor.objects.filter(is_active=True)
    serializer_class = DoctorReadSerializer


class PrivateDoctorListView(generics.ListAPIView):
    permission_classes = [IsAuthenticated]
    queryset = Doctor.objects.all()
    serializer_class = DoctorReadSerializer
```

## Step 6: Custom Permissions

```python
from rest_framework import permissions


class IsOwnerOrReadOnly(permissions.BasePermission):
    def has_object_permission(self, request, view, obj):
        if request.method in permissions.SAFE_METHODS:
            return True
        return obj.created_by == request.user
```

Use it in a view:

```python
class DoctorViewSet(viewsets.ModelViewSet):
    queryset = Doctor.objects.all()
    serializer_class = DoctorReadSerializer
    permission_classes = [IsAuthenticated, IsOwnerOrReadOnly]
```

## Step 7: Action-Based Permissions

```python
class DoctorViewSet(viewsets.ModelViewSet):
    queryset = Doctor.objects.all()

    def get_permissions(self):
        if self.action in {"list", "retrieve"}:
            return [AllowAny()]
        return [IsAuthenticated()]
```

## Step 8: Security Notes

- Session auth requires CSRF for unsafe methods.
- Use HTTPS for all token-based auth.
- Document authentication in your API docs.

## References

- [DRF Authentication](https://www.django-rest-framework.org/api-guide/authentication/)
- [DRF Permissions](https://www.django-rest-framework.org/api-guide/permissions/)

## Next Steps

- [Validation](./07-validation.md) - Data validation patterns
- [API Documentation](./08-api-documentation.md) - OpenAPI and Swagger

---

[Previous: URLs and Routing](./05-urls-routing.md) | [Back to Index](./README.md) | [Next: Validation](./07-validation.md)
