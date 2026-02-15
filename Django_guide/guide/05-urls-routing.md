# URLs and Routing

This chapter covers Django URL patterns, path converters, namespacing, and DRF routers.

## Step 1: Create App URLs

```python
# core/urls.py
from django.urls import path
from . import views

urlpatterns = [
    path("doctors/", views.DoctorListView.as_view(), name="doctor-list"),
    path("doctors/<uuid:pk>/", views.DoctorDetailView.as_view(), name="doctor-detail"),
]
```

Include them in the project URLconf:

```python
# config/urls.py
from django.contrib import admin
from django.urls import include, path

urlpatterns = [
    path("admin/", admin.site.urls),
    path("api/", include("core.urls")),
]
```

## Step 2: Path Converters

Django ships with common converters:

- `str`, `int`, `slug`, `uuid`, `path`

```python
urlpatterns = [
    path("doctors/<int:pk>/", views.DoctorDetailView.as_view()),
    path("articles/<slug:slug>/", views.ArticleView.as_view()),
    path("files/<path:file_path>/", views.FileView.as_view()),
]
```

### Custom Converters

```python
# core/converters.py
class FourDigitYearConverter:
    regex = "[0-9]{4}"

    def to_python(self, value):
        return int(value)

    def to_url(self, value):
        return f"{value:04d}"
```

```python
# core/urls.py
from django.urls import path, register_converter
from .converters import FourDigitYearConverter

register_converter(FourDigitYearConverter, "yyyy")

urlpatterns = [
    path("reports/<yyyy:year>/", views.ReportView.as_view()),
]
```

## Step 3: include() and Namespaces

Add `app_name` for namespacing and reverse lookups.

```python
# core/urls.py
from django.urls import path
from . import views

app_name = "core"

urlpatterns = [
    path("doctors/", views.DoctorListView.as_view(), name="doctor-list"),
]
```

```python
# config/urls.py
urlpatterns = [
    path("api/", include("core.urls")),
]
```

Reverse usage:

```python
from django.urls import reverse
reverse("core:doctor-list")
```

## Step 4: re_path for Regex

Use `re_path` only for complex regex patterns.

```python
from django.urls import re_path

urlpatterns = [
    re_path(r"^legacy/(?P<slug>[\w-]+)/$", views.LegacyView.as_view()),
]
```

## Step 5: DRF Routers (Recommended for ViewSets)

```python
# core/urls.py
from django.urls import include, path
from rest_framework.routers import DefaultRouter
from .viewsets import DoctorViewSet

router = DefaultRouter()
router.register(r"doctors", DoctorViewSet, basename="doctor")

urlpatterns = [
    path("", include(router.urls)),
]
```

DefaultRouter also adds an API root.

## Step 6: Nested Routes

For nested resources, use manual nesting or `drf-nested-routers`.

```python
# Manual nesting
urlpatterns = [
    path("doctors/<uuid:doctor_id>/appointments/", views.DoctorAppointmentsView.as_view()),
]
```

## Step 7: Versioning Strategy

Keep versioning simple:

```python
urlpatterns = [
    path("api/v1/", include("core.urls")),
]
```

## Tips and Best Practices

- Keep URLs plural and lowercase.
- Avoid deep nesting beyond two levels.
- Use UUIDs for public IDs.
- Prefer routers for CRUD APIs.

## References

- [Django URL Dispatcher](https://docs.djangoproject.com/en/5.2/topics/http/urls/)

## Next Steps

- [Authentication and Permissions](./06-authentication-permissions.md) - Secure your API
- [Validation](./07-validation.md) - Data validation patterns

---

[Previous: Views and ViewSets](./04-views-viewsets.md) | [Back to Index](./README.md) | [Next: Authentication and Permissions](./06-authentication-permissions.md)
