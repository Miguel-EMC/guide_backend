# Pagination

Pagination keeps list endpoints fast and predictable. DRF provides PageNumber, LimitOffset, and Cursor pagination.

## Step 1: Set Global Pagination

```python
# config/settings.py
REST_FRAMEWORK = {
    "DEFAULT_PAGINATION_CLASS": "rest_framework.pagination.PageNumberPagination",
    "PAGE_SIZE": 20,
}
```

## Step 2: PageNumberPagination (Most Common)

```python
from rest_framework.pagination import PageNumberPagination


class StandardPagination(PageNumberPagination):
    page_size = 20
    page_size_query_param = "page_size"
    max_page_size = 100
```

Requests:

```bash
GET /api/doctors/?page=2&page_size=50
```

## Step 3: LimitOffsetPagination

```python
from rest_framework.pagination import LimitOffsetPagination


class StandardLimitOffsetPagination(LimitOffsetPagination):
    default_limit = 20
    max_limit = 100
```

Requests:

```bash
GET /api/doctors/?limit=20&offset=40
```

## Step 4: CursorPagination (Best for Large Datasets)

```python
from rest_framework.pagination import CursorPagination


class DoctorCursorPagination(CursorPagination):
    page_size = 20
    ordering = "-created_at"
```

Cursor pagination avoids expensive counts but does not allow jumping to arbitrary pages.

## Step 5: Per-View Pagination

```python
class DoctorListView(generics.ListAPIView):
    queryset = Doctor.objects.all()
    serializer_class = DoctorReadSerializer
    pagination_class = StandardPagination
```

Disable pagination for a specific view:

```python
class AllDoctorsView(generics.ListAPIView):
    queryset = Doctor.objects.all()
    serializer_class = DoctorReadSerializer
    pagination_class = None
```

## Custom Response Format

```python
from rest_framework.response import Response


class CustomPagination(PageNumberPagination):
    page_size = 20

    def get_paginated_response(self, data):
        return Response({
            "count": self.page.paginator.count,
            "next": self.get_next_link(),
            "previous": self.get_previous_link(),
            "results": data,
        })
```

## Tips

- Use CursorPagination for very large tables.
- Set `max_page_size` to prevent abuse.
- Keep ordering stable for cursor pagination.

## References

- [DRF Pagination](https://www.django-rest-framework.org/api-guide/pagination/)

## Next Steps

- [Throttling](./13-throttling.md)
- [Caching](./14-caching.md)

---

[Previous: Filtering and Search](./11-filtering-search.md) | [Back to Index](./README.md) | [Next: Throttling](./13-throttling.md)
