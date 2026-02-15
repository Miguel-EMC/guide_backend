# Views and ViewSets (DRF)

This chapter covers DRF's view options, from simple function views to powerful ViewSets.

## 1) Function-Based Views (Quick and Explicit)

```python
from rest_framework.decorators import api_view
from rest_framework.response import Response
from rest_framework import status
from .models import Doctor
from .serializers import DoctorReadSerializer, DoctorCreateSerializer


@api_view(["GET", "POST"])
def doctor_list(request):
    if request.method == "GET":
        doctors = Doctor.objects.all()
        return Response(DoctorReadSerializer(doctors, many=True).data)

    serializer = DoctorCreateSerializer(data=request.data)
    serializer.is_valid(raise_exception=True)
    doctor = serializer.save()
    return Response(DoctorReadSerializer(doctor).data, status=status.HTTP_201_CREATED)
```

## 2) APIView (Class-Based)

```python
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from django.shortcuts import get_object_or_404


class DoctorDetailView(APIView):
    def get(self, request, pk):
        doctor = get_object_or_404(Doctor, pk=pk)
        return Response(DoctorReadSerializer(doctor).data)

    def put(self, request, pk):
        doctor = get_object_or_404(Doctor, pk=pk)
        serializer = DoctorCreateSerializer(doctor, data=request.data)
        serializer.is_valid(raise_exception=True)
        doctor = serializer.save()
        return Response(DoctorReadSerializer(doctor).data)

    def delete(self, request, pk):
        doctor = get_object_or_404(Doctor, pk=pk)
        doctor.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)
```

## 3) Generic Views (Best Default for Most APIs)

Generic views combine common patterns with built-in mixins.

```python
from rest_framework import generics


class DoctorListView(generics.ListCreateAPIView):
    queryset = Doctor.objects.all()
    serializer_class = DoctorReadSerializer

    def get_serializer_class(self):
        if self.request.method == "POST":
            return DoctorCreateSerializer
        return DoctorReadSerializer

    def perform_create(self, serializer):
        serializer.save(created_by=self.request.user)
```

### Common Generic Views

| View | Methods | Purpose |
|------|---------|---------|
| `ListAPIView` | GET | List resources |
| `CreateAPIView` | POST | Create resource |
| `RetrieveAPIView` | GET | Retrieve single resource |
| `UpdateAPIView` | PUT/PATCH | Update resource |
| `DestroyAPIView` | DELETE | Delete resource |
| `ListCreateAPIView` | GET/POST | List + create |
| `RetrieveUpdateDestroyAPIView` | GET/PUT/PATCH/DELETE | Full detail CRUD |

## 4) ViewSets (Best for CRUD APIs)

ViewSets bundle CRUD actions into a single class and integrate with routers.

```python
from rest_framework import viewsets
from rest_framework.decorators import action
from rest_framework.response import Response


class DoctorViewSet(viewsets.ModelViewSet):
    queryset = Doctor.objects.all()
    serializer_class = DoctorReadSerializer

    def get_serializer_class(self):
        if self.action in {"create", "update", "partial_update"}:
            return DoctorCreateSerializer
        return DoctorReadSerializer

    @action(detail=False, methods=["get"])
    def available(self, request):
        doctors = self.get_queryset().filter(is_active=True)
        serializer = self.get_serializer(doctors, many=True)
        return Response(serializer.data)
```

### ViewSet Types

| ViewSet | Actions |
|---------|---------|
| `ViewSet` | Custom actions only |
| `GenericViewSet` | Generic behavior without defaults |
| `ReadOnlyModelViewSet` | `list`, `retrieve` |
| `ModelViewSet` | Full CRUD |

## 5) Custom Actions

```python
from rest_framework.decorators import action


class DoctorViewSet(viewsets.ModelViewSet):
    queryset = Doctor.objects.all()
    serializer_class = DoctorReadSerializer

    @action(detail=True, methods=["post"], url_path="set-on-vacation")
    def set_on_vacation(self, request, pk=None):
        doctor = self.get_object()
        doctor.is_active = False
        doctor.save(update_fields=["is_active"])
        return Response({"status": "Doctor deactivated"})
```

## 6) Common Hooks

```python
class DoctorViewSet(viewsets.ModelViewSet):
    queryset = Doctor.objects.all()
    serializer_class = DoctorReadSerializer

    def get_queryset(self):
        qs = Doctor.objects.all()
        specialty = self.request.query_params.get("specialty")
        if specialty:
            qs = qs.filter(specialty=specialty)
        return qs

    def perform_create(self, serializer):
        serializer.save(created_by=self.request.user)
```

## 7) Auth, Permissions, Throttling, and Pagination

```python
from rest_framework.permissions import IsAuthenticated
from rest_framework.throttling import UserRateThrottle


class DoctorViewSet(viewsets.ModelViewSet):
    permission_classes = [IsAuthenticated]
    throttle_classes = [UserRateThrottle]
    pagination_class = None  # Or set a custom paginator
```

## Best Practices

- Prefer `ModelViewSet` + routers for CRUD APIs.
- Use `get_serializer_class` for read/write separation.
- Scope `get_queryset` with the current user to avoid data leaks.
- Use `select_related` and `prefetch_related` in viewsets for performance.

## Next Steps

- [URLs and Routing](./05-urls-routing.md) - Routers and URL patterns
- [Authentication and Permissions](./06-authentication-permissions.md) - Secure your API

---

[Previous: Serializers](./03-serializers.md) | [Back to Index](./README.md) | [Next: URLs and Routing](./05-urls-routing.md)
