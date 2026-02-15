# Models and the ORM

Django models define your data schema and power the ORM. This chapter covers modeling fundamentals, relationships, constraints, indexes, and performance patterns.

## Step 1: Create and Register an App

```bash
uv run python manage.py startapp core
```

Add your app to `INSTALLED_APPS`:

```python
# config/settings.py
INSTALLED_APPS = [
    # Django
    "django.contrib.admin",
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.sessions",
    "django.contrib.messages",
    "django.contrib.staticfiles",

    # Third-party
    "rest_framework",

    # Local
    "core",
]
```

## Step 2: Define a Basic Model

```python
# core/models.py
import uuid
from django.db import models


class Doctor(models.Model):
    class Specialty(models.TextChoices):
        CARDIOLOGY = "cardiology", "Cardiology"
        NEUROLOGY = "neurology", "Neurology"
        PEDIATRICS = "pediatrics", "Pediatrics"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    first_name = models.CharField(max_length=100)
    last_name = models.CharField(max_length=100)
    email = models.EmailField(unique=True)
    specialty = models.CharField(max_length=50, choices=Specialty.choices)
    biography = models.TextField(blank=True)
    is_active = models.BooleanField(default=True)

    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["last_name", "first_name"]

    def __str__(self) -> str:
        return f"Dr. {self.first_name} {self.last_name}"
```

## Field Types (Common)

| Field | Use | Example |
|-------|-----|---------|
| `CharField` | Short text | `CharField(max_length=120)` |
| `TextField` | Long text | `TextField()` |
| `IntegerField` | Integers | `IntegerField()` |
| `DecimalField` | Money/precise numbers | `DecimalField(max_digits=10, decimal_places=2)` |
| `BooleanField` | Flags | `BooleanField(default=True)` |
| `DateField` | Date only | `DateField()` |
| `DateTimeField` | Date + time | `DateTimeField(auto_now_add=True)` |
| `EmailField` | Email validation | `EmailField(unique=True)` |
| `UUIDField` | UUID primary keys | `UUIDField(default=uuid.uuid4)` |
| `JSONField` | Semi-structured data | `JSONField(default=dict)` |

## Null vs Blank vs Default

- `blank=True` affects form/serializer validation.
- `null=True` stores `NULL` in the database.
- Prefer `blank=True` without `null=True` for text fields to avoid `NULL` vs empty string confusion.
- Use `default=` for deterministic values, `default_factory` patterns via callable for dynamic values.

## Relationships

### ForeignKey (Many-to-One)

```python
class Appointment(models.Model):
    doctor = models.ForeignKey(Doctor, on_delete=models.CASCADE, related_name="appointments")
    patient_name = models.CharField(max_length=200)
    appointment_date = models.DateField()
```

### OneToOneField

```python
class Profile(models.Model):
    user = models.OneToOneField("auth.User", on_delete=models.CASCADE)
    bio = models.TextField(blank=True)
```

### ManyToManyField

```python
class Department(models.Model):
    name = models.CharField(max_length=100)


class Doctor(models.Model):
    departments = models.ManyToManyField(Department, related_name="doctors", blank=True)
```

### ManyToMany with Through Model

```python
class Membership(models.Model):
    doctor = models.ForeignKey(Doctor, on_delete=models.CASCADE)
    department = models.ForeignKey(Department, on_delete=models.CASCADE)
    role = models.CharField(max_length=50)


class Doctor(models.Model):
    departments = models.ManyToManyField(Department, through=Membership)
```

### on_delete Options

| Option | Behavior |
|--------|----------|
| `CASCADE` | Delete related rows |
| `PROTECT` | Prevent deletion |
| `SET_NULL` | Set FK to NULL (requires `null=True`) |
| `SET_DEFAULT` | Set FK to default |
| `DO_NOTHING` | Leave references as-is |

## Constraints and Indexes

Use modern constraints for clarity and database integrity.

```python
from django.db import models
from django.db.models import Q


class Appointment(models.Model):
    doctor = models.ForeignKey(Doctor, on_delete=models.CASCADE)
    patient_name = models.CharField(max_length=200)
    appointment_date = models.DateField()
    status = models.CharField(max_length=20, default="scheduled")

    class Meta:
        constraints = [
            models.UniqueConstraint(
                fields=["doctor", "appointment_date"],
                name="uniq_doctor_date",
            ),
            models.CheckConstraint(
                condition=Q(status__in=["scheduled", "completed", "canceled"]),
                name="valid_status",
            ),
        ]
        indexes = [
            models.Index(fields=["doctor", "appointment_date"], name="doctor_date_idx"),
            models.Index(
                fields=["status"],
                condition=Q(status="scheduled"),
                name="scheduled_only_idx",
            ),
        ]
```

## Model Managers and QuerySets

Custom QuerySets help keep business logic reusable and testable.

```python
from django.db import models


class DoctorQuerySet(models.QuerySet):
    def active(self):
        return self.filter(is_active=True)

    def search(self, term: str):
        return self.filter(last_name__icontains=term)


class Doctor(models.Model):
    last_name = models.CharField(max_length=100)
    is_active = models.BooleanField(default=True)

    objects = DoctorQuerySet.as_manager()
```

## Query Performance Patterns

```python
# Avoid N+1 with select_related / prefetch_related
appointments = (
    Appointment.objects.select_related("doctor")
    .prefetch_related("doctor__departments")
)

# Limit columns
doctors = Doctor.objects.only("id", "first_name", "last_name")

# Use Q objects for complex filters
from django.db.models import Q
Doctor.objects.filter(Q(is_active=True) | Q(specialty="cardiology"))
```

## Transactions

```python
from django.db import transaction


@transaction.atomic
def book_appointment(doctor, patient_name, date):
    Appointment.objects.create(doctor=doctor, patient_name=patient_name, appointment_date=date)
```

## Async ORM (What Actually Works)

Django supports async views and provides async variants for ORM methods (prefixed with `a`). You can also `async for` on QuerySets. Transactions are not supported in async mode; if you need transactions, run that block in a sync function and call it using `sync_to_async`.

```python
from asgiref.sync import sync_to_async


async def list_doctors():
    return [d async for d in Doctor.objects.filter(is_active=True)]


async def create_doctor(data):
    return await Doctor.objects.acreate(**data)


async def create_with_txn(data):
    return await sync_to_async(create_doctor_sync)(data)
```

## Migrations

```bash
uv run python manage.py makemigrations
uv run python manage.py migrate
uv run python manage.py showmigrations
uv run python manage.py sqlmigrate core 0001
```

## Best Practices

- Use `UUIDField` for public-facing IDs.
- Prefer `UniqueConstraint` and `CheckConstraint` over legacy `unique_together`.
- Always index high-cardinality filters you use in production.
- Use `select_related` and `prefetch_related` early to avoid N+1 queries.
- Keep models thin and move complex logic to services.

## Next Steps

- [Serializers](./03-serializers.md) - Validation and transformation
- [Views and ViewSets](./04-views-viewsets.md) - API endpoints

---

[Previous: Introduction](./01-introduction.md) | [Back to Index](./README.md) | [Next: Serializers](./03-serializers.md)
