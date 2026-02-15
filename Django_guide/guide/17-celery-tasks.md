# Celery and Background Tasks

This chapter shows how to run background jobs with Celery.

## Step 1: Install

```bash
uv add celery redis
```

## Step 2: Create Celery App

```python
# config/celery.py
import os
from celery import Celery

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "config.settings")

app = Celery("config")
app.config_from_object("django.conf:settings", namespace="CELERY")
app.autodiscover_tasks()
```

```python
# config/__init__.py
from .celery import app as celery_app

__all__ = ("celery_app",)
```

## Step 3: Configure Settings

```python
# config/settings.py
CELERY_BROKER_URL = "redis://localhost:6379/0"
CELERY_RESULT_BACKEND = "redis://localhost:6379/0"
CELERY_ACCEPT_CONTENT = ["json"]
CELERY_TASK_SERIALIZER = "json"
CELERY_RESULT_SERIALIZER = "json"
CELERY_TIMEZONE = "UTC"

CELERY_TASK_ACKS_LATE = True
CELERY_WORKER_PREFETCH_MULTIPLIER = 1
```

## Step 4: Write Tasks

```python
# doctors/tasks.py
from celery import shared_task
from django.core.mail import send_mail
from .models import Doctor


@shared_task(bind=True, max_retries=3, default_retry_delay=60)
def send_welcome_email(self, doctor_id: str):
    doctor = Doctor.objects.get(pk=doctor_id)
    send_mail(
        subject="Welcome",
        message=f"Welcome Dr. {doctor.first_name}",
        from_email="noreply@example.com",
        recipient_list=[doctor.email],
    )
```

## Step 5: Call Tasks

```python
send_welcome_email.delay(str(doctor.id))
```

## Step 6: Periodic Tasks (Celery Beat)

```python
# config/settings.py
from celery.schedules import crontab

CELERY_BEAT_SCHEDULE = {
    "daily-reminders": {
        "task": "doctors.tasks.send_daily_reminders",
        "schedule": crontab(hour=8, minute=0),
    }
}
```

## Step 7: Run Workers

```bash
celery -A config worker -l info
celery -A config beat -l info
```

## Tips

- Keep tasks small and idempotent.
- Use retries for transient failures.
- Set time limits for long tasks.

## References

- [Celery Configuration](https://docs.celeryq.dev/en/stable/userguide/configuration.html)

## Next Steps

- [Middleware](./18-middleware.md)
- [Admin Customization](./19-admin-customization.md)

---

[Previous: File Uploads](./16-file-uploads.md) | [Back to Index](./README.md) | [Next: Middleware](./18-middleware.md)
