# File Uploads

This chapter shows how to handle file uploads securely with Django and DRF.

## Step 1: Configure Media Settings

```python
# config/settings.py
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent

MEDIA_URL = "/media/"
MEDIA_ROOT = BASE_DIR / "media"

DATA_UPLOAD_MAX_MEMORY_SIZE = 10 * 1024 * 1024  # 10MB
FILE_UPLOAD_MAX_MEMORY_SIZE = 10 * 1024 * 1024
```

### Serve media in development

```python
# config/urls.py
from django.conf import settings
from django.conf.urls.static import static

urlpatterns = [
    # ...
]

if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
```

## Step 2: Model Fields

```python
# doctors/models.py
import uuid
from django.db import models


def doctor_photo_path(instance, filename):
    return f"doctors/{instance.id}/photo/{filename}"


class Doctor(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    first_name = models.CharField(max_length=100)
    last_name = models.CharField(max_length=100)
    photo = models.ImageField(upload_to=doctor_photo_path, blank=True, null=True)
    cv = models.FileField(upload_to="doctors/cv/", blank=True, null=True)
```

## Step 3: Serializer Validation

```python
# doctors/serializers.py
from rest_framework import serializers
from django.core.validators import FileExtensionValidator


class DoctorFileSerializer(serializers.ModelSerializer):
    photo = serializers.ImageField(required=False)
    cv = serializers.FileField(
        required=False,
        validators=[FileExtensionValidator(allowed_extensions=["pdf", "doc", "docx"])],
    )

    class Meta:
        model = Doctor
        fields = ["id", "photo", "cv"]

    def validate_photo(self, value):
        if value.size > 2 * 1024 * 1024:
            raise serializers.ValidationError("Image must be <= 2MB")
        return value
```

## Step 4: Upload Endpoint

```python
# doctors/views.py
from rest_framework.parsers import MultiPartParser, FormParser
from rest_framework import generics


class DoctorFileUploadView(generics.UpdateAPIView):
    queryset = Doctor.objects.all()
    serializer_class = DoctorFileSerializer
    parser_classes = [MultiPartParser, FormParser]
```

## Step 5: Secure Downloads

```python
from django.http import FileResponse, Http404
from rest_framework.permissions import IsAuthenticated


class DoctorCVDownloadView(generics.GenericAPIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, pk):
        doctor = Doctor.objects.filter(pk=pk).first()
        if not doctor or not doctor.cv:
            raise Http404("File not found")
        return FileResponse(doctor.cv.open("rb"), as_attachment=True)
```

## Optional: Cloud Storage

Django supports pluggable storage backends. For production, use S3 or GCS via `django-storages`.

## Tips

- Validate extensions and file size.
- Store private files outside public media or in private buckets.
- Use unique paths to avoid collisions.
- Consider virus scanning for user uploads.

## References

- [Django Managing Files](https://docs.djangoproject.com/en/5.2/topics/files/)
- [Django File Uploads](https://docs.djangoproject.com/en/5.2/topics/http/file-uploads/)

## Next Steps

- [Celery and Tasks](./17-celery-tasks.md)
- [Middleware](./18-middleware.md)

---

[Previous: Signals](./15-signals.md) | [Back to Index](./README.md) | [Next: Celery and Tasks](./17-celery-tasks.md)
