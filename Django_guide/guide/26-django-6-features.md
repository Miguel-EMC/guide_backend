# Django Version Features and Upgrade Guide

This chapter covers new features in recent Django versions and provides a safe upgrade checklist.

## Django Version Timeline

| Version | Release | End of Support | Python Support |
|---------|---------|----------------|----------------|
| 4.2 LTS | Apr 2023 | Apr 2026 | 3.8 - 3.12 |
| 5.0 | Dec 2023 | Aug 2025 | 3.10 - 3.12 |
| 5.1 | Aug 2024 | Apr 2026 | 3.10 - 3.13 |
| 5.2 LTS | Apr 2025 | Apr 2028 | 3.10 - 3.13 |

## Django 5.2 LTS Features

### Composite Primary Keys

Django 5.2 introduces native composite primary key support:

```python
from django.db import models


class OrderItem(models.Model):
    order = models.ForeignKey("Order", on_delete=models.CASCADE)
    product = models.ForeignKey("Product", on_delete=models.CASCADE)
    quantity = models.IntegerField()

    class Meta:
        # New in Django 5.2
        pk = models.CompositePrimaryKey("order", "product")
```

### Improved Async Support

More async-native ORM operations:

```python
# Async aggregation
total = await Order.objects.filter(status="paid").aaggregate(Sum("total"))

# Async exists
has_orders = await Order.objects.filter(user=user).aexists()

# Async in_bulk
products = await Product.objects.ain_bulk([1, 2, 3])
```

### Generated Columns

Database-computed columns:

```python
class Order(models.Model):
    subtotal = models.DecimalField(max_digits=10, decimal_places=2)
    tax_rate = models.DecimalField(max_digits=4, decimal_places=2)

    # Computed at database level
    total = models.GeneratedField(
        expression=F("subtotal") * (1 + F("tax_rate")),
        output_field=models.DecimalField(max_digits=10, decimal_places=2),
        db_persist=True,  # Stored column vs virtual
    )
```

## Django 5.1 Features

### Simplified LoginRequiredMiddleware

```python
# config/settings.py
MIDDLEWARE = [
    # ...
    "django.contrib.auth.middleware.LoginRequiredMiddleware",
]

# Exempt specific views
from django.contrib.auth.decorators import login_not_required

@login_not_required
def public_view(request):
    return HttpResponse("Public content")
```

### Database Connection Pool

Built-in connection pooling:

```python
# config/settings.py
DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.postgresql",
        "NAME": "mydb",
        "OPTIONS": {
            "pool": {
                "min_size": 2,
                "max_size": 10,
            },
        },
    },
}
```

### Async Form Validation

```python
from django import forms

class ContactForm(forms.Form):
    email = forms.EmailField()

    async def aclean_email(self):
        email = self.cleaned_data["email"]
        if await User.objects.filter(email=email).aexists():
            raise forms.ValidationError("Email already registered")
        return email
```

## Django 5.0 Features

### Facet Filters in Admin

```python
from django.contrib import admin

@admin.register(Product)
class ProductAdmin(admin.ModelAdmin):
    list_display = ["name", "category", "price"]
    list_filter = ["category", "is_active"]
    show_facets = admin.ShowFacets.ALWAYS  # Show counts in filters
```

### Field Default Callables

Database-level defaults with expressions:

```python
from django.db.models.functions import Now

class Event(models.Model):
    name = models.CharField(max_length=100)
    created_at = models.DateTimeField(db_default=Now())
    status = models.CharField(
        max_length=20,
        db_default="pending",  # Database default
    )
```

### Choice Groups in Forms

```python
class TicketForm(forms.Form):
    category = forms.ChoiceField(
        choices={
            "Technical": [
                ("bug", "Bug Report"),
                ("feature", "Feature Request"),
            ],
            "Billing": [
                ("refund", "Refund"),
                ("invoice", "Invoice Issue"),
            ],
        }
    )
```

## Django 4.2 LTS Features

### Psycopg 3 Support

```python
# config/settings.py
DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.postgresql",
        # Uses psycopg3 if installed, falls back to psycopg2
    }
}
```

```bash
uv add psycopg[binary]  # psycopg 3
```

### Async Streaming Responses

```python
from django.http import StreamingHttpResponse

async def stream_data(request):
    async def generate():
        async for chunk in fetch_large_dataset():
            yield f"data: {chunk}\n\n"

    return StreamingHttpResponse(
        generate(),
        content_type="text/event-stream"
    )
```

### Custom File Storage

```python
from django.core.files.storage import FileSystemStorage
from storages.backends.s3boto3 import S3Boto3Storage

# Per-field storage
class Document(models.Model):
    public_file = models.FileField(storage=S3Boto3Storage())
    private_file = models.FileField(storage=FileSystemStorage(location="/private"))
```

## Upgrade Checklist

### Before Upgrading

1. **Read Release Notes**
   ```bash
   # Check for deprecations and breaking changes
   https://docs.djangoproject.com/en/5.2/releases/
   ```

2. **Check Dependencies**
   ```bash
   # Verify all packages support new Django version
   uv pip list --outdated

   # Key packages to verify:
   # - djangorestframework
   # - django-filter
   # - celery
   # - django-debug-toolbar
   ```

3. **Run Deprecation Warnings**
   ```bash
   python -Wa manage.py test
   ```

### Upgrade Steps

```bash
# 1. Create a new branch
git checkout -b upgrade-django-5.2

# 2. Update Django
uv add "Django>=5.2,<5.3"

# 3. Update dependencies
uv sync

# 4. Run migrations check
uv run python manage.py makemigrations --check

# 5. Run tests
uv run pytest

# 6. Check deployment settings
uv run python manage.py check --deploy

# 7. Test in staging environment
# Deploy to staging and run integration tests
```

### Post-Upgrade Verification

```python
# tests/test_upgrade.py
import django
from django.test import TestCase


class UpgradeTest(TestCase):
    def test_django_version(self):
        assert django.VERSION >= (5, 2)

    def test_database_connection(self):
        from django.db import connection
        with connection.cursor() as cursor:
            cursor.execute("SELECT 1")
            self.assertEqual(cursor.fetchone()[0], 1)

    def test_cache_working(self):
        from django.core.cache import cache
        cache.set("test", "value", timeout=10)
        self.assertEqual(cache.get("test"), "value")

    def test_critical_endpoints(self):
        response = self.client.get("/api/healthz/")
        self.assertEqual(response.status_code, 200)
```

## Common Upgrade Issues

### Issue: Removed Features

```python
# Django 5.0 removed these from django.utils.timezone:
# - utc (use datetime.timezone.utc)
# - FixedOffset (use datetime.timezone)

# Before (Django 4.x)
from django.utils.timezone import utc

# After (Django 5.x)
from datetime import timezone
utc = timezone.utc
```

### Issue: Changed Defaults

```python
# Django 5.0: FORMS_URLFIELD_ASSUME_HTTPS default changed
# Old default: False (assumed http://)
# New default: True (assumes https://)

# To keep old behavior:
FORMS_URLFIELD_ASSUME_HTTPS = False
```

### Issue: Model Field Changes

```python
# Django 5.0: CharField/TextField empty string handling
# Empty strings in CharField now stored as empty string, not NULL

# If you relied on NULL behavior:
class MyModel(models.Model):
    name = models.CharField(max_length=100, null=True, blank=True)
```

## Version-Specific DRF Compatibility

| DRF Version | Django 4.2 | Django 5.0 | Django 5.1 | Django 5.2 |
|-------------|------------|------------|------------|------------|
| 3.14.x | Yes | Yes | No | No |
| 3.15.x | Yes | Yes | Yes | No |
| 3.16.x | Yes | Yes | Yes | Yes |

## Best Practices

1. **Prefer LTS versions** for production APIs
2. **Upgrade incrementally** - don't skip major versions
3. **Run deprecation warnings** before upgrading
4. **Test thoroughly** in staging before production
5. **Monitor error rates** after deployment
6. **Have a rollback plan** ready

## References

- [Django Release Notes](https://docs.djangoproject.com/en/5.2/releases/)
- [Django Deprecation Timeline](https://docs.djangoproject.com/en/5.2/internals/deprecation/)
- [DRF Compatibility](https://www.django-rest-framework.org/#requirements)
- [What's New in Django 5.x](https://docs.djangoproject.com/en/5.2/releases/5.0/)

## Next Steps

- [Architecture and Diagrams](./27-architecture-diagrams.md)
- [Observability](./28-observability.md)
- [Performance](./29-performance.md)

---

[Previous: Frontend Integration](./25-frontend-integration.md) | [Back to Index](./README.md) | [Next: Architecture and Diagrams](./27-architecture-diagrams.md)
