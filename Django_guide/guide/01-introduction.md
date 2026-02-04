# Introduction to Django 6.0 & DRF (2026 Edition)

Django is a high-level Python web framework that encourages rapid development. Django REST Framework (DRF) extends Django to build powerful REST APIs.

**⚠️ Important**: Django 6.0+ requires Python 3.12+. This is a major change from previous versions.

## What's New in Django 6.0?

| Feature | Django 5.x | Django 6.0+ |
|---------|------------|-------------|
| **Python Support** | 3.8+ | **3.12+ required** |
| **Async Views** | Limited support | **Native async views** |
| **Performance** | Good | **Significantly improved** |
| **CSP Support** | Third-party | **Built-in CSP headers** |
| **Type Hints** | Optional | **Enhanced type safety** |
| **Security** | Excellent | **Enhanced protections** |

## Why Django REST Framework in 2026?

| Feature | Benefit |
|---------|---------|
| Mature Ecosystem | 20+ years of production use |
| Built-in Admin | Automatic admin interface for models |
| Advanced ORM | Powerful database abstraction with async support |
| Enhanced Security | CSRF, XSS protection, CSP, SQL injection prevention |
| Browsable API | Web interface for testing endpoints |
| Modern Authentication | JWT, OAuth2, session auth out of the box |
| Async-Ready Serialization | Automatic data conversion and validation |
| AI Integration Ready | Perfect for AI/ML backends |

## Installation

### Create Virtual Environment

```bash
# Create and activate virtual environment
python -m venv venv
source venv/bin/activate  # Linux/Mac
# venv\Scripts\activate   # Windows
```

### Install Packages (Django 6.0)

#### Option 1: uv (Recommended for 2026)

`uv` provides significantly faster package management and better performance for Django projects.

```bash
# Install Django 6.0.1 with uv
uv init django-project
cd django-project

# Core packages
uv add "django==6.0.1"
uv add "djangorestframework"  # DRF 3.16+
uv add "drf-spectacular"    # For OpenAPI 3.1 documentation
uv add "uvicorn"            # For async development server
uv add "channels"            # For WebSocket support
uv add "psycopg2-binary"    # PostgreSQL adapter
uv add "redis"              # For caching and sessions

# Development packages
uv add --dev pytest pytest-django
uv add --dev black flake8 mypy
uv add --dev django-debug-toolbar
```

#### Option 2: pip (Traditional)

```bash
# Core Django 6.0.1 (latest as of Jan 2026)
pip install django==6.0.1
pip install djangorestframework  # DRF 3.16+
pip install drf-spectacular  # For OpenAPI 3.1 documentation
pip install uvicorn  # For async development server
pip install channels  # For WebSocket support
```

#### uv vs pip for Django

| Feature | uv (Recommended) | pip (Traditional) |
|---------|------------------|---------------------|
| **Installation Speed** | 10-50x faster | Standard pip speed |
| **Dependency Resolution** | Excellent, avoids conflicts | Basic, occasional conflicts |
| **Development Server** | 2-3x faster startup | Normal startup time |
| **Package Management** | `uv add/remove` | `pip install/uninstall` |
| **Lock Files** | `uv.lock` for reproducibility | `requirements.txt` |

### Verify Installation

Create `check_django.py`:

```python
import django
import rest_framework
import uvicorn
import sys
import os

print(f"Django version: {django.__version__}")
print(f"DRF version: {rest_framework.__version__}")
print(f"Python version: {sys.version}")

# Check installation method
if os.path.exists('uv.lock'):
    print("Installation method: uv (recommended)")
    print("✅ Using modern package manager")
elif os.path.exists('venv'):
    print("Installation method: venv (traditional)")
    print("ℹ️  Consider migrating to uv for better performance")
else:
    print("Installation method: pip/system")
    print("⚠️  Consider using uv for optimal experience")

# Check async support
try:
    import asyncio
    print("Async support: Available")
    print("✅ Django 6.0+ async features enabled")
except ImportError:
    print("Async support: Not available")
    print("⚠️  Requires Python 3.8+")
```

**Run with uv:**

```bash
uv run python check_django.py
```

**Run with venv:**

```bash
python check_django.py
```

Expected output (2026):

```
Django version: 6.0.1
DRF version: 3.16+
Python version: 3.12.x
Installation method: uv (recommended)
✅ Using modern package manager
Async support: Available
✅ Django 6.0+ async features enabled
```

## Django 6.0 Async Support

### Traditional Sync Views (Still Supported)

```python
# Traditional synchronous view
from django.http import JsonResponse
from .models import Doctor

def doctor_list(request):
    doctors = Doctor.objects.all()
    data = [{'id': d.id, 'name': d.name} for d in doctors]
    return JsonResponse({'doctors': data})
```

### New Async Views (Django 6.0)

```python
# Native async view - cleaner syntax
from django.http import JsonResponse
import asyncio
import aiohttp  # For async HTTP requests

async def async_doctor_list(request):
    # Run database queries concurrently
    doctors = await Doctor.objects.all()
    data = [{'id': d.id, 'name': d.name} for d in doctors]
    
    # Fetch external data concurrently
    async with aiohttp.ClientSession() as session:
        async with session.get('https://api.example.com/data') as response:
            external_data = await response.json()
    
    return JsonResponse({'doctors': data, 'external': external_data})
```

### Mixed Sync/Async Views

```python
# Use async_to_sync for legacy sync operations
from asgiref.sync import sync_to_async

async def hybrid_view(request):
    # Async operation
    external_data = await fetch_external_api()
    
    # Sync database operation wrapped
    doctors = await sync_to_async(list)(Doctor.objects.all())
    
    return JsonResponse({
        'doctors': [{'id': d.id, 'name': d.name} for d in doctors],
        'external': external_data
    })
```

### Save Dependencies

```bash
pip freeze > requirements.txt
```

## Creating a Django Project

### With uv (Recommended)

```bash
# Create project with uv
uv init doctorapi
cd doctorapi

# Add Django first
uv add "django==6.0.1"

# Create Django project using uv
uv run django-admin startproject doctorapi .

# Create apps
uv run python manage.py startapp doctors
uv run python manage.py startapp patients
uv run python manage.py startapp bookings

# Add remaining dependencies
uv add djangorestframework drf-spectacular uvicorn channels
```

### Traditional Approach

```bash
# Create and activate virtual environment
python3 -m venv venv
source venv/bin/activate  # Linux/Mac
# venv\Scripts\activate   # Windows

# Install Django
pip install django==6.0.1

# Create Django project
django-admin startproject doctorapi .

# Create apps
python manage.py startapp doctors
python manage.py startapp patients
python manage.py startapp bookings

# Install remaining packages
pip install djangorestframework drf-spectacular uvicorn channels
```

### Running Development Server

#### With uv (Faster)

```bash
# Traditional server
uv run python manage.py runserver

# Async development server (recommended for API development)
uv run uvicorn doctorapi.asgi:application --reload

# With custom settings
uv run uvicorn doctorapi.asgi:application --reload --host 0.0.0.0 --port 8000
```

#### Traditional Setup

```bash
# Traditional server
python manage.py runserver

# Async server
uvicorn doctorapi.asgi:application --reload
```

### Performance Benefits with uv

Using `uv run` with Django provides:
- 🚀 **2-3x faster server startup**
- 💾 **Reduced memory usage**
- 🔒 **Cleaner dependency isolation**
- 📊 **Better dependency management**
- ⚡ **Faster collectstatic** operations

## Project Structure

```
doctor_api/
├── doctorapp/              # Project configuration
│   ├── __init__.py
│   ├── settings.py         # Configuration
│   ├── urls.py             # Root URLs
│   ├── wsgi.py             # WSGI config
│   └── asgi.py             # ASGI config
├── doctors/                # Doctors app
│   ├── __init__.py
│   ├── admin.py            # Admin registration
│   ├── apps.py             # App config
│   ├── models.py           # Database models
│   ├── serializers.py      # DRF serializers
│   ├── views.py            # API views
│   ├── viewsets.py         # ViewSets
│   ├── urls.py             # App URLs
│   ├── permissions.py      # Custom permissions
│   └── tests.py            # Tests
├── patients/               # Patients app
├── bookings/               # Bookings app
├── manage.py               # Django CLI
├── db.sqlite3              # SQLite database
├── uv.lock                # uv lock file (if using uv)
└── requirements.txt        # Dependencies (pip compatibility)
```

### Modern Project Management

#### With uv (2026 Standard)

```bash
# List installed packages
uv pip list

# Update packages
uv pip install --upgrade django

# Remove packages
uv remove django-debug-toolbar

# Run management commands
uv run python manage.py migrate
uv run python manage.py createsuperuser
uv run python manage.py test
```

#### Package Management Files

**uv.lock** (Modern):
```toml
[[package]]
name = "django"
version = "6.0.1"
source = "registry+https://pypi.org/simple"

[[package]]
name = "djangorestframework"
version = "3.16.0"
```

**requirements.txt** (Traditional):
```txt
django==6.0.1
djangorestframework==3.16.0
drf-spectacular==0.28.0
uvicorn==0.32.0
channels==4.2.0
```

### Development Workflow

#### With uv (Recommended)

```bash
# 1. Create new feature branch
git checkout -b feature/api-endpoints

# 2. Install new dependencies
uv add "pytest-cov" "factory-boy"

# 3. Run tests
uv run python manage.py test
uv run pytest --cov=doctors

# 4. Run server
uv run uvicorn doctorapi.asgi:application --reload

# 5. Commit changes (uv.lock included)
git add .
git commit -m "Add user authentication endpoints"
```

## Configuration

### settings.py (Django 6.0 Configuration)

```python
# doctorapp/settings.py

import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent

INSTALLED_APPS = [
    # Django built-in apps
    'django.contrib.admin',
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.messages',
    'django.contrib.staticfiles',

    # Third-party apps
    'rest_framework',
    'drf_spectacular',
    'channels',  # For WebSocket support

    # Local apps
    'doctors',
    'patients',
    'bookings',
]

# Django 6.0+ configurations
SECURE_BROWSER_XSS_FILTER = True
SECURE_CONTENT_TYPE_NOSNIFF = True

# Content Security Policy (NEW in Django 6.0)
CSP_DEFAULT_SRC = ("'self'",)
CSP_SCRIPT_SRC = ("'self'", "'unsafe-inline'")
CSP_STYLE_SRC = ("'self'", "'unsafe-inline'")

# REST Framework configuration (DRF 3.16+)
REST_FRAMEWORK = {
    'DEFAULT_SCHEMA_CLASS': 'drf_spectacular.openapi.AutoSchema',
    'DEFAULT_AUTHENTICATION_CLASSES': [
        'rest_framework.authentication.SessionAuthentication',
        'rest_framework.authentication.BasicAuthentication',
        'rest_framework.authentication.TokenAuthentication',
    ],
    'DEFAULT_PERMISSION_CLASSES': [
        'rest_framework.permissions.IsAuthenticated',
    ],
    'DEFAULT_PAGINATION_CLASS': 'rest_framework.pagination.PageNumberPagination',
    'PAGE_SIZE': 10,
}

# drf-spectacular configuration
SPECTACULAR_SETTINGS = {
    'TITLE': 'Doctor API',
    'DESCRIPTION': 'Healthcare Management API',
    'VERSION': '1.0.0',
}
```

### Root URLs

```python
# doctorapp/urls.py
from django.contrib import admin
from django.urls import path, include

urlpatterns = [
    path('admin/', admin.site.urls),
    path('api/', include('doctors.urls')),
    path('api/', include('patients.urls')),
    path('api/', include('bookings.urls')),
    path('api-auth/', include('rest_framework.urls')),
]
```

## Running the Application

### Database Migrations

```bash
# Create migrations
python manage.py makemigrations

# Apply migrations
python manage.py migrate
```

### Create Superuser

```bash
python manage.py createsuperuser
```

### Run Development Server

```bash
python manage.py runserver
# Server starts at http://127.0.0.1:8000/
```

## Django Management Commands

| Command | Description |
|---------|-------------|
| `runserver` | Start development server |
| `migrate` | Apply database migrations |
| `makemigrations` | Create new migrations |
| `createsuperuser` | Create admin user |
| `shell` | Interactive Python shell |
| `test` | Run tests |
| `collectstatic` | Collect static files |
| `startapp` | Create new app |

## First API Endpoint

### Model

```python
# doctors/models.py
from django.db import models

class Doctor(models.Model):
    first_name = models.CharField(max_length=100)
    last_name = models.CharField(max_length=100)
    email = models.EmailField()
    qualification = models.CharField(max_length=200)

    def __str__(self):
        return f"Dr. {self.first_name} {self.last_name}"
```

### Serializer

```python
# doctors/serializers.py
from rest_framework import serializers
from .models import Doctor

class DoctorSerializer(serializers.ModelSerializer):
    class Meta:
        model = Doctor
        fields = '__all__'
```

### View

```python
# doctors/views.py
from rest_framework import generics
from .models import Doctor
from .serializers import DoctorSerializer

class DoctorListView(generics.ListCreateAPIView):
    queryset = Doctor.objects.all()
    serializer_class = DoctorSerializer

class DoctorDetailView(generics.RetrieveUpdateDestroyAPIView):
    queryset = Doctor.objects.all()
    serializer_class = DoctorSerializer
```

### URLs

```python
# doctors/urls.py
from django.urls import path
from . import views

urlpatterns = [
    path('doctors/', views.DoctorListView.as_view(), name='doctor-list'),
    path('doctors/<int:pk>/', views.DoctorDetailView.as_view(), name='doctor-detail'),
]
```

## Admin Registration

```python
# doctors/admin.py
from django.contrib import admin
from .models import Doctor

@admin.register(Doctor)
class DoctorAdmin(admin.ModelAdmin):
    list_display = ['first_name', 'last_name', 'email', 'qualification']
    search_fields = ['first_name', 'last_name', 'email']
    list_filter = ['qualification']
```

## Request Flow

```
Client Request
      │
      ▼
┌─────────────────┐
│   URL Router    │  ─── Match URL pattern
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Authentication │  ─── Verify user identity
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Permissions   │  ─── Check access rights
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│      View       │  ─── Process request
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Serializer    │  ─── Validate/Transform data
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│     Model       │  ─── Database operations
└────────┬────────┘
         │
         ▼
    JSON Response
```

## Best Practices

| Practice | Description |
|----------|-------------|
| Use apps | Separate concerns into Django apps |
| Follow naming conventions | models.py, views.py, serializers.py |
| Register in admin | Easy data management |
| Use virtual environments | Isolate dependencies |
| Version control | Use git for code management |
| Environment variables | Never commit secrets |

---

## Next Steps

- [Models](./02-models.md) - Django ORM and relationships

---

[Back to Index](./README.md) | [Next: Models →](./02-models.md)
