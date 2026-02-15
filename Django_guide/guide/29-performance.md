# Performance and Profiling

This chapter covers practical performance tuning for Django + DRF: identifying bottlenecks, optimizing queries, caching strategies, and profiling tools.

## Performance Optimization Workflow

1. **Measure first** - Profile before optimizing
2. **Find the bottleneck** - Database? Serialization? Network?
3. **Fix the biggest issue** - 80/20 rule applies
4. **Measure again** - Verify improvement
5. **Repeat** - Until performance goals are met

## Django Debug Toolbar

Essential for development profiling.

### Installation

```bash
uv add django-debug-toolbar
```

### Configuration

```python
# config/settings.py
INSTALLED_APPS = [
    # ...
    "debug_toolbar",
]

MIDDLEWARE = [
    "debug_toolbar.middleware.DebugToolbarMiddleware",
    # ... other middleware
]

INTERNAL_IPS = ["127.0.0.1"]

DEBUG_TOOLBAR_CONFIG = {
    "SHOW_TOOLBAR_CALLBACK": lambda request: DEBUG,
}
```

```python
# config/urls.py
from django.conf import settings

if settings.DEBUG:
    import debug_toolbar
    urlpatterns = [
        path("__debug__/", include(debug_toolbar.urls)),
    ] + urlpatterns
```

### What to Look For

| Panel | Key Metrics |
|-------|-------------|
| SQL | Query count, duplicate queries, slow queries |
| Time | Total request time breakdown |
| Cache | Cache hits/misses |
| Templates | Render time |

## Avoiding N+1 Queries

The most common Django performance issue.

### The Problem

```python
# BAD: N+1 queries
appointments = Appointment.objects.all()
for appointment in appointments:
    print(appointment.doctor.name)  # Each access triggers a query!
    print(appointment.patient.name)  # Another query per iteration!
```

### The Solution: select_related and prefetch_related

```python
# GOOD: 1 query with JOINs for ForeignKey/OneToOne
appointments = Appointment.objects.select_related("doctor", "patient").all()

# GOOD: 2 queries for ManyToMany/reverse ForeignKey
doctors = Doctor.objects.prefetch_related("appointments").all()

# Combined
appointments = (
    Appointment.objects
    .select_related("doctor", "patient")
    .prefetch_related("doctor__specialties")
    .all()
)
```

### In DRF Serializers

```python
# doctors/views.py
class DoctorViewSet(viewsets.ModelViewSet):
    serializer_class = DoctorSerializer

    def get_queryset(self):
        return (
            Doctor.objects
            .select_related("department")
            .prefetch_related("appointments", "specialties")
        )
```

### Testing Query Count

```python
# tests/test_performance.py
from django.test import TestCase
from django.test.utils import CaptureQueriesContext
from django.db import connection


class DoctorAPITest(TestCase):
    def test_list_doctors_query_count(self):
        # Create test data
        for i in range(10):
            Doctor.objects.create(name=f"Doctor {i}")

        with CaptureQueriesContext(connection) as context:
            response = self.client.get("/api/doctors/")

        # Assert maximum query count
        self.assertLessEqual(len(context), 3)
```

## Database Indexes

### Adding Indexes

```python
# doctors/models.py
class Doctor(models.Model):
    last_name = models.CharField(max_length=100, db_index=True)
    email = models.EmailField(unique=True)  # Unique implies index
    created_at = models.DateTimeField(auto_now_add=True, db_index=True)

    class Meta:
        indexes = [
            models.Index(fields=["last_name", "first_name"]),
            models.Index(fields=["-created_at"]),  # Descending
        ]
```

### When to Add Indexes

- Fields used in `filter()`, `exclude()`, `order_by()`
- Foreign keys (Django adds automatically)
- Fields used in `WHERE` clauses

### Checking Index Usage

```python
# In Django shell
from django.db import connection

Doctor.objects.filter(last_name="Smith").explain()
```

```sql
-- PostgreSQL EXPLAIN
EXPLAIN ANALYZE SELECT * FROM doctors_doctor WHERE last_name = 'Smith';
```

## Query Optimization

### Use only() and defer()

```python
# Only load specific fields
doctors = Doctor.objects.only("id", "name", "email")

# Defer heavy fields
doctors = Doctor.objects.defer("biography", "photo")
```

### Use values() and values_list()

```python
# Returns dictionaries instead of model instances
names = Doctor.objects.values("id", "name")

# Returns tuples
ids = Doctor.objects.values_list("id", flat=True)
```

### Aggregate at Database Level

```python
from django.db.models import Count, Avg, Sum

# BAD: Loading all objects into Python
total = sum(a.amount for a in Appointment.objects.all())

# GOOD: Database aggregation
total = Appointment.objects.aggregate(total=Sum("amount"))["total"]

# Count with conditions
Doctor.objects.annotate(
    appointment_count=Count("appointments"),
    avg_duration=Avg("appointments__duration"),
)
```

### Bulk Operations

```python
# BAD: Individual inserts
for data in doctor_data:
    Doctor.objects.create(**data)

# GOOD: Bulk insert
Doctor.objects.bulk_create([
    Doctor(**data) for data in doctor_data
])

# GOOD: Bulk update
Doctor.objects.filter(department="cardiology").update(on_call=True)

# Bulk update with different values
doctors = Doctor.objects.all()
for doctor in doctors:
    doctor.on_call = calculate_on_call(doctor)
Doctor.objects.bulk_update(doctors, ["on_call"])
```

## Serializer Optimization

### Avoid Nested Serializers in Large Lists

```python
# BAD for large lists: Deep nesting
class AppointmentSerializer(serializers.ModelSerializer):
    doctor = DoctorSerializer()  # Full nested object
    patient = PatientSerializer()  # Full nested object

# GOOD: Use IDs or minimal representation
class AppointmentListSerializer(serializers.ModelSerializer):
    doctor_name = serializers.CharField(source="doctor.name", read_only=True)

    class Meta:
        model = Appointment
        fields = ["id", "date", "doctor_name"]

# Use different serializers for list vs detail
class AppointmentViewSet(viewsets.ModelViewSet):
    def get_serializer_class(self):
        if self.action == "list":
            return AppointmentListSerializer
        return AppointmentDetailSerializer
```

### Use SerializerMethodField Sparingly

```python
# Each method field can cause additional queries
class DoctorSerializer(serializers.ModelSerializer):
    # BAD: Causes N+1 if not prefetched
    appointment_count = serializers.SerializerMethodField()

    def get_appointment_count(self, obj):
        return obj.appointments.count()

# GOOD: Use annotation in queryset
class DoctorViewSet(viewsets.ModelViewSet):
    def get_queryset(self):
        return Doctor.objects.annotate(
            appointment_count=Count("appointments")
        )

class DoctorSerializer(serializers.ModelSerializer):
    appointment_count = serializers.IntegerField(read_only=True)
```

## Caching Strategies

### View-Level Caching

```python
from django.views.decorators.cache import cache_page
from django.utils.decorators import method_decorator

class DoctorViewSet(viewsets.ModelViewSet):
    @method_decorator(cache_page(60 * 15))  # 15 minutes
    def list(self, request, *args, **kwargs):
        return super().list(request, *args, **kwargs)
```

### Low-Level Caching

```python
from django.core.cache import cache

def get_doctor_stats(doctor_id: int) -> dict:
    cache_key = f"doctor_stats:{doctor_id}"

    stats = cache.get(cache_key)
    if stats is not None:
        return stats

    # Expensive computation
    stats = calculate_doctor_stats(doctor_id)

    cache.set(cache_key, stats, timeout=60 * 60)  # 1 hour
    return stats
```

### Cache Invalidation

```python
from django.db.models.signals import post_save, post_delete
from django.dispatch import receiver

@receiver([post_save, post_delete], sender=Appointment)
def invalidate_doctor_cache(sender, instance, **kwargs):
    cache.delete(f"doctor_stats:{instance.doctor_id}")
```

## Pagination

Always paginate list endpoints.

```python
# config/settings.py
REST_FRAMEWORK = {
    "DEFAULT_PAGINATION_CLASS": "rest_framework.pagination.PageNumberPagination",
    "PAGE_SIZE": 20,
}
```

```python
# Custom pagination
class LargeResultsSetPagination(PageNumberPagination):
    page_size = 100
    page_size_query_param = "page_size"
    max_page_size = 1000


class DoctorViewSet(viewsets.ModelViewSet):
    pagination_class = LargeResultsSetPagination
```

## Profiling Tools

### cProfile

```python
import cProfile
import pstats

def profile_view():
    profiler = cProfile.Profile()
    profiler.enable()

    # Code to profile
    response = client.get("/api/doctors/")

    profiler.disable()
    stats = pstats.Stats(profiler)
    stats.sort_stats("cumulative")
    stats.print_stats(20)
```

### py-spy (Production Sampling)

```bash
# Install
pip install py-spy

# Profile running process
py-spy top --pid <PID>

# Generate flame graph
py-spy record -o profile.svg --pid <PID>
```

### django-silk

Detailed per-request profiling.

```bash
uv add django-silk
```

```python
# config/settings.py
INSTALLED_APPS = ["silk", ...]
MIDDLEWARE = ["silk.middleware.SilkyMiddleware", ...]

# config/urls.py
urlpatterns = [
    path("silk/", include("silk.urls")),
    ...
]
```

### Memory Profiling

```bash
uv add memory-profiler
```

```python
from memory_profiler import profile

@profile
def memory_intensive_function():
    # Your code here
    pass
```

## Async Views (Django 4.1+)

For I/O-bound operations:

```python
from django.http import JsonResponse
import httpx


async def fetch_external_data(request):
    async with httpx.AsyncClient() as client:
        response = await client.get("https://api.example.com/data")
    return JsonResponse(response.json())
```

### Async ORM (Django 4.1+)

```python
from asgiref.sync import sync_to_async

# Async queryset methods
doctors = await Doctor.objects.filter(active=True).acount()
doctor = await Doctor.objects.aget(id=1)

async for doctor in Doctor.objects.filter(active=True):
    print(doctor.name)
```

## Performance Checklist

### Database

- [ ] Use `select_related` for ForeignKey/OneToOne
- [ ] Use `prefetch_related` for ManyToMany/reverse FK
- [ ] Add indexes for filtered/sorted fields
- [ ] Use `only()`/`defer()` for large models
- [ ] Aggregate in database, not Python
- [ ] Use bulk operations for multiple records

### Caching

- [ ] Cache expensive computations
- [ ] Cache external API responses
- [ ] Implement cache invalidation
- [ ] Use appropriate TTLs

### Serialization

- [ ] Use different serializers for list/detail
- [ ] Avoid deep nesting in lists
- [ ] Use annotations instead of SerializerMethodField

### General

- [ ] Always paginate lists
- [ ] Set query count limits in tests
- [ ] Profile before optimizing
- [ ] Monitor p95/p99 latency

## Benchmarking

### Using locust

```bash
uv add locust
```

```python
# locustfile.py
from locust import HttpUser, task, between


class DoctorUser(HttpUser):
    wait_time = between(1, 3)

    @task(3)
    def list_doctors(self):
        self.client.get("/api/doctors/")

    @task(1)
    def get_doctor(self):
        self.client.get("/api/doctors/1/")
```

```bash
locust -f locustfile.py --host=http://localhost:8000
```

## References

- [Django Database Optimization](https://docs.djangoproject.com/en/5.2/topics/db/optimization/)
- [Django Debug Toolbar](https://django-debug-toolbar.readthedocs.io/)
- [DRF Performance Tips](https://www.django-rest-framework.org/api-guide/performance/)
- [py-spy Profiler](https://github.com/benfred/py-spy)

## Next Steps

- [CI/CD](./30-ci-cd.md)

---

[Previous: Observability](./28-observability.md) | [Back to Index](./README.md) | [Next: CI/CD](./30-ci-cd.md)
