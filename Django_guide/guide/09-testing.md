# Testing

This chapter covers comprehensive testing strategies for Django + DRF APIs using pytest, factories, and best practices for API testing.

## Testing Stack

| Tool | Purpose |
|------|---------|
| pytest | Test runner and framework |
| pytest-django | Django integration |
| pytest-cov | Code coverage |
| factory_boy | Test data factories |
| faker | Realistic fake data |
| responses/httpx-mock | Mock HTTP requests |

## Installation

```bash
uv add --dev pytest pytest-django pytest-cov factory-boy faker responses
```

## Configuration

### pytest.ini / pyproject.toml

```toml
# pyproject.toml
[tool.pytest.ini_options]
DJANGO_SETTINGS_MODULE = "config.settings"
python_files = ["test_*.py", "*_test.py"]
python_classes = ["Test*"]
python_functions = ["test_*"]
addopts = [
    "-ra",
    "-q",
    "--strict-markers",
    "--tb=short",
]
markers = [
    "slow: marks tests as slow",
    "integration: marks tests as integration tests",
]
filterwarnings = [
    "ignore::DeprecationWarning",
]
```

### conftest.py

```python
# tests/conftest.py
import pytest
from rest_framework.test import APIClient
from django.contrib.auth import get_user_model

User = get_user_model()


@pytest.fixture
def api_client():
    """Unauthenticated API client."""
    return APIClient()


@pytest.fixture
def user(db):
    """Create a regular user."""
    return User.objects.create_user(
        username="testuser",
        email="test@example.com",
        password="testpass123",
    )


@pytest.fixture
def admin_user(db):
    """Create an admin user."""
    return User.objects.create_superuser(
        username="admin",
        email="admin@example.com",
        password="adminpass123",
    )


@pytest.fixture
def authenticated_client(api_client, user):
    """API client authenticated as regular user."""
    api_client.force_authenticate(user=user)
    return api_client


@pytest.fixture
def admin_client(api_client, admin_user):
    """API client authenticated as admin."""
    api_client.force_authenticate(user=admin_user)
    return api_client
```

## Test Structure

```
project/
├── doctors/
│   ├── tests/
│   │   ├── __init__.py
│   │   ├── conftest.py        # App-specific fixtures
│   │   ├── factories.py       # Model factories
│   │   ├── test_models.py
│   │   ├── test_serializers.py
│   │   ├── test_views.py
│   │   └── test_permissions.py
├── tests/
│   ├── conftest.py            # Global fixtures
│   └── test_integration.py
```

## Factories with factory_boy

```python
# doctors/tests/factories.py
import factory
from factory.django import DjangoModelFactory
from faker import Faker

from doctors.models import Doctor, Department
from django.contrib.auth import get_user_model

fake = Faker()
User = get_user_model()


class UserFactory(DjangoModelFactory):
    class Meta:
        model = User

    username = factory.Sequence(lambda n: f"user{n}")
    email = factory.LazyAttribute(lambda obj: f"{obj.username}@example.com")
    password = factory.PostGenerationMethodCall("set_password", "password123")
    is_active = True


class DepartmentFactory(DjangoModelFactory):
    class Meta:
        model = Department

    name = factory.Faker("company")
    code = factory.Sequence(lambda n: f"DEPT{n:03d}")


class DoctorFactory(DjangoModelFactory):
    class Meta:
        model = Doctor

    first_name = factory.Faker("first_name")
    last_name = factory.Faker("last_name")
    email = factory.LazyAttribute(
        lambda obj: f"{obj.first_name.lower()}.{obj.last_name.lower()}@hospital.com"
    )
    specialty = factory.Faker("random_element", elements=["cardiology", "neurology", "pediatrics"])
    is_active = True
    department = factory.SubFactory(DepartmentFactory)
    created_by = factory.SubFactory(UserFactory)

    @factory.post_generation
    def certifications(self, create, extracted, **kwargs):
        if not create:
            return
        if extracted:
            self.certifications.set(extracted)
```

### Using Factories in conftest.py

```python
# doctors/tests/conftest.py
import pytest
from .factories import DoctorFactory, DepartmentFactory, UserFactory


@pytest.fixture
def doctor_factory():
    """Factory fixture for creating doctors."""
    return DoctorFactory


@pytest.fixture
def doctor(db):
    """Single doctor instance."""
    return DoctorFactory()


@pytest.fixture
def doctors(db):
    """Multiple doctors."""
    return DoctorFactory.create_batch(5)


@pytest.fixture
def department(db):
    """Single department."""
    return DepartmentFactory()
```

## Testing Models

```python
# doctors/tests/test_models.py
import pytest
from django.db import IntegrityError
from doctors.models import Doctor
from .factories import DoctorFactory


@pytest.mark.django_db
class TestDoctorModel:
    def test_create_doctor(self, doctor):
        assert doctor.id is not None
        assert doctor.email is not None

    def test_doctor_str(self, doctor):
        expected = f"Dr. {doctor.first_name} {doctor.last_name}"
        assert str(doctor) == expected

    def test_doctor_full_name(self, doctor):
        assert doctor.full_name == f"{doctor.first_name} {doctor.last_name}"

    def test_unique_email(self, doctor):
        with pytest.raises(IntegrityError):
            DoctorFactory(email=doctor.email)

    def test_is_available_default(self, doctor):
        assert doctor.is_active is True

    def test_soft_delete(self, doctor):
        doctor.soft_delete()
        assert doctor.is_active is False
        assert Doctor.objects.filter(id=doctor.id).exists()
        assert Doctor.active.filter(id=doctor.id).count() == 0
```

## Testing Serializers

```python
# doctors/tests/test_serializers.py
import pytest
from doctors.serializers import DoctorSerializer, DoctorCreateSerializer
from .factories import DoctorFactory, DepartmentFactory


@pytest.mark.django_db
class TestDoctorSerializer:
    def test_serialize_doctor(self, doctor):
        serializer = DoctorSerializer(doctor)
        data = serializer.data

        assert data["id"] == doctor.id
        assert data["email"] == doctor.email
        assert "password" not in data  # Ensure password not exposed

    def test_serialize_many(self, doctors):
        serializer = DoctorSerializer(doctors, many=True)
        assert len(serializer.data) == 5

    def test_nested_department(self, doctor):
        serializer = DoctorSerializer(doctor)
        assert "department" in serializer.data
        assert serializer.data["department"]["name"] == doctor.department.name


@pytest.mark.django_db
class TestDoctorCreateSerializer:
    def test_valid_data(self, department):
        data = {
            "first_name": "John",
            "last_name": "Smith",
            "email": "john.smith@hospital.com",
            "specialty": "cardiology",
            "department_id": department.id,
        }
        serializer = DoctorCreateSerializer(data=data)
        assert serializer.is_valid(), serializer.errors

    def test_invalid_email(self):
        data = {
            "first_name": "John",
            "last_name": "Smith",
            "email": "invalid-email",
            "specialty": "cardiology",
        }
        serializer = DoctorCreateSerializer(data=data)
        assert not serializer.is_valid()
        assert "email" in serializer.errors

    def test_duplicate_email(self, doctor):
        data = {
            "first_name": "Jane",
            "last_name": "Doe",
            "email": doctor.email,  # Duplicate
            "specialty": "neurology",
        }
        serializer = DoctorCreateSerializer(data=data)
        assert not serializer.is_valid()
        assert "email" in serializer.errors
```

## Testing Views (API Endpoints)

```python
# doctors/tests/test_views.py
import pytest
from django.urls import reverse
from rest_framework import status
from .factories import DoctorFactory


@pytest.mark.django_db
class TestDoctorListView:
    url = reverse("doctor-list")

    def test_list_requires_auth(self, api_client):
        response = api_client.get(self.url)
        assert response.status_code == status.HTTP_401_UNAUTHORIZED

    def test_list_doctors(self, authenticated_client, doctors):
        response = authenticated_client.get(self.url)
        assert response.status_code == status.HTTP_200_OK
        assert len(response.data["results"]) == 5

    def test_list_pagination(self, authenticated_client):
        DoctorFactory.create_batch(25)
        response = authenticated_client.get(self.url)
        assert response.status_code == status.HTTP_200_OK
        assert "next" in response.data
        assert len(response.data["results"]) == 20  # Default page size

    def test_filter_by_specialty(self, authenticated_client):
        DoctorFactory.create_batch(3, specialty="cardiology")
        DoctorFactory.create_batch(2, specialty="neurology")

        response = authenticated_client.get(self.url, {"specialty": "cardiology"})
        assert response.status_code == status.HTTP_200_OK
        assert len(response.data["results"]) == 3

    def test_search_by_name(self, authenticated_client):
        DoctorFactory(first_name="John", last_name="Smith")
        DoctorFactory(first_name="Jane", last_name="Doe")

        response = authenticated_client.get(self.url, {"search": "John"})
        assert response.status_code == status.HTTP_200_OK
        assert len(response.data["results"]) == 1


@pytest.mark.django_db
class TestDoctorDetailView:
    def test_get_doctor(self, authenticated_client, doctor):
        url = reverse("doctor-detail", args=[doctor.id])
        response = authenticated_client.get(url)
        assert response.status_code == status.HTTP_200_OK
        assert response.data["id"] == doctor.id

    def test_get_nonexistent_doctor(self, authenticated_client):
        url = reverse("doctor-detail", args=[99999])
        response = authenticated_client.get(url)
        assert response.status_code == status.HTTP_404_NOT_FOUND


@pytest.mark.django_db
class TestDoctorCreateView:
    url = reverse("doctor-list")

    def test_create_doctor(self, admin_client, department):
        data = {
            "first_name": "New",
            "last_name": "Doctor",
            "email": "new.doctor@hospital.com",
            "specialty": "cardiology",
            "department_id": department.id,
        }
        response = admin_client.post(self.url, data)
        assert response.status_code == status.HTTP_201_CREATED
        assert response.data["email"] == data["email"]

    def test_create_requires_admin(self, authenticated_client, department):
        data = {
            "first_name": "New",
            "last_name": "Doctor",
            "email": "new.doctor@hospital.com",
            "specialty": "cardiology",
        }
        response = authenticated_client.post(self.url, data)
        assert response.status_code == status.HTTP_403_FORBIDDEN

    def test_create_invalid_data(self, admin_client):
        data = {"first_name": "Only"}  # Missing required fields
        response = admin_client.post(self.url, data)
        assert response.status_code == status.HTTP_400_BAD_REQUEST


@pytest.mark.django_db
class TestDoctorUpdateView:
    def test_update_doctor(self, admin_client, doctor):
        url = reverse("doctor-detail", args=[doctor.id])
        data = {"first_name": "Updated"}
        response = admin_client.patch(url, data)
        assert response.status_code == status.HTTP_200_OK
        assert response.data["first_name"] == "Updated"

    def test_owner_can_update(self, api_client, user, doctor):
        doctor.created_by = user
        doctor.save()
        api_client.force_authenticate(user=user)

        url = reverse("doctor-detail", args=[doctor.id])
        response = api_client.patch(url, {"first_name": "Updated"})
        assert response.status_code == status.HTTP_200_OK


@pytest.mark.django_db
class TestDoctorDeleteView:
    def test_delete_doctor(self, admin_client, doctor):
        url = reverse("doctor-detail", args=[doctor.id])
        response = admin_client.delete(url)
        assert response.status_code == status.HTTP_204_NO_CONTENT

    def test_regular_user_cannot_delete(self, authenticated_client, doctor):
        url = reverse("doctor-detail", args=[doctor.id])
        response = authenticated_client.delete(url)
        assert response.status_code == status.HTTP_403_FORBIDDEN
```

## Testing Permissions

```python
# doctors/tests/test_permissions.py
import pytest
from rest_framework.test import APIRequestFactory
from rest_framework.views import APIView
from doctors.permissions import IsOwnerOrReadOnly, IsAdminOrReadOnly
from .factories import DoctorFactory, UserFactory


@pytest.mark.django_db
class TestIsOwnerOrReadOnly:
    def test_read_allowed_for_all(self):
        factory = APIRequestFactory()
        request = factory.get("/")
        request.user = UserFactory()

        doctor = DoctorFactory()
        permission = IsOwnerOrReadOnly()

        assert permission.has_object_permission(request, APIView(), doctor)

    def test_write_allowed_for_owner(self):
        user = UserFactory()
        doctor = DoctorFactory(created_by=user)

        factory = APIRequestFactory()
        request = factory.put("/")
        request.user = user

        permission = IsOwnerOrReadOnly()
        assert permission.has_object_permission(request, APIView(), doctor)

    def test_write_denied_for_non_owner(self):
        owner = UserFactory()
        other_user = UserFactory()
        doctor = DoctorFactory(created_by=owner)

        factory = APIRequestFactory()
        request = factory.put("/")
        request.user = other_user

        permission = IsOwnerOrReadOnly()
        assert not permission.has_object_permission(request, APIView(), doctor)
```

## Mocking External Services

```python
# doctors/tests/test_external_services.py
import pytest
import responses
from doctors.services import sync_doctor_to_crm
from .factories import DoctorFactory


@pytest.mark.django_db
class TestExternalSync:
    @responses.activate
    def test_sync_success(self, doctor):
        responses.add(
            responses.POST,
            "https://crm.example.com/api/doctors",
            json={"id": "ext-123", "status": "created"},
            status=201,
        )

        result = sync_doctor_to_crm(doctor)

        assert result["external_id"] == "ext-123"
        assert len(responses.calls) == 1

    @responses.activate
    def test_sync_failure(self, doctor):
        responses.add(
            responses.POST,
            "https://crm.example.com/api/doctors",
            json={"error": "Server error"},
            status=500,
        )

        with pytest.raises(ExternalServiceError):
            sync_doctor_to_crm(doctor)
```

## Testing Query Count (N+1 Prevention)

```python
# doctors/tests/test_performance.py
import pytest
from django.test.utils import CaptureQueriesContext
from django.db import connection
from rest_framework.test import APIClient
from django.urls import reverse
from .factories import DoctorFactory, DepartmentFactory


@pytest.mark.django_db
class TestQueryPerformance:
    def test_list_doctors_query_count(self, authenticated_client):
        # Create test data with relationships
        dept = DepartmentFactory()
        DoctorFactory.create_batch(10, department=dept)

        with CaptureQueriesContext(connection) as context:
            response = authenticated_client.get(reverse("doctor-list"))

        assert response.status_code == 200
        # Should be constant regardless of doctor count
        # 1: auth, 2: count, 3: doctors with select_related
        assert len(context) <= 5, f"Too many queries: {len(context)}"

    def test_detail_query_count(self, authenticated_client, doctor):
        with CaptureQueriesContext(connection) as context:
            url = reverse("doctor-detail", args=[doctor.id])
            response = authenticated_client.get(url)

        assert response.status_code == 200
        assert len(context) <= 3
```

## Integration Tests

```python
# tests/test_integration.py
import pytest
from django.urls import reverse
from rest_framework import status
from doctors.tests.factories import DoctorFactory, UserFactory


@pytest.mark.django_db
@pytest.mark.integration
class TestDoctorWorkflow:
    """Test complete doctor CRUD workflow."""

    def test_full_crud_workflow(self, api_client):
        # Create admin user
        admin = UserFactory(is_staff=True)
        api_client.force_authenticate(user=admin)

        # CREATE
        create_response = api_client.post(
            reverse("doctor-list"),
            {
                "first_name": "Test",
                "last_name": "Doctor",
                "email": "test@hospital.com",
                "specialty": "cardiology",
            },
        )
        assert create_response.status_code == status.HTTP_201_CREATED
        doctor_id = create_response.data["id"]

        # READ
        detail_url = reverse("doctor-detail", args=[doctor_id])
        read_response = api_client.get(detail_url)
        assert read_response.status_code == status.HTTP_200_OK
        assert read_response.data["email"] == "test@hospital.com"

        # UPDATE
        update_response = api_client.patch(
            detail_url,
            {"specialty": "neurology"},
        )
        assert update_response.status_code == status.HTTP_200_OK
        assert update_response.data["specialty"] == "neurology"

        # DELETE
        delete_response = api_client.delete(detail_url)
        assert delete_response.status_code == status.HTTP_204_NO_CONTENT

        # Verify deleted
        get_response = api_client.get(detail_url)
        assert get_response.status_code == status.HTTP_404_NOT_FOUND
```

## Running Tests

```bash
# Run all tests
uv run pytest

# Run with coverage
uv run pytest --cov=. --cov-report=term-missing

# Run specific test file
uv run pytest doctors/tests/test_views.py

# Run specific test class
uv run pytest doctors/tests/test_views.py::TestDoctorListView

# Run specific test
uv run pytest doctors/tests/test_views.py::TestDoctorListView::test_list_doctors

# Run with markers
uv run pytest -m "not slow"
uv run pytest -m integration

# Parallel execution
uv add --dev pytest-xdist
uv run pytest -n auto

# Stop on first failure
uv run pytest -x

# Verbose output
uv run pytest -v
```

## Coverage Configuration

```toml
# pyproject.toml
[tool.coverage.run]
branch = true
source = ["."]
omit = [
    "*/migrations/*",
    "*/tests/*",
    "*/__pycache__/*",
    "manage.py",
    "config/wsgi.py",
    "config/asgi.py",
]

[tool.coverage.report]
exclude_lines = [
    "pragma: no cover",
    "def __repr__",
    "raise NotImplementedError",
    "if TYPE_CHECKING:",
    "if __name__ == .__main__.:",
]
fail_under = 80
show_missing = true
```

## Best Practices

1. **Use factories** - Don't create models manually in each test
2. **Test behavior, not implementation** - Focus on API contracts
3. **Keep tests independent** - No test should depend on another
4. **Use descriptive names** - `test_create_doctor_with_invalid_email_returns_400`
5. **Test edge cases** - Empty lists, null values, boundaries
6. **Mock external services** - Don't hit real APIs in tests
7. **Check query count** - Prevent N+1 queries
8. **Maintain coverage** - Aim for 80%+ coverage

## References

- [pytest Documentation](https://docs.pytest.org/)
- [pytest-django](https://pytest-django.readthedocs.io/)
- [DRF Testing](https://www.django-rest-framework.org/api-guide/testing/)
- [factory_boy](https://factoryboy.readthedocs.io/)

## Next Steps

- [Project: Doctor API](./10-project-doctor-api.md) - Complete implementation
- [Filtering and Search](./11-filtering-search.md) - Query improvements

---

[Previous: API Documentation](./08-api-documentation.md) | [Back to Index](./README.md) | [Next: Project: Doctor API](./10-project-doctor-api.md)
