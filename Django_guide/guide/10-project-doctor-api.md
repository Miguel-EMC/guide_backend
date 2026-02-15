# Project: Doctor API (Complete Example)

This chapter builds a full DRF project end-to-end with models, serializers, viewsets, filtering, auth, and documentation.

## Step 1: Create the Project

```bash
mkdir doctor_api
cd doctor_api

uv init
uv add "django>=5.2,<6.0" "djangorestframework>=3.16" "drf-spectacular>=0.28"
uv add "django-filter>=24.3" "psycopg[binary]>=3.1" "redis>=5.0"
uv add --dev pytest pytest-django pytest-cov

uv run django-admin startproject config .
uv run python manage.py startapp doctors
uv run python manage.py startapp patients
uv run python manage.py startapp bookings
```

## Step 2: Configure Settings

```python
# config/settings.py
INSTALLED_APPS = [
    "django.contrib.admin",
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.sessions",
    "django.contrib.messages",
    "django.contrib.staticfiles",

    "rest_framework",
    "rest_framework.authtoken",
    "drf_spectacular",
    "django_filters",

    "doctors",
    "patients",
    "bookings",
]

REST_FRAMEWORK = {
    "DEFAULT_AUTHENTICATION_CLASSES": [
        "rest_framework.authentication.TokenAuthentication",
        "rest_framework.authentication.SessionAuthentication",
    ],
    "DEFAULT_PERMISSION_CLASSES": [
        "rest_framework.permissions.IsAuthenticated",
    ],
    "DEFAULT_FILTER_BACKENDS": [
        "django_filters.rest_framework.DjangoFilterBackend",
        "rest_framework.filters.SearchFilter",
        "rest_framework.filters.OrderingFilter",
    ],
    "DEFAULT_PAGINATION_CLASS": "rest_framework.pagination.PageNumberPagination",
    "PAGE_SIZE": 20,
    "DEFAULT_SCHEMA_CLASS": "drf_spectacular.openapi.AutoSchema",
}

SPECTACULAR_SETTINGS = {
    "TITLE": "Doctor API",
    "DESCRIPTION": "Healthcare management API",
    "VERSION": "1.0.0",
}
```

## Step 3: Define Models

```python
# doctors/models.py
import uuid
from django.db import models


class Department(models.Model):
    name = models.CharField(max_length=120, unique=True)

    def __str__(self) -> str:
        return self.name


class Doctor(models.Model):
    class Specialty(models.TextChoices):
        GP = "gp", "General Practice"
        CARDIO = "cardio", "Cardiology"
        NEURO = "neuro", "Neurology"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    first_name = models.CharField(max_length=100)
    last_name = models.CharField(max_length=100)
    email = models.EmailField(unique=True)
    specialty = models.CharField(max_length=30, choices=Specialty.choices)
    is_active = models.BooleanField(default=True)
    departments = models.ManyToManyField(Department, related_name="doctors", blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["last_name", "first_name"]

    def __str__(self) -> str:
        return f"Dr. {self.first_name} {self.last_name}"
```

```python
# patients/models.py
import uuid
from django.db import models


class Patient(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    first_name = models.CharField(max_length=100)
    last_name = models.CharField(max_length=100)
    email = models.EmailField(unique=True)
    date_of_birth = models.DateField()

    def __str__(self) -> str:
        return f"{self.first_name} {self.last_name}"
```

```python
# bookings/models.py
from django.db import models
from doctors.models import Doctor
from patients.models import Patient


class Appointment(models.Model):
    class Status(models.TextChoices):
        SCHEDULED = "scheduled", "Scheduled"
        COMPLETED = "completed", "Completed"
        CANCELED = "canceled", "Canceled"

    doctor = models.ForeignKey(Doctor, on_delete=models.CASCADE, related_name="appointments")
    patient = models.ForeignKey(Patient, on_delete=models.CASCADE, related_name="appointments")
    appointment_date = models.DateField()
    appointment_time = models.TimeField()
    status = models.CharField(max_length=20, choices=Status.choices, default=Status.SCHEDULED)

    class Meta:
        constraints = [
            models.UniqueConstraint(
                fields=["doctor", "appointment_date", "appointment_time"],
                name="uniq_doctor_time",
            )
        ]
```

## Step 4: Serializers (Read/Write)

```python
# doctors/serializers.py
from rest_framework import serializers
from .models import Doctor, Department


class DepartmentSerializer(serializers.ModelSerializer):
    class Meta:
        model = Department
        fields = ["id", "name"]


class DoctorReadSerializer(serializers.ModelSerializer):
    departments = DepartmentSerializer(many=True, read_only=True)

    class Meta:
        model = Doctor
        fields = ["id", "first_name", "last_name", "email", "specialty", "is_active", "departments"]


class DoctorWriteSerializer(serializers.ModelSerializer):
    class Meta:
        model = Doctor
        fields = ["first_name", "last_name", "email", "specialty", "is_active", "departments"]
```

```python
# bookings/serializers.py
from rest_framework import serializers
from .models import Appointment


class AppointmentSerializer(serializers.ModelSerializer):
    class Meta:
        model = Appointment
        fields = ["id", "doctor", "patient", "appointment_date", "appointment_time", "status"]
```

## Step 5: ViewSets

```python
# doctors/viewsets.py
from rest_framework import viewsets
from rest_framework.permissions import IsAuthenticated
from .models import Doctor
from .serializers import DoctorReadSerializer, DoctorWriteSerializer


class DoctorViewSet(viewsets.ModelViewSet):
    queryset = Doctor.objects.all()
    permission_classes = [IsAuthenticated]
    filterset_fields = ["specialty", "is_active"]
    search_fields = ["first_name", "last_name", "email"]
    ordering_fields = ["first_name", "last_name", "created_at"]
    ordering = ["last_name"]

    def get_serializer_class(self):
        if self.action in {"create", "update", "partial_update"}:
            return DoctorWriteSerializer
        return DoctorReadSerializer
```

```python
# bookings/viewsets.py
from rest_framework import viewsets
from rest_framework.permissions import IsAuthenticated
from .models import Appointment
from .serializers import AppointmentSerializer


class AppointmentViewSet(viewsets.ModelViewSet):
    queryset = Appointment.objects.select_related("doctor", "patient")
    serializer_class = AppointmentSerializer
    permission_classes = [IsAuthenticated]
    filterset_fields = ["doctor", "patient", "status"]
    ordering_fields = ["appointment_date", "appointment_time"]
```

## Step 6: URLs and Docs

```python
# config/urls.py
from django.contrib import admin
from django.urls import include, path
from drf_spectacular.views import SpectacularAPIView, SpectacularSwaggerView

urlpatterns = [
    path("admin/", admin.site.urls),
    path("api/", include("doctors.urls")),
    path("api/", include("bookings.urls")),
    path("api/schema/", SpectacularAPIView.as_view(), name="schema"),
    path("api/docs/", SpectacularSwaggerView.as_view(url_name="schema"), name="swagger-ui"),
]
```

```python
# doctors/urls.py
from django.urls import include, path
from rest_framework.routers import DefaultRouter
from .viewsets import DoctorViewSet

router = DefaultRouter()
router.register(r"doctors", DoctorViewSet, basename="doctor")

urlpatterns = [
    path("", include(router.urls)),
]
```

```python
# bookings/urls.py
from django.urls import include, path
from rest_framework.routers import DefaultRouter
from .viewsets import AppointmentViewSet

router = DefaultRouter()
router.register(r"appointments", AppointmentViewSet, basename="appointment")

urlpatterns = [
    path("", include(router.urls)),
]
```

## Step 7: Migrate and Create Superuser

```bash
uv run python manage.py makemigrations
uv run python manage.py migrate
uv run python manage.py createsuperuser
```

## Step 8: Run the Server

```bash
uv run python manage.py runserver
```

## Example Requests

```bash
# List doctors
curl http://localhost:8000/api/doctors/

# Filter doctors
curl "http://localhost:8000/api/doctors/?specialty=cardio&ordering=last_name"

# Create appointment
curl -X POST http://localhost:8000/api/appointments/ \
  -H "Authorization: Token <token>" \
  -H "Content-Type: application/json" \
  -d '{"doctor":"<uuid>","patient":"<uuid>","appointment_date":"2026-03-01","appointment_time":"10:00:00"}'
```

## Tips

- Use `select_related` and `prefetch_related` for list endpoints.
- Separate read/write serializers to avoid leaking fields.
- Use `UniqueConstraint` for booking conflicts.
- Keep docs updated with drf-spectacular.

## Next Steps

- [Filtering and Search](./11-filtering-search.md)
- [Pagination](./12-pagination.md)

---

[Previous: Testing](./09-testing.md) | [Back to Index](./README.md) | [Next: Filtering and Search](./11-filtering-search.md)
