# Django 6.0 Advanced Features

This chapter covers the most significant new features and improvements in Django 6.0 that are essential for modern 2026 applications.

## Native Async Views

Django 6.0 introduces first-class async view support, eliminating the need for `sync_to_async` boilerplate.

### Why Async Views Matter

| Feature | Sync Views | Async Views |
|---------|-------------|-------------|
| **I/O Operations** | Blocking, one-at-a-time | Non-blocking, concurrent |
| **Database Queries** | Sequential | Can run concurrently |
| **External APIs** | Slow, blocking | Fast, non-blocking |
| **Memory Usage** | Higher per request | Lower, more efficient |
| **Concurrency** | Thread-based | Event loop-based |

### Creating Async Views

```python
# views.py
from django.http import JsonResponse
import asyncio
import aiohttp
from asgiref.sync import sync_to_async
from .models import Doctor, Patient

async def dashboard_stats(request):
    """Async dashboard with concurrent data fetching"""
    
    # Fetch data concurrently
    doctors_task = asyncio.create_task(
        sync_to_async(list)(Doctor.objects.all())
    )
    patients_task = asyncio.create_task(
        sync_to_async(list)(Patient.objects.all())
    )
    
    # External API call
    async with aiohttp.ClientSession() as session:
        weather_task = asyncio.create_task(
            session.get('https://api.weather.com/current')
        )
    
    # Wait for all tasks
    doctors, patients, weather_response = await asyncio.gather(
        doctors_task, patients_task, weather_task
    )
    
    weather_data = await weather_response.json()
    
    return JsonResponse({
        'doctors_count': len(doctors),
        'patients_count': len(patients),
        'weather': weather_data,
        'performance': 'async'
    })
```

### Async Database Operations

```python
# models.py
from django.db import models

class Doctor(models.Model):
    name = models.CharField(max_length=100)
    specialty = models.CharField(max_length=50)
    
    def __str__(self):
        return self.name

# Async query operations
async def get_doctors_async(specialty=None):
    if specialty:
        return await Doctor.objects.filter(specialty=specialty).all()
    return await Doctor.objects.all()

async def create_doctor_async(data):
    doctor = Doctor(**data)
    await doctor.asave()
    return doctor
```

## Content Security Policy (CSP)

Django 6.0 includes built-in CSP support for enhanced security.

### Basic CSP Configuration

```python
# settings.py

# Enable CSP middleware
MIDDLEWARE = [
    'django.middleware.security.SecurityMiddleware',
    # ... other middleware
]

# CSP configuration
CSP_DEFAULT_SRC = ("'self'",)
CSP_SCRIPT_SRC = ("'self'", "https://trusted.cdn.com")
CSP_STYLE_SRC = ("'self'", "'unsafe-inline'")
CSP_IMG_SRC = ("'self'", "data:", "https:")
CSP_FONT_SRC = ("'self'", "https://fonts.googleapis.com")
CSP_CONNECT_SRC = ("'self'", "https://api.example.com")

# Report violations (optional)
CSP_REPORT_URI = '/csp-report/'
```

### View-specific CSP

```python
# views.py
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from django.utils.decorators import method_decorator

@method_decorator(csrf_exempt, name='dispatch')
class CSPReportView(View):
    def post(self, request):
        # Log CSP violations
        violation = request.body.decode('utf-8')
        logger.warning(f"CSP Violation: {violation}")
        return JsonResponse({'status': 'recorded'})
```

## Enhanced Type Safety

Django 6.0 improves type hint support throughout the framework.

### Typed Models

```python
# models.py
from django.db import models
from typing import Optional

class Patient(models.Model):
    first_name: str = models.CharField(max_length=50)
    last_name: str = models.CharField(max_length=50)
    email: str = models.EmailField()
    birth_date: datetime = models.DateField()
    doctor: 'Doctor' = models.ForeignKey(Doctor, on_delete=models.CASCADE)
    
    class Meta:
        # New: Typed model metadata
        verbose_name: str = "Patient"
        verbose_name_plural: str = "Patients"

# Typed queryset operations
def get_adult_patients() -> models.QuerySet[Patient]:
    return Patient.objects.filter(
        birth_date__lte=timezone.now() - timedelta(years=18)
    )
```

### Typed Views

```python
# views.py
from django.http import HttpRequest, JsonResponse
from django.views import View
from typing import Dict, Any

class TypedPatientView(View):
    def get(self, request: HttpRequest) -> JsonResponse:
        patients = get_adult_patients()
        data: Dict[str, Any] = {
            'count': patients.count(),
            'patients': list(patients.values())
        }
        return JsonResponse(data)
```

## Performance Improvements

Django 6.0 includes significant performance enhancements.

### Query Optimization

```python
# views.py
from django.db import models
from django.views.decorators.cache import cache_page

async def optimized_patient_list(request):
    """Optimized async patient list with prefetch"""
    
    # Use prefetch_related in async context
    patients = await Patient.objects.select_related(
        'doctor'
    ).prefetch_related(
        'appointments'
    ).all()
    
    # Efficient serialization
    data = []
    for patient in patients:
        data.append({
            'id': patient.id,
            'name': f"{patient.first_name} {patient.last_name}",
            'doctor': {
                'id': patient.doctor.id,
                'name': patient.doctor.name
            },
            'appointments_count': patient.appointments.count()
        })
    
    return JsonResponse({'patients': data})

# Cache expensive queries
@cache_page(60 * 15)  # 15 minutes
async def get_specialty_stats(request):
    """Cached specialty statistics"""
    
    specialties = await Doctor.objects.values(
        'specialty'
    ).annotate(
        count=models.Count('id')
    ).order_by('-count')
    
    return JsonResponse({'specialties': list(specialties)})
```

## WebSocket Support with Channels 4.x

Enhanced WebSocket integration for real-time applications.

### Async Consumers

```python
# consumers.py
import json
from channels.generic.websocket import AsyncWebsocketConsumer
from channels.db import database_sync_to_async
from .models import Appointment

class AppointmentConsumer(AsyncWebsocketConsumer):
    async def connect(self):
        self.user_id = self.scope["user"].id
        self.group_name = f"user_{self.user_id}"
        
        # Join room group
        await self.channel_layer.group_add(
            self.group_name,
            self.channel_name
        )
        await self.accept()
    
    async def disconnect(self, close_code):
        # Leave room group
        await self.channel_layer.group_discard(
            self.group_name,
            self.channel_name
        )
    
    async def receive(self, text_data):
        text_data_json = json.loads(text_data)
        message_type = text_data_json['type']
        
        if message_type == 'create_appointment':
            appointment = await self.create_appointment(text_data_json)
            
            # Notify user group
            await self.channel_layer.group_send(
                self.group_name,
                {
                    'type': 'appointment_update',
                    'appointment': appointment
                }
            )
    
    async def appointment_update(self, event):
        # Send message to WebSocket
        await self.send(text_data=json.dumps({
            'appointment': event['appointment']
        }))
    
    @database_sync_to_async
    def create_appointment(self, data):
        return Appointment.objects.create(
            patient_id=data['patient_id'],
            doctor_id=data['doctor_id'],
            datetime=data['datetime']
        )
```

## Enhanced Security Features

### Advanced Authentication

```python
# authentication.py
from rest_framework.authentication import BaseAuthentication
from rest_framework.exceptions import AuthenticationFailed
from django.contrib.auth.models import User
import jwt
from django.conf import settings

class JWTAuthentication(BaseAuthentication):
    def authenticate(self, request):
        auth_header = request.META.get('HTTP_AUTHORIZATION')
        
        if not auth_header or not auth_header.startswith('Bearer '):
            return None
        
        token = auth_header.split(' ')[1]
        
        try:
            payload = jwt.decode(
                token, 
                settings.SECRET_KEY, 
                algorithms=['HS256']
            )
            user = User.objects.get(id=payload['user_id'])
            return (user, token)
        except jwt.ExpiredSignatureError:
            raise AuthenticationFailed('Token expired')
        except jwt.InvalidTokenError:
            raise AuthenticationFailed('Invalid token')
```

### Rate Limiting

```python
# throttling.py
from rest_framework.throttling import SimpleRateThrottle
from django.core.cache import cache

class CustomRateThrottle(SimpleRateThrottle):
    scope = 'custom'
    
    def get_cache_key(self, request, view):
        ident = self.get_ident(request)
        return f'{self.scope}_{ident}'
    
    def throttle_success(self):
        # Custom logic for successful requests
        cache.incr(f'api_requests_{self.get_ident(self.request)}')
        return super().throttle_success()
    
    def wait(self):
        # Custom wait time
        return super().wait()
```

## Testing Async Code

### Async Test Cases

```python
# tests.py
from django.test import TestCase, AsyncClient
from django.urls import reverse
from asgiref.testing import ApplicationCommunicator
import json

class AsyncPatientTests(TestCase):
    async def asyncSetUp(self):
        self.client = AsyncClient()
        
        # Create test data
        self.doctor = await Doctor.objects.acreate(
            name="Dr. Smith",
            specialty="Cardiology"
        )
    
    async def test_async_patient_list(self):
        """Test async patient list endpoint"""
        response = await self.client.get('/api/patients/')
        
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertIn('patients', data)
    
    async def test_async_appointment_creation(self):
        """Test async appointment creation"""
        appointment_data = {
            'patient_id': 1,
            'doctor_id': self.doctor.id,
            'datetime': '2026-02-02T10:00:00Z'
        }
        
        response = await self.client.post(
            '/api/appointments/',
            data=json.dumps(appointment_data),
            content_type='application/json'
        )
        
        self.assertEqual(response.status_code, 201)
        
        # Verify creation
        created = await Appointment.objects.aget(id=response.json()['id'])
        self.assertEqual(created.doctor.id, self.doctor.id)
```

## Deployment Considerations

### ASGI Configuration

```python
# asgi.py
import os
from django.core.asgi import get_asgi_application
from channels.routing import ProtocolTypeRouter, URLRouter
from channels.auth import AuthMiddlewareStack
from myapp.routing import websocket_urlpatterns

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'myproject.settings')

application = ProtocolTypeRouter({
    "http": get_asgi_application(),
    "websocket": AuthMiddlewareStack(
        URLRouter(websocket_urlpatterns)
    ),
})
```

### Production Server Setup

```bash
# Using uvicorn for async Django
pip install uvicorn

# Development
uvicorn myproject.asgi:application --reload

# Production
uvicorn myproject.asgi:application \
    --host 0.0.0.0 \
    --port 8000 \
    --workers 4 \
    --worker-class uvicorn.workers.UvicornWorker
```

## Migration Guide for Django 6.0

### Breaking Changes

1. **Python 3.12+ Required**
   ```python
   # Old: Python 3.8+
   # New: Python 3.12+ required
   ```

2. **Async Views First-Class**
   ```python
   # Old: sync_to_async wrapper needed
   async def my_view(request):
       data = await sync_to_async(my_sync_operation)()
   
   # New: Native async supported
   async def my_view(request):
       data = await my_async_operation()
   ```

3. **CSP Middleware**
   ```python
   # Old: Third-party CSP packages
   # New: Built-in CSP support
   ```

### Upgrade Steps

1. **Update Python**: Upgrade to Python 3.12+
2. **Update Dependencies**:
   ```bash
   pip install --upgrade django==6.0.1
   pip install --upgrade djangorestframework
   ```
3. **Update Settings**: Add CSP configurations
4. **Test Async Views**: Convert performance-critical views to async
5. **Run Tests**: Ensure all tests pass
6. **Deploy**: Use ASGI server (uvicorn/hypercorn)

## Best Practices for Django 6.0

### Do's

```python
# ✅ Use async for I/O operations
async def data_intensive_view(request):
    external_data = await fetch_external_api()
    db_data = await MyModel.objects.all()
    return JsonResponse({'data': external_data, 'db': db_data})

# ✅ Enable CSP headers
CSP_DEFAULT_SRC = ("'self'",)
CSP_SCRIPT_SRC = ("'self'", "https://trusted.cdn.com")

# ✅ Use type hints
from typing import List
def get_patients() -> List[Patient]:
    return Patient.objects.all()
```

### Don'ts

```python
# ❌ Don't use Python < 3.12
# Django 6.0 requires Python 3.12+

# ❌ Don't ignore CSP
# Always configure Content Security Policy

# ❌ Don't mix sync/async unnecessarily
# Convert to async when it provides benefit
```

## Next Steps

- [Models & Database](./02-models.md) - Advanced Django ORM
- [Async Patterns](./async-patterns.md) - Master async programming
- [Security](./22-security.md) - Advanced security features

---

[Back to Index](./README.md) | [Next: Models](./02-models.md)