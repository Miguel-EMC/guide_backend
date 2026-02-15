# 08 - Eloquent ORM and Models

Eloquent is Laravel's Active Record ORM. It provides a fluent, expressive API for querying and modeling database data. This chapter covers model definitions, relationships, query building, and advanced patterns for API development.

## Overview

| Feature | Description |
|---------|-------------|
| Active Record | Each model represents a database row |
| Relationships | hasOne, hasMany, belongsTo, belongsToMany |
| Query Builder | Fluent interface for building queries |
| Accessors/Mutators | Transform attributes on get/set |
| Scopes | Reusable query constraints |
| Events | Hooks into model lifecycle |

## Creating Models

### Generate Model with Migration

```bash
# Model with migration
php artisan make:model Doctor -m

# Model with migration, factory, seeder, and controller
php artisan make:model Doctor -mfsc

# Model with all options
php artisan make:model Doctor --all
```

### Basic Model

```php
<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Doctor extends Model
{
    use HasFactory;

    /**
     * The table associated with the model.
     */
    protected $table = 'doctors';

    /**
     * The primary key for the model.
     */
    protected $primaryKey = 'id';

    /**
     * Indicates if the model's ID is auto-incrementing.
     */
    public $incrementing = true;

    /**
     * The data type of the primary key.
     */
    protected $keyType = 'int';

    /**
     * Indicates if the model should be timestamped.
     */
    public $timestamps = true;

    /**
     * The attributes that are mass assignable.
     */
    protected $fillable = [
        'name',
        'email',
        'specialty',
        'phone',
        'is_active',
    ];

    /**
     * The attributes that should be hidden for serialization.
     */
    protected $hidden = [
        'password',
        'remember_token',
    ];

    /**
     * The attributes that should be cast.
     */
    protected $casts = [
        'is_active' => 'boolean',
        'email_verified_at' => 'datetime',
        'settings' => 'array',
        'salary' => 'decimal:2',
    ];
}
```

## Mass Assignment

### Fillable vs Guarded

```php
// Whitelist approach (recommended)
protected $fillable = [
    'name',
    'email',
    'specialty',
];

// Blacklist approach
protected $guarded = [
    'id',
    'created_at',
    'updated_at',
];

// Allow all (dangerous!)
protected $guarded = [];
```

### Creating Records

```php
// Create using fillable attributes
$doctor = Doctor::create([
    'name' => 'Dr. Smith',
    'email' => 'smith@hospital.com',
    'specialty' => 'Cardiology',
]);

// Create or find
$doctor = Doctor::firstOrCreate(
    ['email' => 'smith@hospital.com'],
    ['name' => 'Dr. Smith', 'specialty' => 'Cardiology']
);

// Create or update
$doctor = Doctor::updateOrCreate(
    ['email' => 'smith@hospital.com'],
    ['name' => 'Dr. John Smith', 'specialty' => 'Cardiology']
);

// Fill and save
$doctor = new Doctor();
$doctor->fill($validatedData);
$doctor->save();
```

## Query Builder

### Basic Queries

```php
// Get all
$doctors = Doctor::all();

// Get with pagination
$doctors = Doctor::paginate(20);

// Find by ID
$doctor = Doctor::find(1);
$doctor = Doctor::findOrFail(1);

// Find by column
$doctor = Doctor::where('email', 'smith@hospital.com')->first();
$doctor = Doctor::where('email', 'smith@hospital.com')->firstOrFail();

// Find multiple
$doctors = Doctor::find([1, 2, 3]);
$doctors = Doctor::whereIn('id', [1, 2, 3])->get();
```

### Where Clauses

```php
// Basic where
$doctors = Doctor::where('specialty', 'Cardiology')->get();

// Multiple conditions
$doctors = Doctor::where('specialty', 'Cardiology')
    ->where('is_active', true)
    ->get();

// Or where
$doctors = Doctor::where('specialty', 'Cardiology')
    ->orWhere('specialty', 'Neurology')
    ->get();

// Where in
$doctors = Doctor::whereIn('specialty', ['Cardiology', 'Neurology'])->get();

// Where not in
$doctors = Doctor::whereNotIn('specialty', ['Dermatology'])->get();

// Where null
$doctors = Doctor::whereNull('deleted_at')->get();

// Where not null
$doctors = Doctor::whereNotNull('email_verified_at')->get();

// Where between
$doctors = Doctor::whereBetween('created_at', [$startDate, $endDate])->get();

// Where date
$doctors = Doctor::whereDate('created_at', '2024-01-01')->get();
$doctors = Doctor::whereYear('created_at', 2024)->get();
$doctors = Doctor::whereMonth('created_at', 1)->get();

// Where column
$doctors = Doctor::whereColumn('created_at', '=', 'updated_at')->get();

// JSON where (for JSON columns)
$doctors = Doctor::where('settings->notifications', true)->get();
```

### Advanced Where

```php
// Grouped conditions
$doctors = Doctor::where('is_active', true)
    ->where(function ($query) {
        $query->where('specialty', 'Cardiology')
            ->orWhere('specialty', 'Neurology');
    })
    ->get();

// When (conditional)
$doctors = Doctor::query()
    ->when($request->specialty, function ($query, $specialty) {
        return $query->where('specialty', $specialty);
    })
    ->when($request->is_active !== null, function ($query) use ($request) {
        return $query->where('is_active', $request->is_active);
    })
    ->paginate(20);
```

### Ordering and Limiting

```php
// Order by
$doctors = Doctor::orderBy('name', 'asc')->get();
$doctors = Doctor::orderBy('created_at', 'desc')->get();
$doctors = Doctor::latest()->get(); // orderBy('created_at', 'desc')
$doctors = Doctor::oldest()->get(); // orderBy('created_at', 'asc')

// Multiple ordering
$doctors = Doctor::orderBy('specialty')
    ->orderBy('name')
    ->get();

// Random order
$doctors = Doctor::inRandomOrder()->get();

// Limit and offset
$doctors = Doctor::limit(10)->offset(20)->get();

// Take
$doctors = Doctor::take(5)->get();
```

### Aggregates

```php
// Count
$count = Doctor::count();
$count = Doctor::where('is_active', true)->count();

// Max, Min, Avg, Sum
$maxSalary = Doctor::max('salary');
$minSalary = Doctor::min('salary');
$avgSalary = Doctor::avg('salary');
$totalSalary = Doctor::sum('salary');

// Exists
$exists = Doctor::where('email', 'smith@hospital.com')->exists();
$doesntExist = Doctor::where('email', 'unknown@hospital.com')->doesntExist();
```

### Selecting Columns

```php
// Select specific columns
$doctors = Doctor::select('id', 'name', 'email')->get();

// Select with alias
$doctors = Doctor::select('name', 'email as contact')->get();

// Add select
$doctors = Doctor::select('id', 'name')
    ->addSelect('email')
    ->get();

// Select raw
$doctors = Doctor::selectRaw('count(*) as total, specialty')
    ->groupBy('specialty')
    ->get();
```

## Relationships

### One to One

```php
// Doctor has one profile
class Doctor extends Model
{
    public function profile(): HasOne
    {
        return $this->hasOne(DoctorProfile::class);
    }
}

// Profile belongs to doctor
class DoctorProfile extends Model
{
    public function doctor(): BelongsTo
    {
        return $this->belongsTo(Doctor::class);
    }
}

// Usage
$doctor = Doctor::find(1);
$profile = $doctor->profile;

$profile = DoctorProfile::find(1);
$doctor = $profile->doctor;
```

### One to Many

```php
// Doctor has many appointments
class Doctor extends Model
{
    public function appointments(): HasMany
    {
        return $this->hasMany(Appointment::class);
    }
}

// Appointment belongs to doctor
class Appointment extends Model
{
    public function doctor(): BelongsTo
    {
        return $this->belongsTo(Doctor::class);
    }
}

// Usage
$doctor = Doctor::find(1);
$appointments = $doctor->appointments;

$appointment = Appointment::find(1);
$doctor = $appointment->doctor;
```

### Many to Many

```php
// Doctor has many specialties
class Doctor extends Model
{
    public function specialties(): BelongsToMany
    {
        return $this->belongsToMany(Specialty::class)
            ->withPivot('certified_at', 'expires_at')
            ->withTimestamps();
    }
}

// Specialty has many doctors
class Specialty extends Model
{
    public function doctors(): BelongsToMany
    {
        return $this->belongsToMany(Doctor::class)
            ->withPivot('certified_at', 'expires_at')
            ->withTimestamps();
    }
}

// Usage
$doctor = Doctor::find(1);
$specialties = $doctor->specialties;

// Attach
$doctor->specialties()->attach($specialtyId);
$doctor->specialties()->attach($specialtyId, ['certified_at' => now()]);
$doctor->specialties()->attach([1, 2, 3]);

// Detach
$doctor->specialties()->detach($specialtyId);
$doctor->specialties()->detach([1, 2, 3]);
$doctor->specialties()->detach(); // Detach all

// Sync (replace all)
$doctor->specialties()->sync([1, 2, 3]);
$doctor->specialties()->sync([1 => ['certified_at' => now()], 2, 3]);

// Sync without detaching
$doctor->specialties()->syncWithoutDetaching([1, 2]);

// Toggle
$doctor->specialties()->toggle([1, 2, 3]);
```

### Has Many Through

```php
// Country has many posts through users
class Country extends Model
{
    public function posts(): HasManyThrough
    {
        return $this->hasManyThrough(Post::class, User::class);
    }
}

// Hospital has many appointments through doctors
class Hospital extends Model
{
    public function appointments(): HasManyThrough
    {
        return $this->hasManyThrough(
            Appointment::class,
            Doctor::class,
            'hospital_id', // Foreign key on doctors table
            'doctor_id',   // Foreign key on appointments table
            'id',          // Local key on hospitals table
            'id'           // Local key on doctors table
        );
    }
}
```

### Polymorphic Relations

```php
// Comment can belong to Post or Video
class Comment extends Model
{
    public function commentable(): MorphTo
    {
        return $this->morphTo();
    }
}

class Post extends Model
{
    public function comments(): MorphMany
    {
        return $this->morphMany(Comment::class, 'commentable');
    }
}

class Video extends Model
{
    public function comments(): MorphMany
    {
        return $this->morphMany(Comment::class, 'commentable');
    }
}

// Migration
Schema::create('comments', function (Blueprint $table) {
    $table->id();
    $table->text('body');
    $table->morphs('commentable'); // Creates commentable_id and commentable_type
    $table->timestamps();
});
```

## Eager Loading

### Prevent N+1 Queries

```php
// N+1 problem (bad)
$doctors = Doctor::all();
foreach ($doctors as $doctor) {
    echo $doctor->profile->bio; // Query for each doctor
}

// Eager loading (good)
$doctors = Doctor::with('profile')->get();
foreach ($doctors as $doctor) {
    echo $doctor->profile->bio; // No additional queries
}
```

### Multiple Relationships

```php
// Load multiple relations
$doctors = Doctor::with(['profile', 'appointments', 'specialties'])->get();

// Nested relations
$doctors = Doctor::with('appointments.patient')->get();

// Multiple nested
$doctors = Doctor::with([
    'profile',
    'appointments.patient',
    'specialties',
])->get();
```

### Constrained Eager Loading

```php
// Filter eager loaded data
$doctors = Doctor::with([
    'appointments' => function ($query) {
        $query->where('status', 'confirmed')
            ->orderBy('scheduled_at');
    },
])->get();

// Select specific columns
$doctors = Doctor::with('appointments:id,doctor_id,patient_id,scheduled_at')->get();
```

### Lazy Eager Loading

```php
// Load relations on already retrieved models
$doctors = Doctor::all();

if ($needAppointments) {
    $doctors->load('appointments');
}

// Load with constraints
$doctors->load([
    'appointments' => function ($query) {
        $query->where('status', 'pending');
    },
]);
```

### Prevent Lazy Loading

```php
// app/Providers/AppServiceProvider.php
use Illuminate\Database\Eloquent\Model;

public function boot(): void
{
    Model::preventLazyLoading(!app()->isProduction());
}
```

## Scopes

### Local Scopes

```php
class Doctor extends Model
{
    public function scopeActive($query)
    {
        return $query->where('is_active', true);
    }

    public function scopeSpecialty($query, string $specialty)
    {
        return $query->where('specialty', $specialty);
    }

    public function scopeAvailable($query, Carbon $date)
    {
        return $query->whereDoesntHave('appointments', function ($q) use ($date) {
            $q->whereDate('scheduled_at', $date);
        });
    }
}

// Usage
$doctors = Doctor::active()->get();
$doctors = Doctor::active()->specialty('Cardiology')->get();
$doctors = Doctor::active()->available(now())->paginate(20);
```

### Global Scopes

```php
// app/Models/Scopes/ActiveScope.php
namespace App\Models\Scopes;

use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Scope;

class ActiveScope implements Scope
{
    public function apply(Builder $builder, Model $model): void
    {
        $builder->where('is_active', true);
    }
}

// In model
class Doctor extends Model
{
    protected static function booted(): void
    {
        static::addGlobalScope(new ActiveScope);
    }
}

// Or inline
class Doctor extends Model
{
    protected static function booted(): void
    {
        static::addGlobalScope('active', function (Builder $builder) {
            $builder->where('is_active', true);
        });
    }
}

// Remove global scope
Doctor::withoutGlobalScope(ActiveScope::class)->get();
Doctor::withoutGlobalScope('active')->get();
Doctor::withoutGlobalScopes()->get();
```

## Accessors and Mutators

### Define Accessors

```php
use Illuminate\Database\Eloquent\Casts\Attribute;

class Doctor extends Model
{
    // New syntax (Laravel 9+)
    protected function fullName(): Attribute
    {
        return Attribute::make(
            get: fn () => "{$this->first_name} {$this->last_name}",
        );
    }

    protected function email(): Attribute
    {
        return Attribute::make(
            get: fn (string $value) => strtolower($value),
            set: fn (string $value) => strtolower($value),
        );
    }

    // With caching
    protected function averageRating(): Attribute
    {
        return Attribute::make(
            get: fn () => $this->ratings()->avg('score'),
        )->shouldCache();
    }
}

// Usage
$doctor->full_name; // "Dr. John Smith"
```

### Append Accessors to JSON

```php
class Doctor extends Model
{
    protected $appends = ['full_name', 'average_rating'];
}

// Or dynamically
$doctor->append('full_name')->toJson();
$doctors->each->append('full_name');
```

## Attribute Casting

### Built-in Casts

```php
protected $casts = [
    'is_active' => 'boolean',
    'settings' => 'array',
    'metadata' => 'object',
    'options' => 'collection',
    'published_at' => 'datetime',
    'birthday' => 'date',
    'secret' => 'encrypted',
    'salary' => 'decimal:2',
    'rating' => 'float',
    'views' => 'integer',
];
```

### Custom Casts

```php
// app/Casts/Money.php
namespace App\Casts;

use Illuminate\Contracts\Database\Eloquent\CastsAttributes;
use App\ValueObjects\Money as MoneyObject;

class Money implements CastsAttributes
{
    public function __construct(
        protected string $currency = 'USD'
    ) {}

    public function get($model, string $key, $value, array $attributes): MoneyObject
    {
        return new MoneyObject(
            $value,
            $this->currency
        );
    }

    public function set($model, string $key, $value, array $attributes): int
    {
        return $value instanceof MoneyObject
            ? $value->cents
            : $value;
    }
}

// Usage
protected $casts = [
    'price' => Money::class . ':USD',
];
```

### Enum Casting

```php
// app/Enums/DoctorStatus.php
enum DoctorStatus: string
{
    case Active = 'active';
    case OnLeave = 'on_leave';
    case Retired = 'retired';
}

// In model
protected $casts = [
    'status' => DoctorStatus::class,
];

// Usage
$doctor->status = DoctorStatus::Active;
$doctor->status->value; // 'active'
```

## Model Events

### Available Events

```php
class Doctor extends Model
{
    protected static function booted(): void
    {
        static::creating(function (Doctor $doctor) {
            $doctor->uuid = Str::uuid();
        });

        static::created(function (Doctor $doctor) {
            // Send welcome notification
        });

        static::updating(function (Doctor $doctor) {
            // Validate changes
        });

        static::updated(function (Doctor $doctor) {
            // Clear cache
            Cache::forget("doctor:{$doctor->id}");
        });

        static::deleting(function (Doctor $doctor) {
            // Cancel appointments
            $doctor->appointments()->pending()->delete();
        });

        static::deleted(function (Doctor $doctor) {
            // Log deletion
        });
    }
}
```

### Observers

```php
// app/Observers/DoctorObserver.php
namespace App\Observers;

use App\Models\Doctor;

class DoctorObserver
{
    public function creating(Doctor $doctor): void
    {
        $doctor->uuid = Str::uuid();
    }

    public function created(Doctor $doctor): void
    {
        // Send notification
    }

    public function updated(Doctor $doctor): void
    {
        Cache::forget("doctor:{$doctor->id}");
    }

    public function deleted(Doctor $doctor): void
    {
        // Cleanup
    }
}

// Register in AppServiceProvider
public function boot(): void
{
    Doctor::observe(DoctorObserver::class);
}
```

## Soft Deletes

```php
use Illuminate\Database\Eloquent\SoftDeletes;

class Doctor extends Model
{
    use SoftDeletes;
}

// Migration
$table->softDeletes();

// Usage
$doctor->delete(); // Soft delete
$doctor->forceDelete(); // Permanent delete
$doctor->restore(); // Restore

// Queries
Doctor::all(); // Excludes soft deleted
Doctor::withTrashed()->get(); // Includes soft deleted
Doctor::onlyTrashed()->get(); // Only soft deleted

// Check if soft deleted
$doctor->trashed(); // true/false
```

## Best Practices

1. **Use eager loading** - Prevent N+1 queries
2. **Define fillable** - Protect against mass assignment
3. **Use scopes** - Reusable query logic
4. **Cast attributes** - Type safety
5. **Use accessors** - Computed properties
6. **Add indexes** - Query performance
7. **Use factories** - Testing and seeding
8. **Prevent lazy loading** - In development
9. **Use observers** - Clean event handling
10. **Document relationships** - PHPDoc comments

## References

- [Eloquent ORM](https://laravel.com/docs/11.x/eloquent)
- [Relationships](https://laravel.com/docs/11.x/eloquent-relationships)
- [Collections](https://laravel.com/docs/11.x/eloquent-collections)

---

[Previous: Controllers](./07-controllers.md) | [Back to Index](./README.md) | [Next: Migrations and Seeding](./09-database-migrations-and-seeding.md)
