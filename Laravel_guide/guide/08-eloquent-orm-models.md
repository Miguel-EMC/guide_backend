# 08 - Eloquent ORM and Models

Eloquent is Laravel's Active Record ORM. It provides expressive querying, relationships, and model lifecycle hooks that are ideal for API backends.

## Goals

- Model data with relationships
- Write efficient queries without N+1
- Use scopes, casts, and events

## 1. Creating Models

```bash
php artisan make:model Doctor -mfsc
```

```php
namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;

class Doctor extends Model
{
    use HasFactory, SoftDeletes;

    protected $fillable = [
        'name',
        'email',
        'specialty',
        'is_active',
    ];

    protected $casts = [
        'is_active' => 'boolean',
        'settings' => 'array',
        'salary' => 'decimal:2',
        'email_verified_at' => 'datetime',
    ];
}
```

## 2. Relationships

```php
namespace App\Models;

use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Doctor extends Model
{
    public function appointments(): HasMany
    {
        return $this->hasMany(Appointment::class);
    }

    public function hospital(): BelongsTo
    {
        return $this->belongsTo(Hospital::class);
    }
}
```

Many-to-many:

```php
namespace App\Models;

use Illuminate\Database\Eloquent\Relations\BelongsToMany;

class Doctor extends Model
{
    public function patients(): BelongsToMany
    {
        return $this->belongsToMany(Patient::class)
            ->withPivot(['assigned_at'])
            ->withTimestamps();
    }
}
```

Polymorphic:

```php
namespace App\Models;

use Illuminate\Database\Eloquent\Relations\MorphTo;

class Comment extends Model
{
    public function commentable(): MorphTo
    {
        return $this->morphTo();
    }
}
```

## 3. Eager Loading and N+1

```php
use App\Models\Doctor;

$doctors = Doctor::with('appointments')->get();
$doctor = Doctor::with(['appointments', 'hospital'])->findOrFail($id);
```

Use `loadMissing` for conditional loading:

```php
$doctor->loadMissing('appointments');
```

## 4. Query Scopes

```php
namespace App\Models;

use Illuminate\Database\Eloquent\Builder;

class Doctor extends Model
{
    public function scopeActive(Builder $query): Builder
    {
        return $query->where('is_active', true);
    }
}
```

```php
$active = Doctor::active()->orderBy('name')->get();
```

## 5. Accessors and Mutators

```php
namespace App\Models;

use Illuminate\Database\Eloquent\Casts\Attribute;

class Doctor extends Model
{
    protected function name(): Attribute
    {
        return Attribute::make(
            get: fn ($value) => ucwords($value),
            set: fn ($value) => strtolower($value),
        );
    }
}
```

## 6. Mass Assignment

Use `$fillable` for safety.

```php
Doctor::create($request->validated());
```

## 7. Soft Deletes

```php
use App\Models\Doctor;

$doctor->delete();
$withTrashed = Doctor::withTrashed()->find($id);
$onlyTrashed = Doctor::onlyTrashed()->get();
```

## 8. Advanced Queries

```php
use App\Models\Doctor;

$doctors = Doctor::query()
    ->whereHas('appointments', fn ($q) => $q->where('status', 'pending'))
    ->withCount('appointments')
    ->orderByDesc('appointments_count')
    ->paginate(20);
```

Subquery example:

```php
use App\Models\Appointment;
use App\Models\Doctor;

$doctors = Doctor::query()
    ->addSelect([
        'latest_appointment_at' => Appointment::select('scheduled_at')
            ->whereColumn('doctor_id', 'doctors.id')
            ->latest()
            ->limit(1),
    ])
    ->get();
```

## 9. Chunking and Cursors

```php
use App\Models\Doctor;

Doctor::query()->chunk(500, function ($doctors) {
    foreach ($doctors as $doctor) {
        // process
    }
});
```

```php
use App\Models\Doctor;

foreach (Doctor::cursor() as $doctor) {
    // memory efficient
}
```

## 10. Transactions and Locks

```php
use App\Models\Doctor;
use Illuminate\Support\Facades\DB;

DB::transaction(function () use ($doctorId) {
    $doctor = Doctor::where('id', $doctorId)->lockForUpdate()->first();
    $doctor->update(['is_active' => false]);
});
```

## 11. Model Events and Observers

```php
namespace App\Observers;

use App\Models\AuditLog;
use App\Models\Doctor;

class DoctorObserver
{
    public function created(Doctor $doctor): void
    {
        AuditLog::create(['event' => 'doctor.created', 'id' => $doctor->id]);
    }
}
```

Register in a provider:

```php
Doctor::observe(DoctorObserver::class);
```

## Tips

- Always eager-load relationships used in API responses.
- Prefer scopes over repeated query logic.
- Use chunking for large background tasks.

---

[Previous: Controllers](./07-controllers.md) | [Back to Index](./README.md) | [Next: Database Migrations and Seeding ->](./09-database-migrations-and-seeding.md)
