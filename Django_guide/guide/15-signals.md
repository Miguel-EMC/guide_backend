# Signals

Django signals enable decoupled communication between components. They allow certain senders to notify receivers when specific actions occur, without tight coupling between the code. This chapter covers built-in signals, custom signals, and best practices for DRF APIs.

## Overview

| Signal Type | When Fired |
|------------|-----------|
| pre_save | Before model.save() |
| post_save | After model.save() |
| pre_delete | Before model.delete() |
| post_delete | After model.delete() |
| m2m_changed | ManyToMany relationship changed |
| request_started | HTTP request begins |
| request_finished | HTTP request ends |

## Signal Architecture

```
Event (model save) → Signal → Registered Receivers → Side Effects
                              ↓
                           Receiver 1 → Send email
                           Receiver 2 → Update cache
                           Receiver 3 → Create audit log
```

## Basic Setup

### Create Signals Module

```python
# doctors/signals.py
from django.db.models.signals import post_save, post_delete, pre_save
from django.dispatch import receiver
from .models import Doctor


@receiver(post_save, sender=Doctor)
def doctor_post_save(sender, instance, created, **kwargs):
    """Handle doctor creation and updates."""
    if created:
        print(f"New doctor created: {instance.email}")
    else:
        print(f"Doctor updated: {instance.email}")


@receiver(post_delete, sender=Doctor)
def doctor_post_delete(sender, instance, **kwargs):
    """Handle doctor deletion."""
    print(f"Doctor deleted: {instance.email}")
```

### Register in AppConfig

```python
# doctors/apps.py
from django.apps import AppConfig


class DoctorsConfig(AppConfig):
    default_auto_field = "django.db.models.BigAutoField"
    name = "doctors"

    def ready(self):
        # Import signals to register them
        import doctors.signals  # noqa: F401
```

### Verify App Config

```python
# doctors/__init__.py
default_app_config = "doctors.apps.DoctorsConfig"
```

## Model Signals

### pre_save - Validate Before Save

```python
# doctors/signals.py
from django.db.models.signals import pre_save
from django.dispatch import receiver
from django.core.exceptions import ValidationError
from .models import Doctor


@receiver(pre_save, sender=Doctor)
def validate_doctor_license(sender, instance, **kwargs):
    """Validate license number format before save."""
    if instance.license_number:
        if not instance.license_number.startswith("MD"):
            raise ValidationError("License number must start with 'MD'")


@receiver(pre_save, sender=Doctor)
def normalize_email(sender, instance, **kwargs):
    """Normalize email to lowercase before save."""
    if instance.email:
        instance.email = instance.email.lower().strip()


@receiver(pre_save, sender=Doctor)
def set_slug(sender, instance, **kwargs):
    """Auto-generate slug if not set."""
    if not instance.slug:
        from django.utils.text import slugify
        base_slug = slugify(f"{instance.first_name}-{instance.last_name}")
        instance.slug = f"{base_slug}-{instance.pk or 'new'}"
```

### post_save - React to Save

```python
# doctors/signals.py
from django.db.models.signals import post_save
from django.dispatch import receiver
from django.core.mail import send_mail
from django.conf import settings
from .models import Doctor, DoctorProfile


@receiver(post_save, sender=Doctor)
def create_doctor_profile(sender, instance, created, **kwargs):
    """Create profile when doctor is created."""
    if created:
        DoctorProfile.objects.create(doctor=instance)


@receiver(post_save, sender=Doctor)
def send_welcome_email(sender, instance, created, **kwargs):
    """Send welcome email to new doctors."""
    if created:
        send_mail(
            subject="Welcome to our platform",
            message=f"Hello Dr. {instance.last_name}, welcome!",
            from_email=settings.DEFAULT_FROM_EMAIL,
            recipient_list=[instance.email],
            fail_silently=True,
        )


@receiver(post_save, sender=Doctor)
def log_doctor_changes(sender, instance, created, **kwargs):
    """Log changes to doctor records."""
    from core.models import AuditLog

    action = "created" if created else "updated"
    AuditLog.objects.create(
        model_name="Doctor",
        object_id=instance.pk,
        action=action,
        data={"email": instance.email, "is_active": instance.is_active},
    )
```

### post_delete - Cleanup

```python
# doctors/signals.py
from django.db.models.signals import post_delete
from django.dispatch import receiver
import os
from .models import Doctor


@receiver(post_delete, sender=Doctor)
def delete_doctor_photo(sender, instance, **kwargs):
    """Delete photo file when doctor is deleted."""
    if instance.photo:
        if os.path.isfile(instance.photo.path):
            os.remove(instance.photo.path)


@receiver(post_delete, sender=Doctor)
def cleanup_doctor_data(sender, instance, **kwargs):
    """Clean up related data after deletion."""
    # Remove from search index
    from core.search import remove_from_index
    remove_from_index("doctor", instance.pk)

    # Clear cached data
    from django.core.cache import cache
    cache.delete(f"doctor:{instance.pk}")
    cache.delete("doctor:list")
```

## ManyToMany Signals

### m2m_changed

```python
# doctors/signals.py
from django.db.models.signals import m2m_changed
from django.dispatch import receiver
from .models import Doctor


@receiver(m2m_changed, sender=Doctor.specialties.through)
def specialties_changed(sender, instance, action, pk_set, **kwargs):
    """Handle specialty assignments."""
    if action == "post_add":
        print(f"Added specialties {pk_set} to {instance}")
        # Recalculate search tags
        instance.update_search_tags()

    elif action == "post_remove":
        print(f"Removed specialties {pk_set} from {instance}")
        instance.update_search_tags()

    elif action == "post_clear":
        print(f"Cleared all specialties from {instance}")
        instance.search_tags = []
        instance.save(update_fields=["search_tags"])


@receiver(m2m_changed, sender=Doctor.departments.through)
def departments_changed(sender, instance, action, model, pk_set, **kwargs):
    """Handle department assignments with detailed info."""
    if action == "pre_add":
        # Validate before adding
        from .models import Department
        departments = Department.objects.filter(pk__in=pk_set)
        for dept in departments:
            if not dept.is_active:
                raise ValueError(f"Cannot assign inactive department: {dept.name}")

    elif action == "post_add":
        # Notify department heads
        from .models import Department
        for dept_id in pk_set:
            dept = Department.objects.get(pk=dept_id)
            notify_department_head(dept, instance, "added")
```

## Transaction-Safe Signals

### Use transaction.on_commit

For side effects that should only happen if the transaction succeeds:

```python
# doctors/signals.py
from django.db import transaction
from django.db.models.signals import post_save
from django.dispatch import receiver
from .models import Doctor


@receiver(post_save, sender=Doctor)
def send_notification_on_create(sender, instance, created, **kwargs):
    """Send notification only after transaction commits."""
    if created:
        def send_notification():
            from .tasks import send_welcome_notification
            send_welcome_notification.delay(instance.pk)

        transaction.on_commit(send_notification)


@receiver(post_save, sender=Doctor)
def update_search_index(sender, instance, **kwargs):
    """Update search index after commit."""
    def do_update():
        from core.search import update_index
        update_index("doctor", instance.pk, instance.to_search_document())

    transaction.on_commit(do_update)
```

### Why Use on_commit?

```python
# BAD: Task may run before transaction commits
@receiver(post_save, sender=Doctor)
def bad_signal(sender, instance, created, **kwargs):
    if created:
        # This task might query a doctor that doesn't exist yet
        # if the transaction hasn't committed
        process_new_doctor.delay(instance.pk)


# GOOD: Task runs only after successful commit
@receiver(post_save, sender=Doctor)
def good_signal(sender, instance, created, **kwargs):
    if created:
        def enqueue_task():
            process_new_doctor.delay(instance.pk)
        transaction.on_commit(enqueue_task)
```

## Cache Invalidation

### Automatic Cache Invalidation

```python
# doctors/signals.py
from django.db.models.signals import post_save, post_delete
from django.dispatch import receiver
from django.core.cache import cache
from .models import Doctor, Department


@receiver([post_save, post_delete], sender=Doctor, dispatch_uid="doctor_cache_invalidate")
def invalidate_doctor_cache(sender, instance, **kwargs):
    """Invalidate all doctor-related caches."""
    # Individual doctor cache
    cache.delete(f"doctor:{instance.pk}")
    cache.delete(f"doctor:slug:{instance.slug}")

    # List caches
    cache.delete("doctor:list")
    cache.delete("doctor:active:list")

    # Related caches
    if instance.department_id:
        cache.delete(f"department:{instance.department_id}:doctors")

    # Invalidate paginated caches (use pattern if Redis)
    try:
        from django_redis import get_redis_connection
        redis = get_redis_connection("default")
        keys = redis.keys("doctor:list:page:*")
        if keys:
            redis.delete(*keys)
    except ImportError:
        pass


@receiver([post_save, post_delete], sender=Department)
def invalidate_department_cache(sender, instance, **kwargs):
    """Invalidate department caches."""
    cache.delete(f"department:{instance.pk}")
    cache.delete("department:list")

    # Invalidate all doctors in this department
    doctor_ids = list(
        Doctor.objects.filter(department=instance).values_list("pk", flat=True)
    )
    for doctor_id in doctor_ids:
        cache.delete(f"doctor:{doctor_id}")
```

### Batch Cache Operations

```python
# core/cache.py
from django.core.cache import cache


class CacheInvalidator:
    """Collect cache keys to invalidate in batch."""

    def __init__(self):
        self.keys_to_delete = set()

    def add(self, key):
        self.keys_to_delete.add(key)

    def add_pattern(self, pattern):
        """Add keys matching pattern (Redis only)."""
        try:
            from django_redis import get_redis_connection
            redis = get_redis_connection("default")
            keys = redis.keys(pattern)
            self.keys_to_delete.update(k.decode() for k in keys)
        except ImportError:
            pass

    def execute(self):
        if self.keys_to_delete:
            cache.delete_many(list(self.keys_to_delete))
            self.keys_to_delete.clear()
```

```python
# doctors/signals.py
from core.cache import CacheInvalidator
from django.db.models.signals import post_save
from django.dispatch import receiver
from .models import Doctor


@receiver(post_save, sender=Doctor)
def batch_cache_invalidation(sender, instance, **kwargs):
    """Batch invalidate multiple cache keys."""
    invalidator = CacheInvalidator()

    invalidator.add(f"doctor:{instance.pk}")
    invalidator.add("doctor:list")
    invalidator.add_pattern(f"doctor:list:*")

    if instance.department_id:
        invalidator.add(f"department:{instance.department_id}:doctors")

    invalidator.execute()
```

## Custom Signals

### Define Custom Signal

```python
# doctors/signals.py
from django.dispatch import Signal

# Define custom signals
doctor_activated = Signal()  # Provides: doctor, activated_by
doctor_deactivated = Signal()  # Provides: doctor, deactivated_by
appointment_booked = Signal()  # Provides: appointment, patient, doctor
appointment_cancelled = Signal()  # Provides: appointment, cancelled_by, reason
```

### Send Custom Signal

```python
# doctors/models.py
from django.db import models
from .signals import doctor_activated, doctor_deactivated


class Doctor(models.Model):
    # ... fields ...

    def activate(self, activated_by=None):
        """Activate doctor and send signal."""
        self.is_active = True
        self.save(update_fields=["is_active"])

        doctor_activated.send(
            sender=self.__class__,
            doctor=self,
            activated_by=activated_by,
        )

    def deactivate(self, deactivated_by=None):
        """Deactivate doctor and send signal."""
        self.is_active = False
        self.save(update_fields=["is_active"])

        doctor_deactivated.send(
            sender=self.__class__,
            doctor=self,
            deactivated_by=deactivated_by,
        )
```

### Receive Custom Signal

```python
# doctors/signals.py
from django.dispatch import receiver
from .signals import doctor_activated, doctor_deactivated


@receiver(doctor_activated)
def handle_doctor_activated(sender, doctor, activated_by, **kwargs):
    """Handle doctor activation."""
    from .tasks import send_activation_email, sync_to_directory

    # Send email notification
    send_activation_email.delay(doctor.pk)

    # Sync to external directory
    sync_to_directory.delay(doctor.pk)

    # Log the action
    from core.models import AuditLog
    AuditLog.objects.create(
        model_name="Doctor",
        object_id=doctor.pk,
        action="activated",
        performed_by=activated_by,
    )


@receiver(doctor_deactivated)
def handle_doctor_deactivated(sender, doctor, deactivated_by, **kwargs):
    """Handle doctor deactivation."""
    # Cancel pending appointments
    from appointments.models import Appointment
    pending = Appointment.objects.filter(
        doctor=doctor,
        status="pending",
    )
    for appointment in pending:
        appointment.cancel(reason="Doctor deactivated")

    # Remove from search index
    from core.search import remove_from_index
    remove_from_index("doctor", doctor.pk)
```

## Request Signals

### Log All Requests

```python
# core/signals.py
from django.core.signals import request_started, request_finished
from django.dispatch import receiver
import time
import threading

# Thread-local storage for request timing
_request_data = threading.local()


@receiver(request_started)
def log_request_start(sender, environ, **kwargs):
    """Log when request starts."""
    _request_data.start_time = time.time()
    _request_data.path = environ.get("PATH_INFO", "")
    _request_data.method = environ.get("REQUEST_METHOD", "")


@receiver(request_finished)
def log_request_end(sender, **kwargs):
    """Log when request ends."""
    if hasattr(_request_data, "start_time"):
        duration = time.time() - _request_data.start_time

        import logging
        logger = logging.getLogger("requests")
        logger.info(
            "Request completed",
            extra={
                "path": getattr(_request_data, "path", ""),
                "method": getattr(_request_data, "method", ""),
                "duration_ms": duration * 1000,
            },
        )
```

## Signal with dispatch_uid

Prevent duplicate signal handlers:

```python
# doctors/signals.py
from django.db.models.signals import post_save
from django.dispatch import receiver
from .models import Doctor


# Use dispatch_uid to ensure signal is only connected once
@receiver(post_save, sender=Doctor, dispatch_uid="unique_doctor_post_save")
def doctor_post_save(sender, instance, created, **kwargs):
    """This handler will only be registered once."""
    pass


# Alternative: Manual connection with dispatch_uid
def connect_signals():
    post_save.connect(
        doctor_post_save,
        sender=Doctor,
        dispatch_uid="unique_doctor_post_save",
    )
```

## Disconnecting Signals

### Temporarily Disable Signals

```python
# core/utils.py
from contextlib import contextmanager
from django.db.models.signals import post_save, post_delete


@contextmanager
def disable_signals(*signals_and_receivers):
    """
    Context manager to temporarily disable signals.

    Usage:
        with disable_signals((post_save, doctor_post_save, Doctor)):
            doctor.save()  # Signal won't fire
    """
    disconnected = []

    for signal, receiver, sender in signals_and_receivers:
        if signal.disconnect(receiver, sender=sender):
            disconnected.append((signal, receiver, sender))

    try:
        yield
    finally:
        for signal, receiver, sender in disconnected:
            signal.connect(receiver, sender=sender)
```

```python
# Usage
from core.utils import disable_signals
from doctors.signals import doctor_post_save
from doctors.models import Doctor


with disable_signals((post_save, doctor_post_save, Doctor)):
    # This save won't trigger the signal
    Doctor.objects.create(
        first_name="Test",
        last_name="Doctor",
        email="test@example.com",
    )
```

### Bulk Operations Without Signals

```python
# doctors/services.py
from django.db import transaction
from .models import Doctor


class DoctorService:
    @staticmethod
    def bulk_activate(doctor_ids):
        """Bulk activate without individual signals."""
        with transaction.atomic():
            # update() doesn't trigger signals
            count = Doctor.objects.filter(pk__in=doctor_ids).update(is_active=True)

            # Manually trigger side effects once
            from django.core.cache import cache
            cache.delete("doctor:list")

            # Send batch notification
            from .tasks import send_bulk_activation_notification
            send_bulk_activation_notification.delay(doctor_ids)

            return count
```

## Testing Signals

### Test Signal is Called

```python
# tests/test_signals.py
import pytest
from unittest.mock import patch, MagicMock
from doctors.models import Doctor
from doctors.signals import doctor_activated


@pytest.mark.django_db
class TestDoctorSignals:
    def test_post_save_creates_profile(self):
        """Test that profile is created on doctor creation."""
        doctor = Doctor.objects.create(
            first_name="John",
            last_name="Doe",
            email="john@example.com",
        )

        assert hasattr(doctor, "profile")
        assert doctor.profile is not None

    @patch("doctors.signals.send_welcome_email")
    def test_welcome_email_sent_on_create(self, mock_send):
        """Test that welcome email is sent."""
        Doctor.objects.create(
            first_name="John",
            last_name="Doe",
            email="john@example.com",
        )

        mock_send.assert_called_once()

    def test_cache_invalidated_on_save(self):
        """Test that cache is invalidated on save."""
        from django.core.cache import cache

        doctor = Doctor.objects.create(
            first_name="John",
            last_name="Doe",
            email="john@example.com",
        )

        # Set cache
        cache.set(f"doctor:{doctor.pk}", "cached_value")

        # Update doctor
        doctor.first_name = "Jane"
        doctor.save()

        # Cache should be cleared
        assert cache.get(f"doctor:{doctor.pk}") is None


@pytest.mark.django_db
class TestCustomSignals:
    def test_doctor_activated_signal(self):
        """Test custom activation signal."""
        handler = MagicMock()
        doctor_activated.connect(handler)

        try:
            doctor = Doctor.objects.create(
                first_name="John",
                last_name="Doe",
                email="john@example.com",
                is_active=False,
            )
            doctor.activate(activated_by=None)

            handler.assert_called_once()
            call_kwargs = handler.call_args[1]
            assert call_kwargs["doctor"] == doctor
        finally:
            doctor_activated.disconnect(handler)
```

### Test Signal Not Called

```python
# tests/test_signals.py
@pytest.mark.django_db
class TestSignalNotCalled:
    def test_bulk_update_no_signal(self):
        """Test that bulk update doesn't trigger signals."""
        handler = MagicMock()
        post_save.connect(handler, sender=Doctor)

        try:
            Doctor.objects.create(
                first_name="John",
                last_name="Doe",
                email="john@example.com",
            )

            # Reset mock after create
            handler.reset_mock()

            # Bulk update shouldn't trigger signal
            Doctor.objects.all().update(is_active=True)

            handler.assert_not_called()
        finally:
            post_save.disconnect(handler, sender=Doctor)
```

## Best Practices

1. **Keep handlers lightweight** - Don't do heavy computation in signals
2. **Use transaction.on_commit** - For external side effects (emails, tasks)
3. **Use dispatch_uid** - Prevent duplicate handlers
4. **Don't use signals for business logic** - Use services instead
5. **Test signal behavior** - Verify side effects work correctly
6. **Document signal handlers** - Explain what each handler does
7. **Consider async tasks** - Offload heavy work to Celery
8. **Handle exceptions** - Don't let one handler break others
9. **Avoid circular imports** - Import inside handler if needed
10. **Be careful with bulk operations** - `update()` and `delete()` don't trigger signals

## When to Use Signals vs Alternatives

| Use Case | Signals | Service/Manager | Model Method |
|----------|---------|-----------------|--------------|
| Cache invalidation | Yes | Maybe | No |
| Audit logging | Yes | Yes | No |
| Send notifications | Yes | Yes | No |
| Business logic | No | Yes | Yes |
| Data validation | No | No | Yes |
| Cross-app communication | Yes | No | No |

## References

- [Django Signals](https://docs.djangoproject.com/en/5.2/topics/signals/)
- [Django Signals Reference](https://docs.djangoproject.com/en/5.2/ref/signals/)
- [transaction.on_commit](https://docs.djangoproject.com/en/5.2/topics/db/transactions/#performing-actions-after-commit)

## Next Steps

- [File Uploads](./16-file-uploads.md)
- [Celery and Tasks](./17-celery-tasks.md)

---

[Previous: Caching](./14-caching.md) | [Back to Index](./README.md) | [Next: File Uploads](./16-file-uploads.md)
