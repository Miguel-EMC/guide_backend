# Logging

This chapter shows a clean logging setup for Django + DRF.

## Step 1: Basic Logging

```python
# config/settings.py
LOGGING = {
    "version": 1,
    "disable_existing_loggers": False,
    "handlers": {
        "console": {"class": "logging.StreamHandler"},
    },
    "root": {"handlers": ["console"], "level": "INFO"},
}
```

## Step 2: Structured Logs (Optional)

```python
# config/settings.py
LOGGING = {
    "version": 1,
    "disable_existing_loggers": False,
    "formatters": {
        "json": {
            "()": "pythonjsonlogger.jsonlogger.JsonFormatter",
            "format": "%(asctime)s %(levelname)s %(name)s %(message)s",
        },
    },
    "handlers": {
        "console": {
            "class": "logging.StreamHandler",
            "formatter": "json",
        },
    },
    "root": {"handlers": ["console"], "level": "INFO"},
}
```

## Step 3: Request ID Logging

```python
# core/logging.py
import logging


class RequestIdFilter(logging.Filter):
    def filter(self, record):
        record.request_id = getattr(record, "request_id", "-")
        return True
```

Attach the filter to a handler and set a formatter that includes `%(request_id)s`.

## Tips

- Log at INFO in production.
- Avoid logging secrets or tokens.
- Use JSON logs for centralized log platforms.

## References

- [Django Logging](https://docs.djangoproject.com/en/5.2/topics/logging/)

## Next Steps

- [Security](./22-security.md)
- [Parsers and Renderers](./23-parsers-renderers.md)

---

[Previous: Deployment](./20-deployment.md) | [Back to Index](./README.md) | [Next: Security](./22-security.md)
