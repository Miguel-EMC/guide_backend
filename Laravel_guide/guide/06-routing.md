# 06 - Routing

Routing maps HTTP requests to controllers. Laravel's routing system is expressive, fast, and provides powerful features for building RESTful APIs.

## Overview

| Route Type | Use Case |
|-----------|----------|
| Basic Routes | Simple endpoints |
| Resource Routes | CRUD operations |
| API Resource Routes | RESTful APIs |
| Grouped Routes | Shared middleware/prefix |
| Nested Routes | Parent-child resources |

## Basic Routes

### Defining Routes

```php
// routes/api.php
use Illuminate\Support\Facades\Route;

// GET request
Route::get('/health', function () {
    return response()->json(['status' => 'ok']);
});

// POST request
Route::post('/users', [UserController::class, 'store']);

// PUT request
Route::put('/users/{id}', [UserController::class, 'update']);

// PATCH request
Route::patch('/users/{id}', [UserController::class, 'update']);

// DELETE request
Route::delete('/users/{id}', [UserController::class, 'destroy']);

// Multiple methods
Route::match(['get', 'post'], '/search', [SearchController::class, 'handle']);

// Any HTTP method
Route::any('/webhook', [WebhookController::class, 'handle']);
```

### Route Parameters

```php
// Required parameter
Route::get('/users/{id}', function (string $id) {
    return response()->json(['id' => $id]);
});

// Multiple parameters
Route::get('/doctors/{doctor}/appointments/{appointment}', function (string $doctor, string $appointment) {
    return response()->json([
        'doctor_id' => $doctor,
        'appointment_id' => $appointment,
    ]);
});

// Optional parameter
Route::get('/users/{name?}', function (?string $name = 'Guest') {
    return response()->json(['name' => $name]);
});
```

### Parameter Constraints

```php
// Numeric constraint
Route::get('/users/{id}', [UserController::class, 'show'])
    ->whereNumber('id');

// Alpha constraint
Route::get('/categories/{slug}', [CategoryController::class, 'show'])
    ->whereAlpha('slug');

// Alphanumeric constraint
Route::get('/posts/{slug}', [PostController::class, 'show'])
    ->whereAlphaNumeric('slug');

// UUID constraint
Route::get('/orders/{uuid}', [OrderController::class, 'show'])
    ->whereUuid('uuid');

// ULID constraint
Route::get('/events/{ulid}', [EventController::class, 'show'])
    ->whereUlid('ulid');

// Custom regex
Route::get('/users/{id}', [UserController::class, 'show'])
    ->where('id', '[0-9]+');

// Multiple constraints
Route::get('/posts/{id}/{slug}', [PostController::class, 'show'])
    ->whereNumber('id')
    ->whereAlpha('slug');
```

### Global Constraints

```php
// app/Providers/RouteServiceProvider.php
use Illuminate\Support\Facades\Route;

public function boot(): void
{
    Route::pattern('id', '[0-9]+');
    Route::pattern('uuid', '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}');
}
```

## Named Routes

```php
// Define named route
Route::get('/users/{id}', [UserController::class, 'show'])
    ->name('users.show');

// Generate URL
$url = route('users.show', ['id' => 1]);
// Result: /api/users/1

// With query parameters
$url = route('users.show', ['id' => 1, 'tab' => 'profile']);
// Result: /api/users/1?tab=profile

// In responses
return response()->json([
    'user' => $user,
    'links' => [
        'self' => route('users.show', $user),
    ],
]);
```

## Route Groups

### Prefix Groups

```php
Route::prefix('v1')->group(function () {
    Route::get('/users', [UserController::class, 'index']);
    Route::get('/users/{user}', [UserController::class, 'show']);
    Route::get('/posts', [PostController::class, 'index']);
});
// Creates: /api/v1/users, /api/v1/users/{user}, /api/v1/posts
```

### Middleware Groups

```php
Route::middleware(['auth:sanctum'])->group(function () {
    Route::get('/profile', [ProfileController::class, 'show']);
    Route::put('/profile', [ProfileController::class, 'update']);
    Route::apiResource('posts', PostController::class);
});
```

### Controller Groups

```php
Route::controller(DoctorController::class)->group(function () {
    Route::get('/doctors', 'index');
    Route::get('/doctors/{doctor}', 'show');
    Route::post('/doctors', 'store');
    Route::put('/doctors/{doctor}', 'update');
    Route::delete('/doctors/{doctor}', 'destroy');
});
```

### Combined Groups

```php
Route::prefix('v1')
    ->middleware(['auth:sanctum', 'throttle:api'])
    ->name('api.v1.')
    ->group(function () {
        Route::apiResource('doctors', DoctorController::class);
        Route::apiResource('patients', PatientController::class);
        Route::apiResource('appointments', AppointmentController::class);
    });
```

### Nested Groups

```php
Route::prefix('admin')->name('admin.')->middleware(['auth:sanctum', 'admin'])->group(function () {
    Route::get('/dashboard', [AdminDashboardController::class, 'index'])->name('dashboard');

    Route::prefix('users')->name('users.')->group(function () {
        Route::get('/', [AdminUserController::class, 'index'])->name('index');
        Route::post('/', [AdminUserController::class, 'store'])->name('store');
        Route::get('/{user}', [AdminUserController::class, 'show'])->name('show');
        Route::delete('/{user}', [AdminUserController::class, 'destroy'])->name('destroy');
    });
});
```

## Route Model Binding

### Implicit Binding

```php
// routes/api.php
Route::get('/doctors/{doctor}', [DoctorController::class, 'show']);

// Controller receives the model automatically
public function show(Doctor $doctor): JsonResponse
{
    return response()->json($doctor);
}
```

### Custom Key Binding

```php
// By slug instead of ID
Route::get('/doctors/{doctor:slug}', [DoctorController::class, 'show']);

// Or define in model
class Doctor extends Model
{
    public function getRouteKeyName(): string
    {
        return 'slug';
    }
}
```

### Scoped Binding

```php
// Appointment must belong to the doctor
Route::get('/doctors/{doctor}/appointments/{appointment}', function (Doctor $doctor, Appointment $appointment) {
    return $appointment;
})->scopeBindings();

// Or for entire group
Route::scopeBindings()->group(function () {
    Route::get('/doctors/{doctor}/appointments/{appointment}', [AppointmentController::class, 'show']);
});
```

### Customizing Resolution

```php
// app/Providers/RouteServiceProvider.php
public function boot(): void
{
    Route::bind('doctor', function (string $value) {
        return Doctor::query()
            ->where('id', $value)
            ->orWhere('slug', $value)
            ->where('is_active', true)
            ->firstOrFail();
    });
}
```

### Explicit Binding

```php
// app/Providers/RouteServiceProvider.php
use App\Models\Doctor;

public function boot(): void
{
    Route::model('doctor', Doctor::class);
}
```

### Missing Model Behavior

```php
Route::get('/doctors/{doctor}', [DoctorController::class, 'show'])
    ->missing(function () {
        return response()->json([
            'message' => 'Doctor not found',
        ], 404);
    });
```

### Soft Deleted Models

```php
// Include soft deleted models
Route::get('/doctors/{doctor}', [DoctorController::class, 'show'])
    ->withTrashed();
```

## Resource Routes

### API Resource

```php
Route::apiResource('doctors', DoctorController::class);

// Generates:
// GET    /doctors           -> index
// POST   /doctors           -> store
// GET    /doctors/{doctor}  -> show
// PUT    /doctors/{doctor}  -> update
// DELETE /doctors/{doctor}  -> destroy
```

### Multiple Resources

```php
Route::apiResources([
    'doctors' => DoctorController::class,
    'patients' => PatientController::class,
    'appointments' => AppointmentController::class,
]);
```

### Partial Resources

```php
// Only specific actions
Route::apiResource('doctors', DoctorController::class)
    ->only(['index', 'show']);

// Except specific actions
Route::apiResource('doctors', DoctorController::class)
    ->except(['destroy']);
```

### Nested Resources

```php
Route::apiResource('doctors.appointments', DoctorAppointmentController::class);

// Generates:
// GET    /doctors/{doctor}/appointments                    -> index
// POST   /doctors/{doctor}/appointments                    -> store
// GET    /doctors/{doctor}/appointments/{appointment}      -> show
// PUT    /doctors/{doctor}/appointments/{appointment}      -> update
// DELETE /doctors/{doctor}/appointments/{appointment}      -> destroy
```

### Shallow Nesting

```php
Route::apiResource('doctors.appointments', DoctorAppointmentController::class)
    ->shallow();

// Generates:
// GET    /doctors/{doctor}/appointments       -> index
// POST   /doctors/{doctor}/appointments       -> store
// GET    /appointments/{appointment}          -> show
// PUT    /appointments/{appointment}          -> update
// DELETE /appointments/{appointment}          -> destroy
```

### Resource Names

```php
Route::apiResource('doctors', DoctorController::class)
    ->names([
        'index' => 'doctors.list',
        'store' => 'doctors.create',
    ]);

// Or with prefix
Route::apiResource('doctors', DoctorController::class)
    ->names('api.doctors');
```

### Resource Parameters

```php
Route::apiResource('doctors', DoctorController::class)
    ->parameters([
        'doctors' => 'doc',
    ]);
// Creates: /doctors/{doc}
```

### Singleton Resources

```php
// Single resource per parent
Route::singleton('doctors.profile', DoctorProfileController::class);

// Generates:
// GET    /doctors/{doctor}/profile  -> show
// PUT    /doctors/{doctor}/profile  -> update
```

## Supplementing Resource Routes

```php
Route::apiResource('doctors', DoctorController::class);

// Add custom routes
Route::get('/doctors/{doctor}/schedule', [DoctorController::class, 'schedule'])
    ->name('doctors.schedule');

Route::post('/doctors/{doctor}/activate', [DoctorController::class, 'activate'])
    ->name('doctors.activate');

Route::post('/doctors/{doctor}/deactivate', [DoctorController::class, 'deactivate'])
    ->name('doctors.deactivate');
```

## API Versioning

### URL Versioning

```php
// routes/api.php
Route::prefix('v1')->name('v1.')->group(function () {
    Route::apiResource('doctors', App\Http\Controllers\Api\V1\DoctorController::class);
});

Route::prefix('v2')->name('v2.')->group(function () {
    Route::apiResource('doctors', App\Http\Controllers\Api\V2\DoctorController::class);
});
```

### Separate Route Files

```php
// app/Providers/RouteServiceProvider.php
public function boot(): void
{
    Route::prefix('api/v1')
        ->middleware('api')
        ->group(base_path('routes/api_v1.php'));

    Route::prefix('api/v2')
        ->middleware('api')
        ->group(base_path('routes/api_v2.php'));
}
```

## Fallback Routes

```php
// Must be registered last
Route::fallback(function () {
    return response()->json([
        'message' => 'Endpoint not found',
    ], 404);
});
```

## Rate Limiting

### Define Rate Limiters

```php
// app/Providers/RouteServiceProvider.php
use Illuminate\Cache\RateLimiting\Limit;
use Illuminate\Support\Facades\RateLimiter;

protected function configureRateLimiting(): void
{
    // Default API limiter
    RateLimiter::for('api', function (Request $request) {
        return Limit::perMinute(60)->by($request->user()?->id ?: $request->ip());
    });

    // Custom limiter
    RateLimiter::for('uploads', function (Request $request) {
        return Limit::perMinute(10)->by($request->user()?->id ?: $request->ip());
    });

    // Tiered limiter
    RateLimiter::for('premium', function (Request $request) {
        return $request->user()?->isPremium()
            ? Limit::perMinute(1000)
            : Limit::perMinute(100);
    });

    // Multiple limits
    RateLimiter::for('global', function (Request $request) {
        return [
            Limit::perMinute(60)->by($request->ip()),
            Limit::perDay(1000)->by($request->user()?->id ?: $request->ip()),
        ];
    });
}
```

### Apply Rate Limiting

```php
Route::middleware(['throttle:api'])->group(function () {
    Route::apiResource('doctors', DoctorController::class);
});

// Custom limiter
Route::middleware(['throttle:uploads'])->group(function () {
    Route::post('/upload', [UploadController::class, 'store']);
});
```

## CORS Configuration

```php
// config/cors.php
return [
    'paths' => ['api/*'],
    'allowed_methods' => ['*'],
    'allowed_origins' => ['https://app.example.com'],
    'allowed_origins_patterns' => [],
    'allowed_headers' => ['*'],
    'exposed_headers' => [],
    'max_age' => 0,
    'supports_credentials' => true,
];
```

## Route Caching

```bash
# Cache routes for production
php artisan route:cache

# Clear route cache
php artisan route:clear

# List all routes
php artisan route:list

# List routes with middleware
php artisan route:list --columns=name,uri,middleware
```

## Testing Routes

```php
<?php

namespace Tests\Feature;

use Tests\TestCase;

class RoutingTest extends TestCase
{
    public function test_health_endpoint_returns_ok(): void
    {
        $response = $this->getJson('/api/health');

        $response->assertOk()
            ->assertJson(['status' => 'ok']);
    }

    public function test_not_found_returns_json(): void
    {
        $response = $this->getJson('/api/nonexistent');

        $response->assertNotFound()
            ->assertJson(['message' => 'Endpoint not found']);
    }

    public function test_rate_limiting_works(): void
    {
        for ($i = 0; $i < 61; $i++) {
            $response = $this->getJson('/api/doctors');
        }

        $response->assertStatus(429);
    }
}
```

## Complete API Routes Example

```php
<?php
// routes/api.php

use App\Http\Controllers\Api\AuthController;
use App\Http\Controllers\Api\DoctorController;
use App\Http\Controllers\Api\PatientController;
use App\Http\Controllers\Api\AppointmentController;
use App\Http\Controllers\HealthCheckController;
use Illuminate\Support\Facades\Route;

// Public routes
Route::get('/health', HealthCheckController::class);

Route::post('/auth/login', [AuthController::class, 'login']);
Route::post('/auth/register', [AuthController::class, 'register']);
Route::post('/auth/forgot-password', [AuthController::class, 'forgotPassword']);

// Protected routes
Route::middleware(['auth:sanctum'])->group(function () {
    // Auth
    Route::post('/auth/logout', [AuthController::class, 'logout']);
    Route::get('/auth/me', [AuthController::class, 'me']);
    Route::post('/auth/refresh', [AuthController::class, 'refresh']);

    // Doctors
    Route::apiResource('doctors', DoctorController::class);
    Route::get('/doctors/{doctor}/schedule', [DoctorController::class, 'schedule']);
    Route::post('/doctors/{doctor}/activate', [DoctorController::class, 'activate']);
    Route::post('/doctors/{doctor}/deactivate', [DoctorController::class, 'deactivate']);

    // Nested appointments
    Route::apiResource('doctors.appointments', AppointmentController::class)
        ->scoped()
        ->shallow();

    // Patients
    Route::apiResource('patients', PatientController::class);
    Route::get('/patients/{patient}/history', [PatientController::class, 'history']);
});

// Admin routes
Route::middleware(['auth:sanctum', 'admin'])->prefix('admin')->name('admin.')->group(function () {
    Route::get('/stats', [AdminController::class, 'stats']);
    Route::get('/users', [AdminController::class, 'users']);
});

// Fallback
Route::fallback(function () {
    return response()->json(['message' => 'Endpoint not found'], 404);
});
```

## Best Practices

1. **Use API resources** - `apiResource` for CRUD operations
2. **Version your API** - Use URL prefixes like `/v1/`
3. **Group related routes** - Share middleware and prefixes
4. **Use route model binding** - Cleaner controllers
5. **Name all routes** - Easier URL generation
6. **Apply rate limiting** - Protect against abuse
7. **Cache routes in production** - Better performance
8. **Test your routes** - Verify behavior
9. **Document endpoints** - Use OpenAPI/Swagger
10. **Keep routes organized** - Separate by feature

## References

- [Laravel Routing](https://laravel.com/docs/11.x/routing)
- [Route Model Binding](https://laravel.com/docs/11.x/routing#route-model-binding)
- [Rate Limiting](https://laravel.com/docs/11.x/routing#rate-limiting)

---

[Previous: Project Structure](./05-project-structure.md) | [Back to Index](./README.md) | [Next: Controllers](./07-controllers.md)
