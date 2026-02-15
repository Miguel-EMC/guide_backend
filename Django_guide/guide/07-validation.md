# Validation

Validation in DRF happens at multiple layers: serializer fields, serializer-level validation, and database constraints. This chapter shows the recommended patterns.

## Step 1: Field-Level Validation

Use `validate_<fieldname>` for field-specific checks.

```python
from rest_framework import serializers
from .models import Doctor


class DoctorSerializer(serializers.ModelSerializer):
    class Meta:
        model = Doctor
        fields = ["first_name", "last_name", "email"]

    def validate_email(self, value):
        if not value.endswith("@example.com"):
            raise serializers.ValidationError("Email must be @example.com")
        return value.lower()
```

## Step 2: Object-Level Validation

Use `validate(self, attrs)` for cross-field rules.

```python
class AvailabilitySerializer(serializers.Serializer):
    start = serializers.DateTimeField()
    end = serializers.DateTimeField()

    def validate(self, attrs):
        if attrs["end"] <= attrs["start"]:
            raise serializers.ValidationError({"end": "End must be after start"})
        return attrs
```

## Step 3: Built-in Validators

DRF provides validators such as `UniqueValidator` and `UniqueTogetherValidator`.

```python
from rest_framework.validators import UniqueValidator, UniqueTogetherValidator


class AppointmentSerializer(serializers.ModelSerializer):
    patient_email = serializers.EmailField(
        validators=[UniqueValidator(queryset=Appointment.objects.all())]
    )

    class Meta:
        model = Appointment
        fields = ["doctor", "appointment_date", "patient_email"]
        validators = [
            UniqueTogetherValidator(
                queryset=Appointment.objects.all(),
                fields=["doctor", "appointment_date"],
                message="Doctor already booked for that date",
            )
        ]
```

## Step 4: Custom Validators

```python
from rest_framework import serializers
from datetime import date


def validate_future_date(value):
    if value < date.today():
        raise serializers.ValidationError("Date must be in the future")
    return value
```

```python
class AppointmentSerializer(serializers.ModelSerializer):
    appointment_date = serializers.DateField(validators=[validate_future_date])

    class Meta:
        model = Appointment
        fields = ["appointment_date", "doctor"]
```

## Step 5: Raise Exceptions Correctly

```python
serializer = DoctorSerializer(data=request.data)
serializer.is_valid(raise_exception=True)
```

When validation fails, DRF returns a `400` response with field errors and `non_field_errors` for object-level issues.

## Step 6: Database Constraints Still Matter

Always enforce critical constraints in the database using unique constraints and check constraints.

```python
class Appointment(models.Model):
    doctor = models.ForeignKey(Doctor, on_delete=models.CASCADE)
    appointment_date = models.DateField()

    class Meta:
        constraints = [
            models.UniqueConstraint(
                fields=["doctor", "appointment_date"],
                name="uniq_doctor_date",
            )
        ]
```

## Tips

- Keep validation close to the serializer.
- Use database constraints for real integrity.
- Avoid heavy queries inside validation on list endpoints.

## References

- [DRF Serializer Validation](https://www.django-rest-framework.org/api-guide/serializers/#validation)
- [DRF Validators](https://www.django-rest-framework.org/api-guide/validators/)

## Next Steps

- [API Documentation](./08-api-documentation.md) - OpenAPI and Swagger
- [Testing](./09-testing.md) - Unit and API tests

---

[Previous: Authentication and Permissions](./06-authentication-permissions.md) | [Back to Index](./README.md) | [Next: API Documentation](./08-api-documentation.md)
