# Admin Customization

Django Admin is a powerful tool for internal operations. This chapter covers production-ready customization including security, performance, and advanced features.

## Basic Registration

```python
# doctors/admin.py
from django.contrib import admin
from .models import Doctor, Department, Appointment

admin.site.register(Doctor)
admin.site.register(Department)
admin.site.register(Appointment)
```

## ModelAdmin Customization

### List Display and Filtering

```python
# doctors/admin.py
from django.contrib import admin
from django.utils.html import format_html
from .models import Doctor


@admin.register(Doctor)
class DoctorAdmin(admin.ModelAdmin):
    # Columns in list view
    list_display = [
        "id",
        "full_name",
        "email",
        "specialty",
        "department",
        "is_active_badge",
        "created_at",
    ]

    # Filters in sidebar
    list_filter = [
        "specialty",
        "is_active",
        "department",
        ("created_at", admin.DateFieldListFilter),
    ]

    # Search fields
    search_fields = [
        "first_name",
        "last_name",
        "email",
    ]

    # Default ordering
    ordering = ["-created_at"]

    # Items per page
    list_per_page = 25

    # Editable in list view
    list_editable = ["is_active"]

    # Links in list view
    list_display_links = ["id", "full_name"]

    # Date hierarchy navigation
    date_hierarchy = "created_at"

    def full_name(self, obj):
        return f"Dr. {obj.first_name} {obj.last_name}"
    full_name.short_description = "Name"
    full_name.admin_order_field = "last_name"

    def is_active_badge(self, obj):
        if obj.is_active:
            return format_html('<span style="color: green;">Active</span>')
        return format_html('<span style="color: red;">Inactive</span>')
    is_active_badge.short_description = "Status"
```

### Form Customization

```python
@admin.register(Doctor)
class DoctorAdmin(admin.ModelAdmin):
    # Read-only fields
    readonly_fields = ["created_at", "updated_at", "created_by"]

    # Field grouping
    fieldsets = (
        ("Personal Information", {
            "fields": ("first_name", "last_name", "email", "phone"),
        }),
        ("Professional", {
            "fields": ("specialty", "department", "license_number"),
        }),
        ("Status", {
            "fields": ("is_active",),
            "classes": ("collapse",),  # Collapsible section
        }),
        ("Metadata", {
            "fields": ("created_at", "updated_at", "created_by"),
            "classes": ("collapse",),
        }),
    )

    # Different fieldsets for add vs change
    add_fieldsets = (
        (None, {
            "fields": ("first_name", "last_name", "email", "specialty"),
        }),
    )

    def get_fieldsets(self, request, obj=None):
        if not obj:
            return self.add_fieldsets
        return super().get_fieldsets(request, obj)
```

### Autocomplete and Raw ID

```python
@admin.register(Doctor)
class DoctorAdmin(admin.ModelAdmin):
    # Autocomplete for ForeignKey (requires search_fields on related model)
    autocomplete_fields = ["department"]

    # Raw ID input for large tables
    raw_id_fields = ["created_by"]


@admin.register(Department)
class DepartmentAdmin(admin.ModelAdmin):
    search_fields = ["name"]  # Required for autocomplete
```

## Inline Models

```python
# doctors/admin.py
from django.contrib import admin
from .models import Doctor, Certification


class CertificationInline(admin.TabularInline):
    model = Certification
    extra = 1
    min_num = 0
    max_num = 10


class AppointmentInline(admin.StackedInline):
    model = Appointment
    extra = 0
    readonly_fields = ["created_at"]
    show_change_link = True


@admin.register(Doctor)
class DoctorAdmin(admin.ModelAdmin):
    inlines = [CertificationInline, AppointmentInline]
```

## Admin Actions

```python
# doctors/admin.py
from django.contrib import admin
from django.contrib import messages


@admin.action(description="Activate selected doctors")
def activate_doctors(modeladmin, request, queryset):
    count = queryset.update(is_active=True)
    messages.success(request, f"Activated {count} doctors.")


@admin.action(description="Deactivate selected doctors")
def deactivate_doctors(modeladmin, request, queryset):
    count = queryset.update(is_active=False)
    messages.warning(request, f"Deactivated {count} doctors.")


@admin.action(description="Export selected to CSV")
def export_csv(modeladmin, request, queryset):
    import csv
    from django.http import HttpResponse

    response = HttpResponse(content_type="text/csv")
    response["Content-Disposition"] = 'attachment; filename="doctors.csv"'

    writer = csv.writer(response)
    writer.writerow(["ID", "Name", "Email", "Specialty"])

    for doctor in queryset:
        writer.writerow([
            doctor.id,
            f"{doctor.first_name} {doctor.last_name}",
            doctor.email,
            doctor.specialty,
        ])

    return response


@admin.register(Doctor)
class DoctorAdmin(admin.ModelAdmin):
    actions = [activate_doctors, deactivate_doctors, export_csv]
```

## Performance Optimization

```python
@admin.register(Doctor)
class DoctorAdmin(admin.ModelAdmin):
    list_display = ["id", "full_name", "department_name", "specialty"]

    # Optimize ForeignKey queries
    list_select_related = ["department", "created_by"]

    # Optimize ManyToMany queries
    list_prefetch_related = ["certifications"]

    def department_name(self, obj):
        return obj.department.name if obj.department else "-"

    def get_queryset(self, request):
        qs = super().get_queryset(request)
        return qs.select_related("department").prefetch_related("certifications")
```

## Custom Filters

```python
# doctors/admin.py
from django.contrib import admin
from django.utils import timezone
from datetime import timedelta


class RecentlyCreatedFilter(admin.SimpleListFilter):
    title = "recently created"
    parameter_name = "recent"

    def lookups(self, request, model_admin):
        return [
            ("today", "Today"),
            ("week", "This week"),
            ("month", "This month"),
        ]

    def queryset(self, request, queryset):
        now = timezone.now()
        if self.value() == "today":
            return queryset.filter(created_at__date=now.date())
        if self.value() == "week":
            return queryset.filter(created_at__gte=now - timedelta(days=7))
        if self.value() == "month":
            return queryset.filter(created_at__gte=now - timedelta(days=30))
        return queryset


class HasCertificationsFilter(admin.SimpleListFilter):
    title = "has certifications"
    parameter_name = "has_certs"

    def lookups(self, request, model_admin):
        return [
            ("yes", "Yes"),
            ("no", "No"),
        ]

    def queryset(self, request, queryset):
        if self.value() == "yes":
            return queryset.filter(certifications__isnull=False).distinct()
        if self.value() == "no":
            return queryset.filter(certifications__isnull=True)
        return queryset


@admin.register(Doctor)
class DoctorAdmin(admin.ModelAdmin):
    list_filter = [
        "specialty",
        "is_active",
        RecentlyCreatedFilter,
        HasCertificationsFilter,
    ]
```

## Custom Admin Views

```python
# doctors/admin.py
from django.contrib import admin
from django.urls import path
from django.shortcuts import render
from django.db.models import Count


@admin.register(Doctor)
class DoctorAdmin(admin.ModelAdmin):
    change_list_template = "admin/doctors/doctor_changelist.html"

    def get_urls(self):
        urls = super().get_urls()
        custom_urls = [
            path("stats/", self.admin_site.admin_view(self.stats_view), name="doctor_stats"),
        ]
        return custom_urls + urls

    def stats_view(self, request):
        stats = Doctor.objects.values("specialty").annotate(count=Count("id"))
        context = {
            **self.admin_site.each_context(request),
            "title": "Doctor Statistics",
            "stats": stats,
        }
        return render(request, "admin/doctors/stats.html", context)
```

```html
<!-- templates/admin/doctors/doctor_changelist.html -->
{% extends "admin/change_list.html" %}
{% block object-tools-items %}
    <li>
        <a href="{% url 'admin:doctor_stats' %}">View Statistics</a>
    </li>
    {{ block.super }}
{% endblock %}
```

## Admin Security

### Restrict by IP

```python
# core/admin.py
from django.contrib.admin import AdminSite
from django.http import HttpResponseForbidden


class SecureAdminSite(AdminSite):
    ALLOWED_IPS = ["127.0.0.1", "10.0.0.0/8"]

    def has_permission(self, request):
        if not self.is_ip_allowed(request):
            return False
        return super().has_permission(request)

    def is_ip_allowed(self, request):
        import ipaddress
        client_ip = self.get_client_ip(request)
        try:
            ip = ipaddress.ip_address(client_ip)
            for allowed in self.ALLOWED_IPS:
                if "/" in allowed:
                    if ip in ipaddress.ip_network(allowed):
                        return True
                elif str(ip) == allowed:
                    return True
        except ValueError:
            pass
        return False

    def get_client_ip(self, request):
        x_forwarded_for = request.META.get("HTTP_X_FORWARDED_FOR")
        if x_forwarded_for:
            return x_forwarded_for.split(",")[0].strip()
        return request.META.get("REMOTE_ADDR")


admin_site = SecureAdminSite(name="secure_admin")
```

### Two-Factor Authentication

```bash
uv add django-otp django-two-factor-auth
```

```python
# config/settings.py
INSTALLED_APPS = [
    "django_otp",
    "django_otp.plugins.otp_totp",
    "two_factor",
    # ...
]

MIDDLEWARE = [
    # ...
    "django_otp.middleware.OTPMiddleware",
]
```

### Audit Logging

```python
# doctors/admin.py
from django.contrib import admin
from django.contrib.admin.models import LogEntry


@admin.register(LogEntry)
class LogEntryAdmin(admin.ModelAdmin):
    list_display = ["action_time", "user", "content_type", "object_repr", "action_flag"]
    list_filter = ["action_time", "user", "content_type"]
    search_fields = ["object_repr", "change_message"]
    readonly_fields = [f.name for f in LogEntry._meta.fields]

    def has_add_permission(self, request):
        return False

    def has_change_permission(self, request, obj=None):
        return False

    def has_delete_permission(self, request, obj=None):
        return False
```

## Admin Site Customization

```python
# config/admin.py
from django.contrib import admin

admin.site.site_header = "Doctor API Admin"
admin.site.site_title = "Doctor API"
admin.site.index_title = "Dashboard"
```

```python
# config/urls.py
from django.contrib import admin

urlpatterns = [
    path("internal-admin/", admin.site.urls),  # Custom URL
]
```

## Import/Export

```bash
uv add django-import-export
```

```python
# doctors/admin.py
from import_export import resources
from import_export.admin import ImportExportModelAdmin
from .models import Doctor


class DoctorResource(resources.ModelResource):
    class Meta:
        model = Doctor
        fields = ["id", "first_name", "last_name", "email", "specialty"]
        import_id_fields = ["email"]


@admin.register(Doctor)
class DoctorAdmin(ImportExportModelAdmin):
    resource_class = DoctorResource
    list_display = ["id", "first_name", "last_name", "email"]
```

## Best Practices

1. **Use select_related/prefetch_related** - Optimize list view queries
2. **Limit inlines** - Don't load thousands of related objects
3. **Add search fields** - Make data findable
4. **Use readonly_fields** - Protect computed and sensitive data
5. **Customize actions** - Add bulk operations
6. **Secure admin URL** - Change from `/admin/`
7. **Enable audit logging** - Track changes
8. **Restrict by IP** - In production environments

## References

- [Django Admin Site](https://docs.djangoproject.com/en/5.2/ref/contrib/admin/)
- [Django Import Export](https://django-import-export.readthedocs.io/)
- [Django Two-Factor Auth](https://django-two-factor-auth.readthedocs.io/)

## Next Steps

- [Deployment](./20-deployment.md)
- [Logging](./21-logging.md)

---

[Previous: Middleware](./18-middleware.md) | [Back to Index](./README.md) | [Next: Deployment](./20-deployment.md)
