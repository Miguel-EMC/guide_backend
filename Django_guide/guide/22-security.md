# Security

This chapter summarizes practical security settings and DRF-specific protections.

## Step 1: Core Django Security Settings

```python
# config/settings.py
DEBUG = False
SECRET_KEY = os.environ.get("SECRET_KEY")
ALLOWED_HOSTS = os.environ.get("ALLOWED_HOSTS", "").split(",")

SECURE_SSL_REDIRECT = True
SECURE_PROXY_SSL_HEADER = ("HTTP_X_FORWARDED_PROTO", "https")

SESSION_COOKIE_SECURE = True
SESSION_COOKIE_HTTPONLY = True
SESSION_COOKIE_SAMESITE = "Lax"

CSRF_COOKIE_SECURE = True
CSRF_COOKIE_HTTPONLY = True
CSRF_COOKIE_SAMESITE = "Lax"

SECURE_HSTS_SECONDS = 31536000
SECURE_HSTS_INCLUDE_SUBDOMAINS = True
SECURE_HSTS_PRELOAD = True

SECURE_CONTENT_TYPE_NOSNIFF = True
X_FRAME_OPTIONS = "DENY"
```

## Step 2: Password Validation

```python
AUTH_PASSWORD_VALIDATORS = [
    {"NAME": "django.contrib.auth.password_validation.UserAttributeSimilarityValidator"},
    {"NAME": "django.contrib.auth.password_validation.MinimumLengthValidator", "OPTIONS": {"min_length": 12}},
    {"NAME": "django.contrib.auth.password_validation.CommonPasswordValidator"},
    {"NAME": "django.contrib.auth.password_validation.NumericPasswordValidator"},
]
```

## Step 3: CORS

Install and configure `django-cors-headers` for browser clients.

```python
INSTALLED_APPS = ["corsheaders", *INSTALLED_APPS]
MIDDLEWARE = ["corsheaders.middleware.CorsMiddleware", *MIDDLEWARE]

CORS_ALLOWED_ORIGINS = [
    "https://app.example.com",
    "http://localhost:3000",
]
```

## Step 4: CSRF for Cookie-Based Auth

If you use session auth, ensure frontend sends CSRF token.

```python
CSRF_TRUSTED_ORIGINS = ["https://app.example.com"]
```

## Step 5: DRF Auth Defaults

```python
REST_FRAMEWORK = {
    "DEFAULT_AUTHENTICATION_CLASSES": [
        "rest_framework.authentication.TokenAuthentication",
        "rest_framework.authentication.SessionAuthentication",
    ],
    "DEFAULT_PERMISSION_CLASSES": [
        "rest_framework.permissions.IsAuthenticated",
    ],
}
```

## Tips

- Use HTTPS everywhere.
- Rotate secrets regularly.
- Never expose admin publicly without IP restrictions.

## References

- [Django Security](https://docs.djangoproject.com/en/5.2/topics/security/)

## Next Steps

- [Parsers and Renderers](./23-parsers-renderers.md)
- [API Schema Generation](./24-api-schema-generation.md)

---

[Previous: Logging](./21-logging.md) | [Back to Index](./README.md) | [Next: Parsers and Renderers](./23-parsers-renderers.md)
