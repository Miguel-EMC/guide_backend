# Security

This chapter covers comprehensive security hardening for Django + DRF APIs, including OWASP protections, authentication security, and production best practices.

## Security Overview

| Category | Protections |
|----------|-------------|
| Transport | HTTPS, HSTS, secure cookies |
| Authentication | Strong passwords, rate limiting, JWT security |
| Authorization | Permission checks, object-level access |
| Input Validation | Serializer validation, SQL injection prevention |
| Output | XSS prevention, content type headers |
| Infrastructure | Secret management, dependency scanning |

## Core Django Security Settings

```python
# config/settings.py
import os

# CRITICAL: Never expose in production
DEBUG = False
SECRET_KEY = os.environ.get("SECRET_KEY")
ALLOWED_HOSTS = os.environ.get("ALLOWED_HOSTS", "").split(",")

# HTTPS and SSL
SECURE_SSL_REDIRECT = True
SECURE_PROXY_SSL_HEADER = ("HTTP_X_FORWARDED_PROTO", "https")

# HTTP Strict Transport Security (HSTS)
SECURE_HSTS_SECONDS = 31536000  # 1 year
SECURE_HSTS_INCLUDE_SUBDOMAINS = True
SECURE_HSTS_PRELOAD = True

# Session Security
SESSION_COOKIE_SECURE = True
SESSION_COOKIE_HTTPONLY = True
SESSION_COOKIE_SAMESITE = "Lax"
SESSION_COOKIE_AGE = 3600  # 1 hour
SESSION_EXPIRE_AT_BROWSER_CLOSE = True

# CSRF Security
CSRF_COOKIE_SECURE = True
CSRF_COOKIE_HTTPONLY = True
CSRF_COOKIE_SAMESITE = "Lax"
CSRF_TRUSTED_ORIGINS = ["https://app.example.com"]

# Content Security
SECURE_CONTENT_TYPE_NOSNIFF = True
X_FRAME_OPTIONS = "DENY"
SECURE_BROWSER_XSS_FILTER = True

# Referrer Policy
SECURE_REFERRER_POLICY = "strict-origin-when-cross-origin"
```

## Password Security

### Password Validators

```python
# config/settings.py
AUTH_PASSWORD_VALIDATORS = [
    {
        "NAME": "django.contrib.auth.password_validation.UserAttributeSimilarityValidator",
    },
    {
        "NAME": "django.contrib.auth.password_validation.MinimumLengthValidator",
        "OPTIONS": {"min_length": 12},
    },
    {
        "NAME": "django.contrib.auth.password_validation.CommonPasswordValidator",
    },
    {
        "NAME": "django.contrib.auth.password_validation.NumericPasswordValidator",
    },
]
```

### Custom Password Validator

```python
# core/validators.py
from django.core.exceptions import ValidationError
import re


class ComplexityValidator:
    """Require uppercase, lowercase, digit, and special character."""

    def validate(self, password, user=None):
        if not re.search(r"[A-Z]", password):
            raise ValidationError("Password must contain uppercase letter.")
        if not re.search(r"[a-z]", password):
            raise ValidationError("Password must contain lowercase letter.")
        if not re.search(r"\d", password):
            raise ValidationError("Password must contain digit.")
        if not re.search(r"[!@#$%^&*(),.?\":{}|<>]", password):
            raise ValidationError("Password must contain special character.")

    def get_help_text(self):
        return "Password must contain uppercase, lowercase, digit, and special character."
```

### Password Hashing

```python
# config/settings.py
PASSWORD_HASHERS = [
    "django.contrib.auth.hashers.Argon2PasswordHasher",  # Recommended
    "django.contrib.auth.hashers.PBKDF2PasswordHasher",
    "django.contrib.auth.hashers.PBKDF2SHA1PasswordHasher",
]
```

```bash
# Install argon2
uv add argon2-cffi
```

## CORS Configuration

```bash
uv add django-cors-headers
```

```python
# config/settings.py
INSTALLED_APPS = [
    "corsheaders",
    # ...
]

MIDDLEWARE = [
    "corsheaders.middleware.CorsMiddleware",  # Must be before CommonMiddleware
    "django.middleware.common.CommonMiddleware",
    # ...
]

# Specific origins (recommended for production)
CORS_ALLOWED_ORIGINS = [
    "https://app.example.com",
    "https://admin.example.com",
]

# For development only
# CORS_ALLOW_ALL_ORIGINS = True  # Never in production!

CORS_ALLOW_CREDENTIALS = True

CORS_ALLOWED_HEADERS = [
    "accept",
    "accept-encoding",
    "authorization",
    "content-type",
    "origin",
    "x-csrftoken",
    "x-request-id",
]

CORS_EXPOSE_HEADERS = [
    "x-request-id",
]
```

## DRF Authentication Security

### Token Authentication

```python
# config/settings.py
REST_FRAMEWORK = {
    "DEFAULT_AUTHENTICATION_CLASSES": [
        "rest_framework.authentication.TokenAuthentication",
    ],
    "DEFAULT_PERMISSION_CLASSES": [
        "rest_framework.permissions.IsAuthenticated",
    ],
}
```

### JWT Authentication with SimpleJWT

```bash
uv add djangorestframework-simplejwt
```

```python
# config/settings.py
from datetime import timedelta

REST_FRAMEWORK = {
    "DEFAULT_AUTHENTICATION_CLASSES": [
        "rest_framework_simplejwt.authentication.JWTAuthentication",
    ],
}

SIMPLE_JWT = {
    "ACCESS_TOKEN_LIFETIME": timedelta(minutes=15),
    "REFRESH_TOKEN_LIFETIME": timedelta(days=7),
    "ROTATE_REFRESH_TOKENS": True,
    "BLACKLIST_AFTER_ROTATION": True,
    "UPDATE_LAST_LOGIN": True,

    "ALGORITHM": "HS256",
    "SIGNING_KEY": SECRET_KEY,

    "AUTH_HEADER_TYPES": ("Bearer",),
    "AUTH_HEADER_NAME": "HTTP_AUTHORIZATION",

    "USER_ID_FIELD": "id",
    "USER_ID_CLAIM": "user_id",

    # Token blacklisting
    "TOKEN_BLACKLIST_ENABLED": True,
}
```

```python
# config/urls.py
from rest_framework_simplejwt.views import (
    TokenObtainPairView,
    TokenRefreshView,
    TokenBlacklistView,
)

urlpatterns = [
    path("api/token/", TokenObtainPairView.as_view(), name="token_obtain_pair"),
    path("api/token/refresh/", TokenRefreshView.as_view(), name="token_refresh"),
    path("api/token/blacklist/", TokenBlacklistView.as_view(), name="token_blacklist"),
]
```

## Rate Limiting for Authentication

```python
# config/settings.py
REST_FRAMEWORK = {
    "DEFAULT_THROTTLE_CLASSES": [
        "rest_framework.throttling.ScopedRateThrottle",
    ],
    "DEFAULT_THROTTLE_RATES": {
        "login": "5/minute",
        "password_reset": "3/hour",
        "registration": "10/hour",
    },
}
```

```python
# auth/views.py
from rest_framework.throttling import ScopedRateThrottle

class LoginView(APIView):
    throttle_scope = "login"
    throttle_classes = [ScopedRateThrottle]
    permission_classes = [AllowAny]

    def post(self, request):
        # Login logic
        pass


class PasswordResetView(APIView):
    throttle_scope = "password_reset"
    throttle_classes = [ScopedRateThrottle]
    permission_classes = [AllowAny]

    def post(self, request):
        # Password reset logic
        pass
```

## Input Validation and SQL Injection Prevention

### Serializer Validation

```python
# doctors/serializers.py
from rest_framework import serializers
import re


class DoctorSerializer(serializers.ModelSerializer):
    class Meta:
        model = Doctor
        fields = ["id", "name", "email", "phone"]

    def validate_email(self, value):
        # Normalize email
        return value.lower().strip()

    def validate_phone(self, value):
        # Remove non-numeric characters
        cleaned = re.sub(r"[^\d+]", "", value)
        if len(cleaned) < 10:
            raise serializers.ValidationError("Invalid phone number")
        return cleaned

    def validate_name(self, value):
        # Prevent XSS in name fields
        if re.search(r"[<>\"']", value):
            raise serializers.ValidationError("Invalid characters in name")
        return value.strip()
```

### Safe Queryset Filtering

```python
# NEVER do this - SQL injection vulnerable
# Doctor.objects.raw(f"SELECT * FROM doctors WHERE name = '{name}'")

# SAFE: Use ORM
Doctor.objects.filter(name=name)

# SAFE: Use parameterized raw queries
Doctor.objects.raw(
    "SELECT * FROM doctors WHERE name = %s",
    [name]
)

# SAFE: Use F() and Q() objects
from django.db.models import F, Q

Doctor.objects.filter(
    Q(salary__gt=F("minimum_salary")) & Q(is_active=True)
)
```

## Object-Level Permissions

```python
# core/permissions.py
from rest_framework.permissions import BasePermission, SAFE_METHODS


class IsOwnerOrReadOnly(BasePermission):
    """Object owner can edit, others can only read."""

    def has_object_permission(self, request, view, obj):
        if request.method in SAFE_METHODS:
            return True
        return obj.created_by == request.user


class IsAdminOrOwner(BasePermission):
    """Admin or object owner can access."""

    def has_object_permission(self, request, view, obj):
        return request.user.is_staff or obj.user == request.user


class DenyAll(BasePermission):
    """Deny all access - useful for disabling endpoints."""

    def has_permission(self, request, view):
        return False
```

```python
# doctors/views.py
from core.permissions import IsOwnerOrReadOnly

class DoctorViewSet(viewsets.ModelViewSet):
    permission_classes = [IsAuthenticated, IsOwnerOrReadOnly]

    def get_queryset(self):
        # Filter to only user's doctors (data isolation)
        if self.request.user.is_staff:
            return Doctor.objects.all()
        return Doctor.objects.filter(organization=self.request.user.organization)
```

## Security Headers Middleware

```python
# core/middleware.py
class SecurityHeadersMiddleware:
    """Add additional security headers."""

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        response = self.get_response(request)

        # Content Security Policy
        response["Content-Security-Policy"] = (
            "default-src 'self'; "
            "script-src 'self'; "
            "style-src 'self' 'unsafe-inline'; "
            "img-src 'self' data: https:; "
            "font-src 'self'; "
            "frame-ancestors 'none';"
        )

        # Permissions Policy
        response["Permissions-Policy"] = (
            "geolocation=(), "
            "microphone=(), "
            "camera=()"
        )

        # Additional headers
        response["X-Content-Type-Options"] = "nosniff"
        response["X-Frame-Options"] = "DENY"
        response["X-XSS-Protection"] = "1; mode=block"

        return response
```

## Secret Management

### Environment Variables

```python
# config/settings.py
import os
from pathlib import Path

# Use environment variables for all secrets
SECRET_KEY = os.environ["SECRET_KEY"]
DATABASE_URL = os.environ["DATABASE_URL"]
REDIS_URL = os.environ.get("REDIS_URL", "redis://localhost:6379/0")

# Never hardcode secrets
# SECRET_KEY = "hardcoded-secret"  # NEVER DO THIS
```

### Using django-environ

```bash
uv add django-environ
```

```python
# config/settings.py
import environ

env = environ.Env(
    DEBUG=(bool, False),
)

# Read .env file
environ.Env.read_env()

DEBUG = env("DEBUG")
SECRET_KEY = env("SECRET_KEY")
DATABASES = {"default": env.db()}
```

## Dependency Security Scanning

### pip-audit

```bash
uv add --dev pip-audit

# Scan for vulnerabilities
uv run pip-audit
```

### Safety

```bash
uv add --dev safety

# Check dependencies
uv run safety check
```

### Bandit (Code Security)

```bash
uv add --dev bandit

# Scan Python code
uv run bandit -r . -x tests/
```

## Admin Security

```python
# config/urls.py
from django.contrib import admin

# Change admin URL (security through obscurity, but helps)
urlpatterns = [
    path("secret-admin-panel/", admin.site.urls),
]

# config/settings.py
# Restrict admin to specific IPs in production
INTERNAL_IPS = ["127.0.0.1"]
```

```python
# core/middleware.py
class AdminIPRestrictionMiddleware:
    """Restrict admin access to specific IPs."""

    ALLOWED_IPS = ["10.0.0.0/8", "192.168.0.0/16"]

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        if request.path.startswith("/admin/"):
            client_ip = self.get_client_ip(request)
            if not self.is_ip_allowed(client_ip):
                from django.http import HttpResponseForbidden
                return HttpResponseForbidden("Access denied")
        return self.get_response(request)

    def get_client_ip(self, request):
        x_forwarded_for = request.META.get("HTTP_X_FORWARDED_FOR")
        if x_forwarded_for:
            return x_forwarded_for.split(",")[0].strip()
        return request.META.get("REMOTE_ADDR")

    def is_ip_allowed(self, ip):
        import ipaddress
        client_ip = ipaddress.ip_address(ip)
        for allowed in self.ALLOWED_IPS:
            if client_ip in ipaddress.ip_network(allowed):
                return True
        return False
```

## Sensitive Data Protection

### Model Field Encryption

```bash
uv add django-encrypted-model-fields
```

```python
from encrypted_model_fields.fields import EncryptedCharField

class Patient(models.Model):
    name = models.CharField(max_length=100)
    ssn = EncryptedCharField(max_length=11)  # Encrypted at rest
```

### Logging Sanitization

```python
# core/logging.py
import re

SENSITIVE_PATTERNS = [
    (r'"password"\s*:\s*"[^"]*"', '"password": "[REDACTED]"'),
    (r'"token"\s*:\s*"[^"]*"', '"token": "[REDACTED]"'),
    (r'"secret"\s*:\s*"[^"]*"', '"secret": "[REDACTED]"'),
    (r'"ssn"\s*:\s*"[^"]*"', '"ssn": "[REDACTED]"'),
]


def sanitize_log_message(message: str) -> str:
    for pattern, replacement in SENSITIVE_PATTERNS:
        message = re.sub(pattern, replacement, message, flags=re.IGNORECASE)
    return message
```

## Security Checklist

### Authentication

- [ ] Strong password requirements (12+ chars, complexity)
- [ ] Rate limiting on login/registration
- [ ] JWT with short access token lifetime
- [ ] Token blacklisting for logout
- [ ] Secure password reset flow

### Authorization

- [ ] Default to deny (IsAuthenticated)
- [ ] Object-level permissions
- [ ] Data isolation per organization/user
- [ ] Admin access restricted

### Transport

- [ ] HTTPS enforced (SECURE_SSL_REDIRECT)
- [ ] HSTS enabled
- [ ] Secure cookies

### Headers

- [ ] Content-Security-Policy
- [ ] X-Frame-Options: DENY
- [ ] X-Content-Type-Options: nosniff

### Data

- [ ] Input validation on all endpoints
- [ ] Parameterized queries only
- [ ] Sensitive fields encrypted
- [ ] Logs sanitized

### Dependencies

- [ ] Regular pip-audit scans
- [ ] Bandit code analysis
- [ ] Automated security updates

## References

- [Django Security](https://docs.djangoproject.com/en/5.2/topics/security/)
- [DRF Authentication](https://www.django-rest-framework.org/api-guide/authentication/)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Django Security Checklist](https://docs.djangoproject.com/en/5.2/howto/deployment/checklist/)

## Next Steps

- [Parsers and Renderers](./23-parsers-renderers.md)
- [API Schema Generation](./24-api-schema-generation.md)

---

[Previous: Logging](./21-logging.md) | [Back to Index](./README.md) | [Next: Parsers and Renderers](./23-parsers-renderers.md)
