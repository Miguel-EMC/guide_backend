# Filtering, Search, and Ordering

This chapter shows how to filter querysets with django-filter and DRF's search/ordering backends.

## Step 1: Install django-filter

```bash
uv add django-filter
```

## Step 2: Enable Filter Backends

```python
# config/settings.py
INSTALLED_APPS = [
    # ...
    "django_filters",
]

REST_FRAMEWORK = {
    "DEFAULT_FILTER_BACKENDS": [
        "django_filters.rest_framework.DjangoFilterBackend",
        "rest_framework.filters.SearchFilter",
        "rest_framework.filters.OrderingFilter",
    ],
}
```

## Step 3: Basic Filtering

```python
from rest_framework import generics
from .models import Doctor
from .serializers import DoctorReadSerializer


class DoctorListView(generics.ListAPIView):
    queryset = Doctor.objects.all()
    serializer_class = DoctorReadSerializer
    filterset_fields = ["specialty", "is_active"]
```

Requests:

```bash
GET /api/doctors/?specialty=cardio
GET /api/doctors/?is_active=true
```

## Step 4: Custom FilterSet

```python
# doctors/filters.py
import django_filters
from django.db.models import Q
from .models import Doctor


class DoctorFilter(django_filters.FilterSet):
    name = django_filters.CharFilter(method="filter_name")
    specialty = django_filters.CharFilter(lookup_expr="iexact")
    active = django_filters.BooleanFilter(field_name="is_active")

    class Meta:
        model = Doctor
        fields = ["name", "specialty", "active"]

    def filter_name(self, queryset, name, value):
        return queryset.filter(Q(first_name__icontains=value) | Q(last_name__icontains=value))
```

```python
# doctors/views.py
from django_filters.rest_framework import DjangoFilterBackend
from .filters import DoctorFilter


class DoctorListView(generics.ListAPIView):
    queryset = Doctor.objects.all()
    serializer_class = DoctorReadSerializer
    filter_backends = [DjangoFilterBackend]
    filterset_class = DoctorFilter
```

## Step 5: SearchFilter

```python
from rest_framework.filters import SearchFilter


class DoctorListView(generics.ListAPIView):
    queryset = Doctor.objects.all()
    serializer_class = DoctorReadSerializer
    filter_backends = [SearchFilter]
    search_fields = ["first_name", "last_name", "email", "specialty"]
```

Requests:

```bash
GET /api/doctors/?search=john
```

## Step 6: OrderingFilter

```python
from rest_framework.filters import OrderingFilter


class DoctorListView(generics.ListAPIView):
    queryset = Doctor.objects.all()
    serializer_class = DoctorReadSerializer
    filter_backends = [OrderingFilter]
    ordering_fields = ["first_name", "last_name", "created_at"]
    ordering = ["last_name"]
```

Requests:

```bash
GET /api/doctors/?ordering=first_name
GET /api/doctors/?ordering=-created_at
```

## Step 7: Combine All Three

```python
from django_filters.rest_framework import DjangoFilterBackend
from rest_framework.filters import SearchFilter, OrderingFilter


class DoctorViewSet(viewsets.ModelViewSet):
    queryset = Doctor.objects.all()
    serializer_class = DoctorReadSerializer
    filter_backends = [DjangoFilterBackend, SearchFilter, OrderingFilter]
    filterset_fields = ["specialty", "is_active"]
    search_fields = ["first_name", "last_name", "email"]
    ordering_fields = ["first_name", "last_name", "created_at"]
    ordering = ["last_name"]
```

## Tips

- Keep `filterset_fields` small and indexed.
- Prefer `FilterSet` when you need custom logic.
- Always validate that filters are safe for your data size.

## References

- [DRF Filtering](https://www.django-rest-framework.org/api-guide/filtering/)
- [django-filter](https://django-filter.readthedocs.io/)

## Next Steps

- [Pagination](./12-pagination.md)
- [Throttling](./13-throttling.md)

---

[Previous: Project Doctor API](./10-project-doctor-api.md) | [Back to Index](./README.md) | [Next: Pagination](./12-pagination.md)
