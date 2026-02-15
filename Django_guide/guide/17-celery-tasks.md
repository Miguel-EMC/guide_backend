# Celery and Background Tasks

This chapter covers production-ready background job processing with Celery, including task patterns, error handling, monitoring, and deployment.

## Why Background Tasks?

| Use Case | Example |
|----------|---------|
| Email/Notifications | Welcome emails, password resets |
| Data Processing | Report generation, CSV exports |
| External APIs | Payment processing, webhooks |
| Scheduled Jobs | Daily reports, cleanup tasks |
| Long Operations | Image processing, PDF generation |

## Installation

```bash
uv add celery redis
```

## Celery Configuration

### Create Celery App

```python
# config/celery.py
import os
from celery import Celery

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "config.settings")

app = Celery("config")

# Load config from Django settings with CELERY_ prefix
app.config_from_object("django.conf:settings", namespace="CELERY")

# Auto-discover tasks in all registered apps
app.autodiscover_tasks()


@app.task(bind=True, ignore_result=True)
def debug_task(self):
    print(f"Request: {self.request!r}")
```

```python
# config/__init__.py
from .celery import app as celery_app

__all__ = ("celery_app",)
```

### Settings

```python
# config/settings.py
import os

# Broker (message queue)
CELERY_BROKER_URL = os.environ.get("REDIS_URL", "redis://localhost:6379/0")

# Result backend
CELERY_RESULT_BACKEND = os.environ.get("REDIS_URL", "redis://localhost:6379/0")

# Serialization
CELERY_ACCEPT_CONTENT = ["json"]
CELERY_TASK_SERIALIZER = "json"
CELERY_RESULT_SERIALIZER = "json"

# Timezone
CELERY_TIMEZONE = "UTC"
CELERY_ENABLE_UTC = True

# Task execution settings
CELERY_TASK_ACKS_LATE = True  # Acknowledge after task completes
CELERY_WORKER_PREFETCH_MULTIPLIER = 1  # Fair task distribution
CELERY_TASK_REJECT_ON_WORKER_LOST = True  # Requeue if worker dies

# Task time limits
CELERY_TASK_SOFT_TIME_LIMIT = 300  # 5 minutes soft limit
CELERY_TASK_TIME_LIMIT = 600  # 10 minutes hard limit

# Result expiration
CELERY_RESULT_EXPIRES = 3600  # 1 hour

# Task routes (optional)
CELERY_TASK_ROUTES = {
    "doctors.tasks.send_email_*": {"queue": "emails"},
    "reports.tasks.*": {"queue": "reports"},
}
```

## Writing Tasks

### Basic Task

```python
# doctors/tasks.py
from celery import shared_task
from django.core.mail import send_mail
from .models import Doctor


@shared_task
def send_welcome_email(doctor_id: int):
    """Send welcome email to new doctor."""
    doctor = Doctor.objects.get(pk=doctor_id)
    send_mail(
        subject="Welcome to our platform",
        message=f"Welcome Dr. {doctor.first_name}!",
        from_email="noreply@example.com",
        recipient_list=[doctor.email],
    )
```

### Task with Retry Logic

```python
# doctors/tasks.py
from celery import shared_task
from celery.exceptions import MaxRetriesExceededError
import requests


@shared_task(
    bind=True,
    max_retries=3,
    default_retry_delay=60,  # 1 minute between retries
    autoretry_for=(requests.RequestException,),
    retry_backoff=True,  # Exponential backoff
    retry_backoff_max=600,  # Max 10 minutes
    retry_jitter=True,  # Add randomness to prevent thundering herd
)
def sync_doctor_to_external_system(self, doctor_id: int):
    """Sync doctor data to external CRM."""
    try:
        doctor = Doctor.objects.get(pk=doctor_id)
        response = requests.post(
            "https://crm.example.com/api/doctors",
            json={"name": doctor.name, "email": doctor.email},
            timeout=30,
        )
        response.raise_for_status()
        return {"status": "synced", "external_id": response.json()["id"]}
    except requests.RequestException as exc:
        raise self.retry(exc=exc)
    except Doctor.DoesNotExist:
        return {"status": "skipped", "reason": "doctor not found"}
```

### Task with Custom Exception Handling

```python
# doctors/tasks.py
from celery import shared_task
from celery.exceptions import Reject, Ignore
import logging

logger = logging.getLogger(__name__)


@shared_task(bind=True, max_retries=3)
def process_payment(self, appointment_id: int):
    """Process payment for appointment."""
    from appointments.models import Appointment

    try:
        appointment = Appointment.objects.get(pk=appointment_id)

        if appointment.is_paid:
            logger.info(f"Appointment {appointment_id} already paid, skipping")
            raise Ignore()  # Don't retry, don't save result

        result = payment_gateway.charge(
            amount=appointment.amount,
            customer_id=appointment.patient.stripe_id,
        )

        appointment.is_paid = True
        appointment.payment_id = result["id"]
        appointment.save()

        return {"status": "success", "payment_id": result["id"]}

    except PaymentDeclinedError:
        # Don't retry - payment was declined
        raise Reject(reason="Payment declined", requeue=False)

    except PaymentGatewayError as exc:
        # Retry - temporary gateway issue
        logger.warning(f"Payment gateway error: {exc}")
        raise self.retry(exc=exc, countdown=60)
```

### Chaining Tasks

```python
# doctors/tasks.py
from celery import shared_task, chain, group, chord


@shared_task
def validate_doctor(doctor_id: int) -> dict:
    doctor = Doctor.objects.get(pk=doctor_id)
    return {"doctor_id": doctor_id, "valid": True}


@shared_task
def create_account(data: dict) -> dict:
    # Create account in external system
    return {**data, "account_created": True}


@shared_task
def send_welcome_email(data: dict) -> dict:
    # Send welcome email
    return {**data, "email_sent": True}


@shared_task
def notify_admin(data: dict) -> dict:
    # Notify admin of new doctor
    return {**data, "admin_notified": True}


# Chain: execute sequentially
def onboard_doctor(doctor_id: int):
    workflow = chain(
        validate_doctor.s(doctor_id),
        create_account.s(),
        send_welcome_email.s(),
    )
    workflow.apply_async()


# Group: execute in parallel
def notify_all(doctor_id: int):
    workflow = group(
        send_welcome_email.s(doctor_id),
        notify_admin.s(doctor_id),
    )
    workflow.apply_async()


# Chord: parallel tasks with callback
def process_batch(doctor_ids: list[int]):
    workflow = chord(
        (validate_doctor.s(id) for id in doctor_ids),
        notify_admin.s()  # Called after all validations complete
    )
    workflow.apply_async()
```

## Calling Tasks

```python
# Async execution (recommended)
send_welcome_email.delay(doctor.id)

# Async with options
send_welcome_email.apply_async(
    args=[doctor.id],
    countdown=60,  # Delay 60 seconds
    expires=3600,  # Expire after 1 hour
    priority=5,  # Higher priority (0-9)
)

# Scheduled execution
from datetime import datetime, timedelta

send_welcome_email.apply_async(
    args=[doctor.id],
    eta=datetime.now() + timedelta(hours=1),
)

# Get result (blocking - use sparingly)
result = send_welcome_email.delay(doctor.id)
value = result.get(timeout=10)  # Wait up to 10 seconds
```

## Periodic Tasks (Celery Beat)

### Configuration

```python
# config/settings.py
from celery.schedules import crontab

CELERY_BEAT_SCHEDULE = {
    # Every day at 8 AM
    "daily-appointment-reminders": {
        "task": "appointments.tasks.send_daily_reminders",
        "schedule": crontab(hour=8, minute=0),
    },

    # Every hour
    "hourly-sync": {
        "task": "doctors.tasks.sync_all_doctors",
        "schedule": crontab(minute=0),
    },

    # Every Monday at 9 AM
    "weekly-report": {
        "task": "reports.tasks.generate_weekly_report",
        "schedule": crontab(hour=9, minute=0, day_of_week=1),
    },

    # Every 5 minutes
    "health-check": {
        "task": "core.tasks.health_check",
        "schedule": 300.0,  # 300 seconds
    },
}
```

### Database-backed Scheduling

```bash
uv add django-celery-beat
```

```python
# config/settings.py
INSTALLED_APPS = [
    # ...
    "django_celery_beat",
]
```

```bash
uv run python manage.py migrate
```

Now you can manage schedules via Django Admin.

## Task Monitoring with Flower

```bash
uv add flower
```

```bash
# Run Flower
celery -A config flower --port=5555

# With basic auth
celery -A config flower --basic_auth=admin:password
```

Access at `http://localhost:5555`

## Error Tracking

### Sentry Integration

```python
# config/celery.py
import sentry_sdk
from sentry_sdk.integrations.celery import CeleryIntegration

sentry_sdk.init(
    dsn=os.environ.get("SENTRY_DSN"),
    integrations=[CeleryIntegration()],
    traces_sample_rate=0.1,
)
```

### Custom Error Handler

```python
# config/celery.py
from celery.signals import task_failure
import logging

logger = logging.getLogger(__name__)


@task_failure.connect
def handle_task_failure(sender, task_id, exception, args, kwargs, traceback, einfo, **kw):
    logger.error(
        f"Task {sender.name}[{task_id}] failed",
        extra={
            "task_name": sender.name,
            "task_id": task_id,
            "args": args,
            "kwargs": kwargs,
            "exception": str(exception),
        },
        exc_info=True,
    )
```

## Task Patterns

### Idempotent Tasks

```python
# Always make tasks idempotent (safe to run multiple times)
@shared_task
def process_order(order_id: int):
    order = Order.objects.select_for_update().get(pk=order_id)

    # Check if already processed
    if order.status == "processed":
        return {"status": "already_processed"}

    # Process order
    order.status = "processed"
    order.save()

    return {"status": "success"}
```

### Task Locking (Prevent Duplicates)

```python
# core/tasks.py
from celery import shared_task
from django.core.cache import cache
from contextlib import contextmanager


@contextmanager
def task_lock(lock_id: str, timeout: int = 300):
    """Acquire a lock to prevent duplicate task execution."""
    acquired = cache.add(lock_id, "locked", timeout)
    try:
        yield acquired
    finally:
        if acquired:
            cache.delete(lock_id)


@shared_task(bind=True)
def sync_all_doctors(self):
    lock_id = f"task-lock-{self.name}"

    with task_lock(lock_id) as acquired:
        if not acquired:
            return {"status": "skipped", "reason": "already running"}

        # Do the actual work
        for doctor in Doctor.objects.all():
            sync_doctor_to_external_system.delay(doctor.id)

        return {"status": "success"}
```

### Progress Tracking

```python
# reports/tasks.py
from celery import shared_task


@shared_task(bind=True)
def generate_large_report(self, report_id: int):
    report = Report.objects.get(pk=report_id)
    items = report.items.all()
    total = items.count()

    for i, item in enumerate(items):
        process_item(item)

        # Update progress
        self.update_state(
            state="PROGRESS",
            meta={"current": i + 1, "total": total}
        )

    return {"status": "complete", "processed": total}
```

```python
# Check progress
result = generate_large_report.delay(report_id)

# Later...
if result.state == "PROGRESS":
    progress = result.info
    print(f"Progress: {progress['current']}/{progress['total']}")
```

## Running Workers

### Development

```bash
# Single worker
celery -A config worker -l info

# With Beat scheduler
celery -A config worker -l info -B

# Specific queues
celery -A config worker -l info -Q emails,reports
```

### Production

```bash
# Multiple workers with concurrency
celery -A config worker -l info --concurrency=4

# Separate workers per queue
celery -A config worker -l info -Q default --concurrency=4
celery -A config worker -l info -Q emails --concurrency=2
celery -A config worker -l info -Q reports --concurrency=1

# Beat scheduler (only run ONE instance)
celery -A config beat -l info --scheduler django_celery_beat.schedulers:DatabaseScheduler
```

### Docker Compose

```yaml
# docker-compose.yml
services:
  celery-worker:
    build: .
    command: celery -A config worker -l info --concurrency=4
    environment:
      - REDIS_URL=redis://redis:6379/0
    depends_on:
      - redis
      - db

  celery-beat:
    build: .
    command: celery -A config beat -l info
    environment:
      - REDIS_URL=redis://redis:6379/0
    depends_on:
      - redis

  flower:
    build: .
    command: celery -A config flower --port=5555
    ports:
      - "5555:5555"
    depends_on:
      - redis
```

### Systemd Service

```ini
# /etc/systemd/system/celery.service
[Unit]
Description=Celery Worker
After=network.target

[Service]
Type=forking
User=app
Group=app
WorkingDirectory=/app
ExecStart=/app/.venv/bin/celery -A config worker -l info --concurrency=4
ExecStop=/bin/kill -s TERM $MAINPID
Restart=always

[Install]
WantedBy=multi-user.target
```

## Testing Tasks

```python
# tests/test_tasks.py
import pytest
from unittest.mock import patch
from doctors.tasks import send_welcome_email, sync_doctor_to_external_system


@pytest.mark.django_db
def test_send_welcome_email(doctor_factory):
    doctor = doctor_factory()

    with patch("doctors.tasks.send_mail") as mock_send:
        result = send_welcome_email(doctor.id)

        mock_send.assert_called_once()
        assert doctor.email in mock_send.call_args[1]["recipient_list"]


@pytest.mark.django_db
def test_sync_doctor_retries_on_error(doctor_factory):
    doctor = doctor_factory()

    with patch("doctors.tasks.requests.post") as mock_post:
        mock_post.side_effect = requests.RequestException("Connection error")

        with pytest.raises(requests.RequestException):
            sync_doctor_to_external_system(doctor.id)


# Test with CELERY_TASK_ALWAYS_EAGER = True for synchronous execution
@pytest.fixture
def celery_eager(settings):
    settings.CELERY_TASK_ALWAYS_EAGER = True
    settings.CELERY_TASK_EAGER_PROPAGATES = True
```

## Best Practices

1. **Keep tasks small and focused** - One task, one responsibility
2. **Make tasks idempotent** - Safe to run multiple times
3. **Use retries for transient failures** - Network errors, timeouts
4. **Set time limits** - Prevent runaway tasks
5. **Log task execution** - Debug and monitor
6. **Use separate queues** - Prioritize critical tasks
7. **Monitor with Flower** - Track task health
8. **Test tasks thoroughly** - Mock external dependencies

## References

- [Celery Documentation](https://docs.celeryq.dev/)
- [Django Celery Beat](https://django-celery-beat.readthedocs.io/)
- [Flower Monitoring](https://flower.readthedocs.io/)

## Next Steps

- [Middleware](./18-middleware.md)
- [Admin Customization](./19-admin-customization.md)

---

[Previous: File Uploads](./16-file-uploads.md) | [Back to Index](./README.md) | [Next: Middleware](./18-middleware.md)
