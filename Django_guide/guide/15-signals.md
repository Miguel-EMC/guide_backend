# Signals

Signals let you react to model or request events without tightly coupling code. Use them carefully and keep handlers lightweight.

## Step 1: Create a signals module

```python
# doctors/signals.py
from django.db.models.signals import post_save, post_delete
from django.dispatch import receiver
from .models import Doctor


@receiver(post_save, sender=Doctor)
def doctor_saved(sender, instance, created, **kwargs):
    if created:
        # Side effects should be minimal
        print(f"Created {instance}")


@receiver(post_delete, sender=Doctor)
def doctor_deleted(sender, instance, **kwargs):
    print(f"Deleted {instance}")
```

## Step 2: Register signals in AppConfig

```python
# doctors/apps.py
from django.apps import AppConfig


class DoctorsConfig(AppConfig):
    default_auto_field = "django.db.models.BigAutoField"
    name = "doctors"

    def ready(self):
        import doctors.signals  # noqa
```

## Step 3: Invalidate Cache on Save/Delete

```python
from django.core.cache import cache


@receiver([post_save, post_delete], sender=Doctor, dispatch_uid="doctor_cache_invalidate")
def invalidate_cache(sender, instance, **kwargs):
    cache.delete(f"doctor:{instance.pk}")
    cache.delete("doctor:list")
```

## Step 4: Use transaction.on_commit

If you trigger external effects (emails, tasks), do it after commit.

```python
from django.db import transaction
from django.dispatch import receiver
from django.db.models.signals import post_save


@receiver(post_save, sender=Doctor)
def notify_created(sender, instance, created, **kwargs):
    if created:
        transaction.on_commit(lambda: send_welcome_email(instance.pk))
```

## Step 5: m2m_changed

```python
from django.db.models.signals import m2m_changed


@receiver(m2m_changed, sender=Doctor.departments.through)
def departments_changed(sender, instance, action, pk_set, **kwargs):
    if action == "post_add":
        print(f"Added departments {pk_set} to {instance}")
```

## Caution

Signals can make code harder to trace. Use them for cross-cutting concerns like cache invalidation, analytics, or side effects that should not live in your core business logic.

## References

- [Django Signals](https://docs.djangoproject.com/en/5.1/topics/signals/)

## Next Steps

- [File Uploads](./16-file-uploads.md)
- [Celery and Tasks](./17-celery-tasks.md)

---

[Previous: Caching](./14-caching.md) | [Back to Index](./README.md) | [Next: File Uploads](./16-file-uploads.md)
