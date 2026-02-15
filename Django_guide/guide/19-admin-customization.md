# Admin Customization

Django Admin is a powerful internal tool. This chapter covers practical customization for production.

## Step 1: Register Models

```python
# doctors/admin.py
from django.contrib import admin
from .models import Doctor, Department

admin.site.register(Doctor)
admin.site.register(Department)
```

## Step 2: Customize ModelAdmin

```python
from django.contrib import admin
from .models import Doctor


@admin.register(Doctor)
class DoctorAdmin(admin.ModelAdmin):
    list_display = ["id", "first_name", "last_name", "email", "specialty", "is_active"]
    list_filter = ["specialty", "is_active"]
    search_fields = ["first_name", "last_name", "email"]
    ordering = ["last_name"]
    list_per_page = 25
```

## Step 3: Read-Only Fields and Fieldsets

```python
@admin.register(Doctor)
class DoctorAdmin(admin.ModelAdmin):
    readonly_fields = ["created_at"]
    fieldsets = (
        ("Profile", {"fields": ("first_name", "last_name", "email", "specialty")}),
        ("Status", {"fields": ("is_active",)}),
        ("System", {"fields": ("created_at",)}),
    )
```

## Step 4: Inlines

```python
from django.contrib import admin
from .models import Doctor, Department


class DepartmentInline(admin.TabularInline):
    model = Doctor.departments.through
    extra = 1


@admin.register(Doctor)
class DoctorAdmin(admin.ModelAdmin):
    inlines = [DepartmentInline]
```

## Step 5: Admin Actions

```python
@admin.action(description="Deactivate selected doctors")
def deactivate_doctors(modeladmin, request, queryset):
    queryset.update(is_active=False)


@admin.register(Doctor)
class DoctorAdmin(admin.ModelAdmin):
    actions = [deactivate_doctors]
```

## Tips

- Keep admin for internal operations only.
- Add `list_select_related` to reduce queries.
- Use actions for bulk operations.

## References

- [Django Admin Site](https://docs.djangoproject.com/en/5.2/ref/contrib/admin/)

## Next Steps

- [Deployment](./20-deployment.md)
- [Logging](./21-logging.md)

---

[Previous: Middleware](./18-middleware.md) | [Back to Index](./README.md) | [Next: Deployment](./20-deployment.md)
