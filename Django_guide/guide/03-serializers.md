# Serializers (DRF)

Serializers transform model instances into Python primitives for JSON output, and validate/deserialize input into Python objects.

## Step 1: Choose Serializer Type

- `Serializer`: explicit fields and custom logic.
- `ModelSerializer`: automatic field generation from models.

```python
from rest_framework import serializers


class DoctorSerializer(serializers.Serializer):
    id = serializers.UUIDField(read_only=True)
    first_name = serializers.CharField(max_length=100)
    last_name = serializers.CharField(max_length=100)
    email = serializers.EmailField()
```

```python
from rest_framework import serializers
from .models import Doctor


class DoctorModelSerializer(serializers.ModelSerializer):
    class Meta:
        model = Doctor
        fields = ["id", "first_name", "last_name", "email", "specialty"]
```

## Step 2: Common Field Options

```python
class DoctorSerializer(serializers.ModelSerializer):
    email = serializers.EmailField(
        required=True,
        error_messages={"invalid": "Enter a valid email"},
    )
    password = serializers.CharField(write_only=True, min_length=8)

    class Meta:
        model = Doctor
        fields = ["id", "first_name", "last_name", "email", "password"]
        read_only_fields = ["id"]
```

## Step 3: Validation

### Field-level validation

```python
class DoctorSerializer(serializers.ModelSerializer):
    class Meta:
        model = Doctor
        fields = "__all__"

    def validate_email(self, value):
        if not value.endswith("@example.com"):
            raise serializers.ValidationError("Email must be @example.com")
        return value
```

### Object-level validation

```python
class AvailabilitySerializer(serializers.Serializer):
    start = serializers.DateTimeField()
    end = serializers.DateTimeField()

    def validate(self, data):
        if data["end"] <= data["start"]:
            raise serializers.ValidationError({"end": "End must be after start"})
        return data
```

### Built-in validators

```python
from rest_framework.validators import UniqueTogetherValidator


class AppointmentSerializer(serializers.ModelSerializer):
    class Meta:
        model = Appointment
        fields = ["doctor", "appointment_date", "patient_name"]
        validators = [
            UniqueTogetherValidator(
                queryset=Appointment.objects.all(),
                fields=["doctor", "appointment_date"],
                message="Doctor already booked for that date",
            )
        ]
```

## Step 4: Relationships

### Primary key relationships

```python
class AppointmentSerializer(serializers.ModelSerializer):
    doctor = serializers.PrimaryKeyRelatedField(queryset=Doctor.objects.all())

    class Meta:
        model = Appointment
        fields = ["id", "doctor", "appointment_date"]
```

### Slug and string relationships

```python
class AppointmentSerializer(serializers.ModelSerializer):
    doctor = serializers.SlugRelatedField(slug_field="email", queryset=Doctor.objects.all())

    class Meta:
        model = Appointment
        fields = ["id", "doctor", "appointment_date"]
```

## Step 5: Nested Serialization

### Read nested

```python
class AppointmentReadSerializer(serializers.ModelSerializer):
    doctor = DoctorModelSerializer(read_only=True)

    class Meta:
        model = Appointment
        fields = ["id", "doctor", "appointment_date"]
```

### Write nested (custom create/update)

```python
class PatientSerializer(serializers.ModelSerializer):
    insurance = InsuranceSerializer()

    class Meta:
        model = Patient
        fields = ["first_name", "last_name", "insurance"]

    def create(self, validated_data):
        insurance_data = validated_data.pop("insurance")
        patient = Patient.objects.create(**validated_data)
        Insurance.objects.create(patient=patient, **insurance_data)
        return patient
```

## Step 6: Separate Read and Write Serializers

This pattern keeps input and output clean and prevents data leaks.

```python
class DoctorCreateSerializer(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True)

    class Meta:
        model = Doctor
        fields = ["first_name", "last_name", "email", "password"]


class DoctorReadSerializer(serializers.ModelSerializer):
    class Meta:
        model = Doctor
        fields = ["id", "first_name", "last_name", "email", "specialty"]
```

## Step 7: SerializerMethodField and source

```python
class DoctorReadSerializer(serializers.ModelSerializer):
    full_name = serializers.SerializerMethodField()
    department_name = serializers.CharField(source="department.name", read_only=True)

    class Meta:
        model = Doctor
        fields = ["id", "full_name", "department_name"]

    def get_full_name(self, obj):
        return f"Dr. {obj.first_name} {obj.last_name}"
```

## Step 8: Performance Tips

- Avoid heavy nested serializers on list endpoints.
- Use `select_related` and `prefetch_related` in views.
- Use `.only()` or `.defer()` for large models.

## Common Pitfalls

- Using `depth` for deep nested data can explode queries.
- Forgetting `write_only=True` for sensitive fields.
- Relying on serializer validation instead of database constraints.

## Next Steps

- [Views and ViewSets](./04-views-viewsets.md) - Build API endpoints
- [URLs and Routing](./05-urls-routing.md) - Wire your API

---

[Previous: Models](./02-models.md) | [Back to Index](./README.md) | [Next: Views and ViewSets](./04-views-viewsets.md)
