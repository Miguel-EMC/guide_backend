# Testing

This chapter covers Django and DRF testing with `pytest-django` and DRF's testing utilities.

## Step 1: Install Test Dependencies

```bash
uv add --dev pytest pytest-django pytest-cov
```

## Step 2: Configure pytest

```toml
# pyproject.toml
[tool.pytest.ini_options]
DJANGO_SETTINGS_MODULE = "config.settings"
python_files = ["test_*.py"]
```

## Step 3: Structure Your Tests

```
core/
├── tests/
│   ├── test_models.py
│   ├── test_serializers.py
│   ├── test_views.py
│   └── test_permissions.py
```

## Step 4: APIClient Basics

DRF provides `APIClient` for request testing.

```python
from rest_framework.test import APIClient


def test_public_endpoint(db):
    client = APIClient()
    response = client.get("/api/doctors/")
    assert response.status_code == 200
```

## Step 5: Authenticated Requests

```python
from django.contrib.auth.models import User
from rest_framework.test import APIClient


def test_authenticated_request(db):
    user = User.objects.create_user(username="u1", password="pass123")
    client = APIClient()
    client.force_authenticate(user=user)

    response = client.get("/api/doctors/")
    assert response.status_code == 200
```

## Step 6: APITestCase (Django TestCase Style)

```python
from rest_framework.test import APITestCase
from django.urls import reverse


class DoctorAPITest(APITestCase):
    def setUp(self):
        self.url = reverse("doctor-list")

    def test_list(self):
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, 200)
```

## Step 7: APIRequestFactory (Low-Level)

Use `APIRequestFactory` for view-level permission tests.

```python
from rest_framework.test import APIRequestFactory
from rest_framework.views import APIView
from django.contrib.auth.models import User


factory = APIRequestFactory()
request = factory.get("/api/doctors/")
request.user = User.objects.create_user(username="u1", password="pass123")
```

## Step 8: Permissions Tests

```python
from rest_framework.test import APIRequestFactory
from rest_framework.views import APIView
from .permissions import IsOwnerOrReadOnly


def test_permission_allows_owner(db, user_factory, doctor_factory):
    user = user_factory()
    doctor = doctor_factory(created_by=user)
    request = APIRequestFactory().put("/api/doctors/")
    request.user = user
    perm = IsOwnerOrReadOnly()
    assert perm.has_object_permission(request, APIView(), doctor)
```

## Step 9: Coverage

```bash
uv run pytest --cov=core --cov-report=term-missing
```

## Best Practices

- Prefer pytest + fixtures for speed and clarity.
- Use `force_authenticate` to avoid auth boilerplate.
- Keep tests independent and deterministic.
- Test permissions and edge cases early.

## References

- [DRF Testing](https://www.django-rest-framework.org/api-guide/testing/)

## Next Steps

- [Project: Doctor API](./10-project-doctor-api.md) - Complete implementation
- [Filtering and Search](./11-filtering-search.md) - Query improvements

---

[Previous: API Documentation](./08-api-documentation.md) | [Back to Index](./README.md) | [Next: Project: Doctor API](./10-project-doctor-api.md)
